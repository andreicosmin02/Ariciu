package ro.andrei.arichu.feature.reminders.ui

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.arichu.feature.delivery.model.*
import ro.andrei.arichu.feature.locationpicker.model.SelectedLocation
import ro.andrei.arichu.feature.reminders.model.*

class ReminderMapToneTest {
    private val ready = DeliveryCapabilities(true, true, true, true, true)
    private val base = Reminder("id", "Bread", SelectedLocation(45.0, 25.0, 200), ReminderTransition.ENTER, ReminderRepeat.ONCE,
        registration = RegistrationState.REGISTERED)
    @Test fun readyIsReady() = assertEquals(MapTone.READY, base.mapTone(ready))
    @Test fun setupProblemsNeedAttention() {
        assertEquals(MapTone.ATTENTION, base.mapTone(ready.copy(notifications = false)))
        assertEquals(MapTone.ATTENTION, base.copy(registration = RegistrationState.FAILED).mapTone(ready))
        assertEquals(MapTone.ATTENTION, base.copy(registration = RegistrationState.LIMIT_REACHED).mapTone(ready))
    }
    @Test fun pendingIsWaitingAndCompletedIsMutedEvenWithoutPermissions() {
        assertEquals(MapTone.WAITING, base.copy(registration = RegistrationState.PENDING).mapTone(ready))
        assertEquals(MapTone.DONE, base.copy(delivery = DeliveryState(phase = DeliveryPhase.COMPLETED)).mapTone(ready.copy(notifications = false)))
    }
    @Test fun everyToneHasATextLabelSoColourIsNeverTheOnlySignal() {
        MapTone.entries.forEach { assertTrue(it.label.isNotBlank()) }
    }
}
