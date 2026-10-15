package ro.andrei.ariciu.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteContrastTest {
    private fun ratio(a: Color, b: Color): Double {
        val values = listOf(a.luminance().toDouble(), b.luminance().toDouble()).sorted()
        return (values[1] + 0.05) / (values[0] + 0.05)
    }
    @Test fun bothPalettesHaveReadableTextContrast() {
        listOf(DarkColorScheme, LightColorScheme).forEach { c ->
            listOf(c.onSurface to c.surface, c.onSurfaceVariant to c.surface,
                c.onBackground to c.background, c.onPrimary to c.primary,
                c.onPrimaryContainer to c.primaryContainer, c.onTertiaryContainer to c.tertiaryContainer,
                c.onErrorContainer to c.errorContainer).forEach { (text, background) ->
                assertTrue("Contrast must be at least 4.5:1: ${ratio(text, background)}", ratio(text, background) >= 4.5)
            }
        }
    }
}
