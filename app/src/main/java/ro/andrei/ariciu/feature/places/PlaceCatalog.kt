package ro.andrei.ariciu.feature.places

import kotlin.math.*

data class CatalogPlace(val id: String, val name: String, val kind: String, val address: String,
    val latitude: Double, val longitude: Double, val chain: String = "", val category: String = "")

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat = Math.toRadians(lat2 - lat1); val lon = Math.toRadians(lon2 - lon1)
    val a = sin(lat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(lon / 2).pow(2)
    return 6_371_000 * 2 * asin(sqrt(a.coerceIn(0.0, 1.0)))
}
