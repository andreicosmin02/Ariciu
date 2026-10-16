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
fun searchPlaces(places: List<CatalogPlace>, query: String, latitude: Double, longitude: Double): List<CatalogPlace> {
    val words = normalized(query).trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return emptyList()
    return places.filter { p -> words.all { it in normalized("${p.name} ${p.address}") } }
        .sortedWith(compareBy<CatalogPlace> { if (normalized(it.name) == normalized(query.trim())) 0 else 1 }
            .thenBy { distanceMeters(latitude, longitude, it.latitude, it.longitude) }.thenBy { it.id }).take(30)
}
fun chainPlaces(places: List<CatalogPlace>, chain: String, latitude: Double, longitude: Double, limit: Int = 40) =
    places.filter { it.chain == chain }.sortedWith(compareBy<CatalogPlace> {
        distanceMeters(latitude, longitude, it.latitude, it.longitude) }.thenBy { it.id }).take(limit)
