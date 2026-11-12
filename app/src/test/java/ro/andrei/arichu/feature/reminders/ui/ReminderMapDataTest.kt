package ro.andrei.arichu.feature.reminders.ui

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.arichu.feature.delivery.model.*
import ro.andrei.arichu.feature.locationpicker.model.SelectedLocation
import ro.andrei.arichu.feature.reminders.model.*

class ReminderMapDataTest {
    private val ready = DeliveryCapabilities(true, true, true, true, true)
    private fun r(id: String, lat: Double = 45.0, lon: Double = 25.0, radius: Int = 200, done: Boolean = false, areas: Int = 0) = Reminder(id, "T$id",
        SelectedLocation(lat, lon, radius), ReminderTransition.ENTER, ReminderRepeat.ONCE,
        delivery = DeliveryState(phase = if (done) DeliveryPhase.COMPLETED else DeliveryPhase.WAITING), registration = RegistrationState.REGISTERED,
        chainName = if (areas > 0) "Lidl" else null, chainAreas = (1..areas).map { ChainArea("n$it", lat + it * 0.001, lon, ) })

    @Test fun onlyActiveRemindersGetAPin() {
        val json = reminderPinsGeoJson(listOf(r("a"), r("b", done = true)), ready)
        assertTrue(json, json.contains(""""id":"a"""")); assertFalse(json, json.contains(""""id":"b""""))
        assertTrue(json, json.contains(""""tone":"READY""""))
    }
    @Test fun pinCoordinatesAreLongitudeFirst() {
        assertTrue(reminderPinsGeoJson(listOf(r("a", 45.5, 25.25)), ready).contains(""""coordinates":[25.25,45.5]"""))
    }
    @Test fun singleReminderHasOneClosedCircle() {
        val json = reminderCirclesGeoJson(listOf(r("a")), ready)
        assertEquals(1, Regex(""""type":"Polygon"""").findAll(json).count())
        assertTrue(json.contains(""""id":"a""""))
    }
    @Test fun chainRemindersDrawOneCirclePerStoreArea() {
        assertEquals(3, Regex(""""type":"Polygon"""").findAll(reminderCirclesGeoJson(listOf(r("c", areas = 3)), ready)).count())
    }
    @Test fun toneFollowsPermissions() {
        assertTrue(reminderPinsGeoJson(listOf(r("a")), ready.copy(notifications = false)).contains(""""tone":"ATTENTION""""))
    }
    @Test fun boundsCoverAllActiveCirclesAndEmptyIsNull() {
        assertNull(reminderMapBounds(emptyList()))
        assertNull(reminderMapBounds(listOf(r("a", done = true))))
        val b = reminderMapBounds(listOf(r("a", 45.0, 25.0, 500), r("b", 46.0, 26.0, 200)))!!
        assertTrue(b.south < 45.0 && b.north > 46.0 && b.west < 25.0 && b.east > 26.0)
    }
}
