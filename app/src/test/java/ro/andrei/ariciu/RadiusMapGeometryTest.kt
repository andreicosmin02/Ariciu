package ro.andrei.ariciu

import org.junit.Assert.assertEquals
import org.junit.Test

class RadiusMapGeometryTest {
    private val center = SelectedCoordinates(latitude = 45.6579, longitude = 25.6012)
    private val boundary = buildRadiusBoundary(center, radiusMeters = 200)

    @Test
    fun polygon_hasOneRingWithAll65BoundaryPositions() {
        val polygon = boundary.toGeoJsonPolygon()

        assertEquals(1, polygon.coordinates.size)
        assertEquals(65, polygon.coordinates.single().size)
    }

    @Test
    fun polygon_preservesClosedFirstAndLastPosition() {
        val ring = boundary.toGeoJsonPolygon().coordinates.single()

        assertEquals(ring.first(), ring.last())
    }

    @Test
    fun polygon_convertsLatitudeAndLongitudeWithoutReversal() {
        val position = boundary.toGeoJsonPolygon().coordinates.single().first()

        assertEquals(boundary.first().latitude, position.latitude, 0.0)
        assertEquals(boundary.first().longitude, position.longitude, 0.0)
    }

    @Test
    fun polygon_conversionPreservesEveryBoundaryPoint() {
        val ring = boundary.toGeoJsonPolygon().coordinates.single()

        assertEquals(boundary.map { it.latitude }, ring.map { it.latitude })
        assertEquals(boundary.map { it.longitude }, ring.map { it.longitude })
    }
}
