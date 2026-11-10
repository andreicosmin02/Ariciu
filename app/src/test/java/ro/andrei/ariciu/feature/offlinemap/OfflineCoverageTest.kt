package ro.andrei.ariciu.feature.offlinemap

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.places.GeoBounds

class OfflineCoverageTest {
    private fun region(b: GeoBounds, dark: Boolean = false, complete: Boolean = true, minZoom: Int = 10) =
        DownloadedRegion(b, dark, minZoom, 14, complete)
    private val brasovPack = region(GeoBounds(45.60, 25.50, 45.72, 25.70))
    private val inside = GeoBounds(45.64, 25.58, 45.68, 25.62)

    @Test fun viewportInsideACompleteSameThemePackIsCovered() = assertTrue(viewportCovered(inside, 14.0, dark = false, listOf(brasovPack)))
    @Test fun otherThemeIncompleteOrPartialPacksDoNotCover() {
        assertFalse(viewportCovered(inside, 14.0, dark = true, listOf(brasovPack)))
        assertFalse(viewportCovered(inside, 14.0, false, listOf(region(brasovPack.bounds, complete = false))))
        assertFalse(viewportCovered(GeoBounds(45.64, 25.65, 45.68, 25.75), 14.0, false, listOf(brasovPack)))
    }
    @Test fun aPackInOneCityNeverCoversAnotherCountry() {
        val vienna = GeoBounds(48.18, 16.34, 48.23, 16.40)
        assertFalse(viewportCovered(vienna, 14.0, false, listOf(brasovPack)))
        assertTrue(viewportCovered(vienna, 14.0, false, listOf(brasovPack, region(GeoBounds(48.10, 16.25, 48.30, 16.50)))))
    }
    @Test fun zoomedOutBelowThePackMinimumIsNotCovered() {
        assertFalse(viewportCovered(inside, 9.0, false, listOf(brasovPack)))
        assertTrue(viewportCovered(inside, 16.5, false, listOf(brasovPack)))
    }
    @Test fun noPacksMeansNotCovered() = assertFalse(viewportCovered(inside, 14.0, false, emptyList()))

    @Test fun averageBytesPerTileUsesOnlyCompletedPacksWithEnoughTiles() {
        assertNull(averageBytesPerTile(emptyList()))
        assertNull(averageBytesPerTile(listOf(PackStats(complete = true, tiles = 3, tileBytes = 1_000_000, totalBytes = 5_000_000))))
        assertNull(averageBytesPerTile(listOf(PackStats(complete = false, tiles = 500, tileBytes = 9_000_000, totalBytes = 30_000_000))))
        assertEquals(24_000L, averageBytesPerTile(listOf(PackStats(true, 300, 6_000_000, 25_000_000), PackStats(true, 100, 2_000_000, 21_000_000), PackStats(false, 900, 90_000_000, 99_000_000))))
    }
    @Test fun theEstimateFollowsTheDensestPackSeenPlusASafetyMargin() {
        // Real packs: Brasov 8 tiles 680,255 bytes (85 KB each), Vienna 8 tiles 2,094,121 bytes (261 KB each).
        val packs = listOf(PackStats(true, 8, 680_255, 18_708_284), PackStats(true, 8, 2_094_121, 20_122_150))
        assertEquals(314_118L, averageBytesPerTile(packs))
        // Paris measured 335 KB per tile after Vienna: the margin must be at least that large a step.
        assertTrue(averageBytesPerTile(packs)!! * 1.07 > 335_669)
    }
    @Test fun measuredOverheadIsTheLargestNonTileShareOfCompletedPacks() {
        assertNull(measuredOverheadBytes(emptyList()))
        assertNull(measuredOverheadBytes(listOf(PackStats(false, 100, 1_000_000, 30_000_000))))
        assertEquals(19_000_000L, measuredOverheadBytes(listOf(PackStats(true, 8, 700_000, 18_700_000), PackStats(true, 100, 2_000_000, 21_000_000), PackStats(false, 5, 1, 99_000_000))))
    }
    @Test fun averageIsNeverBelowAFloorSoTheEstimateStaysConservative() {
        assertEquals(OfflineLimits.MIN_BYTES_PER_TILE, averageBytesPerTile(listOf(PackStats(true, 1000, 1_000_000, 22_000_000))))
    }

    @Test fun threeAreasIsTheLimit() {
        assertEquals(AreaGate.ALLOWED, areaGate(areaCount = 2, downloading = false))
        assertEquals(AreaGate.AT_LIMIT, areaGate(areaCount = 3, downloading = false))
        assertEquals(AreaGate.AT_LIMIT, areaGate(areaCount = 5, downloading = false))
        assertEquals(AreaGate.BUSY, areaGate(areaCount = 1, downloading = true))
        assertEquals(AreaGate.BUSY, areaGate(areaCount = 3, downloading = true))
    }

    @Test fun outlinesAreClosedPolygonsWithLongitudeFirst() {
        val json = boundsGeoJson(listOf(GeoBounds(45.0, 25.0, 46.0, 26.0)))
        assertTrue(json, json.contains("[[25.0,45.0],[26.0,45.0],[26.0,46.0],[25.0,46.0],[25.0,45.0]]"))
        assertEquals(1, Regex("\"type\":\"Polygon\"").findAll(json).count())
        assertEquals("""{"type":"FeatureCollection","features":[]}""", boundsGeoJson(emptyList()))
        assertEquals(2, Regex("\"type\":\"Polygon\"").findAll(boundsGeoJson(listOf(GeoBounds(0.0, 0.0, 1.0, 1.0), GeoBounds(5.0, 5.0, 6.0, 6.0)))).count())
    }
}
