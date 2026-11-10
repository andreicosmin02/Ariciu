package ro.andrei.ariciu.feature.places

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.floor

/** 0.25 degree cells, key "<lonCell>_<latCell>", matching tools/build_place_catalog.py. */
object PlaceGrid {
    const val CELL = 0.25
    fun cellKey(lat: Double, lon: Double) = "${floor(lon / CELL).toInt()}_${floor(lat / CELL).toInt()}"
    fun cellsNear(lat: Double, lon: Double, radiusMeters: Double): List<String> {
        val dLat = radiusMeters / 111_320.0
        val dLon = radiusMeters / (111_320.0 * cos(Math.toRadians(lat)).coerceAtLeast(0.01))
        val x0 = floor((lon - dLon) / CELL).toInt(); val x1 = floor((lon + dLon) / CELL).toInt()
        val y0 = floor((lat - dLat) / CELL).toInt(); val y1 = floor((lat + dLat) / CELL).toInt()
        return (x0..x1).flatMap { x -> (y0..y1).map { y -> "${x}_$y" } }
    }
}

/** One catalogue row: id, name, kind, address, lat, lon[, brand[, category]]. Null for comments and malformed rows. */
fun parsePlaceLine(line: String): CatalogPlace? {
    if (line.isEmpty() || line[0] == '#') return null
    val f = ArrayList<String>(8)
    var start = 0
    while (f.size < 7) {
        val i = line.indexOf('\t', start)
        if (i < 0) break
        f += line.substring(start, i); start = i + 1
    }
    val rest = line.substring(start)
    val tail = rest.indexOf('\t')
    if (tail < 0) f += rest else { f += rest.substring(0, tail); f += rest.substring(tail + 1) }
    if (f.size < 6) return null
    val lat = f[4].toDoubleOrNull() ?: return null
    val lon = f[5].toDoubleOrNull() ?: return null
    return CatalogPlace(f[0], f[1], f[2], f[3], lat, lon, f.getOrElse(6) { "" }, f.getOrElse(7) { "" }.trim())
}

/**
 * Lazy grid index over bundled files. [open] returns a stream for a path under places/ or null when absent.
 * Only cells near a query are read; each cell is parsed once and cached.
 */
class PlaceIndex(private val open: (String) -> InputStream?) {
    private val cells = ConcurrentHashMap<String, List<CatalogPlace>>()
    @Volatile private var existing: Set<String>? = null
    @Volatile private var towns: List<CatalogPlace>? = null

    private fun read(path: String): List<CatalogPlace> =
        open(path)?.bufferedReader()?.useLines { lines -> lines.mapNotNull(::parsePlaceLine).toList() } ?: emptyList()

    private fun cellList(): Set<String> = existing ?: (open("grid/index.txt")?.bufferedReader()?.useLines { l ->
        l.map { it.trim() }.filter { it.isNotEmpty() }.toSet() } ?: emptySet()).also { existing = it }

    suspend fun towns(): List<CatalogPlace> = towns ?: withContext(Dispatchers.IO) { towns ?: read("towns.tsv").also { towns = it } }

    /** Non-town rows within [radiusMeters] of the point. */
    suspend fun near(lat: Double, lon: Double, radiusMeters: Double): List<CatalogPlace> = withContext(Dispatchers.IO) {
        val known = cellList()
        PlaceGrid.cellsNear(lat, lon, radiusMeters).filter { it in known }
            .flatMap { key -> cells.getOrPut(key) { read("grid/c$key.tsv") } }
            .filter { distanceMeters(lat, lon, it.latitude, it.longitude) <= radiusMeters }
    }
}
