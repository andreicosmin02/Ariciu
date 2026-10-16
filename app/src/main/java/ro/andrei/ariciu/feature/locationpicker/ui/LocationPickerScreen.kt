package ro.andrei.ariciu.feature.locationpicker.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import ro.andrei.ariciu.feature.places.*
import ro.andrei.ariciu.feature.offlinemap.OfflineMaps
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
    val baseStyle = remember(dark) { BaseStyle.Uri(OfflineMaps.styleUri(dark)) }
    val scope = rememberCoroutineScope()
    val offlineMaps = remember(context) { OfflineMaps.get(context) }
    val offlineError by offlineMaps.error.collectAsState()
    val downloadedAreas by offlineMaps.areas.collectAsState()
    var mapFailed by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var downloadConfirm by remember { mutableStateOf(false) }
    var locationExplanation by remember { mutableStateOf(false) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    fun locate() {
        if (locating) return
        scope.launch {
            locating = true
            try {
                val result = context.currentMapPosition()
                if (result == null) message = "We couldn’t find your position. Try again outdoors, or search for a place."
                else cameraState.animateTo(CameraPosition(target = Position(latitude = result.latitude, longitude = result.longitude), zoom = 15.0))
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { message = "Position unavailable. Check phone Location and permissions, or use search." }
            finally { locating = false }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (context.hasMapLocationPermission()) locate() else message = "Location wasn’t allowed. Search or move the map, or allow it in app settings."
    }
    ro.andrei.ariciu.feature.offlinemap.PauseOfflineMapsWhenBackgrounded(offlineMaps)
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
            FlowRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { showSearch = true }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Search places") }
                TextButton(onClick = { if (context.hasMapLocationPermission()) locate() else locationExplanation = true },
                    enabled = !locating, modifier = Modifier.heightIn(min = 48.dp)) { Text(if (locating) "Finding you…" else "My location") }
            }
            Box(Modifier.fillMaxWidth().then(if (compact) Modifier.height(280.dp) else Modifier.weight(1f))
                .testTag("location_picker_map_area"), contentAlignment = Alignment.Center) {
                MaplibreMap(Modifier.fillMaxSize(), baseStyle = baseStyle,
                    cameraState = cameraState, onMapLoadFailed = { mapFailed = true }, onMapLoadFinished = { mapFailed = false },
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
                    Text(if (mapFailed) "Map unavailable here. Check internet or use a downloaded area." else "Move the map to choose your spot", Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                        downloadedAreas.lastOrNull()?.let { area -> Text(if (area.complete) "Offline ${area.name.substringAfter(" · ").lowercase(java.util.Locale.ROOT)} area saved. Manage in Settings." else if (area.active) "Downloading ${(area.progress * 100).toInt()}%" else "Download paused. Resume in Settings.", style = MaterialTheme.typography.bodySmall) }
                        TextButton(onClick = { downloadConfirm = true }) { Text("Download this area") }
                        Text("Download a small area for this theme; new areas need internet. Offline packs are in Settings.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    PrimaryAction("Use this place", onClick = { onLocationSelected(SelectedLocation(
                        selectedCoordinates.latitude, selectedCoordinates.longitude, radiusMeters)) },
                        enabled = !cameraState.isCameraMoving, modifier = Modifier.testTag("location_picker_confirm_button"))
                }
            }
        }
    }
    if (showSearch) PlaceSearchDialog(selectedCoordinates, onDismiss = { showSearch = false }, onSelect = { place ->
        showSearch = false
        scope.launch { cameraState.animateTo(CameraPosition(target = Position(latitude = place.latitude, longitude = place.longitude), zoom = 15.0)) }
    })
    if (locationExplanation) AlertDialog(onDismissRequest = { locationExplanation = false }, title = { Text("Find your location") },
        text = { Text("Allow location while using Ariciu to centre the map. Approximate location also works for the map. Background access is a separate step for reminders.") },
        confirmButton = { TextButton(onClick = {
            locationExplanation = false
            if (permissionAsked) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            else { permissionAsked = true; locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }
        }) { Text(if (permissionAsked) "Open settings" else "Allow location") } },
        dismissButton = { TextButton(onClick = { locationExplanation = false }) { Text("Not now") } })
    if (downloadConfirm) AlertDialog(onDismissRequest = { downloadConfirm = false }, title = { Text("Keep this area offline?") },
        text = { Text("Download about 4 × 4 km around your pin, for the ${if (dark) "dark" else "light"} map, at neighbourhood detail. Uses internet and up to 45 MB; up to three areas. Keep Create open while downloading. Manage or delete areas in Settings. Offline maps don’t guarantee offline location detection. Map requests are sent to OpenFreeMap and its CDN.") },
        confirmButton = { TextButton(onClick = { downloadConfirm = false; offlineMaps.download(selectedCoordinates.latitude, selectedCoordinates.longitude, dark); message = "Preparing the area. View progress or resume it in Settings → Offline maps." }) { Text("Download area") } },
        dismissButton = { TextButton(onClick = { downloadConfirm = false }) { Text("Cancel") } })
    offlineError?.let { error -> AlertDialog(onDismissRequest = { offlineMaps.error.value = null }, title = { Text("Offline maps") }, text = { Text(error) },
        confirmButton = { TextButton(onClick = { offlineMaps.error.value = null }) { Text("OK") } }) }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("Map") }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }) }

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

@Composable
private fun PlaceSearchDialog(centre: SelectedCoordinates, onDismiss: () -> Unit, onSelect: (CatalogPlace) -> Unit) {
    val context = LocalContext.current
    var query by rememberSaveable { mutableStateOf("") }
    var places by remember { mutableStateOf<List<CatalogPlace>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(context) { try { places = LocalPlaces.load(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { failed = true } }
    val matches by produceState(emptyList<CatalogPlace>(), query, places) {
        value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { searchPlaces(places.orEmpty(), query, centre.latitude, centre.longitude) }
    }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Find a place") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text("Town, store or store address") }, singleLine = true, modifier = Modifier.fillMaxWidth().testTag("place_search"))
            Text("Romania · offline · OpenStreetMap. Selected shops and towns, not every address. Dataset: 16 Oct 2026.", style = MaterialTheme.typography.bodySmall)
            when {
                failed -> Text("Could not open the local catalogue. You can still choose a pin on the map.")
                places == null -> Text("Opening local places…")
                query.isBlank() -> Text("Try Brașov or Kaufland Brașov.")
                matches.isEmpty() -> Text("No matches in this catalogue. Try a nearby town, then move the map.")
                else -> LazyColumn(Modifier.heightIn(max = 300.dp)) { items(matches, key = { it.id }) { place ->
                    TextButton(onClick = { onSelect(place) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(place.name, style = MaterialTheme.typography.titleMedium)
                            Text(place.address.ifBlank { "Town or village in Romania" }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
