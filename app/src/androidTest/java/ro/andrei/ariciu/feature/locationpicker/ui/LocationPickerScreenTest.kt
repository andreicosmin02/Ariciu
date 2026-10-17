package ro.andrei.ariciu.feature.locationpicker.ui

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
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
            ro.andrei.ariciu.ui.theme.AriciuTheme { LocationPickerScreen(onLocationSelected = { }) }
        }

        composeTestRule.onNodeWithText("Choose a place").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_map_area").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_center_pin").assertIsDisplayed()
        composeTestRule.onNodeWithTag("place_sheet_toggle").performClick()
        composeTestRule.onNodeWithText("Location details").performClick()
        composeTestRule.onNodeWithText("Show coordinates").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("location_picker_latitude").performScrollTo()
        composeTestRule.waitForIdle()
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        val capture = java.io.File(instrumentation.targetContext.filesDir, "review/review-coordinates.png").apply { parentFile!!.mkdirs() }
        instrumentation.uiAutomation.takeScreenshot().let { bitmap ->
            capture.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle()
        }
        composeTestRule.onNodeWithTag("location_picker_latitude").assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_longitude").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("location_picker_radius_value", useUnmergedTree = true).assertTextEquals("200 m")

        composeTestRule.onNodeWithText("More distances").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("location_picker_radius_slider")
            .assertIsDisplayed()
            .performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
                setProgress(500f)
            }

        composeTestRule.onNodeWithTag("location_picker_radius_value", useUnmergedTree = true).assertTextEquals("500 m")
    }

    @Test
    fun locationPickerScreen_emitsInitialLocationWhenConfirmed() {
        var selectedLocation: SelectedLocation? = null

        composeTestRule.setContent {
            ro.andrei.ariciu.ui.theme.AriciuTheme { LocationPickerScreen(onLocationSelected = { selectedLocation = it }) }
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
            ro.andrei.ariciu.ui.theme.AriciuTheme { LocationPickerScreen(onLocationSelected = { selectedLocation = it }) }
        }

        composeTestRule.onNodeWithTag("place_sheet_toggle").performClick()
        composeTestRule.onNodeWithText("More distances").performScrollTo().performClick()
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
