package ro.andrei.ariciu.feature.places

import java.text.Normalizer
import java.util.Locale
import kotlin.math.*

data class CatalogPlace(val id: String, val name: String, val kind: String, val address: String,
    val latitude: Double, val longitude: Double, val chain: String = "")

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat = Math.toRadians(lat2 - lat1); val lon = Math.toRadians(lon2 - lon1)
    val a = sin(lat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(lon / 2).pow(2)
    return 6_371_000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}
private fun normalized(value: String) = Normalizer.normalize(value, Normalizer.Form.NFD)
    .replace(Regex("\\p{M}+"), "").lowercase(Locale.ROOT)
private fun tokens(value: String) = normalized(value).split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
fun searchPlaces(places: List<CatalogPlace>, query: String, latitude: Double, longitude: Double): List<CatalogPlace> {
    val words = tokens(query)
    if (words.isEmpty()) return emptyList()
    fun rank(p: CatalogPlace): Int {
        val name = tokens(p.name)
        return when {
            name == words -> 0
            words.all { it in name } -> 1
            words.all { q -> name.any { it.startsWith(q) } } -> 2
            else -> 3
        }
    }
    return places.filter { p -> val all = tokens("${p.name} ${p.address}")
        words.all { q -> all.any { it.startsWith(q) } } }
        .sortedWith(compareBy<CatalogPlace> { rank(it) }
            .thenBy { distanceMeters(latitude, longitude, it.latitude, it.longitude) }.thenBy { it.id }).take(30)
}

/** New/explicitly refreshed snapshots cover nearby shops, never fill slots across Romania. */
const val CHAIN_COVERAGE_METERS = 25_000.0
fun chainPlaces(places: List<CatalogPlace>, chain: String, latitude: Double, longitude: Double, limit: Int = 40) =
    places.filter { it.chain == chain && distanceMeters(latitude, longitude, it.latitude, it.longitude) <= CHAIN_COVERAGE_METERS }
        .sortedWith(compareBy<CatalogPlace> { distanceMeters(latitude, longitude, it.latitude, it.longitude) }.thenBy { it.id })
        .take(limit.coerceIn(0, 40))
