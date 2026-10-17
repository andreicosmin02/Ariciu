package ro.andrei.ariciu.ui.theme

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class ThemePreferencesTest {
    @Test fun selectedAppearanceIsRestoredByANewStoreInstance() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-appearance-${UUID.randomUUID()}"
        try {
            val preferences = ThemePreferences(context, name)
            assertEquals(ThemePreference.SYSTEM, preferences.preference.value)
            ThemePreference.entries.forEach {
                preferences.select(it)
                assertEquals(it, ThemePreferences(context, name).preference.value)
            }
        } finally { context.deleteSharedPreferences(name) }
    }
}
