package ro.andrei.ariciu.feature.locationpicker.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class LocationPickerScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun locationPickerScreen_showsTitleAndMapArea() {
        composeTestRule.setContent {
            LocationPickerScreen(onLocationSelected = { })
        }

        composeTestRule.onNodeWithText("Choose location").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_map_area").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_center_pin").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_latitude").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_longitude").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_radius_value").assertIsDisplayed()
        composeTestRule.onNodeWithText("Radius: 200 m").assertIsDisplayed()

        composeTestRule.onNodeWithTag("location_picker_radius_slider")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(500f)
            }

        composeTestRule.onNodeWithText("Radius: 500 m").assertIsDisplayed()
    }

    @Test
    fun locationPickerScreen_emitsInitialLocationWhenConfirmed() {
        var selectedLocation: SelectedLocation? = null

        composeTestRule.setContent {
            LocationPickerScreen(onLocationSelected = { selectedLocation = it })
        }

        composeTestRule.onNodeWithTag("location_picker_confirm_button")
            .assertIsDisplayed()
            .performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                SelectedLocation(
                    latitude = 45.6579,
                    longitude = 25.6012,
                    radiusMeters = 200,
                ),
                selectedLocation,
            )
        }
    }

    @Test
    fun locationPickerScreen_emitsCurrentRadiusWhenConfirmed() {
        var selectedLocation: SelectedLocation? = null

        composeTestRule.setContent {
            LocationPickerScreen(onLocationSelected = { selectedLocation = it })
        }

        composeTestRule.onNodeWithTag("location_picker_radius_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(500f)
            }
        composeTestRule.onNodeWithTag("location_picker_confirm_button").performClick()

        composeTestRule.runOnIdle {
            assertEquals(
                SelectedLocation(
                    latitude = 45.6579,
                    longitude = 25.6012,
                    radiusMeters = 500,
                ),
                selectedLocation,
            )
        }
    }
}
