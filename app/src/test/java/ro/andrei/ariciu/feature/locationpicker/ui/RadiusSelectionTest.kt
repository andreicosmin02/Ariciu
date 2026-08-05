package ro.andrei.ariciu.feature.locationpicker.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadiusSelectionTest {
    @Test
    fun defaultRadius_formatsInMeters() {
        assertEquals("Radius: 200 m", formatRadiusMeters(DEFAULT_RADIUS_METERS))
    }

    @Test
    fun snapRadiusMeters_roundsToNearestSupportedInterval() {
        assertEquals(300, snapRadiusMeters(276f))
    }

    @Test
    fun snapRadiusMeters_clampsBelowMinimum() {
        assertEquals(100, snapRadiusMeters(50f))
    }

    @Test
    fun snapRadiusMeters_clampsAboveMaximum() {
        assertEquals(1000, snapRadiusMeters(1200f))
    }

    @Test
    fun snapRadiusMeters_returnsAnInteger() {
        val radius: Any = snapRadiusMeters(276f)

        assertTrue(radius is Int)
    }
}
