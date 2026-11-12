package ro.andrei.arichu.feature.delivery.ui

import androidx.compose.ui.test.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ro.andrei.arichu.feature.delivery.model.DeliveryCapabilities
import ro.andrei.arichu.ui.theme.ArichuTheme

class DeliverySetupScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deniedPermissionsShowStatusAndExplanationWithoutOpeningASystemPrompt() {
        compose.setContent {
            ArichuTheme { DeliverySetupScreen(DeliveryCapabilities(false, false, true, true, false), {}) }
        }
        compose.onNodeWithText("A few things to set up").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Your location").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("setup_Your location").performScrollTo().performClick()
        compose.onNodeWithText("Let Arichu remind you").assertIsDisplayed()
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithText("A few things to set up").performScrollTo().assertIsDisplayed()
    }
    @Test fun readyStatusIsBesideItsActionAndCenteredAtNormalAndLargeText() {
        var large by androidx.compose.runtime.mutableStateOf(false)
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalDensity provides
                androidx.compose.ui.unit.Density(density.density, if (large) 2f else 1f)) {
                ArichuTheme { DeliverySetupScreen(DeliveryCapabilities(true,true,true,true,true), {}) }
            }
        }
        fun verify() {
            compose.onNodeWithTag("setup_Your location").performScrollTo().assertIsDisplayed()
            val icon = compose.onNodeWithTag("setup_status_Your location", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            val action = compose.onNodeWithTag("setup_Your location").fetchSemanticsNode().boundsInRoot
            org.junit.Assert.assertTrue(icon.right <= action.left)
            org.junit.Assert.assertEquals(icon.center.y, action.center.y, 1f)
            compose.onNodeWithTag("setup_Your location").assertContentDescriptionEquals("Your location. Review")
        }
        verify(); compose.runOnIdle { large = true }; verify()
    }

}
