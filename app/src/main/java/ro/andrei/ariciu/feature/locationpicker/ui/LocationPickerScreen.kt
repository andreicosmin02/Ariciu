package ro.andrei.ariciu.feature.locationpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ro.andrei.ariciu.feature.locationpicker.geometry.buildRadiusBoundary
import ro.andrei.ariciu.feature.locationpicker.map.toGeoJsonPolygon
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.ui.components.*
import ro.andrei.ariciu.ui.theme.isAriciuDarkTheme
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LocationPickerScreen(
    onLocationSelected: (SelectedLocation) -> Unit,
    initialLocation: SelectedLocation? = null,
    onCancel: (() -> Unit)? = null,
) {
    var selectedCoordinates by rememberSaveable(stateSaver = listSaver(
        save = { listOf(it.latitude, it.longitude) }, restore = { SelectedCoordinates(it[0], it[1]) },
    )) { mutableStateOf(SelectedCoordinates(initialLocation?.latitude ?: 45.6579, initialLocation?.longitude ?: 25.6012)) }
    var radiusMeters by rememberSaveable { mutableIntStateOf(initialLocation?.radiusMeters ?: DEFAULT_RADIUS_METERS) }
    var moreRadius by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    val cameraState = rememberCameraState(firstPosition = CameraPosition(
        target = Position(latitude = selectedCoordinates.latitude, longitude = selectedCoordinates.longitude), zoom = 14.0))
    val colors = MaterialTheme.colorScheme
    val boundary = remember(selectedCoordinates, radiusMeters) { buildRadiusBoundary(selectedCoordinates, radiusMeters).toGeoJsonPolygon() }
    val dark = isAriciuDarkTheme()
    val context = LocalContext.current
    val baseStyle = remember(dark, context) {
        if (dark) BaseStyle.Json(context.assets.open("map/ariciu-graphite.json").bufferedReader().use { it.readText() })
        else BaseStyle.Uri("https://tiles.openfreemap.org/styles/positron")
    }
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isCameraMoving }.collect { moving ->
            if (!moving) selectedCoordinates = SelectedCoordinates(cameraState.position.target.latitude, cameraState.position.target.longitude)
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 620.dp || LocalDensity.current.fontScale > 1.3f
        Column(Modifier.fillMaxSize().then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Ariciu", style = MaterialTheme.typography.titleMedium, color = colors.onSurfaceVariant)
                    Text("Choose a place", style = MaterialTheme.typography.headlineMedium)
                }
                if (onCancel != null) TextButton(onClick = onCancel) { Text("Back") }
            }
            Box(Modifier.fillMaxWidth().then(if (compact) Modifier.height(280.dp) else Modifier.weight(1f))
                .testTag("location_picker_map_area"), contentAlignment = Alignment.Center) {
                MaplibreMap(Modifier.fillMaxSize(), baseStyle = baseStyle,
                    cameraState = cameraState,
                    options = MapOptions(ornamentOptions = OrnamentOptions(isLogoEnabled = false, isScaleBarEnabled = false))) {
                    val source = rememberGeoJsonSource(data = GeoJsonData.Features(boundary))
                    FillLayer(id = "reminder-radius-fill", source = source, color = const(colors.primary), opacity = const(0.14f))
                    LineLayer(id = "reminder-radius-outline", source = source, color = const(colors.primary), width = const(2.dp))
                }
                CenterSelectionMarker()
                Surface(Modifier.align(Alignment.BottomStart).padding(12.dp).widthIn(max = 300.dp),
                    shape = MaterialTheme.shapes.extraSmall, color = colors.surface) {
                    Text("© OpenMapTiles · © OpenStreetMap", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                Surface(Modifier.align(Alignment.TopCenter).padding(16.dp), shape = MaterialTheme.shapes.small,
                    color = colors.surface, shadowElevation = 2.dp) {
                    Text("Move the map to choose your spot", Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = colors.surface, shadowElevation = 6.dp) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Around this spot", style = MaterialTheme.typography.titleLarge)
                        Text("$radiusMeters m", style = MaterialTheme.typography.titleMedium, color = colors.primary,
                            modifier = Modifier.testTag("location_picker_radius_value"))
                    }
                    Text("How close should you be?", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100, 200, 500).forEach { radius ->
                            FilterChip(selected = radiusMeters == radius, onClick = { radiusMeters = radius },
                                leadingIcon = if (radiusMeters == radius) ({ Icon(AriciuIcons.Check, null, Modifier.size(16.dp)) }) else null,
                                label = { Text("$radius m") }, modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                                shape = MaterialTheme.shapes.small,
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = colors.primaryContainer,
                                    selectedLabelColor = colors.onPrimaryContainer))
                        }
                    }
                    if (moreRadius) Slider(value = radiusMeters.toFloat(), onValueChange = { radiusMeters = snapRadiusMeters(it) },
                        valueRange = MIN_RADIUS_METERS.toFloat()..MAX_RADIUS_METERS.toFloat(), steps = RADIUS_SLIDER_STEPS,
                        modifier = Modifier.testTag("location_picker_radius_slider").semantics { contentDescription = "Reminder radius in metres" })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { moreRadius = !moreRadius }, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (moreRadius) "Less" else "More distances") }
                        TextButton(onClick = { details = !details }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Location details") }
                    }
                    if (details) {
                        Text(selectedCoordinates.formatLatitude(), Modifier.testTag("location_picker_latitude"), style = MaterialTheme.typography.bodyMedium)
                        Text(selectedCoordinates.formatLongitude(), Modifier.testTag("location_picker_longitude"), style = MaterialTheme.typography.bodyMedium)
                        Text("Maps need internet for new areas.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    PrimaryAction("Use this place", onClick = { onLocationSelected(SelectedLocation(
                        selectedCoordinates.latitude, selectedCoordinates.longitude, radiusMeters)) },
                        enabled = !cameraState.isCameraMoving, modifier = Modifier.testTag("location_picker_confirm_button"))
                }
            }
        }
    }
}

@Composable
private fun CenterSelectionMarker() {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.size(32.dp).testTag("location_picker_center_pin").semantics { contentDescription = "Selected place at the centre of the map" }) {
        drawCircle(color = colors.surface, radius = size.minDimension / 2)
        drawCircle(color = colors.primary, radius = size.minDimension / 3)
        drawCircle(color = colors.onPrimary, radius = size.minDimension / 9)
    }
}
