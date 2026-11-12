package ro.andrei.arichu.feature.reminders.ui

import ro.andrei.arichu.feature.delivery.model.DeliveryCapabilities
import ro.andrei.arichu.feature.locationpicker.geometry.buildRadiusBoundary
import ro.andrei.arichu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.arichu.feature.places.GeoBounds
import ro.andrei.arichu.feature.reminders.model.Reminder
import kotlin.math.cos

private fun active(reminders: List<Reminder>) = reminders.filter { it.delivery.active }
private fun escape(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

/** Chain reminders are monitored per store area, plain ones at their own pin. */
private fun Reminder.circles(): List<SelectedCoordinates> =
    if (chainName == null || chainAreas.isEmpty()) listOf(SelectedCoordinates(location.latitude, location.longitude))
    else chainAreas.map { SelectedCoordinates(it.latitude, it.longitude) }

/** One Point per active reminder: properties id, tone (MapTone name). */
fun reminderPinsGeoJson(reminders: List<Reminder>, capabilities: DeliveryCapabilities): String =
    active(reminders).joinToString(",", """{"type":"FeatureCollection","features":[""", "]}") {
        """{"type":"Feature","geometry":{"type":"Point","coordinates":[${it.location.longitude},${it.location.latitude}]},""" +
            """"properties":{"id":"${escape(it.id)}","tone":"${it.mapTone(capabilities).name}"}}"""
    }

/** One closed radius Polygon per monitored area of each active reminder: properties id, tone. */
fun reminderCirclesGeoJson(reminders: List<Reminder>, capabilities: DeliveryCapabilities): String =
    active(reminders).flatMap { reminder ->
        reminder.circles().map { centre ->
            val ring = buildRadiusBoundary(centre, reminder.location.radiusMeters).joinToString(",", "[", "]") { "[${it.longitude},${it.latitude}]" }
            """{"type":"Feature","geometry":{"type":"Polygon","coordinates":[$ring]},"properties":{"id":"${escape(reminder.id)}","tone":"${reminder.mapTone(capabilities).name}"}}"""
        }
    }.joinToString(",", """{"type":"FeatureCollection","features":[""", "]}")

/** Box around every active circle, or null when there is nothing to show. */
fun reminderMapBounds(reminders: List<Reminder>): GeoBounds? {
    val boxes = active(reminders).flatMap { r ->
        val dLat = r.location.radiusMeters / 111_320.0
        r.circles().map { c ->
            val dLon = r.location.radiusMeters / (111_320.0 * cos(Math.toRadians(c.latitude)).coerceAtLeast(0.01))
            GeoBounds(c.latitude - dLat, c.longitude - dLon, c.latitude + dLat, c.longitude + dLon)
        }
    }
    if (boxes.isEmpty()) return null
    return GeoBounds(boxes.minOf { it.south }, boxes.minOf { it.west }, boxes.maxOf { it.north }, boxes.maxOf { it.east })
}
