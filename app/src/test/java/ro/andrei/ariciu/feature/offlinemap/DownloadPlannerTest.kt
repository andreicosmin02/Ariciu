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
        assertEquals(3, OfflineLimits.MAX_AREAS)
    }
    @Test fun aSmallViewportGetsFullDetailInBothCities() {
        listOf(brasov, vienna).forEach { city ->
            val p = ready(planDownload(box(city, 0.05), cameraZoom = 14.0, bytesPerTile = null))
            assertEquals(DownloadPlan.Detail.FULL, p.detail); assertEquals(14, p.maxZoom); assertEquals(10, p.minZoom)
            assertEquals(p.tiles * OfflineLimits.DEFAULT_BYTES_PER_TILE + OfflineLimits.OVERHEAD_BYTES, p.estimatedBytes)
            assertTrue(p.tiles in 70L..130L)
        }
    }
    @Test fun theBoxIncludesASmallMarginAroundTheViewport() {
        val v = box(brasov, 0.05)
        val p = ready(planDownload(v, 14.0, null))
        assertTrue(p.bounds.south < v.south && p.bounds.north > v.north && p.bounds.west < v.west && p.bounds.east > v.east)
        assertTrue(p.bounds.north - v.north <= 0.05 * 2 * OfflineLimits.MARGIN + 1e-9)
    }
    @Test fun aMeasuredAverageLowersTheEstimate() {
        val v = box(vienna, 0.05)
        val conservative = ready(planDownload(v, 14.0, null)); val measured = ready(planDownload(v, 14.0, 20_000))
        assertTrue(measured.estimatedBytes < conservative.estimatedBytes)
        assertEquals(measured.tiles * 20_000 + OfflineLimits.OVERHEAD_BYTES, measured.estimatedBytes)
    }
    @Test fun aLargerViewportReducesMaxZoomBeforeShrinking() {
        listOf(brasov, vienna).forEach { city ->
            val p = ready(planDownload(box(city, 0.3), cameraZoom = 11.5, bytesPerTile = null))
            assertEquals(DownloadPlan.Detail.REDUCED, p.detail); assertTrue(p.maxZoom in 12..13)
            assertTrue(p.estimatedBytes <= OfflineLimits.SOFT_BYTES)
        }
    }
    @Test fun accurateSmallTilesKeepFullDetailForTheSameViewport() {
        val p = ready(planDownload(box(brasov, 0.2), cameraZoom = 11.8, bytesPerTile = 15_000))
        assertEquals(14, p.maxZoom)
        assertTrue(p.tiles <= OfflineLimits.SOFT_TILES && p.estimatedBytes <= OfflineLimits.SOFT_BYTES)
    }
    @Test fun tooFarOutAsksForAZoomInsteadOfProducingAHugeArea() {
        assertEquals(DownloadPlan.TooFarOut, planDownload(box(brasov, 3.0), cameraZoom = 8.5, bytesPerTile = null))
        assertEquals(DownloadPlan.TooFarOut, planDownload(box(vienna, 0.4), cameraZoom = 10.9, bytesPerTile = null))
    }
    @Test fun aHugeBoxAtAnAcceptableZoomIsShrunkAroundTheCentreOrRefused() {
        val v = box(vienna, 1.5)
        when (val p = planDownload(v, cameraZoom = 11.0, bytesPerTile = null)) {
            is DownloadPlan.Ready -> {
                assertEquals(DownloadPlan.Detail.SHRUNK, p.detail)
                assertTrue(p.bounds.south > v.south && p.bounds.north < v.north && p.bounds.west > v.west && p.bounds.east < v.east)
                assertEquals((v.north + v.south) / 2, (p.bounds.north + p.bounds.south) / 2, 1e-9)
                assertEquals((v.east + v.west) / 2, (p.bounds.east + p.bounds.west) / 2, 1e-9)
                assertTrue(p.tiles <= OfflineLimits.SOFT_TILES && p.estimatedBytes <= OfflineLimits.SOFT_BYTES)
            }
            DownloadPlan.TooFarOut -> Unit
            else -> fail("unexpected $p")
        }
    }
    @Test fun everyReadyPlanStaysWithinTheSoftLimits() {
        for (city in listOf(brasov, vienna)) for (d in listOf(0.02, 0.05, 0.1, 0.2, 0.4, 0.8, 1.6)) for (bpt in listOf(null, 10_000L, 40_000L, 150_000L)) {
            val p = planDownload(box(city, d), cameraZoom = 11.5, bytesPerTile = bpt)
            if (p is DownloadPlan.Ready) {
                assertTrue("$city $d $bpt", p.tiles <= OfflineLimits.SOFT_TILES && p.estimatedBytes <= OfflineLimits.SOFT_BYTES)
                assertTrue(p.maxZoom in 12..14 && p.minZoom == 10)
            }
        }
    }
    @Test fun polarAndDateLineViewportsAreUnsupported() {
        assertEquals(DownloadPlan.Unsupported, planDownload(GeoBounds(81.0, 10.0, 82.0, 11.0), 12.0, null))
        assertEquals(DownloadPlan.Unsupported, planDownload(GeoBounds(10.0, 179.5, 10.5, 179.9), 12.0, null))
        assertEquals(DownloadPlan.Unsupported, planDownload(GeoBounds(Double.NaN, 10.0, 11.0, 11.0), 12.0, null))
    }
    @Test fun invertedBoxIsUnsupported() {
        assertEquals(DownloadPlan.Unsupported, planDownload(GeoBounds(46.0, 25.0, 45.0, 26.0), 12.0, null))
    }
}
