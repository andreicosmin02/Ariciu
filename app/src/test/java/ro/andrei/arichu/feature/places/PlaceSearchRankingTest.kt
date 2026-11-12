package ro.andrei.arichu.feature.places

import org.junit.Assert.*
import org.junit.Test

class PlaceSearchRankingTest {
    private fun p(id: String, name: String, cat: String, lat: Double, lon: Double, brand: String = "", address: String = "") =
        CatalogPlace(id, name, if (cat == "town") "town" else "store", address, lat, lon, brand, cat)
    private val lidl1 = p("n1", "Lidl Astra", "supermarket", 45.650, 25.600, "Lidl", "Brașov, Str Lunga 1")
    private val lidl2 = p("n2", "Magazin 2", "supermarket", 45.660, 25.610, "Lidl")
    private val pharm = p("n3", "Catena", "pharmacy", 45.651, 25.601, "Catena", "Brașov")
    private val fuel = p("n4", "OMV", "fuel", 45.652, 25.602, "OMV")
    private val far = p("n5", "Lidl Cluj", "supermarket", 46.77, 23.59, "Lidl")
    private val town = p("n6", "Brașov", "town", 45.65, 25.60)
    private val farTown = p("n7", "Cluj-Napoca", "town", 46.77, 23.59)
    private val all = listOf(lidl1, lidl2, pharm, fuel, far)
    private val none = emptySet<PlaceCategory>()

    @Test fun brandIsSearchableEvenWhenNameDiffers() {
        assertEquals(listOf("n1", "n2"), searchCatalog(all, "lidl", none, 45.65, 25.60).map { it.id })
    }
    @Test fun resultsAreLimitedToTheRadius() {
        assertFalse("n5" in searchCatalog(all, "lidl", none, 45.65, 25.60).map { it.id })
        assertTrue("n5" in searchCatalog(all, "lidl", none, 46.77, 23.59).map { it.id })
    }
    @Test fun categoryChipFiltersAndNeedsNoText() {
        assertEquals(listOf("n3"), searchCatalog(all, "", setOf(PlaceCategory.PHARMACY), 45.65, 25.60).map { it.id })
        assertTrue(searchCatalog(all, "", none, 45.65, 25.60).isEmpty())
    }
    @Test fun categoryWordsAndAliasesMatchWithoutBrandOrName() {
        assertEquals(listOf("n3"), searchCatalog(all, "farmacie", none, 45.65, 25.60).map { it.id })
        assertEquals(listOf("n4"), searchCatalog(all, "fuel", none, 45.65, 25.60).map { it.id })
    }
    @Test fun addressAndAccentInsensitiveMatching() {
        assertTrue("n1" in searchCatalog(all, "lunga", none, 45.65, 25.60).map { it.id })
        assertTrue("n1" in searchCatalog(all, "BRASOV lidl", none, 45.65, 25.60).map { it.id })
    }
    @Test fun nameMatchesOutrankAddressMatchesThenNearestThenId() {
        val byName = p("n8", "Lunga", "supermarket", 45.70, 25.60)
        assertEquals(listOf("n8", "n1"), searchCatalog(listOf(lidl1, byName), "lunga", none, 45.65, 25.60).map { it.id })
        val a = p("b", "Lidl", "supermarket", 45.65, 25.60); val b = p("a", "Lidl", "supermarket", 45.65, 25.60)
        assertEquals(listOf("a", "b"), searchCatalog(listOf(a, b), "lidl", none, 45.65, 25.60).map { it.id })
    }
    @Test fun resultsAreCappedAtForty() {
        val many = (1..90).map { p("n$it", "Lidl $it", "supermarket", 45.65 + it * 0.0005, 25.60, "Lidl") }
        val out = searchCatalog(many, "lidl", none, 45.65, 25.60)
        assertEquals(40, out.size)
        assertEquals((1..40).map { "n$it" }, out.map { it.id })
    }
    @Test fun townsMatchByNameAnywhereButOnlyAsPrefixMatches() {
        val out = searchCatalog(all, "cluj", none, 45.65, 25.60, towns = listOf(town, farTown))
        assertEquals(listOf("n7"), out.map { it.id })
        assertTrue(searchCatalog(all, "", none, 45.65, 25.60, towns = listOf(farTown)).isEmpty())
    }
    @Test fun legacyRowsWithoutCategoryStillSearchByKind() {
        val legacy = CatalogPlace("n9", "Kaufland", "store", "", 45.65, 25.60, "Kaufland")
        assertEquals(PlaceCategory.SUPERMARKET, PlaceCategory.of(legacy))
        assertEquals(PlaceCategory.TOWN, PlaceCategory.of(CatalogPlace("n", "X", "town", "", 0.0, 0.0)))
        assertEquals(PlaceCategory.MALL, PlaceCategory.of(CatalogPlace("n", "X", "mall", "", 0.0, 0.0)))
        assertEquals(listOf("n9"), searchCatalog(listOf(legacy), "kaufland", none, 45.65, 25.60).map { it.id })
    }
}
