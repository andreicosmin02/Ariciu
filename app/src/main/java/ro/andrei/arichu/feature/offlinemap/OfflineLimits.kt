package ro.andrei.arichu.feature.offlinemap

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
    const val MIN_ZOOM = 10
    const val MAX_ZOOM = 14
    const val MIN_CAMERA_ZOOM = 11.0
    /** Used until a completed pack gives a measured value. Deliberately high: city-centre packs measured 262 KB (Vienna) and 335 KB (Paris) per tile. */
    const val DEFAULT_BYTES_PER_TILE = 350_000L
    const val MIN_BYTES_PER_TILE = 8_000L
    /**
     * Style, sprites and glyph ranges on top of tiles. The first real pack (8 tiles) measured about 18 MB of this,
     * so the default is deliberately high; completed packs replace it with their measured value.
     */
    const val DEFAULT_OVERHEAD_BYTES = 20_000_000L
    const val MIN_OVERHEAD_BYTES = 3_000_000L
    const val MARGIN = 0.10
    const val SAFETY = 1.2
}

/** Free storage kept for the phone itself on top of a download; a full disk made the real test download stall silently. */
const val STORAGE_HEADROOM_BYTES = 50_000_000L
const val STORAGE_LOW_BYTES = 25_000_000L
fun hasRoomFor(freeBytes: Long, planBytes: Long) = freeBytes >= planBytes + STORAGE_HEADROOM_BYTES
fun storageIsLow(freeBytes: Long) = freeBytes < STORAGE_LOW_BYTES

enum class AreaGate { ALLOWED, BUSY }

/** One download at a time; the number of saved areas is not limited (each download keeps to the per-download limits). */
fun areaGate(downloading: Boolean): AreaGate = if (downloading) AreaGate.BUSY else AreaGate.ALLOWED
