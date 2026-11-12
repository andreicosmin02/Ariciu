package ro.andrei.arichu.feature.offlinemap

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.arichu.feature.places.GeoBounds

class TileMathTest {
    private fun box(lat: Double, lon: Double, d: Double) = GeoBounds(lat - d, lon - d, lat + d, lon + d)

    @Test fun tileCoordinatesMatchReferenceValuesInDifferentCountries() {
        // Reference values computed independently with the standard Web Mercator slippy-map formulas.
        val cities = mapOf(
            "Brasov RO" to Triple(45.6579, 25.6012, listOf(Triple(10, 584, 365), Triple(12, 2339, 1462), Triple(14, 9357, 5851))),
            "Vienna AT" to Triple(48.2082, 16.3738, listOf(Triple(10, 558, 355), Triple(12, 2234, 1420), Triple(14, 8937, 5681))),
            "Reykjavik IS" to Triple(64.1466, -21.9426, listOf(Triple(10, 449, 272), Triple(12, 1798, 1088), Triple(14, 7193, 4354))),
            "Sydney AU" to Triple(-33.8688, 151.2093, listOf(Triple(10, 942, 614), Triple(12, 3768, 2457), Triple(14, 15073, 9831))))
        cities.forEach { (name, c) ->
            c.third.forEach { (z, x, y) ->
                assertEquals("$name x z$z", x, TileMath.tileX(c.second, z))
                assertEquals("$name y z$z", y, TileMath.tileY(c.first, z))
            }
        }
    }
    @Test fun tileCountPerZoomForTheSameBoxInTwoCities() {
        val brasov = box(45.6579, 25.6012, 0.05); val vienna = box(48.2082, 16.3738, 0.05)
        assertEquals(listOf(1L, 2L, 6L, 15L, 48L), (10..14).map { TileMath.tileCount(brasov, it) })
        assertEquals(listOf(2L, 4L, 6L, 15L, 48L), (10..14).map { TileMath.tileCount(vienna, it) })
    }
    @Test fun largerBoxAndPyramidTotals() {
        val brasov = box(45.6579, 25.6012, 0.3); val vienna = box(48.2082, 16.3738, 0.3)
        assertEquals(listOf(9L, 30L, 88L, 315L, 1120L), (10..14).map { TileMath.tileCount(brasov, it) })
        assertEquals(listOf(12L, 24L, 88L, 315L, 1176L), (10..14).map { TileMath.tileCount(vienna, it) })
        assertEquals(1562L, TileMath.pyramidTileCount(brasov, 10, 14))
        assertEquals(1615L, TileMath.pyramidTileCount(vienna, 10, 14))
        assertEquals(442L, TileMath.pyramidTileCount(brasov, 10, 13))
    }
    @Test fun aSinglePointStillNeedsOneTilePerZoom() {
        val point = GeoBounds(45.0, 25.0, 45.0, 25.0)
        assertEquals(5L, TileMath.pyramidTileCount(point, 10, 14))
    }
    @Test fun outOfRangeInputsAreClampedNotCrashing() {
        assertEquals(0, TileMath.tileX(-200.0, 10)); assertEquals((1 shl 10) - 1, TileMath.tileX(200.0, 10))
        assertEquals(0, TileMath.tileY(89.9, 10)); assertEquals((1 shl 10) - 1, TileMath.tileY(-89.9, 10))
    }
    @Test fun eachZoomRoughlyQuadruplesTheTiles() {
        val b = box(48.2082, 16.3738, 0.3)
        val counts = (10..14).map { TileMath.tileCount(b, it).toDouble() }
        counts.zipWithNext().forEach { (a, c) -> assertTrue(c / a in 1.9..5.0) }
    }
}
