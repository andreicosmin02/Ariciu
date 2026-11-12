package ro.andrei.arichu.feature.places

import org.junit.Assert.*
import org.junit.Test

class PlaceGeoJsonTest {
    @Test fun emptyListIsAnEmptyCollection() = assertEquals("""{"type":"FeatureCollection","features":[]}""", placesGeoJson(emptyList()))
    @Test fun pointUsesLongitudeFirstAndCategoryKey() {
        val json = placesGeoJson(listOf(CatalogPlace("n1", "Lidl", "store", "", 45.5, 25.25)))
        assertTrue(json, json.contains(""""coordinates":[25.25,45.5]"""))
        assertTrue(json, json.contains(""""id":"n1""""))
        assertTrue(json, json.contains(""""cat":"supermarket""""))
    }
    @Test fun namesAreEscaped() {
        val json = placesGeoJson(listOf(CatalogPlace("n2", "Bob \"Mega\"\\ \n shop", "mall", "", 1.0, 2.0)))
        assertTrue(json, json.contains(""""name":"Bob \"Mega\"\\ \n shop""""))
    }
}
