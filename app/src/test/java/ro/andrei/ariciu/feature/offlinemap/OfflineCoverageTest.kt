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
        assertNull(averageBytesPerTile(listOf(PackStats(complete = true, tiles = 10, tileBytes = 1_000_000))))
        assertNull(averageBytesPerTile(listOf(PackStats(complete = false, tiles = 500, tileBytes = 9_000_000))))
        assertEquals(20_000L, averageBytesPerTile(listOf(PackStats(true, 300, 6_000_000), PackStats(true, 100, 2_000_000), PackStats(false, 900, 90_000_000))))
    }
    @Test fun averageIsNeverBelowAFloorSoTheEstimateStaysConservative() {
        assertEquals(OfflineLimits.MIN_BYTES_PER_TILE, averageBytesPerTile(listOf(PackStats(true, 1000, 1_000_000))))
    }

    @Test fun threeAreasIsTheLimit() {
        assertEquals(AreaGate.ALLOWED, areaGate(areaCount = 2, downloading = false))
        assertEquals(AreaGate.AT_LIMIT, areaGate(areaCount = 3, downloading = false))
        assertEquals(AreaGate.AT_LIMIT, areaGate(areaCount = 5, downloading = false))
        assertEquals(AreaGate.BUSY, areaGate(areaCount = 1, downloading = true))
        assertEquals(AreaGate.BUSY, areaGate(areaCount = 3, downloading = true))
    }
}
