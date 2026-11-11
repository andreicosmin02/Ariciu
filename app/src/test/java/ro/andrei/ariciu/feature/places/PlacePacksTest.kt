package ro.andrei.ariciu.feature.places

import kotlinx.coroutines.runBlocking
import ro.andrei.ariciu.feature.offlinemap.hasRoomFor
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PlacePacksTest {
    private val sha = "a".repeat(64)
    private fun row(code: String = "fr", file: String = "places-fr.zip", bytes: String = "1000", hash: String = sha, bbox: String = "-5.2\t41.3\t9.6\t51.1") =
        "$code\tFrance\t$file\t$bytes\t$hash\t500\t$bbox\t2026-11-11"

    @Test fun parsesRowsAndSkipsCommentsAndBadRows() {
        val text = "# ariciu place packs v1\n${row()}\n\n${row("de", "places-de.zip")}\nbroken\tline\n"
        val packs = parsePackIndex(text)
        assertEquals(listOf("fr", "de"), packs.map { it.code })
        val fr = packs.first()
        assertEquals("France", fr.name); assertEquals(1000L, fr.bytes); assertEquals(sha, fr.sha256)
        assertEquals(-5.2, fr.bounds.west, 0.0); assertEquals(51.1, fr.bounds.north, 0.0)
    }

    @Test fun rejectsRowsThatCouldEscapeOrBeTampered() {
        val bad = listOf(
            row(file = "../places-fr.zip"), row(file = "places-fr.zip/../x"), row(file = "https://evil.example/x.zip"),
            row(code = "FR!"), row(code = ""), row(hash = "xyz"), row(bytes = "0"), row(bytes = "-4"), row(bytes = "abc"),
            row(bbox = "1\t2\t3"), row(bbox = "-5.2\t41.3\t9.6\tNaN"))
        bad.forEach { assertTrue("accepted: $it", parsePackIndex(it).isEmpty()) }
    }

    @Test fun duplicateCodesKeepTheFirstRow() {
        assertEquals(listOf("places-fr.zip"), parsePackIndex(row() + "\n" + row(file = "places-fr2.zip")).map { it.file })
    }

    @Test fun statusFollowsTheInstalledHash() {
        val pack = parsePackIndex(row()).single()
        assertEquals(PackStatus.NOT_INSTALLED, packStatus(pack, null))
        assertEquals(PackStatus.INSTALLED, packStatus(pack, InstalledPack("fr", sha, "2026-11-11")))
        assertEquals(PackStatus.UPDATE, packStatus(pack, InstalledPack("fr", "b".repeat(64), "2026-10-01")))
    }

    @Test fun onlyPackLayoutEntriesAreSafe() {
        listOf("towns.tsv", "grid/index.txt", "grid/c102_182.tsv", "grid/c-12_-3.tsv", "grid/c0_0.tsv").forEach { assertTrue(it, isSafePackEntry(it)) }
        listOf("../towns.tsv", "/etc/passwd", "grid/../towns.tsv", "grid/c1_2.tsv/../../x", "grid/", "pack.txt", "grid/other.tsv", "grid\\c1_2.tsv",
            "towns.tsv\u0000.png", "grid/c1_2.tsv.exe", "").forEach { assertFalse(it, isSafePackEntry(it)) }
    }

    @Test fun coverageUsesTheBoundingBoxWithAMargin() {
        val fr = parsePackIndex(row()).single()
        assertTrue(packCovers(fr, 48.85, 2.35))
        assertTrue(packCovers(fr, 51.2, 2.35))
        assertFalse(packCovers(fr, 45.0, 25.0))
    }

    @Test fun unzipAcceptsOnlySafeEntriesAndReportsSize() {
        fun zip(vararg e: Pair<String, String>) = ByteArrayOutputStream().also { o -> ZipOutputStream(o).use { z -> e.forEach { (n, c) -> z.putNextEntry(ZipEntry(n)); z.write(c.toByteArray()); z.closeEntry() } } }.toByteArray()
        val written = mutableMapOf<String, String>()
        val total = unpackPack(ByteArrayInputStream(zip("towns.tsv" to "n1\tA\ttown\t\t1\t2\t\ttown\n", "grid/index.txt" to "1_2\n", "grid/c1_2.tsv" to "x\n")), maxBytes = 1_000_000) { name, bytes ->
            written[name] = String(bytes.readBytes())
        }
        assertEquals(setOf("towns.tsv", "grid/index.txt", "grid/c1_2.tsv"), written.keys)
        assertEquals(written.values.sumOf { it.length.toLong() }, total)
        assertThrows(PackFormatException::class.java) { unpackPack(ByteArrayInputStream(zip("../evil" to "x")), 1_000_000) { _, b -> b.readBytes() } }
        assertFalse(hasRequiredEntries(setOf("towns.tsv")))
        assertTrue(hasRequiredEntries(setOf("towns.tsv", "grid/index.txt")))
    }

    @Test fun unzipStopsAtTheByteLimit() {
        val big = ByteArrayOutputStream().also { o -> ZipOutputStream(o).use { z -> z.putNextEntry(ZipEntry("towns.tsv")); z.write(ByteArray(10_000) { 'a'.code.toByte() }); z.closeEntry() } }.toByteArray()
        assertThrows(PackFormatException::class.java) { unpackPack(ByteArrayInputStream(big), maxBytes = 5_000) { _, b -> b.readBytes() } }
    }

    @Test fun storageNeedIsTheUnpackedSizePlusHeadroom() {
        assertTrue(hasRoomFor(freeBytes = 400_000_000, planBytes = 100_000_000))
        assertFalse(hasRoomFor(freeBytes = 120_000_000, planBytes = 100_000_000))
    }

    @Test fun concatenatedSourcesMergeIndexAndCells() = runBlocking {
        val a = mapOf("grid/index.txt" to "1_2\n", "grid/c1_2.tsv" to "# h\nn1\tA\tstore\t\t0.6\t0.3\t\tfuel\n", "towns.tsv" to "n9\tTown\ttown\t\t0.6\t0.3\t\ttown\n")
        val b = mapOf("grid/index.txt" to "1_2\n", "grid/c1_2.tsv" to "# h\nn2\tB\tstore\t\t0.6\t0.3\t\tfuel\n")
        val index = PlaceIndex { path -> mergedStream(listOf(a[path], b[path]).filterNotNull().map { it.byteInputStream() }) }
        assertEquals(setOf("n1", "n2"), index.near(0.6, 0.3, 1_000.0).map { it.id }.toSet())
        assertEquals(listOf("n9"), index.towns().map { it.id })
    }

    @Test fun resetMakesTheIndexReadTheSourcesAgain() = runBlocking {
        var files = mapOf("grid/index.txt" to "1_2\n", "grid/c1_2.tsv" to "n1\tA\tstore\t\t0.6\t0.3\t\tfuel\n")
        val index = PlaceIndex { path -> files[path]?.byteInputStream() }
        assertEquals(listOf("n1"), index.near(0.6, 0.3, 1_000.0).map { it.id })
        files = emptyMap()
        index.reset()
        assertTrue(index.near(0.6, 0.3, 1_000.0).isEmpty())
    }

    @Test fun hintNamesTheNotDownloadedPackThatCoversThePoint() {
        val packs = parsePackIndex(row() + "\n" + row("ro", "places-ro.zip", bbox = "20.2\t43.6\t29.7\t48.3").replace("France", "Romania"))
        assertEquals("France", missingPackFor(packs, emptySet(), 48.85, 2.35)?.name)
        assertNull(missingPackFor(packs, setOf("fr"), 48.85, 2.35))
        assertNull(missingPackFor(packs, emptySet(), 10.0, -60.0))
        assertNull(missingPackFor(emptyList(), emptySet(), 48.85, 2.35))
    }
}
