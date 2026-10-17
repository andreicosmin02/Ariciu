package ro.andrei.ariciu.feature.reminders.ui

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*

class ReminderPresentationTest {
    private val ready = DeliveryCapabilities(true, true, true, true, true)
    private val reminder = Reminder("id", "Bread", SelectedLocation(45.0, 25.0, 200), ReminderTransition.ENTER, ReminderRepeat.ONCE,
        registration = RegistrationState.REGISTERED)
    @Test fun missingPermissionsNeverAppearAsReady() {
        assertTrue(reminder.presentStatus(ready.copy(notifications = false)).needsAttention)
        assertEquals("Setup needed", reminder.presentStatus(ready.copy(backgroundLocation = false)).label)
    }
    @Test fun completedRemindersStayClearlyCompletedEvenWhenPermissionsAreMissing() {
        val done = reminder.copy(delivery = DeliveryState(phase = DeliveryPhase.COMPLETED))
        assertEquals("Completed", done.presentStatus(ready.copy(notifications = false)).label)
        assertFalse(done.presentStatus(ready.copy(notifications = false)).needsAttention)
    }
    @Test fun registrationFailuresAndCapacityLimitsNeedAction() {
        assertTrue(reminder.copy(registration = RegistrationState.FAILED).presentStatus(ready).needsAttention)
        assertEquals("On hold", reminder.copy(registration = RegistrationState.LIMIT_REACHED).presentStatus(ready).label)
    }
    @Test fun nextVisitExplainsTheCorrectCrossingForDepartures() {
        val departure = reminder.copy(transition = ReminderTransition.EXIT, delivery = DeliveryState(phase = DeliveryPhase.WAITING_NEXT_VISIT))
        assertTrue(departure.presentStatus(ready).description.contains("return and leave"))
    }

    @Test fun pendingUrgentRetryIsVisibleEvenWithoutBackgroundPermissionAndExplainsDisabledChannels() {
        val pending = reminder.copy(intensity = NotificationIntensity.URGENT,delivery = DeliveryState(phase = DeliveryPhase.TRIGGERED,occurrence = 1,urgentRetryAtMs = 301_000))
        assertEquals("Re-alert scheduled",pending.presentStatus(ready.copy(backgroundLocation = false)).label)
        assertTrue(pending.presentStatus(ready).description.contains("Android may delay"))
        assertEquals("Re-alert waiting",pending.presentStatus(ready.copy(blockedIntensities = setOf(NotificationIntensity.URGENT))).label)
        assertTrue(pending.presentStatus(ready.copy(notifications = false)).needsAttention)
        val due = pending.copy(delivery = pending.delivery.onUrgentRetryDue(900_000))
        assertEquals("Re-alert waiting",due.presentStatus(ready).label)
        assertEquals("Re-alert waiting",due.presentStatus(ready.copy(notifications = false)).label)
    }

}
