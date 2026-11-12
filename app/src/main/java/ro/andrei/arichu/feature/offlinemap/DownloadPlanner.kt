package ro.andrei.arichu.feature.offlinemap

import ro.andrei.arichu.feature.places.GeoBounds

sealed interface DownloadPlan {
    /** [bounds] is what will be downloaded; [estimatedBytes] is an estimate, the hard stop is DownloadBudget. */
    data class Ready(val bounds: GeoBounds, val minZoom: Int, val maxZoom: Int, val tiles: Long, val estimatedBytes: Long,
        /** False while the size per tile is the built-in default; true once a measured value from a stored pack was used. */
        val basedOnMeasurement: Boolean = false) : DownloadPlan
    /** Even a tiny area exceeds the limits (fonts and styling alone use up the byte budget). */
    data object NothingFits : DownloadPlan
    /** Invalid box, polar regions or the date line. */
    data object Unsupported : DownloadPlan
}

private fun GeoBounds.valid() = south.isFinite() && west.isFinite() && north.isFinite() && east.isFinite() &&
    south <= north && west <= east && south >= -80.0 && north <= 80.0 && west >= -179.0 && east <= 179.0

/**
 * Chooses the largest area that fits the soft limits (2,000 tiles / 40 MB) at full detail (zoom 10-14): a box around the
 * centre of the visible [viewport] with the viewport's shape, grown or shrunk to the limit. The caller then fits the camera
 * to that box, so the user never has to zoom by hand. Plans stay below the hard stops.
 */
fun planDownload(viewport: GeoBounds, bytesPerTile: Long?, overheadBytes: Long? = null): DownloadPlan {
    if (!viewport.valid()) return DownloadPlan.Unsupported
    val bpt = (bytesPerTile ?: OfflineLimits.DEFAULT_BYTES_PER_TILE).coerceAtLeast(OfflineLimits.MIN_BYTES_PER_TILE)
    val overhead = overheadBytes?.coerceAtLeast(OfflineLimits.MIN_OVERHEAD_BYTES) ?: OfflineLimits.DEFAULT_OVERHEAD_BYTES
    val cLat = (viewport.north + viewport.south) / 2; val cLon = (viewport.east + viewport.west) / 2
    val hLat = ((viewport.north - viewport.south) / 2).coerceAtLeast(1e-4); val hLon = ((viewport.east - viewport.west) / 2).coerceAtLeast(1e-4)
    fun at(scale: Double) = GeoBounds((cLat - hLat * scale).coerceAtLeast(-80.0), (cLon - hLon * scale).coerceAtLeast(-179.0),
        (cLat + hLat * scale).coerceAtMost(80.0), (cLon + hLon * scale).coerceAtMost(179.0))
    fun plan(scale: Double): DownloadPlan.Ready? {
        val b = at(scale)
        val tiles = TileMath.pyramidTileCount(b, OfflineLimits.MIN_ZOOM, OfflineLimits.MAX_ZOOM)
        val bytes = tiles * bpt + overhead
        return if (tiles <= OfflineLimits.SOFT_TILES && bytes <= OfflineLimits.SOFT_BYTES)
            DownloadPlan.Ready(b, OfflineLimits.MIN_ZOOM, OfflineLimits.MAX_ZOOM, tiles, bytes, basedOnMeasurement = bytesPerTile != null) else null
    }
    var lo = 0.001; var hi = 64.0
    var best = plan(lo) ?: return DownloadPlan.NothingFits
    repeat(48) {
        val mid = (lo + hi) / 2
        val p = plan(mid)
        if (p != null) { lo = mid; best = p } else hi = mid
    }
    return best
}

/** Free storage a download should be checked against: the estimate if it rests on a measurement, otherwise the hard cap, because a guessed size can be far off. */
fun storageNeededFor(plan: DownloadPlan.Ready): Long = if (plan.basedOnMeasurement) plan.estimatedBytes else OfflineLimits.HARD_BYTES

/** A downloaded (or downloading) pack, as far as coverage and estimates need to know. */
data class DownloadedRegion(val bounds: GeoBounds, val dark: Boolean, val minZoom: Int, val maxZoom: Int, val complete: Boolean)
/** [stoppedAtLimit]: the download hit the safety limit, so its sizes show how heavy that area really is. */
data class PackStats(val complete: Boolean, val tiles: Long, val tileBytes: Long, val totalBytes: Long, val stoppedAtLimit: Boolean = false) {
    val measured get() = complete || stoppedAtLimit
}

/** The viewport lies fully inside one complete pack of the current theme, and the camera is not below the pack's minimum zoom. */
fun viewportCovered(viewport: GeoBounds, cameraZoom: Double, dark: Boolean, regions: List<DownloadedRegion>): Boolean =
    regions.any { r ->
        r.complete && r.dark == dark && cameraZoom >= r.minZoom &&
            viewport.south >= r.bounds.south && viewport.north <= r.bounds.north &&
            viewport.west >= r.bounds.west && viewport.east <= r.bounds.east
    }

/**
 * Conservative measured tile size: the densest completed pack (at least 5 tiles) plus a 20% margin, rather than the mean,
 * because city centres differ a lot (Brasov 85 KB per tile, Vienna 262 KB, Paris 335 KB). Null until a pack completed. Never below the floor.
 */
fun averageBytesPerTile(packs: List<PackStats>): Long? =
    packs.filter { it.measured && it.tiles >= 5 }.maxOfOrNull { it.tileBytes / it.tiles }
        ?.let { (it * OfflineLimits.SAFETY).toLong() }?.coerceAtLeast(OfflineLimits.MIN_BYTES_PER_TILE)
        // A pack that stopped at the limit has only its light low-zoom tiles in the average, so stay cautious.
        ?.let { if (packs.any { p -> p.stoppedAtLimit }) it.coerceAtLeast(OfflineLimits.DEFAULT_BYTES_PER_TILE) else it }

/** Largest non-tile share (fonts, sprites, styling) seen in a completed pack, or null before any pack completed. */
fun measuredOverheadBytes(packs: List<PackStats>): Long? =
    packs.filter { it.measured }.maxOfOrNull { (it.totalBytes - it.tileBytes).coerceAtLeast(0) }

/** Rectangles as closed Polygon features (longitude first) for map outlines. */
fun boundsGeoJson(boxes: List<GeoBounds>): String = boxes.joinToString(",", """{"type":"FeatureCollection","features":[""", "]}") { b ->
    """{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[${b.west},${b.south}],[${b.east},${b.south}],[${b.east},${b.north}],[${b.west},${b.north}],[${b.west},${b.south}]]]},"properties":{}}"""
}
