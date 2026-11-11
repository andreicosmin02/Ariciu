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
class PlaceIndex(private val maxCachedCells: Int = 48, private val open: (String) -> InputStream?) {
    private val cells = ConcurrentHashMap<String, List<CatalogPlace>>()
    @Volatile private var existing: Set<String>? = null
    @Volatile private var towns: List<CatalogPlace>? = null

    /** Forget everything read so far (a pack was installed or removed); the next query reads the sources again. */
    fun reset() { cells.clear(); existing = null; towns = null }

    private fun read(path: String): List<CatalogPlace> =
        open(path)?.bufferedReader()?.useLines { lines -> lines.mapNotNull(::parsePlaceLine).toList() } ?: emptyList()

    private fun cellList(): Set<String> = existing ?: (open("grid/index.txt")?.bufferedReader()?.useLines { l ->
        l.map { it.trim() }.filter { it.isNotEmpty() }.toSet() } ?: emptySet()).also { existing = it }

    suspend fun towns(): List<CatalogPlace> = towns ?: withContext(Dispatchers.IO) { towns ?: read("towns.tsv").onEach { it.nameWords }.also { towns = it } }

    /**
     * Towns whose name starts with every word of [query], streamed line by line so that millions of towns from many installed
     * countries never sit in memory. Exact names first, then nearest to the point; at most [limit] are kept.
     * Rows may carry a pre-folded name as a 9th column (packs do); older rows (bundled Romania) are folded on the fly.
     */
    suspend fun townMatches(query: String, latitude: Double, longitude: Double, limit: Int = 300): List<CatalogPlace> = withContext(Dispatchers.IO) {
        val q = searchWords(query)
        if (q.isEmpty() || limit <= 0) return@withContext emptyList()
        class Hit(val place: CatalogPlace, val exact: Boolean, val distance: Double)
        val order = compareBy<Hit> { !it.exact }.thenBy { it.distance }.thenBy { it.place.id }
        val kept = ArrayList<Hit>()
        open("towns.tsv")?.bufferedReader()?.useLines { lines ->
            for (line in lines) {
                if (line.isEmpty() || line[0] == '#') continue
                val folded = foldedNameOf(line) ?: continue
                if (!q.all { it in folded }) continue
                val words = searchWords(folded)
                val exact = words == q
                if (!exact && !q.all { w -> words.any { it.startsWith(w) } }) continue
                val place = parsePlaceLine(line) ?: continue
                kept += Hit(place, exact, distanceMeters(latitude, longitude, place.latitude, place.longitude))
                if (kept.size >= limit * 8) { kept.sortWith(order); kept.subList(limit, kept.size).clear() }
            }
        }
        kept.sortWith(order)
        kept.take(limit).map { it.place }
    }

    /** Non-town rows within [radiusMeters] of the point. */
    suspend fun near(lat: Double, lon: Double, radiusMeters: Double): List<CatalogPlace> = withContext(Dispatchers.IO) {
        val known = cellList()
        PlaceGrid.cellsNear(lat, lon, radiusMeters).filter { it in known }
            .also { if (cells.size > maxCachedCells) cells.clear() } // dense city cells are large; keep memory bounded
            .flatMap { key -> cells.getOrPut(key) { read("grid/c$key.tsv").onEach { it.searchText; it.nameWords; it.brandWords; it.addressWords } } }
            .filter { distanceMeters(lat, lon, it.latitude, it.longitude) <= radiusMeters }
    }
}

/** The folded name of a towns row: its 9th column when present, else the folded 2nd column. Null for malformed rows. */
internal fun foldedNameOf(line: String): String? {
    var tabs = 0; var last = -1; var firstEnd = -1; var firstStart = -1
    for (i in line.indices) if (line[i] == '\t') {
        tabs++
        if (tabs == 1) firstStart = i + 1 else if (tabs == 2) firstEnd = i
        last = i
    }
    if (tabs < 5) return null
    return if (tabs >= 8) line.substring(last + 1) else foldForSearch(line.substring(firstStart, firstEnd))
}
