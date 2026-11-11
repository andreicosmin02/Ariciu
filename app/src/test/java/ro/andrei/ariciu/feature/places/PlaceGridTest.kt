package ro.andrei.ariciu.feature.places

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlaceGridTest {
    @Test fun cellKeyUsesQuarterDegreeCellsLongitudeFirst() {
        assertEquals("102_182", PlaceGrid.cellKey(45.6579, 25.6012))
        assertEquals("100_180", PlaceGrid.cellKey(45.0, 25.0))
        assertEquals("99_179", PlaceGrid.cellKey(44.99, 24.99))
    }
    @Test fun smallRadiusTouchesFewCellsAndBigRadiusMore() {
        val small = PlaceGrid.cellsNear(45.125, 25.125, 1_000.0)
        assertEquals(listOf("100_180"), small)
        val big = PlaceGrid.cellsNear(45.6579, 25.6012, 25_000.0)
        assertTrue(big.size in 2..12); assertTrue("102_182" in big)
        assertEquals(big.distinct(), big)
    }
    @Test fun parsesNewAndOldRows() {
        val new = parsePlaceLine("n1\tLidl\tstore\tBrașov, Str 1\t45.5\t25.5\tLidl\tsupermarket")!!
        assertEquals("supermarket", new.category); assertEquals("Lidl", new.chain); assertEquals(25.5, new.longitude, 0.0)
        val old = parsePlaceLine("n2\tKaufland\tstore\t\t45.6\t25.6\tKaufland")!!
        assertEquals("", old.category); assertEquals("Kaufland", old.chain)
        val bare = parsePlaceLine("n3\tBrașov\ttown\t\t45.6\t25.6")!!
        assertEquals("", bare.chain)
        assertNull(parsePlaceLine("# comment")); assertNull(parsePlaceLine("")); assertNull(parsePlaceLine("n4\tbroken"))
        assertNull(parsePlaceLine("n5\tX\tstore\t\tabc\t25.6"))
    }
    @Test fun indexReadsOnlyNeededCellsAndCachesThem() = runBlocking {
        val opened = mutableListOf<String>()
        val files = mapOf(
            "grid/index.txt" to "102_182\n102_183\n",
            "grid/c102_182.tsv" to "# h\nn1\tLidl\tstore\t\t45.65\t25.60\tLidl\tsupermarket\nn2\tFar\tstore\t\t45.74\t25.60\t\tfuel\n",
            "towns.tsv" to "n9\tBrașov\ttown\t\t45.65\t25.60\t\ttown\n")
        val index = PlaceIndex { path -> opened += path; files[path]?.byteInputStream() }
        val near = index.near(45.65, 25.60, 2_000.0)
        assertEquals(listOf("n1"), near.map { it.id })
        index.near(45.65, 25.60, 2_000.0)
        assertEquals(1, opened.count { it == "grid/c102_182.tsv" })
        assertFalse("grid/c102_183.tsv" in opened)
        assertEquals(listOf("n9"), index.townMatches("brasov", 45.65, 25.60).map { it.id })
    }
    @Test fun missingCellOrCatalogueDoesNotCrash() = runBlocking {
        val index = PlaceIndex { null }
        assertTrue(index.near(45.0, 25.0, 25_000.0).isEmpty())
        assertTrue(index.townMatches("brasov", 45.0, 25.0).isEmpty())
    }
}
