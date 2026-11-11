package ro.andrei.ariciu.feature.offlinemap

import ro.andrei.ariciu.feature.places.GeoBounds
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.tan

/** Standard Web Mercator (slippy map) tile numbering, y counted from the north. */
object TileMath {
    private const val MAX_LAT = 85.0511287798
    fun tileX(lon: Double, z: Int): Int {
        val n = 1 shl z
        return floor((lon + 180.0) / 360.0 * n).toInt().coerceIn(0, n - 1)
    }
    fun tileY(lat: Double, z: Int): Int {
        val n = 1 shl z
        val r = Math.toRadians(lat.coerceIn(-MAX_LAT, MAX_LAT))
        return floor((1.0 - ln(tan(r) + 1.0 / cos(r)) / PI) / 2.0 * n).toInt().coerceIn(0, n - 1)
    }
    /** Tiles intersecting [b] at zoom [z] (a box edge exactly on a tile border counts both tiles: conservative). */
    fun tileCount(b: GeoBounds, z: Int): Long {
        val w = tileX(b.west, z); val e = tileX(b.east, z)
        val n = tileY(b.north, z); val s = tileY(b.south, z)
        return (e - w + 1).toLong() * (s - n + 1).toLong()
    }
    fun pyramidTileCount(b: GeoBounds, minZoom: Int, maxZoom: Int): Long = (minZoom..maxZoom).sumOf { tileCount(b, it) }
}
