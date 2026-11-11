package ro.andrei.ariciu.feature.offlinemap

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.places.GeoBounds

class DownloadPlannerTest {
    private val brasov = 45.6579 to 25.6012
    private val vienna = 48.2082 to 16.3738
    private fun box(c: Pair<Double, Double>, d: Double) = GeoBounds(c.first - d, c.second - d, c.first + d, c.second + d)
    private fun ready(plan: DownloadPlan) = plan as DownloadPlan.Ready

    @Test fun limitsKeepTheAppMarginsBelowTheProviderTerms() {
        assertEquals(2500, OfflineLimits.HARD_TILES); assertEquals(45_000_000L, OfflineLimits.HARD_BYTES)
        assertTrue(OfflineLimits.SOFT_TILES < OfflineLimits.HARD_TILES && OfflineLimits.SOFT_BYTES < OfflineLimits.HARD_BYTES)
        assertTrue(OfflineLimits.HARD_TILES <= 3_000 && OfflineLimits.HARD_BYTES <= 50_000_000L) // OpenFreeMap terms
    }
    @Test fun thePlanIsTheLargestBoxThatFitsAndAlwaysFullDetail() {
        listOf(brasov, vienna).forEach { city -> listOf(0.01, 0.05, 0.3, 3.0).forEach { d ->
            val p = ready(planDownload(box(city, d), bytesPerTile = null))
            assertTrue(p.tiles <= OfflineLimits.SOFT_TILES && p.estimatedBytes <= OfflineLimits.SOFT_BYTES)
            assertEquals(10, p.minZoom); assertEquals(14, p.maxZoom)
            // Growing the same box a little would break a limit.
            val grown = (p.bounds.north - p.bounds.south) / 2 * 1.05
            val bigger = GeoBounds(city.first - grown, city.second - grown * (p.bounds.east - p.bounds.west) / (p.bounds.north - p.bounds.south), city.first + grown, city.second + grown * (p.bounds.east - p.bounds.west) / (p.bounds.north - p.bounds.south))
            assertTrue(TileMath.pyramidTileCount(bigger, 10, 14) * OfflineLimits.DEFAULT_BYTES_PER_TILE + OfflineLimits.DEFAULT_OVERHEAD_BYTES > OfflineLimits.SOFT_BYTES ||
                TileMath.pyramidTileCount(bigger, 10, 14) > OfflineLimits.SOFT_TILES)
        } }
    }
    @Test fun theBoxIsCentredOnTheViewportAndKeepsItsShape() {
        val v = GeoBounds(45.60, 25.50, 45.72, 25.64)
        val p = ready(planDownload(v, null))
        assertEquals(45.66, (p.bounds.north + p.bounds.south) / 2, 1e-6); assertEquals(25.57, (p.bounds.east + p.bounds.west) / 2, 1e-6)
        assertEquals(0.12 / 0.14, (p.bounds.north - p.bounds.south) / (p.bounds.east - p.bounds.west), 1e-6)
    }
    @Test fun aMeasuredAverageGrowsTheArea() {
        val v = box(brasov, 0.05)
        val conservative = ready(planDownload(v, null)); val measured = ready(planDownload(v, 20_000))
        assertTrue(measured.bounds.north - measured.bounds.south > conservative.bounds.north - conservative.bounds.south)
    }
    @Test fun anyZoomLevelGivesAPlanInsteadOfAskingToZoom() {
        assertTrue(planDownload(box(brasov, 3.0), null) is DownloadPlan.Ready)
        assertTrue(planDownload(box(vienna, 0.001), null) is DownloadPlan.Ready)
    }
    @Test fun theDefaultTileSizeCoversDenseCityCentres() {
        // Measured: Brasov 85 KB per tile, Vienna 262 KB, Paris 335 KB.
        assertTrue(OfflineLimits.DEFAULT_BYTES_PER_TILE >= 350_000L)
    }
    @Test fun measuredOverheadReplacesTheDefaultAndFontsAndStylingAreCounted() {
        val v = box(brasov, 0.05)
        val measured = ready(planDownload(v, 85_000, overheadBytes = 18_000_000))
        assertEquals(measured.tiles * 85_000 + 18_000_000, measured.estimatedBytes)
        // The overhead alone (about 18 MB in the first real pack) is what limits how many tiles fit under 40 MB.
        assertTrue(OfflineLimits.DEFAULT_OVERHEAD_BYTES >= 18_000_000L)
        val fewTiles = (OfflineLimits.SOFT_BYTES - OfflineLimits.DEFAULT_OVERHEAD_BYTES) / OfflineLimits.DEFAULT_BYTES_PER_TILE
        assertTrue(fewTiles in 40..400)
    }
    @Test fun anOverheadAboveTheLimitMeansNothingFits() {
        assertEquals(DownloadPlan.NothingFits, planDownload(box(vienna, 0.05), 80_000, overheadBytes = 39_900_000))
    }
    @Test fun polarAndDateLineViewportsAreUnsupported() {
        assertEquals(DownloadPlan.Unsupported, planDownload(GeoBounds(81.0, 10.0, 82.0, 11.0), null))
        assertEquals(DownloadPlan.Unsupported, planDownload(GeoBounds(10.0, 179.5, 10.5, 179.9), null))
        assertEquals(DownloadPlan.Unsupported, planDownload(GeoBounds(Double.NaN, 10.0, 11.0, 11.0), null))
    }
    @Test fun invertedBoxIsUnsupported() {
        assertEquals(DownloadPlan.Unsupported, planDownload(GeoBounds(46.0, 25.0, 45.0, 26.0), null))
    }
}
