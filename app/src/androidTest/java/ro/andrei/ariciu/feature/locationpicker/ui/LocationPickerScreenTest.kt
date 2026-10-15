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
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation

private const val COORDINATE_TOLERANCE = 1e-6

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

        composeTestRule.onNodeWithText("Choose a place").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_map_area").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_center_pin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Location details").performClick()
        composeTestRule.onNodeWithTag("location_picker_latitude").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_longitude").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_radius_value").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_radius_value").assertIsDisplayed()

        composeTestRule.onNodeWithText("More distances").performClick()
        composeTestRule.onNodeWithTag("location_picker_radius_slider")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(500f)
            }

        composeTestRule.onNodeWithTag("location_picker_radius_value").assertIsDisplayed()
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
            assertSelectedLocationEquals(
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

        composeTestRule.onNodeWithText("More distances").performClick()
        composeTestRule.onNodeWithTag("location_picker_radius_slider")
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(500f)
            }
        composeTestRule.onNodeWithTag("location_picker_confirm_button").performClick()

        composeTestRule.runOnIdle {
            assertSelectedLocationEquals(
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

private fun assertSelectedLocationEquals(
    expected: SelectedLocation,
    actual: SelectedLocation?,
) {
    assertNotNull(actual)
    val actualLocation = actual ?: return

    assertEquals(expected.latitude, actualLocation.latitude, COORDINATE_TOLERANCE)
    assertEquals(expected.longitude, actualLocation.longitude, COORDINATE_TOLERANCE)
    assertEquals(expected.radiusMeters, actualLocation.radiusMeters)
}
