package ro.andrei.arichu.feature.places

import java.io.FilterInputStream
import java.io.InputStream
import java.util.Collections
import java.util.zip.ZipInputStream

/** One downloadable "Search data" pack (a country), as listed in the release's index.tsv. */
data class PlacePack(val code: String, val name: String, val file: String, val bytes: Long, val sha256: String, val rows: Long,
    val bounds: GeoBounds, val generated: String)

/** What is on the phone for a pack: the hash of the zip it was unpacked from. */
data class InstalledPack(val code: String, val sha256: String, val generated: String)

enum class PackStatus { NOT_INSTALLED, INSTALLED, UPDATE }

class PackFormatException(message: String) : Exception(message)

private val CODE = Regex("[a-z][a-z-]{1,19}")
private val FILE = Regex("places-[a-z0-9-]{2,24}\\.zip")
private val SHA = Regex("[0-9a-f]{64}")
private val ENTRY = Regex("towns\\.tsv|grid/index\\.txt|grid/c-?\\d{1,4}_-?\\d{1,4}\\.tsv")

/**
 * Parses index.tsv: `code, name, file, bytes, sha256, rows, west, south, east, north, generated` per line, `#` comments.
 * Rows that are malformed, point outside the release (file names are checked) or repeat a code are dropped.
 */
fun parsePackIndex(text: String): List<PlacePack> {
    val seen = HashSet<String>()
    return text.lineSequence().mapNotNull { line ->
        if (line.isBlank() || line[0] == '#') return@mapNotNull null
        val f = line.split('\t')
        if (f.size < 11) return@mapNotNull null
        val bytes = f[3].toLongOrNull() ?: return@mapNotNull null
        val rows = f[5].toLongOrNull() ?: return@mapNotNull null
        val box = f.subList(6, 10).map { it.toDoubleOrNull()?.takeIf(Double::isFinite) ?: return@mapNotNull null }
        if (!CODE.matches(f[0]) || f[1].isBlank() || !FILE.matches(f[2]) || bytes <= 0 || rows < 0 || !SHA.matches(f[4])) return@mapNotNull null
        if (!seen.add(f[0])) return@mapNotNull null
        PlacePack(f[0], f[1].trim(), f[2], bytes, f[4], rows, GeoBounds(south = box[1], west = box[0], north = box[3], east = box[2]), f[10].trim())
    }.toList()
}

fun packStatus(pack: PlacePack, installed: InstalledPack?): PackStatus = when {
    installed == null -> PackStatus.NOT_INSTALLED
    installed.sha256 == pack.sha256 -> PackStatus.INSTALLED
    else -> PackStatus.UPDATE
}

/** The point lies in the pack's bounding box, widened by a quarter degree (one grid cell). Boxes are rough, so this is a hint only. */
fun packCovers(pack: PlacePack, latitude: Double, longitude: Double, margin: Double = PlaceGrid.CELL): Boolean =
    latitude in (pack.bounds.south - margin)..(pack.bounds.north + margin) && longitude in (pack.bounds.west - margin)..(pack.bounds.east + margin)

/** A listed pack that covers the point but is not downloaded (smallest box first), or null. Used to explain an empty search. */
fun missingPackFor(catalog: List<PlacePack>, installedCodes: Set<String>, latitude: Double, longitude: Double): PlacePack? =
    catalog.filter { it.code !in installedCodes && packCovers(it, latitude, longitude, margin = 0.0) }
        .minByOrNull { (it.bounds.north - it.bounds.south) * (it.bounds.east - it.bounds.west) }

/** Only the pack layout may be written; anything else in a zip (paths with .., absolute paths, other names) is refused. */
fun isSafePackEntry(name: String): Boolean = ENTRY.matches(name)

fun hasRequiredEntries(names: Set<String>) = "towns.tsv" in names && "grid/index.txt" in names

/**
 * Reads a pack zip, handing each safe entry to [sink] (which must read it); the total unpacked size may not exceed [maxBytes].
 * Returns the unpacked size.
 */
fun unpackPack(input: InputStream, maxBytes: Long, sink: (name: String, data: InputStream) -> Unit): Long {
    var total = 0L
    ZipInputStream(input).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (entry.isDirectory) continue
            if (!isSafePackEntry(entry.name)) throw PackFormatException("Unexpected file in pack: ${entry.name.take(60)}")
            val counted = object : FilterInputStream(zip) {
                override fun read(): Int = super.read().also { if (it >= 0) count(1) }
                override fun read(b: ByteArray, off: Int, len: Int): Int = super.read(b, off, len).also { if (it > 0) count(it.toLong()) }
                override fun close() {} // the zip stream stays open for the next entry
                private fun count(n: Long) { total += n; if (total > maxBytes) throw PackFormatException("The pack is larger than expected") }
            }
            sink(entry.name, counted)
        }
    }
    return total
}

/** Reads the same path from several sources one after the other (each file ends with a newline); null when there is none. */
fun mergedStream(streams: List<InputStream>): InputStream? = when (streams.size) {
    0 -> null
    1 -> streams[0]
    else -> java.io.SequenceInputStream(Collections.enumeration(streams))
}
