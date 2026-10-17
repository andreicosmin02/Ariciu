package ro.andrei.ariciu.feature.delivery.model

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.reminders.model.*

class UrgentLifecycleTest {
    private fun triggered() = DeliveryState().onTransition(ReminderTransition.ENTER,ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT,10,1,holdUnresolved = true)
    @Test fun firstSwipeWaitsFiveMinutesThenStartsAnotherAttemptOfTheSameOccurrence() {
        val first = triggered()
        val pending = first.onUrgentDismiss(1,0,1_000)
        assertEquals(301_000L,pending.urgentRetryAtMs)
        assertEquals(DeliveryPhase.TRIGGERED,pending.phase)
        assertFalse(pending.notificationVisible); assertFalse(pending.notificationPending)
        assertEquals(pending,pending.onUrgentRetryDue(300_999))
        val second = pending.onUrgentRetryDue(301_000)
        assertEquals(1,second.occurrence); assertEquals(1,second.notificationAttempt)
        assertTrue(second.notificationVisible); assertTrue(second.notificationPending); assertFalse(second.quiet)
        assertEquals(second,second.onUrgentRetryDue(900_000))
    }
    @Test fun successiveSwipesScheduleOnceAndOldAttemptCannotDismissTheNewAlert() {
        var state = triggered()
        for (attempt in 0..3) {
            assertEquals(attempt,state.notificationAttempt)
            val pending = state.onUrgentDismiss(1,attempt,1_000L + attempt * 1_000,delayMs = 100)
            assertEquals(pending,pending.onUrgentDismiss(1,attempt,9_000))
            state = pending.onUrgentRetryDue(pending.urgentRetryAtMs)
            assertEquals(state,state.onUrgentDismiss(1,attempt,10_000))
        }
        assertEquals(1,state.occurrence)
        assertEquals(state,state.onUrgentDismiss(99,state.notificationAttempt,1_000))
    }
    @Test fun doneAndNextTimeResolveHiddenRetriesAndWinEitherOrderAgainstDueWake() {
        val pending = triggered().onUrgentDismiss(1,0,1_000)
        fun resolve(s: DeliveryState,a: OccurrenceAction) = s.onAction(a,1,ReminderTransition.ENTER,allowUnresolvedUrgent = true)
        for (action in listOf(OccurrenceAction.DONE,OccurrenceAction.NEXT_TIME)) {
            val before = resolve(pending,action)
            assertEquals(0L,before.urgentRetryAtMs)
            assertEquals(before,before.onUrgentRetryDue(900_000))
            val after = resolve(pending.onUrgentRetryDue(900_000),action)
            assertEquals(before.phase,after.phase); assertFalse(after.notificationVisible)
            assertEquals(after,after.onUrgentRetryDue(1_000_000))
        }
        assertEquals(pending,pending.onAction(OccurrenceAction.DONE,99,ReminderTransition.ENTER,allowUnresolvedUrgent = true))
    }
    @Test fun unresolvedArrivalAndDepartureIgnoreRepeatRearmingAcrossBootsAndCrossings() {
        for (wanted in ReminderTransition.entries) {
            val opposite = if (wanted == ReminderTransition.ENTER) ReminderTransition.EXIT else ReminderTransition.ENTER
            val first = DeliveryState().onTransition(wanted,wanted,ReminderRepeat.EVERY_VISIT,10,1,true).onUrgentDismiss(1,0,1_000)
            val outside = first.onTransition(opposite,wanted,ReminderRepeat.EVERY_VISIT,1,2,true)
            val returned = outside.onTransition(wanted,wanted,ReminderRepeat.EVERY_VISIT,2,2,true)
            assertEquals(DeliveryPhase.TRIGGERED,returned.phase); assertEquals(1,returned.occurrence)
            assertEquals(first.urgentRetryAtMs,returned.urgentRetryAtMs)
            val next = returned.onAction(OccurrenceAction.NEXT_TIME,1,wanted,allowUnresolvedUrgent = true)
            assertEquals(DeliveryPhase.WAITING_NEXT_VISIT,next.phase)
            assertEquals(2,next.onTransition(opposite,wanted,ReminderRepeat.EVERY_VISIT,3,2,true)
                .onTransition(wanted,wanted,ReminderRepeat.EVERY_VISIT,4,2,true).occurrence)
        }
    }
    @Test fun nextTimeAfterLeavingCanWaitForTheVeryNextQualifyingCrossing() {
        val outside = triggered().onUrgentDismiss(1,0,1_000)
            .onTransition(ReminderTransition.EXIT,ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT,20,1,true)
        val next = outside.onAction(OccurrenceAction.NEXT_TIME,1,ReminderTransition.ENTER,allowUnresolvedUrgent = true)
        assertEquals(DeliveryPhase.WAITING,next.phase)
        assertEquals(2,next.onTransition(ReminderTransition.ENTER,ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT,30,1,true).occurrence)
    }
    @Test fun onlyThreeLevelsAreAvailableWhileAlarmRemainsSerializable() {
        assertEquals(listOf(NotificationIntensity.SILENT,NotificationIntensity.NORMAL,NotificationIntensity.URGENT),NotificationIntensity.available)
        assertEquals(NotificationIntensity.ALARM,NotificationIntensity.valueOf("ALARM"))
        assertTrue(NotificationIntensity.URGENT.explanation.contains("5 minutes"))
    }
}
