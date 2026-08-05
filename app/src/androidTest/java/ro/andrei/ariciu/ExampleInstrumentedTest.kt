package ro.andrei.ariciu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun locationPickerScreen_showsTitleAndMapArea() {
        composeTestRule.setContent {
            LocationPickerScreen()
        }

        composeTestRule.onNodeWithText("Choose location").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_map_area").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_center_pin").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_latitude").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_longitude").assertIsDisplayed()
    }
}
