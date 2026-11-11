package ro.andrei.ariciu.feature.places

import org.junit.Assert.*
import org.junit.Test

class SearchSnapTest {
    private fun p(id: String, lat: Double, lon: Double) = CatalogPlace(id, id, "store", "", lat, lon, "", "supermarket")
    @Test fun nearestResultWinsAndTiesBreakById() {
        val list = listOf(p("c", 45.70, 25.60), p("b", 45.651, 25.60), p("a", 45.651, 25.60))
        assertEquals("a", nearestResult(list, 45.65, 25.60)?.id)
        assertNull(nearestResult(emptyList(), 45.65, 25.60))
    }
    @Test fun fitBoundsCoverEveryResultAndTheReference() {
        val b = fitBounds(listOf(p("a", 45.60, 25.50), p("b", 45.70, 25.70)), 45.65, 25.60)!!
        assertEquals(45.60, b.south, 1e-9); assertEquals(45.70, b.north, 1e-9)
        assertEquals(25.50, b.west, 1e-9); assertEquals(25.70, b.east, 1e-9)
        val wide = fitBounds(listOf(p("a", 45.60, 25.50)), 45.90, 25.90)!!
        assertEquals(45.90, wide.north, 1e-9); assertEquals(25.90, wide.east, 1e-9)
        assertNull(fitBounds(emptyList(), 45.0, 25.0))
    }
    @Test fun searchThisAreaAppearsOnlyAfterASignificantMove() {
        assertFalse(mapMovedEnough(45.65, 25.60, 45.6501, 25.6001, zoom = 14.0))
        assertTrue(mapMovedEnough(45.65, 25.60, 45.70, 25.60, zoom = 14.0))
        assertFalse(mapMovedEnough(45.65, 25.60, 45.652, 25.60, zoom = 10.0))
    }
    @Test fun labelPrefersSharedBrandThenQueryThenChips() {
        val lidl = listOf(CatalogPlace("a", "Lidl 1", "store", "", 0.0, 0.0, "Lidl", "supermarket"), CatalogPlace("b", "Lidl 2", "store", "", 0.0, 0.0, "Lidl", "supermarket"))
        assertEquals("Lidl", searchLabel("shop", emptySet(), lidl))
        val mixed = listOf(p("a", 0.0, 0.0), p("b", 0.0, 0.0))
        assertEquals("Pharmacy", searchLabel(" pharmacy ", emptySet(), mixed))
        assertEquals("Pharmacies & Fuel", searchLabel("", setOf(PlaceCategory.FUEL, PlaceCategory.PHARMACY), mixed))
        assertEquals("Places", searchLabel("", emptySet(), emptyList()))
    }
}
