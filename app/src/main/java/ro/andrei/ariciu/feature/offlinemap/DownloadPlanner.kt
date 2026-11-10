package ro.andrei.ariciu.feature.offlinemap

import ro.andrei.ariciu.feature.places.GeoBounds

sealed interface DownloadPlan {
    enum class Detail { FULL, REDUCED, SHRUNK }
    /** [bounds] is what will be downloaded; [estimatedBytes] is an estimate, the hard stop is DownloadBudget. */
    data class Ready(val bounds: GeoBounds, val minZoom: Int, val maxZoom: Int, val tiles: Long, val estimatedBytes: Long, val detail: Detail) : DownloadPlan
    /** The visible area is too large for a pack within the limits: zoom in. */
    data object TooFarOut : DownloadPlan
    /** Invalid box, polar regions or the date line. */
    data object Unsupported : DownloadPlan
}

private fun GeoBounds.valid() = south.isFinite() && west.isFinite() && north.isFinite() && east.isFinite() &&
    south <= north && west <= east && south >= -80.0 && north <= 80.0 && west >= -179.0 && east <= 179.0

/**
 * Chooses what to download for the visible [viewport]: the viewport plus a margin at zoom 10-14; if that exceeds the
 * limits, a lower maximum zoom (13, then 12); if still too large, a box around the centre at zoom 14. When nothing fits
 * the user must zoom in. Plans stay within the soft limits (2,000 tiles / 40 MB), below the hard stops.
 */
fun planDownload(viewport: GeoBounds, cameraZoom: Double, bytesPerTile: Long?, overheadBytes: Long? = null): DownloadPlan {
    if (!viewport.valid()) return DownloadPlan.Unsupported
    if (cameraZoom < OfflineLimits.MIN_CAMERA_ZOOM) return DownloadPlan.TooFarOut
    val bpt = (bytesPerTile ?: OfflineLimits.DEFAULT_BYTES_PER_TILE).coerceAtLeast(OfflineLimits.MIN_BYTES_PER_TILE)
    val overhead = overheadBytes?.coerceAtLeast(OfflineLimits.MIN_OVERHEAD_BYTES) ?: OfflineLimits.DEFAULT_OVERHEAD_BYTES
    val padLat = (viewport.north - viewport.south) * OfflineLimits.MARGIN
    val padLon = (viewport.east - viewport.west) * OfflineLimits.MARGIN
    val padded = GeoBounds((viewport.south - padLat).coerceAtLeast(-80.0), (viewport.west - padLon).coerceAtLeast(-179.0),
        (viewport.north + padLat).coerceAtMost(80.0), (viewport.east + padLon).coerceAtMost(179.0))
    fun plan(b: GeoBounds, maxZoom: Int, detail: DownloadPlan.Detail): DownloadPlan.Ready? {
        val tiles = TileMath.pyramidTileCount(b, OfflineLimits.MIN_ZOOM, maxZoom)
        val bytes = tiles * bpt + overhead
        return if (tiles <= OfflineLimits.SOFT_TILES && bytes <= OfflineLimits.SOFT_BYTES)
            DownloadPlan.Ready(b, OfflineLimits.MIN_ZOOM, maxZoom, tiles, bytes, detail) else null
    }
    plan(padded, OfflineLimits.MAX_ZOOM, DownloadPlan.Detail.FULL)?.let { return it }
    for (maxZoom in OfflineLimits.MAX_ZOOM - 1 downTo 12) plan(padded, maxZoom, DownloadPlan.Detail.REDUCED)?.let { return it }
    val cLat = (padded.north + padded.south) / 2; val cLon = (padded.east + padded.west) / 2
    var factor = 0.9
    while (factor >= 0.3) {
        val hLat = (padded.north - padded.south) / 2 * factor; val hLon = (padded.east - padded.west) / 2 * factor
        plan(GeoBounds(cLat - hLat, cLon - hLon, cLat + hLat, cLon + hLon), OfflineLimits.MAX_ZOOM, DownloadPlan.Detail.SHRUNK)?.let { return it }
        factor *= 0.85
    }
    return DownloadPlan.TooFarOut
}

/** A downloaded (or downloading) pack, as far as coverage and estimates need to know. */
data class DownloadedRegion(val bounds: GeoBounds, val dark: Boolean, val minZoom: Int, val maxZoom: Int, val complete: Boolean)
data class PackStats(val complete: Boolean, val tiles: Long, val tileBytes: Long, val totalBytes: Long)

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
    packs.filter { it.complete && it.tiles >= 5 }.maxOfOrNull { it.tileBytes / it.tiles }
        ?.let { (it * OfflineLimits.SAFETY).toLong() }?.coerceAtLeast(OfflineLimits.MIN_BYTES_PER_TILE)

/** Largest non-tile share (fonts, sprites, styling) seen in a completed pack, or null before any pack completed. */
fun measuredOverheadBytes(packs: List<PackStats>): Long? =
    packs.filter { it.complete }.maxOfOrNull { (it.totalBytes - it.tileBytes).coerceAtLeast(0) }

/** Rectangles as closed Polygon features (longitude first) for map outlines. */
fun boundsGeoJson(boxes: List<GeoBounds>): String = boxes.joinToString(",", """{"type":"FeatureCollection","features":[""", "]}") { b ->
    """{"type":"Feature","geometry":{"type":"Polygon","coordinates":[[[${b.west},${b.south}],[${b.east},${b.south}],[${b.east},${b.north}],[${b.west},${b.north}],[${b.west},${b.south}]]]},"properties":{}}"""
}
