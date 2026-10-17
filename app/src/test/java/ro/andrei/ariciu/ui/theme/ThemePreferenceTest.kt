package ro.andrei.ariciu.ui.theme

import org.junit.Assert.*
import org.junit.Test

class ThemePreferenceTest {
    @Test fun systemFollowsDeviceWhileExplicitChoicesOverrideIt() {
        assertTrue(ThemePreference.SYSTEM.isDark(true))
        assertFalse(ThemePreference.SYSTEM.isDark(false))
        assertTrue(ThemePreference.DARK.isDark(false))
        assertFalse(ThemePreference.LIGHT.isDark(true))
    }
    @Test fun missingOrUnknownSavedValuesUseSystem() {
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStored(null))
        assertEquals(ThemePreference.SYSTEM, ThemePreference.fromStored("unsupported"))
        ThemePreference.entries.forEach { assertEquals(it, ThemePreference.fromStored(it.name)) }
    }
}
