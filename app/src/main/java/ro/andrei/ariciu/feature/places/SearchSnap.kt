package ro.andrei.ariciu.feature.places

import kotlin.math.cos
import kotlin.math.pow

data class GeoBounds(val south: Double, val west: Double, val north: Double, val east: Double) {
    fun contains(lat: Double, lon: Double) = lat in south..north && lon in west..east
}

/** Closest result to the reference point; ties by id. */
fun nearestResult(results: List<CatalogPlace>, latitude: Double, longitude: Double): CatalogPlace? =
    results.minWithOrNull(compareBy<CatalogPlace> { distanceMeters(latitude, longitude, it.latitude, it.longitude) }.thenBy { it.id })

/** Box covering every result and the reference point, or null without results. */
fun fitBounds(results: List<CatalogPlace>, latitude: Double, longitude: Double): GeoBounds? {
    if (results.isEmpty()) return null
    return GeoBounds(
        minOf(latitude, results.minOf { it.latitude }), minOf(longitude, results.minOf { it.longitude }),
        maxOf(latitude, results.maxOf { it.latitude }), maxOf(longitude, results.maxOf { it.longitude }))
}

/** "Search this area" shows once the centre moved about 100 dp, so tiny drags and jitter do not trigger it. */
fun mapMovedEnough(oldLat: Double, oldLon: Double, newLat: Double, newLon: Double, zoom: Double): Boolean {
    val metersPerDp = 156_543.0 * cos(Math.toRadians(newLat)) / 2.0.pow(zoom)
    return distanceMeters(oldLat, oldLon, newLat, newLon) > metersPerDp * 100
}

/** Label stored in Reminder.chainName for a multi-place reminder: the shared brand, else the typed query, else the chip labels. */
fun searchLabel(query: String, categories: Set<PlaceCategory>, results: List<CatalogPlace>): String {
    val brands = results.map { it.chain }.distinct()
    if (brands.size == 1 && brands[0].isNotBlank()) return brands[0]
    val q = query.trim()
    if (q.isNotEmpty()) return q.replaceFirstChar { it.uppercase() }
    val labels = categories.sortedBy { it.ordinal }.map { it.label }
    return labels.joinToString(" & ").ifBlank { "Places" }
}
