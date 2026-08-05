package ro.andrei.ariciu.feature.locationpicker.ui

import java.util.Locale
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import org.junit.Assert.assertEquals
import org.junit.Test

class SelectedCoordinatesFormatterTest {
    @Test
    fun formatLatitude_usesFiveFractionalDigits() {
        val coordinates = SelectedCoordinates(latitude = 45.6579, longitude = 25.6012)

        assertEquals("Latitude: 45.65790", coordinates.formatLatitude())
    }

    @Test
    fun formatCoordinates_keepsLatitudeAndLongitudeInTheirOwnLabels() {
        val coordinates = SelectedCoordinates(latitude = 45.6579, longitude = 25.6012)

        assertEquals("Latitude: 45.65790", coordinates.formatLatitude())
        assertEquals("Longitude: 25.60120", coordinates.formatLongitude())
    }

    @Test
    fun formatCoordinates_usesDecimalPointRegardlessOfDefaultLocale() {
        val previousLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            val coordinates = SelectedCoordinates(latitude = 45.6579, longitude = 25.6012)

            assertEquals("Latitude: 45.65790", coordinates.formatLatitude())
            assertEquals("Longitude: 25.60120", coordinates.formatLongitude())
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
