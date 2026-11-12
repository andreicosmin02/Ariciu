package ro.andrei.arichu.feature.places

import kotlin.math.*

data class CatalogPlace(val id: String, val name: String, val kind: String, val address: String,
    val latitude: Double, val longitude: Double, val chain: String = "", val category: String = "") {
    /** Search text folded once (accents, case) so each query is a substring check. Filled when a grid cell loads. */
    internal val searchText: String by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val c = PlaceCategory.of(this)
        foldForSearch("$name $chain $address ${c.label} ${c.aliases.joinToString(" ")}")
    }
    internal val nameWords: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) { searchWords(name) }
    internal val brandWords: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) { searchWords(chain) }
    internal val addressWords: List<String> by lazy(LazyThreadSafetyMode.PUBLICATION) { searchWords(address) }
}

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat = Math.toRadians(lat2 - lat1); val lon = Math.toRadians(lon2 - lon1)
    val a = sin(lat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(lon / 2).pow(2)
    return 6_371_000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}
