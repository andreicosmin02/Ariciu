package ro.andrei.ariciu.feature.locationpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ro.andrei.ariciu.feature.locationpicker.geometry.buildRadiusBoundary
import ro.andrei.ariciu.feature.locationpicker.map.toGeoJsonPolygon
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

private const val OPEN_FREE_MAP_POSITRON_STYLE_URL =
    "https://tiles.openfreemap.org/styles/positron"
private const val BRASOV_DEVELOPMENT_LATITUDE = 45.6579
private const val BRASOV_DEVELOPMENT_LONGITUDE = 25.6012
private const val BRASOV_DEVELOPMENT_ZOOM = 13.0

@Composable
fun LocationPickerScreen(
    onLocationSelected: (SelectedLocation) -> Unit,
    initialLocation: SelectedLocation? = null,
    onShowReminders: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onDeliverySetup: (() -> Unit)? = null,
    deliveryStatus: String? = null,
) {
    var selectedCoordinates by rememberSaveable(stateSaver = listSaver(
        save = { listOf(it.latitude, it.longitude) },
        restore = { SelectedCoordinates(it[0], it[1]) },
    )) {
        mutableStateOf(SelectedCoordinates(
            initialLocation?.latitude ?: BRASOV_DEVELOPMENT_LATITUDE,
            initialLocation?.longitude ?: BRASOV_DEVELOPMENT_LONGITUDE,
        ))
    }
    var radiusMeters by rememberSaveable {
        mutableIntStateOf(initialLocation?.radiusMeters ?: DEFAULT_RADIUS_METERS)
    }
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(
                latitude = selectedCoordinates.latitude,
                longitude = selectedCoordinates.longitude,
            ),
            zoom = BRASOV_DEVELOPMENT_ZOOM,
        )
    )
    val radiusColor = MaterialTheme.colorScheme.primary
    val radiusBoundary = remember(selectedCoordinates, radiusMeters) {
        buildRadiusBoundary(
            center = selectedCoordinates,
            radiusMeters = radiusMeters,
        )
    }
    val radiusPolygon = remember(radiusBoundary) { radiusBoundary.toGeoJsonPolygon() }

    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isCameraMoving }.collect { isCameraMoving ->
            if (!isCameraMoving) {
                val target = cameraState.position.target
                selectedCoordinates = SelectedCoordinates(
                    latitude = target.latitude,
                    longitude = target.longitude,
                )
            }
        }
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = if (onShowReminders == null) "Choose location" else "Ariciu",
                style = MaterialTheme.typography.headlineSmall
            )
            if (onShowReminders != null) {
                Text("Choose a place. We’ll help you remember.")
                Text("Map tiles need internet; saved reminders stay on this device.",
                    style = MaterialTheme.typography.bodySmall)
                FilledTonalButton(onClick = onShowReminders) { Text("Your reminders") }
            }
            if (onDeliverySetup != null) {
                Text(deliveryStatus ?: "Reminder delivery setup needed", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = onDeliverySetup) { Text("Delivery setup") }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (onShowReminders == null) 240.dp else 360.dp)
                    .testTag("location_picker_map_area"),
                contentAlignment = Alignment.Center
            ) {
                MaplibreMap(
                    modifier = Modifier.fillMaxSize(),
                    baseStyle = BaseStyle.Uri(OPEN_FREE_MAP_POSITRON_STYLE_URL),
                    cameraState = cameraState,
                ) {
                    val radiusSource = rememberGeoJsonSource(
                        data = GeoJsonData.Features(radiusPolygon),
                    )
                    FillLayer(
                        id = "reminder-radius-fill",
                        source = radiusSource,
                        color = const(radiusColor),
                        opacity = const(0.2f),
                    )
                    LineLayer(
                        id = "reminder-radius-outline",
                        source = radiusSource,
                        color = const(radiusColor),
                        width = const(2.dp),
                    )
                }
                CenterSelectionMarker(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            Text(
                text = selectedCoordinates.formatLatitude(),
                modifier = Modifier.testTag("location_picker_latitude"),
            )
            Text(
                text = selectedCoordinates.formatLongitude(),
                modifier = Modifier.testTag("location_picker_longitude"),
            )
            Text(
                text = formatRadiusMeters(radiusMeters),
                modifier = Modifier.testTag("location_picker_radius_value"),
            )
            Slider(
                value = radiusMeters.toFloat(),
                onValueChange = { radiusMeters = snapRadiusMeters(it) },
                valueRange = MIN_RADIUS_METERS.toFloat()..MAX_RADIUS_METERS.toFloat(),
                steps = RADIUS_SLIDER_STEPS,
                modifier = Modifier
                    .testTag("location_picker_radius_slider")
                    .semantics { contentDescription = "Reminder radius" },
            )
            Button(
                enabled = !cameraState.isCameraMoving,
                onClick = {
                    onLocationSelected(
                        SelectedLocation(
                            latitude = selectedCoordinates.latitude,
                            longitude = selectedCoordinates.longitude,
                            radiusMeters = radiusMeters,
                        )
                    )
                },
                modifier = Modifier.testTag("location_picker_confirm_button"),
            ) {
                Text("Use this location")
            }
            if (onCancel != null) {
                TextButton(onClick = onCancel) { Text("Back to reminder") }
            }
        }
    }
}

@Composable
private fun CenterSelectionMarker(modifier: Modifier = Modifier) {
    val outlineColor = MaterialTheme.colorScheme.surface
    val centerColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = modifier
            .size(32.dp)
            .testTag("location_picker_center_pin")
            .semantics { contentDescription = "Selected location" }
    ) {
        drawCircle(color = outlineColor, radius = size.minDimension / 2)
        drawCircle(color = centerColor, radius = size.minDimension / 4)
    }
}
