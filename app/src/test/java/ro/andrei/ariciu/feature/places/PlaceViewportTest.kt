package ro.andrei.ariciu.feature.places

import org.junit.Assert.*
import org.junit.Test

class PlaceViewportTest {
    private val places = listOf(
        CatalogPlace("n1", "Lidl", "store", "", 45.65, 25.60),
        CatalogPlace("n2", "AFI", "mall", "", 45.66, 25.61),
        CatalogPlace("n3", "Săcele", "town", "", 45.62, 25.69),
        CatalogPlace("n4", "Far away", "store", "", 44.40, 26.10))
    private val bounds = GeoBounds(south = 45.60, west = 25.55, north = 45.70, east = 25.70)

    @Test fun categoryComesFromKindAndUnknownKindsAreShops() {
        assertEquals(PlaceCategory.SHOPS, PlaceCategory.of("store"))
        assertEquals(PlaceCategory.SHOPS, PlaceCategory.of("anything"))
        assertEquals(PlaceCategory.MALLS, PlaceCategory.of("mall"))
        assertEquals(PlaceCategory.TOWNS, PlaceCategory.of("town"))
    }
    @Test fun onlyPlacesInsideTheBoxAndEnabledCategoriesAreReturned() {
        assertEquals(listOf("n1", "n2", "n3"), placesInViewport(places, bounds, PlaceCategory.entries.toSet()).map { it.id })
        assertEquals(listOf("n2"), placesInViewport(places, bounds, setOf(PlaceCategory.MALLS)).map { it.id })
        assertTrue(placesInViewport(places, bounds, emptySet()).isEmpty())
    }
    @Test fun boxEdgesAreInclusive() {
        assertEquals(1, placesInViewport(listOf(places[0]), GeoBounds(45.65, 25.60, 45.65, 25.60), PlaceCategory.entries.toSet()).size)
    }
    @Test fun resultIsCappedDeterministicallyAndNearestToCentreWins() {
        val many = (1..50).map { CatalogPlace("n$it", "S", "store", "", 45.60 + it * 0.001, 25.60) }
        val out = placesInViewport(many, bounds, PlaceCategory.entries.toSet(), limit = 5)
        assertEquals(5, out.size)
        assertEquals(out, placesInViewport(many, bounds, PlaceCategory.entries.toSet(), limit = 5))
        val centre = (bounds.south + bounds.north) / 2
        val worst = out.maxOf { kotlin.math.abs(it.latitude - centre) }
        assertTrue(many.filter { it !in out }.all { kotlin.math.abs(it.latitude - centre) >= worst })
    }
    @Test fun invertedBoxReturnsNothing() {
        assertTrue(placesInViewport(places, GeoBounds(46.0, 25.0, 45.0, 26.0), PlaceCategory.entries.toSet()).isEmpty())
    }
    @Test fun zoomGateStartsAtTwelve() {
        assertFalse(placeMarkersVisible(11.9)); assertTrue(placeMarkersVisible(12.0))
    }
}
