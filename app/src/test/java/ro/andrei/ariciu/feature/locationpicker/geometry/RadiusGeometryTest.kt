package ro.andrei.ariciu.feature.locationpicker.geometry

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates

class RadiusGeometryTest {
    private val brasov = SelectedCoordinates(latitude = 45.6579, longitude = 25.6012)

    @Test
    fun boundary_has64SegmentsAndIsClosed() {
        val boundary = buildRadiusBoundary(brasov, radiusMeters = 200)

        assertEquals(65, boundary.size)
        assertEquals(boundary.first(), boundary.last())
    }

    @Test
    fun boundary_doesNotContainItsCenter() {
        val boundary = buildRadiusBoundary(brasov, radiusMeters = 200)

        assertFalse(boundary.dropLast(1).contains(brasov))
    }

    @Test
    fun boundaryPointsAre200MetersFromBrasov() {
        val boundary = buildRadiusBoundary(brasov, radiusMeters = 200)

        boundary.dropLast(1).forEach { point ->
            assertEquals(200.0, haversineDistanceMeters(brasov, point), 0.5)
        }
    }

    @Test
    fun boundaryPointsAre1000MetersFromBrasov() {
        val boundary = buildRadiusBoundary(brasov, radiusMeters = 1000)

        boundary.dropLast(1).forEach { point ->
            assertEquals(1000.0, haversineDistanceMeters(brasov, point), 0.5)
        }
    }

    @Test
    fun firstBearingPointsNorthWithoutReversingLatitudeAndLongitude() {
        val firstPoint = buildRadiusBoundary(brasov, radiusMeters = 200).first()

        assertTrue(firstPoint.latitude > brasov.latitude)
        assertEquals(brasov.longitude, firstPoint.longitude, 0.0001)
    }

    @Test
    fun boundaryNormalizesLongitudes() {
        val boundary = buildRadiusBoundary(
            center = SelectedCoordinates(latitude = 0.0, longitude = 179.999),
            radiusMeters = 1000,
        )

        assertTrue(boundary.all { it.longitude in -180.0..180.0 })
    }

    @Test
    fun boundaryRejectsNonPositiveRadius() {
        assertThrows(IllegalArgumentException::class.java) {
            buildRadiusBoundary(brasov, radiusMeters = 0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            buildRadiusBoundary(brasov, radiusMeters = -1)
        }
    }

    @Test
    fun boundaryRejectsInvalidCenterCoordinates() {
        assertThrows(IllegalArgumentException::class.java) {
            buildRadiusBoundary(SelectedCoordinates(latitude = 91.0, longitude = 25.0), 200)
        }
        assertThrows(IllegalArgumentException::class.java) {
            buildRadiusBoundary(SelectedCoordinates(latitude = 45.0, longitude = 181.0), 200)
        }
    }

    private fun haversineDistanceMeters(
        first: SelectedCoordinates,
        second: SelectedCoordinates,
    ): Double {
        val latitudeDifference = Math.toRadians(second.latitude - first.latitude)
        val longitudeDifference = Math.toRadians(second.longitude - first.longitude)
        val firstLatitude = Math.toRadians(first.latitude)
        val secondLatitude = Math.toRadians(second.latitude)
        val haversine = sin(latitudeDifference / 2).pow(2) +
            cos(firstLatitude) * cos(secondLatitude) * sin(longitudeDifference / 2).pow(2)
        return 2 * 6_371_000.0 * asin(sqrt(haversine))
    }
}
