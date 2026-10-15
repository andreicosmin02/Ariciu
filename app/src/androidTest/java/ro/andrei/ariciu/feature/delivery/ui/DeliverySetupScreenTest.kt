package ro.andrei.ariciu.feature.delivery.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import ro.andrei.ariciu.feature.delivery.model.DeliveryCapabilities
import ro.andrei.ariciu.ui.theme.AriciuTheme

class DeliverySetupScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun deniedPermissionsShowStatusAndExplanationWithoutOpeningASystemPrompt() {
        compose.setContent {
            AriciuTheme { DeliverySetupScreen(DeliveryCapabilities(false, false, true, true, false), {}) }
        }
        compose.onNodeWithText("Setup needs attention").assertIsDisplayed()
        compose.onNodeWithText("Precise location · Needed").assertIsDisplayed()
        compose.onAllNodesWithText("Set up")[0].performClick()
        compose.onNodeWithText("Enable reminder delivery").assertIsDisplayed()
        compose.onNodeWithText("Not now").performClick()
        compose.onNodeWithText("Setup needs attention").assertIsDisplayed()
    }
}
