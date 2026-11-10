package ro.andrei.ariciu.feature.delivery.model

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.*

class GeofenceBudgetTest {
    private fun r(id: String, reg: RegistrationState, active: Boolean = true, areas: Int = 0) = Reminder(id, id,
        SelectedLocation(45.0, 25.0, 200), ReminderTransition.ENTER, ReminderRepeat.ONCE,
        delivery = DeliveryState(phase = if (active) DeliveryPhase.WAITING else DeliveryPhase.COMPLETED), registration = reg,
        chainName = if (areas > 0) "Lidl" else null, chainAreas = (1..areas).map { ChainArea("n$it", 45.0, 25.0) })
    @Test fun emptyIsZero() = assertEquals(0, geofenceAreasUsed(emptyList()))
    @Test fun registeredSinglesAndChainAreasAreCounted() {
        assertEquals(1 + 12, geofenceAreasUsed(listOf(r("a", RegistrationState.REGISTERED), r("b", RegistrationState.REGISTERED, areas = 12))))
    }
    @Test fun inactivePendingBlockedAndFailedAreNotCounted() {
        val list = listOf(r("a", RegistrationState.REGISTERED, active = false), r("b", RegistrationState.PENDING),
            r("c", RegistrationState.BLOCKED), r("d", RegistrationState.FAILED), r("e", RegistrationState.INACTIVE))
        assertEquals(0, geofenceAreasUsed(list))
    }
    @Test fun partialOrLimitReachedMeansFull() {
        assertEquals(100, geofenceAreasUsed(listOf(r("a", RegistrationState.REGISTERED), r("b", RegistrationState.PARTIAL, areas = 40))))
        assertEquals(100, geofenceAreasUsed(listOf(r("a", RegistrationState.LIMIT_REACHED))))
    }
    @Test fun neverExceedsTheLimit() {
        assertEquals(100, geofenceAreasUsed((1..3).map { r("c$it", RegistrationState.REGISTERED, areas = 40) }))
    }
    @Test fun textIsPlainAndClamped() {
        assertEquals("7/100 areas used", geofenceBudgetText(7))
        assertEquals("100/100 areas used", geofenceBudgetText(500))
        assertEquals("0/100 areas used", geofenceBudgetText(-3))
    }
}
