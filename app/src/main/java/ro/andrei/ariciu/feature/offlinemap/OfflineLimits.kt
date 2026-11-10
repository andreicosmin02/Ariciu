package ro.andrei.ariciu.feature.offlinemap

/**
 * OpenFreeMap terms allow at most 3,000 tiles or 50 MB per download, 4 parallel requests and a unique User-Agent.
 * The app's hard stops (DownloadBudget) are 2,500 tiles and 45 MB; never raise them. The soft limits are where a pack
 * is paused cleanly before the hard stop, and where plans are sized so a pack can actually finish.
 */
object OfflineLimits {
    const val HARD_TILES = 2500
    const val HARD_BYTES = 45_000_000L
    const val SOFT_TILES = 2000
    const val SOFT_BYTES = 40_000_000L
    const val MAX_AREAS = 3
    const val MIN_ZOOM = 10
    const val MAX_ZOOM = 14
    const val MIN_CAMERA_ZOOM = 11.0
    /** Used until a completed pack gives a measured average. Deliberately high (dense city tiles). */
    const val DEFAULT_BYTES_PER_TILE = 80_000L
    const val MIN_BYTES_PER_TILE = 8_000L
    /** Style, sprites and glyph ranges on top of tiles. */
    const val OVERHEAD_BYTES = 3_000_000L
    const val MARGIN = 0.10
}

enum class AreaGate { ALLOWED, AT_LIMIT, BUSY }

/** One download at a time and at most [OfflineLimits.MAX_AREAS] areas. At the limit the user may replace one. */
fun areaGate(areaCount: Int, downloading: Boolean): AreaGate = when {
    downloading -> AreaGate.BUSY
    areaCount >= OfflineLimits.MAX_AREAS -> AreaGate.AT_LIMIT
    else -> AreaGate.ALLOWED
}
