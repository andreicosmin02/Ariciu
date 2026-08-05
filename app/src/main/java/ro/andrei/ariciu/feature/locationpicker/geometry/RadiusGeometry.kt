package ro.andrei.ariciu.feature.locationpicker.geometry

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val RADIUS_BOUNDARY_SEGMENTS = 64

fun buildRadiusBoundary(
    center: SelectedCoordinates,
    radiusMeters: Int,
): List<SelectedCoordinates> {
    require(radiusMeters > 0) { "radiusMeters must be positive" }
    require(center.latitude in -90.0..90.0) { "center latitude must be between -90 and 90" }
    require(center.longitude in -180.0..180.0) { "center longitude must be between -180 and 180" }

    val centerLatitudeRadians = Math.toRadians(center.latitude)
    val centerLongitudeRadians = Math.toRadians(center.longitude)
    val angularDistance = radiusMeters / EARTH_RADIUS_METERS

    val boundary = (0 until RADIUS_BOUNDARY_SEGMENTS).map { segment ->
        val bearingRadians = 2 * PI * segment / RADIUS_BOUNDARY_SEGMENTS
        val destinationLatitudeRadians = asin(
            sin(centerLatitudeRadians) * cos(angularDistance) +
                cos(centerLatitudeRadians) * sin(angularDistance) * cos(bearingRadians)
        )
        val destinationLongitudeRadians = centerLongitudeRadians + atan2(
            sin(bearingRadians) * sin(angularDistance) * cos(centerLatitudeRadians),
            cos(angularDistance) -
                sin(centerLatitudeRadians) * sin(destinationLatitudeRadians),
        )

        SelectedCoordinates(
            latitude = Math.toDegrees(destinationLatitudeRadians),
            longitude = normalizeLongitude(Math.toDegrees(destinationLongitudeRadians)),
        )
    }

    return boundary + boundary.first()
}

private fun normalizeLongitude(longitude: Double): Double {
    val normalized = (longitude + 180.0) % 360.0
    return if (normalized < 0) normalized + 180.0 else normalized - 180.0
}
