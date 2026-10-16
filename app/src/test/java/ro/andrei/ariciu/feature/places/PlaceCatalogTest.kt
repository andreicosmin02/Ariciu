package ro.andrei.ariciu.feature.places

import org.junit.Assert.*
import org.junit.Test

class PlaceCatalogTest {
    private val places = listOf(
        CatalogPlace("n1", "Brașov", "town", "", 45.65, 25.6),
        CatalogPlace("n2", "Kaufland", "store", "Brașov, Strada Avram Iancu 3", 45.66, 25.61, "Kaufland"),
        CatalogPlace("n3", "Kaufland", "store", "București", 44.4, 26.1, "Kaufland"))
    @Test fun searchIgnoresAccentsMatchesAddressAndSortsNearby() {
        assertEquals("n1", searchPlaces(places, "brasov", 45.65, 25.6).first().id)
        assertEquals("n2", searchPlaces(places, "avram", 45.65, 25.6).single().id)
        assertEquals("n2", searchPlaces(places, "kaufland", 45.65, 25.6).first().id)
        assertTrue(searchPlaces(places, " ", 45.65, 25.6).isEmpty())
    }
    @Test fun chainAreasUseStableIdsAndNearestLocations() {
        val selected = chainPlaces(places, "Kaufland", 45.65, 25.6, 1)
        assertEquals(listOf("n2"), selected.map { it.id })
        assertTrue(chainPlaces(places, "Missing", 0.0, 0.0, 50).isEmpty())
    }
}
