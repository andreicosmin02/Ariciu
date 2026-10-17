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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    onLocationSelected: (SelectedLocation) -> Unit,
    initialLocation: SelectedLocation? = null,
    onCancel: (() -> Unit)? = null,
    chainName: String? = null,
    onChainSelected: (String?) -> Unit = {},
) {
    var selectedCoordinates by rememberSaveable(stateSaver = listSaver(
        save = { listOf(it.latitude, it.longitude) }, restore = { SelectedCoordinates(it[0], it[1]) },
    )) { mutableStateOf(SelectedCoordinates(initialLocation?.latitude ?: 45.6579, initialLocation?.longitude ?: 25.6012)) }
    var radiusMeters by rememberSaveable { mutableIntStateOf(initialLocation?.radiusMeters ?: DEFAULT_RADIUS_METERS) }
    var moreRadius by rememberSaveable { mutableStateOf(false) }
    var coordinatesVisible by rememberSaveable { mutableStateOf(false) }
    var preview by remember { mutableStateOf(false) }
    var choosingChain by rememberSaveable { mutableStateOf(false) }
    var catalog by remember { mutableStateOf<List<CatalogPlace>>(emptyList()) }
    var catalogFailed by remember { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    val cameraState = rememberCameraState(firstPosition = CameraPosition(
        target = Position(latitude = selectedCoordinates.latitude, longitude = selectedCoordinates.longitude), zoom = 14.0))
    val colors = MaterialTheme.colorScheme
    val boundary = remember(selectedCoordinates, radiusMeters) { buildRadiusBoundary(selectedCoordinates, radiusMeters).toGeoJsonPolygon() }
    val dark = isAriciuDarkTheme()
    val context = LocalContext.current
    val baseStyle = remember(dark) { BaseStyle.Uri(OfflineMaps.styleUri(dark)) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(context) { try { catalog = LocalPlaces.load(context) } catch (e: CancellationException) { throw e } catch (_: Exception) { catalogFailed = true } }
    val nearbyStores = remember(catalog, chainName, selectedCoordinates) {
        if (chainName == null) emptyList() else chainPlaces(catalog, chainName, selectedCoordinates.latitude, selectedCoordinates.longitude)
    }
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    val density = LocalDensity.current
    var sheetPeek by remember { mutableStateOf(88.dp) }
    val sheetScaffold = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    LaunchedEffect(details, moreRadius, coordinatesVisible) {
        if (details || moreRadius || coordinatesVisible) {
            withFrameNanos { }
            sheetState.expand()
        }
    }
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
    if (preview && chainName != null) {
        ro.andrei.ariciu.feature.places.StorePreviewScreen(
            nearbyStores.map { ro.andrei.ariciu.feature.delivery.model.ChainArea(it.id, it.latitude, it.longitude) }, catalog,
            SelectedLocation(selectedCoordinates.latitude, selectedCoordinates.longitude, radiusMeters), { preview = false })
        return
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxHeight < 600.dp && density.fontScale > 1.3f || maxHeight < 400.dp
    val compactMapHeight = (maxHeight - 88.dp).coerceIn(180.dp,300.dp)
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ariciu", style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                Text("Choose a place", style = MaterialTheme.typography.titleLarge)
            }
            if (onCancel != null) TextButton(onClick = onCancel) { Text("Back") }
        }
        Row(Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.CenterHorizontally).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { showSearch = true }, modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                shape = MaterialTheme.shapes.medium, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
                Text("Search places", modifier = Modifier.weight(1f))
                Icon(AriciuIcons.Search, null, Modifier.size(20.dp))
            }
            FilledTonalIconButton(onClick = { if (context.hasMapLocationPermission()) locate() else locationExplanation = true },
                enabled = !locating, modifier = Modifier.size(52.dp)) {
                if (locating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(AriciuIcons.Locate, "My location", Modifier.size(24.dp))
            }
        }
        FlowRow(Modifier.fillMaxWidth().padding(horizontal = 20.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(chainName == null, { onChainSelected(null) }, label = { Text("Single place") }, modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(chainName != null, { choosingChain = true }, label = { Text(chainName ?: "Supermarket chain") }, modifier = Modifier.heightIn(min = 48.dp))
        }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(if (compact) Modifier.height(compactMapHeight) else Modifier.weight(1f)) {
        val sheetBodyHeight = (maxHeight - sheetPeek).coerceAtLeast(48.dp)
        BottomSheetScaffold(modifier = Modifier.fillMaxSize(), scaffoldState = sheetScaffold,
            sheetPeekHeight = sheetPeek, sheetContainerColor = colors.surface,
            sheetDragHandle = null, sheetContent = {
                Column(Modifier.fillMaxWidth().onSizeChanged { sheetPeek = with(density) { it.height.toDp() } }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(Modifier.padding(top = 8.dp).size(width = 32.dp, height = 4.dp), shape = MaterialTheme.shapes.small, color = colors.outlineVariant) {}
                    TextButton(onClick = { scope.launch { if (sheetState.currentValue == SheetValue.Expanded) sheetState.partialExpand() else sheetState.expand() } },
                        modifier = Modifier.fillMaxWidth().testTag("place_sheet_toggle").semantics { stateDescription = if (sheetState.currentValue == SheetValue.Expanded) "Expanded" else "Collapsed" }, contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text(if (compact) "Options" else if (chainName == null) "Around this spot" else "Around selected stores", style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
                            if (!compact) Text(if (sheetState.currentValue == SheetValue.Expanded) "Collapse place options" else "Expand place options", style = MaterialTheme.typography.bodySmall)
                        }
                        Text("$radiusMeters m", style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("location_picker_radius_value"))
                    }
                }
                Column(Modifier.fillMaxWidth().heightIn(max = sheetBodyHeight).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100, 200, 500).forEach { radius ->
                            FilterChip(selected = radiusMeters == radius, onClick = { radiusMeters = radius },
                                leadingIcon = if (radiusMeters == radius) ({ Icon(AriciuIcons.Check, null, Modifier.size(16.dp)) }) else null,
                                label = { Text("$radius m") }, modifier = (if (density.fontScale > 1.3f) Modifier.fillMaxWidth() else Modifier.weight(1f)).heightIn(min = 48.dp), shape = MaterialTheme.shapes.small)
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { moreRadius = !moreRadius; scope.launch { sheetState.expand() } }) { Text(if (moreRadius) "Less" else "More distances") }
                        TextButton(onClick = { details = !details; scope.launch { sheetState.expand() } }) { Text("Location details") }
                    }
                    if (moreRadius) Slider(value = radiusMeters.toFloat(), onValueChange = { radiusMeters = snapRadiusMeters(it) },
                        valueRange = MIN_RADIUS_METERS.toFloat()..MAX_RADIUS_METERS.toFloat(), steps = RADIUS_SLIDER_STEPS,
                        modifier = Modifier.testTag("location_picker_radius_slider").semantics { contentDescription = "Reminder radius in metres" })
                    if (chainName != null) {
                        Text("${nearbyStores.size} listed stores within 25 km of this pin. Move the map to change the area. At most 40 stores; coverage may be incomplete.", style = MaterialTheme.typography.bodySmall)
                        if (nearbyStores.isEmpty()) Text(if (catalogFailed) "Catalogue unavailable. Try a single place." else "No nearby stores. Move the map or choose another chain.", color = colors.error)
                        TextButton(onClick = { preview = true }, enabled = nearbyStores.isNotEmpty()) { Text("Preview stores on map") }

                    }
                    if (details) Surface(color = colors.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Keep the map available offline", style = MaterialTheme.typography.titleSmall)
                            Text("Saves roads, labels and map detail for about 4 × 4 km in this theme. It does not add stores to search or change which places trigger reminders. Search is already offline. Other areas and zoom levels may need internet.", style = MaterialTheme.typography.bodySmall)
                            downloadedAreas.lastOrNull()?.let { area -> Text(if (area.complete) "Map area saved. Manage in Settings → Offline maps." else if (area.active) "Downloading ${(area.progress * 100).toInt()}%" else "Download paused. Resume in Settings → Offline maps.", style = MaterialTheme.typography.bodySmall) }
                            TextButton(onClick = { downloadConfirm = true }) { Text("Download this area") }
                            TextButton(onClick = { coordinatesVisible = !coordinatesVisible }) { Text(if (coordinatesVisible) "Hide coordinates" else "Show coordinates") }
                            if (coordinatesVisible) {
                                Text(selectedCoordinates.formatLatitude(), Modifier.testTag("location_picker_latitude"), style = MaterialTheme.typography.bodySmall)
                                Text(selectedCoordinates.formatLongitude(), Modifier.testTag("location_picker_longitude"), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }) { _ ->
            Box(Modifier.fillMaxSize().padding(bottom = sheetPeek)
                .testTag("location_picker_map_area"), contentAlignment = Alignment.Center) {
                MaplibreMap(Modifier.fillMaxSize(), baseStyle = baseStyle,
                    cameraState = cameraState, onMapLoadFailed = { mapFailed = true }, onMapLoadFinished = { mapFailed = false },
                    options = MapOptions(ornamentOptions = OrnamentOptions(isLogoEnabled = false, isScaleBarEnabled = false))) {
                    val source = rememberGeoJsonSource(data = GeoJsonData.Features(boundary))
                    FillLayer(id = "reminder-radius-fill", source = source, color = const(colors.primary), opacity = const(0.14f))
                    LineLayer(id = "reminder-radius-outline", source = source, color = const(colors.primary), width = const(2.dp))
                    nearbyStores.forEach { place ->
                        val storeBoundary = remember(place, radiusMeters) { buildRadiusBoundary(SelectedCoordinates(place.latitude, place.longitude), radiusMeters).toGeoJsonPolygon() }
                        val storeSource = rememberGeoJsonSource(data = GeoJsonData.Features(storeBoundary))
                        FillLayer(id = "store-${place.id}", source = storeSource, color = const(colors.primary), opacity = const(0.4f))
                        LineLayer(id = "store-outline-${place.id}", source = storeSource, color = const(colors.primary), width = const(2.dp))
                    }
                }
                CenterSelectionMarker()
                Surface(Modifier.align(Alignment.BottomStart).padding(12.dp).widthIn(max = 300.dp),
                    shape = MaterialTheme.shapes.extraSmall, color = colors.surface) {
                    Text("© OpenMapTiles · © OpenStreetMap", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                if (!compact) Surface(Modifier.align(Alignment.TopCenter).padding(16.dp), shape = MaterialTheme.shapes.small,
                    color = colors.surface, shadowElevation = 2.dp) {
                    Text(if (mapFailed) "Map unavailable here. Check internet or use a downloaded area." else "Move the map to choose your spot", Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        }
        }
        Surface(color = colors.surface) {
            PrimaryAction(if (chainName == null) "Use this place" else "Use these stores", onClick = {
                onLocationSelected(SelectedLocation(selectedCoordinates.latitude, selectedCoordinates.longitude, radiusMeters))
            }, enabled = !cameraState.isCameraMoving && (chainName == null || nearbyStores.isNotEmpty()),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp).testTag("location_picker_confirm_button"))
        }
    }
    }
    if (choosingChain) AlertDialog(onDismissRequest = { choosingChain = false }, title = { Text("Choose a supermarket chain") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Text("Up to 40 listed stores within 25 km of your pin. Romania catalogue; missing stores are possible. Review the map before saving.", style = MaterialTheme.typography.bodyMedium)
            if (catalogFailed) Text("Could not load stores. A single place still works.")
            catalog.map { it.chain }.filter { it.isNotBlank() }.distinct().sorted().forEach { chain ->
                TextButton(onClick = { onChainSelected(chain); choosingChain = false }, modifier = Modifier.fillMaxWidth()) { Text(chain) }
            }
        }
    }, confirmButton = { TextButton(onClick = { choosingChain = false }) { Text("Close") } })
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
        text = { Text("Download about 4 × 4 km around your pin, for the ${if (dark) "dark" else "light"} map, at neighbourhood detail. Saves the basemap (roads and labels), not extra searchable shops or reminder coverage. Search already works offline. Uses internet and up to 45 MB; up to three areas. Keep Create open while downloading. Manage or delete areas in Settings. Offline maps don’t guarantee offline location detection. Map requests are sent to OpenFreeMap and its CDN.") },
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
