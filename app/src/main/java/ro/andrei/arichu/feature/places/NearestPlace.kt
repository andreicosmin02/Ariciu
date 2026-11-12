package ro.andrei.arichu.feature.places

import java.util.Locale
import kotlin.math.roundToInt

data class NearestPlace(val place: CatalogPlace, val meters: Double)

const val NEAREST_PLACE_MAX_METERS = 5_000.0
private const val ON_SITE_METERS = 30.0

/** Closest bundled catalogue row within [maxMeters]. Pure and offline; ties break on id. */
fun nearestPlace(places: List<CatalogPlace>, latitude: Double, longitude: Double,
    maxMeters: Double = NEAREST_PLACE_MAX_METERS): NearestPlace? =
    places.map { NearestPlace(it, distanceMeters(latitude, longitude, it.latitude, it.longitude)) }
        .filter { it.meters <= maxMeters }
        .minWithOrNull(compareBy<NearestPlace> { it.meters }.thenBy { it.place.id })

/** "120 m" (rounded to 10 m) or "1.4 km". */
fun distanceText(meters: Double): String =
    if (meters < 1_000) "${(meters / 10).roundToInt() * 10} m" else String.format(Locale.ROOT, "%.1f km", meters / 1_000)

fun nearestPlaceLabel(nearest: NearestPlace?): String = when {
    nearest == null -> "No listed place nearby"
    nearest.meters <= ON_SITE_METERS -> "At ${nearest.place.name}"
    else -> "About ${distanceText(nearest.meters)} from ${nearest.place.name}"
}
