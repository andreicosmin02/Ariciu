package ro.andrei.ariciu.feature.places

import kotlin.math.abs

enum class PlaceCategory(val label: String) {
    SHOPS("Shops"), MALLS("Malls"), TOWNS("Towns");
    companion object {
        fun of(kind: String) = when (kind) { "town" -> TOWNS; "mall" -> MALLS; else -> SHOPS }
    }
}

data class GeoBounds(val south: Double, val west: Double, val north: Double, val east: Double) {
    fun contains(lat: Double, lon: Double) = lat in south..north && lon in west..east
}

/** Markers are hidden when zoomed out so the 26k-row catalogue is never drawn as noise. */
const val PLACE_MARKER_MIN_ZOOM = 12.0
fun placeMarkersVisible(zoom: Double) = zoom >= PLACE_MARKER_MIN_ZOOM

const val PLACE_MARKER_LIMIT = 1_500

/** Catalogue rows inside [bounds] for the enabled categories, nearest to the box centre first, capped at [limit]. */
fun placesInViewport(places: List<CatalogPlace>, bounds: GeoBounds, categories: Set<PlaceCategory>,
    limit: Int = PLACE_MARKER_LIMIT): List<CatalogPlace> {
    if (categories.isEmpty() || bounds.south > bounds.north || bounds.west > bounds.east) return emptyList()
    val inside = places.filter { bounds.contains(it.latitude, it.longitude) && PlaceCategory.of(it.kind) in categories }
    if (inside.size <= limit) return inside.sortedBy { it.id }
    val lat = (bounds.south + bounds.north) / 2; val lon = (bounds.west + bounds.east) / 2
    return inside.sortedWith(compareBy<CatalogPlace> { abs(it.latitude - lat) + abs(it.longitude - lon) }.thenBy { it.id }).take(limit)
}
