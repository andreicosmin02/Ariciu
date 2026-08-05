package ro.andrei.ariciu

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
fun LocationPickerScreen() {
    val cameraState = rememberCameraState(
        firstPosition = CameraPosition(
            target = Position(
                latitude = BRASOV_DEVELOPMENT_LATITUDE,
                longitude = BRASOV_DEVELOPMENT_LONGITUDE,
            ),
            zoom = BRASOV_DEVELOPMENT_ZOOM,
        )
    )
    var selectedCoordinates by remember {
        mutableStateOf(
            SelectedCoordinates(
                latitude = BRASOV_DEVELOPMENT_LATITUDE,
                longitude = BRASOV_DEVELOPMENT_LONGITUDE,
            )
        )
    }
    var radiusMeters by remember { mutableIntStateOf(DEFAULT_RADIUS_METERS) }
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
                .padding(16.dp)
        ) {
            Text(
                text = "Choose location",
                style = MaterialTheme.typography.headlineSmall
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
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
