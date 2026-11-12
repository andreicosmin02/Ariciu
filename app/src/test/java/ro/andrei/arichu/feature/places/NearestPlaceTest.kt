package ro.andrei.arichu.feature.places

import org.junit.Assert.*
import org.junit.Test

class NearestPlaceTest {
    private val places = listOf(
        CatalogPlace("n1", "Lidl", "store", "", 45.6500, 25.6000),
        CatalogPlace("n2", "Brașov", "town", "", 45.6400, 25.6100))
    @Test fun picksNearestWithinRange() {
        val near = nearestPlace(places, 45.6501, 25.6001)!!
        assertEquals("n1", near.place.id)
        assertTrue(near.meters < 20)
    }
    @Test fun nothingBeyondMaxDistanceOrInEmptyCatalogue() {
        assertNull(nearestPlace(places, 44.0, 26.0))
        assertNull(nearestPlace(emptyList(), 45.0, 25.0))
    }
    @Test fun tiesBreakById() {
        val twins = listOf(CatalogPlace("b", "B", "store", "", 45.0, 25.0), CatalogPlace("a", "A", "store", "", 45.0, 25.0))
        assertEquals("a", nearestPlace(twins, 45.0, 25.0)!!.place.id)
    }
    @Test fun labelRoundsDistanceAndSaysOnSiteWhenVeryClose() {
        assertEquals("At Lidl", nearestPlaceLabel(NearestPlace(places[0], 12.0)))
        assertEquals("About 120 m from Lidl", nearestPlaceLabel(NearestPlace(places[0], 123.0)))
        assertEquals("About 1.4 km from Lidl", nearestPlaceLabel(NearestPlace(places[0], 1_440.0)))
        assertEquals("No listed place nearby", nearestPlaceLabel(null))
    }
}
