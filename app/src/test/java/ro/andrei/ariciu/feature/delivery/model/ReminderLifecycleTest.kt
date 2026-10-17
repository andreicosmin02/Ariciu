package ro.andrei.ariciu.feature.delivery.model

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.reminders.model.*

class ReminderLifecycleTest {
    @Test fun explicitCompletionWorksBeforeArrivalAndWhileSnoozed() {
        listOf(DeliveryState(), DeliveryState(phase = DeliveryPhase.TRIGGERED, occurrence = 2,
            notificationVisible = true, notificationPending = true, snoozeUntilMs = 600_000)).forEach { state ->
            val done = state.completed()
            assertFalse(done.active)
            assertFalse(done.notificationVisible)
            assertFalse(done.notificationPending)
            assertEquals(0L, done.snoozeUntilMs)
            assertEquals(state.occurrence, done.occurrence)
            assertEquals(done, done.onSnoozeDue(900_000))
        }
    }

    @Test fun alarmStopSnoozeAndDueArePersistentAndRejectStaleTokens() {
        val triggered = DeliveryState().onTransition(ReminderTransition.ENTER, ReminderTransition.ENTER, ReminderRepeat.ONCE, 10, 1)
        assertEquals(triggered, triggered.onAction(OccurrenceAction.SNOOZE, 99, ReminderTransition.ENTER, 1000))
        val stopped = triggered.onAction(OccurrenceAction.STOP, 1, ReminderTransition.ENTER, 1000)
        assertTrue(stopped.notificationVisible)
        assertTrue(stopped.quiet)
        val snoozed = triggered.onAction(OccurrenceAction.SNOOZE, 1, ReminderTransition.ENTER, 1000)
        assertFalse(snoozed.notificationVisible)
        assertEquals(601000L, snoozed.snoozeUntilMs)
        assertEquals(snoozed, snoozed.onSnoozeDue(600999))
        val due = snoozed.onSnoozeDue(601000)
        assertTrue(due.notificationVisible)
        assertTrue(due.notificationPending)
        assertEquals(2, due.occurrence)
        assertEquals(due, due.onAction(OccurrenceAction.DONE, 1, ReminderTransition.ENTER))
        assertEquals(DeliveryPhase.COMPLETED, due.onAction(OccurrenceAction.DONE, 2, ReminderTransition.ENTER).phase)
    }

    private fun event(s: DeliveryState, t: ReminderTransition, wanted: ReminderTransition = ReminderTransition.ENTER,
        repeat: ReminderRepeat = ReminderRepeat.ONCE, time: Long = s.lastEventElapsedMs + 1) =
        s.onTransition(t, wanted, repeat, time, 1)

    @Test fun arrivalTriggersOnlyOnceDespiteDuplicatesAndLaterVisits() {
        val triggered = event(DeliveryState(), ReminderTransition.ENTER)
        assertEquals(DeliveryPhase.TRIGGERED, triggered.phase)
        assertEquals(1, triggered.occurrence)
        assertEquals(triggered, event(triggered, ReminderTransition.ENTER))
        val outside = event(triggered, ReminderTransition.EXIT)
        assertEquals(1, event(outside, ReminderTransition.ENTER).occurrence)
    }

    @Test fun repeatingArrivalRequiresLeavingBeforeAnotherArrival() {
        val first = event(DeliveryState(), ReminderTransition.ENTER, repeat = ReminderRepeat.EVERY_VISIT)
        assertEquals(1, event(first, ReminderTransition.ENTER, repeat = ReminderRepeat.EVERY_VISIT).occurrence)
        val outside = event(first, ReminderTransition.EXIT, repeat = ReminderRepeat.EVERY_VISIT)
        assertEquals(2, event(outside, ReminderTransition.ENTER, repeat = ReminderRepeat.EVERY_VISIT).occurrence)
    }

    @Test fun departureRequiresReturnBeforeNextDeparture() {
        val inside = event(DeliveryState(), ReminderTransition.ENTER, ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT)
        assertEquals(0, inside.occurrence)
        val first = event(inside, ReminderTransition.EXIT, ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT)
        assertEquals(1, first.occurrence)
        assertEquals(first, event(first, ReminderTransition.EXIT, ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT))
        val returned = event(first, ReminderTransition.ENTER, ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT)
        assertEquals(2, event(returned, ReminderTransition.EXIT, ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT).occurrence)
    }

    @Test fun nextTimeAndSwipeRequireNewVisitEvenForOnce() {
        val triggered = event(DeliveryState(), ReminderTransition.ENTER)
        val next = triggered.onAction(OccurrenceAction.NEXT_TIME, triggered.occurrence, ReminderTransition.ENTER)
        assertFalse(next.notificationVisible)
        assertEquals(DeliveryPhase.WAITING_NEXT_VISIT, next.phase)
        assertEquals(1, event(next, ReminderTransition.ENTER).occurrence)
        val outside = event(next, ReminderTransition.EXIT)
        assertEquals(2, event(outside, ReminderTransition.ENTER).occurrence)
    }

    @Test fun nextTimeAfterAlreadyLeavingDoesNotSkipTheNextVisit() {
        val outside = event(event(DeliveryState(), ReminderTransition.ENTER), ReminderTransition.EXIT)
        val next = outside.onAction(OccurrenceAction.NEXT_TIME, 1, ReminderTransition.ENTER)
        assertEquals(2, event(next, ReminderTransition.ENTER).occurrence)
    }

    @Test fun doneCompletesBothRepeatChoicesAndStaleActionsAreIgnored() {
        val first = event(DeliveryState(), ReminderTransition.ENTER, repeat = ReminderRepeat.EVERY_VISIT)
        val second = event(event(first, ReminderTransition.EXIT, repeat = ReminderRepeat.EVERY_VISIT),
            ReminderTransition.ENTER, repeat = ReminderRepeat.EVERY_VISIT)
        assertEquals(second, second.onAction(OccurrenceAction.DONE, 1, ReminderTransition.ENTER))
        val done = second.onAction(OccurrenceAction.DONE, 2, ReminderTransition.ENTER)
        assertEquals(DeliveryPhase.COMPLETED, done.phase)
        assertEquals(done, event(done, ReminderTransition.EXIT))
        assertFalse(done.notificationVisible)
    }

    @Test fun oldAndOutOfOrderBroadcastsCannotInventANewVisit() {
        val inside = event(DeliveryState(), ReminderTransition.ENTER, repeat = ReminderRepeat.EVERY_VISIT, time = 10)
        val outside = event(inside, ReminderTransition.EXIT, repeat = ReminderRepeat.EVERY_VISIT, time = 20)
        assertEquals(outside, event(outside, ReminderTransition.ENTER, repeat = ReminderRepeat.EVERY_VISIT, time = 10))
    }

    @Test fun newBootAllowsEventsWithSmallerElapsedTimeWithoutResettingOccurrence() {
        val outside = event(event(DeliveryState(), ReminderTransition.ENTER, repeat = ReminderRepeat.EVERY_VISIT),
            ReminderTransition.EXIT, repeat = ReminderRepeat.EVERY_VISIT)
        val next = outside.onTransition(ReminderTransition.ENTER, ReminderTransition.ENTER, ReminderRepeat.EVERY_VISIT, 1, 2)
        assertEquals(2, next.occurrence)
    }
}
