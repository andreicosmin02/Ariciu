package ro.andrei.ariciu.feature.locationpicker.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import ro.andrei.ariciu.feature.delivery.model.ChainArea
import ro.andrei.ariciu.feature.delivery.model.geofenceBlockMessage
import ro.andrei.ariciu.feature.delivery.model.geofenceBudgetText
import ro.andrei.ariciu.feature.delivery.model.geofenceFits
import ro.andrei.ariciu.feature.locationpicker.geometry.buildRadiusBoundary
import ro.andrei.ariciu.feature.locationpicker.map.toGeoJsonPolygon
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.offlinemap.*
import ro.andrei.ariciu.feature.places.*
import ro.andrei.ariciu.ui.components.*
import ro.andrei.ariciu.ui.theme.isAriciuDarkTheme
import kotlin.time.Duration.Companion.milliseconds

/**
 * Two modes. MANUAL: the centre crosshair is the selection. RESULTS: a search produced result pins and the selection is the
 * selected pin. Long-press or clearing the search returns to MANUAL.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    onLocationSelected: (SelectedLocation) -> Unit,
    initialLocation: SelectedLocation? = null,
    onCancel: (() -> Unit)? = null,
    geofenceAreasUsed: Int? = null,
    replacingAreas: Int = 0,
    onPlaceSelected: (SelectedLocation, String) -> Unit = { location, _ -> onLocationSelected(location) },
    onPlacesSelected: (SelectedLocation, String, List<ChainArea>) -> Unit = { location, _, _ -> onLocationSelected(location) },
) {
    var selectedCoordinates by rememberSaveable(stateSaver = listSaver(
        save = { listOf(it.latitude, it.longitude) }, restore = { SelectedCoordinates(it[0], it[1]) },
    )) { mutableStateOf(SelectedCoordinates(initialLocation?.latitude ?: 45.6579, initialLocation?.longitude ?: 25.6012)) }
    var radiusMeters by rememberSaveable { mutableIntStateOf(initialLocation?.radiusMeters ?: DEFAULT_RADIUS_METERS) }
    var moreRadius by rememberSaveable { mutableStateOf(false) }
    var coordinatesVisible by rememberSaveable { mutableStateOf(false) }
    var details by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var chipKeys by rememberSaveable { mutableStateOf("") }
    var searchRef by rememberSaveable { mutableStateOf<DoubleArray?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var results by remember { mutableStateOf<List<CatalogPlace>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchMessage by remember { mutableStateOf<String?>(null) }
    var lastSearchCentre by remember { mutableStateOf<SelectedCoordinates?>(null) }
    var moved by remember { mutableStateOf(false) }
    var animating by remember { mutableStateOf(false) }
    var userFix by remember { mutableStateOf<UserFix?>(null) }
    var nearby by remember { mutableStateOf<List<CatalogPlace>>(emptyList()) }
    val cameraState = rememberCameraState(firstPosition = CameraPosition(
        target = Position(latitude = selectedCoordinates.latitude, longitude = selectedCoordinates.longitude), zoom = 14.0))
    val colors = MaterialTheme.colorScheme
    val dark = isAriciuDarkTheme()
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val index = remember(context) { LocalPlaces.index(context) }
    val baseStyle = remember(dark) { BaseStyle.Uri(OfflineMaps.styleUri(dark)) }
    val scope = rememberCoroutineScope()
    val chips = remember(chipKeys) { PlaceCategory.entries.filter { it.key in chipKeys.split(',') }.toSet() }
    val selectedPlace = results.firstOrNull { it.id == selectedId }
    val resultsMode = results.isNotEmpty()
    val resultsModeNow by rememberUpdatedState(resultsMode)
    val selection = selectedPlace?.let { SelectedCoordinates(it.latitude, it.longitude) } ?: selectedCoordinates
    val boundary = remember(selection, radiusMeters) { buildRadiusBoundary(selection, radiusMeters).toGeoJsonPolygon() }
    val pinsJson = remember(results, selectedId) { placesGeoJson(results.filter { it.id != selectedId }) }
    val inRange = remember(results, searchRef) {
        val ref = searchRef
        if (ref == null) results else results.filter { distanceMeters(ref[0], ref[1], it.latitude, it.longitude) <= SEARCH_RADIUS_METERS }
    }
    val nearest = remember(nearby, selection) { nearestPlace(nearby, selection.latitude, selection.longitude) }
    val density = LocalDensity.current
    val sheetState = rememberStandardBottomSheetState(initialValue = SheetValue.PartiallyExpanded)
    var sheetPeek by remember { mutableStateOf(88.dp) }
    val sheetScaffold = rememberBottomSheetScaffoldState(bottomSheetState = sheetState)
    val listState = rememberLazyListState()
    val offlineMaps = remember(context) { OfflineMaps.get(context) }
    val offlineError by offlineMaps.error.collectAsState()
    val downloadedAreas by offlineMaps.areas.collectAsState()
    var mapFailed by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var downloadUi by remember { mutableStateOf<DownloadUi>(DownloadUi.Idle) }
    var covered by remember { mutableStateOf(false) }
    var cameraBeforePlan by remember { mutableStateOf<CameraPosition?>(null) }
    val currentPackId by offlineMaps.currentId.collectAsState()
    var locationExplanation by remember { mutableStateOf(false) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    fun centre() = cameraState.position.target.let { SelectedCoordinates(it.latitude, it.longitude) }
    fun fly(target: CameraPosition, offerSearch: Boolean = false) {
        scope.launch {
            animating = true
            try { withContext(Dispatchers.Main) { cameraState.animateTo(target, 600.milliseconds) } } finally { animating = false; moved = offerSearch; if (!offerSearch) lastSearchCentre = centre() }
        }
    }
    fun fitTo(found: List<CatalogPlace>, ref: SelectedCoordinates) {
        scope.launch {
            animating = true
            try {
                val local = found.filter { distanceMeters(ref.latitude, ref.longitude, it.latitude, it.longitude) <= SEARCH_RADIUS_METERS }
                val b = fitBounds(local.ifEmpty { found }, ref.latitude, ref.longitude)
                withContext(Dispatchers.Main) {
                    if (b == null || (b.north - b.south < 0.0006 && b.east - b.west < 0.0006)) {
                        val one = found.first()
                        cameraState.animateTo(CameraPosition(target = Position(one.longitude, one.latitude), zoom = 16.0), 600.milliseconds)
                    } else cameraState.animateTo(BoundingBox(b.west, b.south, b.east, b.north),
                        padding = PaddingValues(horizontal = 40.dp, vertical = 72.dp), duration = 600.milliseconds)
                }
            } finally { animating = false; lastSearchCentre = centre(); moved = false }
        }
    }
    fun clearSearch(keepSelection: Boolean) {
        searchJob?.cancel()
        if (keepSelection) selectedCoordinates = selection
        query = ""; chipKeys = ""; results = emptyList(); selectedId = null; searchRef = null; searchMessage = null; moved = false; searching = false
    }
    fun runSearch(ref: SelectedCoordinates, text: String, categories: Set<PlaceCategory>, snap: Boolean = true) {
        if (text.isBlank() && categories.isEmpty()) { clearSearch(keepSelection = false); return }
        searchJob?.cancel()
        lastSearchCentre = centre(); moved = false
        searchJob = scope.launch {
            searching = true; searchMessage = null
            try {
                val t0 = System.nanoTime()
                val near = index.near(ref.latitude, ref.longitude, SEARCH_RADIUS_METERS)
                val towns = if (text.isNotBlank()) index.towns() else emptyList()
                val t1 = System.nanoTime()
                val found = withContext(Dispatchers.Default) { searchCatalog(near, text, categories, ref.latitude, ref.longitude, towns = towns) }
                android.util.Log.i("AriciuMap", "search '$text' ${categories.size} chips: ${near.size} candidates, ${found.size} results; load ${(t1 - t0) / 1_000_000} ms, rank ${(System.nanoTime() - t1) / 1_000_000} ms")
                searchRef = doubleArrayOf(ref.latitude, ref.longitude)
                if (found.isEmpty()) {
                    results = emptyList(); selectedId = null
                    searchMessage = "No places found within 25 km. Try another word, or move the map and search this area."
                } else {
                    results = found
                    if (snap) { selectedId = nearestResult(found, ref.latitude, ref.longitude)?.id; fitTo(found, ref) }
                    sheetState.partialExpand()
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { searchMessage = "Search failed. The place catalogue could not be read. You can still move the map." }
            finally { searching = false }
        }
    }
    fun reference() = userFix?.position ?: centre()
    fun select(place: CatalogPlace, fly: Boolean) {
        selectedId = place.id
        if (fly) fly(CameraPosition(target = Position(place.longitude, place.latitude), zoom = maxOf(cameraState.position.zoom, 15.0)))
        scope.launch { results.indexOfFirst { it.id == place.id }.takeIf { it >= 0 }?.let { listState.animateScrollToItem(it + 3 + (if (moreRadius) 1 else 0) + (if (geofenceAreasUsed != null) 1 else 0)) } }
    }
    fun dropManualPin(at: Position) {
        clearSearch(keepSelection = false)
        selectedCoordinates = SelectedCoordinates(at.latitude, at.longitude)
        fly(CameraPosition(target = at, zoom = maxOf(cameraState.position.zoom, 15.0)))
    }
    fun locate() {
        if (locating) return
        scope.launch {
            locating = true
            try {
                val fix = context.currentMapFix()
                if (fix == null) message = "We couldn’t find your position. Try again outdoors, or search for a place."
                else {
                    userFix = fix
                    // With an active search, offer to search around the new spot instead of silently keeping old results.
                    fly(CameraPosition(target = Position(latitude = fix.position.latitude, longitude = fix.position.longitude), zoom = 15.0),
                        offerSearch = resultsMode || query.isNotBlank() || chips.isNotEmpty())
                }
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) { message = "Position unavailable. Check phone Location and permissions, or use search." }
            finally { locating = false }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (context.hasMapLocationPermission()) locate() else message = "Location wasn’t allowed. Search or move the map, or allow it in app settings."
    }
    fun requestLocate() { if (context.hasMapLocationPermission()) locate() else locationExplanation = true }
    ro.andrei.ariciu.feature.offlinemap.PauseOfflineMapsWhenBackgrounded(offlineMaps)
    LaunchedEffect(Unit) { searchRef?.let { ref -> runSearch(SelectedCoordinates(ref[0], ref[1]), query, chips, snap = false) } }
    val offlineRegions = remember(downloadedAreas) { downloadedAreas.mapNotNull { it.toRegion() } }
    val regionsNow by rememberUpdatedState(offlineRegions)
    val darkNow by rememberUpdatedState(dark)
    fun viewportBounds(): GeoBounds? = cameraState.projection?.queryVisibleBoundingBox()?.let { GeoBounds(it.south, it.west, it.north, it.east) }
    fun recomputeCovered() { val v = viewportBounds(); covered = v != null && viewportCovered(v, cameraState.position.zoom, darkNow, regionsNow) }
    fun openDownload() {
        val v = viewportBounds() ?: return
        if (covered) { downloadUi = DownloadUi.Covered; return }
        val stats = downloadedAreas.map { it.toStats() }
        val plan = planDownload(v, averageBytesPerTile(stats), measuredOverheadBytes(stats))
        downloadUi = DownloadUi.Plan(plan)
        // Zoom in or out so the whole box to be saved is visible; Cancel returns to the previous view.
        if (plan is DownloadPlan.Ready) {
            cameraBeforePlan = cameraState.position
            val b = plan.bounds
            scope.launch {
                animating = true
                kotlinx.coroutines.delay(350) // let the panel take its place so the box is fitted to the map that is left
                try { withContext(Dispatchers.Main) { cameraState.animateTo(BoundingBox(b.west, b.south, b.east, b.north),
                    padding = PaddingValues(start = 24.dp, end = 96.dp, top = 72.dp, bottom = 24.dp), duration = 600.milliseconds) } } finally { animating = false }
            }
        }
    }
    val downloadActions = DownloadActions(
        start = { plan -> when (areaGate(downloadedAreas.size, downloadedAreas.any { it.active })) {
            AreaGate.ALLOWED -> { downloadUi = DownloadUi.Starting; offlineMaps.startDownload(plan, dark) }
            AreaGate.AT_LIMIT -> downloadUi = DownloadUi.Replace(plan)
            AreaGate.BUSY -> offlineMaps.error.value = "Finish or pause the current download first."
        } },
        replace = { plan, id -> downloadUi = DownloadUi.Starting; offlineMaps.startDownload(plan, dark, replaceId = id) },
        close = { val before = cameraBeforePlan; val saved = downloadUi is DownloadUi.Tracking; downloadUi = DownloadUi.Idle; cameraBeforePlan = null; if (before != null && !saved) fly(before) }, cancel = { offlineMaps.cancel(it) }, resume = { offlineMaps.resume(it) }, remove = { offlineMaps.delete(it) })
    LaunchedEffect(offlineRegions, dark) { recomputeCovered() }
    LaunchedEffect(currentPackId) { val id = currentPackId; if (id != null && downloadUi is DownloadUi.Starting) downloadUi = DownloadUi.Tracking(id) }
    LaunchedEffect(offlineError) { if (offlineError != null && downloadUi is DownloadUi.Starting) downloadUi = DownloadUi.Idle }
    LaunchedEffect(selection) {
        nearby = try { index.near(selection.latitude, selection.longitude, NEAREST_PLACE_MAX_METERS) } catch (e: CancellationException) { throw e } catch (_: Exception) { emptyList() }
    }
    LaunchedEffect(cameraState) {
        snapshotFlow { cameraState.isCameraMoving }.collect { movingNow ->
            if (movingNow) return@collect
            val c = centre()
            recomputeCovered()
            if (!resultsModeNow) selectedCoordinates = c
            val last = lastSearchCentre
            if (!animating && last != null && mapMovedEnough(last.latitude, last.longitude, c.latitude, c.longitude, cameraState.position.zoom)) moved = true
        }
    }
    LaunchedEffect(details, moreRadius, coordinatesVisible) {
        if (details || moreRadius || coordinatesVisible) { withFrameNanos { }; sheetState.expand() }
    }

    val singleFits = geofenceAreasUsed == null || geofenceFits(geofenceAreasUsed, 1, replacingAreas)
    val allFits = geofenceAreasUsed == null || geofenceFits(geofenceAreasUsed, inRange.size, replacingAreas)
    val chosenLocation = SelectedLocation(selection.latitude, selection.longitude, radiusMeters)

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxHeight < 600.dp && density.fontScale > 1.3f || maxHeight < 400.dp
    val compactMapHeight = (maxHeight - 120.dp).coerceIn(180.dp, 300.dp)
    val actions: @Composable () -> Unit = {
                Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!resultsMode) PrimaryAction("Use this place", onClick = { onLocationSelected(chosenLocation) },
                    enabled = !cameraState.isCameraMoving && singleFits, modifier = Modifier.testTag("location_picker_confirm_button"))
                else {
                    PrimaryAction("Remind me at this place", onClick = { selectedPlace?.let { onPlaceSelected(chosenLocation, it.name) } },
                        enabled = selectedPlace != null && singleFits, modifier = Modifier.testTag("remind_this_place"))
                    if (inRange.size > 1) OutlinedButton(onClick = {
                        val ref = searchRef?.let { SelectedCoordinates(it[0], it[1]) } ?: selection
                        onPlacesSelected(SelectedLocation(ref.latitude, ref.longitude, radiusMeters), searchLabel(query, chips, inRange),
                            inRange.take(MAX_CHAIN_AREAS).map { ChainArea(it.id, it.latitude, it.longitude) })
                    }, enabled = allFits, shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("remind_all_places")) { Text("Remind me at all ${minOf(inRange.size, MAX_CHAIN_AREAS)} places") }
                }
                if (geofenceAreasUsed != null && (!singleFits || !allFits)) Text(
                    geofenceBlockMessage(geofenceAreasUsed, if (!singleFits) 1 else inRange.size, replacingAreas),
                    style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag("geofence_block_message"))
            }
    }
    val bottomPanel: @Composable () -> Unit = {
        if (downloadUi == DownloadUi.Idle) actions()
        else Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp)) { DownloadPanel(downloadUi, downloadedAreas, dark, downloadActions) }
    }
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).then(if (compact) Modifier.verticalScroll(rememberScrollState()) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Choose a place", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
            if (onCancel != null) TextButton(onClick = onCancel) { Text("Back") }
        }
        Row(Modifier.widthIn(max = 640.dp).fillMaxWidth().align(Alignment.CenterHorizontally).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(query, { query = it }, modifier = Modifier.weight(1f).heightIn(min = 52.dp).testTag("place_search"), singleLine = true,
                placeholder = { Text("Search places or brands") }, shape = MaterialTheme.shapes.medium,
                leadingIcon = { Icon(AriciuIcons.Search, null, Modifier.size(20.dp)) },
                trailingIcon = if (query.isNotEmpty() || resultsMode) ({
                    IconButton(onClick = { clearSearch(keepSelection = true) }, modifier = Modifier.size(48.dp).testTag("place_search_clear")) {
                        Icon(AriciuIcons.Close, "Clear search", Modifier.size(20.dp)) }
                }) else null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focus.clearFocus(); keyboard?.hide(); runSearch(reference(), query, chips) }))
            FilledTonalIconButton(onClick = { requestLocate() }, enabled = !locating, modifier = Modifier.size(52.dp)) {
                if (locating) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                else Icon(AriciuIcons.Locate, "My location", Modifier.size(24.dp))
            }
        }
        LazyRow(Modifier.fillMaxWidth().selectableGroup().testTag("place_category_chips"), contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(PlaceCategory.entries.toList(), key = { it.key }) { category ->
                val on = category in chips
                FilterChip(on, {
                    chipKeys = (if (on) chips - category else chips + category).joinToString(",") { it.key }
                    focus.clearFocus(); keyboard?.hide()
                    runSearch(reference(), query, if (on) chips - category else chips + category)
                }, label = { Text(category.label) }, modifier = Modifier.testTag("category_${category.key}"),
                    leadingIcon = { Icon(category.icon(), null, Modifier.size(16.dp)) })
            }
        }
        Spacer(Modifier.height(4.dp))
        BoxWithConstraints(if (compact) Modifier.height(compactMapHeight) else Modifier.weight(1f)) {
        // Expanded = about 60% of the map area (half sheet) so the pins stay visible; short screens keep the full height.
        val sheetBodyHeight = (if (compact) maxHeight - sheetPeek else maxHeight * 0.6f - sheetPeek).coerceAtLeast(120.dp)
        BottomSheetScaffold(modifier = Modifier.fillMaxSize(), scaffoldState = sheetScaffold,
            sheetPeekHeight = sheetPeek, sheetContainerColor = colors.surface,
            sheetDragHandle = null, sheetContent = {
                Column(Modifier.fillMaxWidth().onSizeChanged { sheetPeek = with(density) { it.height.toDp() } }, horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(Modifier.padding(top = 8.dp).size(width = 32.dp, height = 4.dp), shape = MaterialTheme.shapes.small, color = colors.outlineVariant) {}
                    TextButton(onClick = { scope.launch { if (sheetState.currentValue == SheetValue.Expanded) sheetState.partialExpand() else sheetState.expand() } },
                        modifier = Modifier.fillMaxWidth().testTag("place_sheet_toggle").semantics { stateDescription = if (sheetState.currentValue == SheetValue.Expanded) "Expanded" else "Collapsed" },
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp)) {
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.Start) {
                            Text(when { selectedPlace != null -> selectedPlace.name; compact -> "Options"; else -> "Around this spot" },
                                style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 2)
                            if (!compact) Text(if (sheetState.currentValue == SheetValue.Expanded) "Collapse place options" else "Expand place options", style = MaterialTheme.typography.bodySmall)
                            Text(selectedPlace?.let { "${PlaceCategory.of(it).label}${it.address.takeIf { a -> a.isNotBlank() }?.let { a -> " · $a" } ?: ""}" }
                                ?: if (nearby.isNotEmpty() || userFix != null || searchRef != null) nearestPlaceLabel(nearest) else "",
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant, modifier = Modifier.testTag("nearest_place_label"))
                        }
                        Text("$radiusMeters m", style = MaterialTheme.typography.titleSmall, modifier = Modifier.testTag("location_picker_radius_value"))
                    }
                if (!compact) bottomPanel()
                }
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = sheetBodyHeight).testTag("place_results_list"), state = listState,
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item(key = "status") {
                        Column(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (searching) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Text("Searching places…", style = MaterialTheme.typography.bodyMedium) }
                            searchMessage?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
                            if (resultsMode) Text("${results.size} ${if (results.size == 1) "place" else "places"}${if (inRange.size < results.size) "" else " within 25 km"}. Select one on the map or in this list.",
                                style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    item(key = "radius-chips") {
                        FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(100, 200, 500).forEach { radius ->
                                FilterChip(selected = radiusMeters == radius, onClick = { radiusMeters = radius },
                                    leadingIcon = if (radiusMeters == radius) ({ Icon(AriciuIcons.Check, null, Modifier.size(16.dp)) }) else null,
                                    label = { Text("$radius m") }, modifier = (if (density.fontScale > 1.3f) Modifier.fillMaxWidth() else Modifier.weight(1f)).heightIn(min = 48.dp), shape = MaterialTheme.shapes.small)
                            }
                        }
                    }
                    item(key = "radius-actions") {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { moreRadius = !moreRadius; scope.launch { sheetState.expand() } }) { Text(if (moreRadius) "Less" else "More distances") }
                            TextButton(onClick = { details = !details; scope.launch { sheetState.expand() } }) { Text("Location details") }
                        }
                    }
                    if (moreRadius) item(key = "radius-slider") {
                        Slider(value = radiusMeters.toFloat(), onValueChange = { radiusMeters = snapRadiusMeters(it) },
                            valueRange = MIN_RADIUS_METERS.toFloat()..MAX_RADIUS_METERS.toFloat(), steps = RADIUS_SLIDER_STEPS,
                            modifier = Modifier.testTag("location_picker_radius_slider").semantics { contentDescription = "Reminder radius in metres" })
                    }
                    if (geofenceAreasUsed != null) item(key = "budget") {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(AriciuIcons.Pin, null, Modifier.size(16.dp), tint = colors.onSurfaceVariant)
                            Text(geofenceBudgetText(geofenceAreasUsed), Modifier.testTag("geofence_budget_text"), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                        }
                    }
                    items(results, key = { it.id }) { place ->
                        ResultRow(place, place.id == selectedId, searchRef?.let { distanceMeters(it[0], it[1], place.latitude, place.longitude) }) { select(place, fly = true) }
                    }
                    if (details) item(key = "details") {
                        Surface(color = colors.surfaceContainer, shape = MaterialTheme.shapes.medium) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Keep the map available offline", style = MaterialTheme.typography.titleSmall)
                                Text("Map tiles need internet unless a downloaded area covers this spot. Place search is already offline. Use Download area in the map controls to save the visible map for the current theme. Manage or delete areas in Settings.", style = MaterialTheme.typography.bodySmall)
                                downloadedAreas.lastOrNull()?.let { area -> Text(if (area.complete) "Map area saved. Manage in Settings → Offline maps." else if (area.active) "Downloading ${(area.progress * 100).toInt()}%" else "Download paused. Resume in Settings → Offline maps.", style = MaterialTheme.typography.bodySmall) }
                                TextButton(onClick = { coordinatesVisible = !coordinatesVisible }) { Text(if (coordinatesVisible) "Hide coordinates" else "Show coordinates") }
                                if (coordinatesVisible) {
                                    Text(selection.formatLatitude(), Modifier.testTag("location_picker_latitude"), style = MaterialTheme.typography.bodySmall)
                                    Text(selection.formatLongitude(), Modifier.testTag("location_picker_longitude"), style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    item(key = "end") { Spacer(Modifier.height(16.dp)) }
                }
            }) { _ ->
            Box(Modifier.fillMaxSize().padding(bottom = sheetPeek).testTag("location_picker_map_area"), contentAlignment = Alignment.Center) {
                MaplibreMap(Modifier.fillMaxSize(), baseStyle = baseStyle,
                    cameraState = cameraState, onMapLoadFailed = { mapFailed = true }, onMapLoadFinished = { mapFailed = false },
                    onMapLongClick = { position, _ -> dropManualPin(position); ClickResult.Consume },
                    options = MapOptions(ornamentOptions = OrnamentOptions(isLogoEnabled = false, isScaleBarEnabled = false))) {
                    val source = rememberGeoJsonSource(data = GeoJsonData.Features(boundary))
                    FillLayer(id = "reminder-radius-fill", source = source, color = const(colors.primary), opacity = const(0.14f))
                    LineLayer(id = "reminder-radius-outline", source = source, color = const(colors.primary), width = const(2.dp))
                    OfflineOutlineLayers(offlineRegions.filter { it.dark == dark }.map { it.bounds },
                        ((downloadUi as? DownloadUi.Plan)?.plan as? DownloadPlan.Ready)?.bounds ?: (downloadUi as? DownloadUi.Replace)?.plan?.bounds, colors)
                    userFix?.let { UserLocationLayers(it.position, it.accuracyMeters, colors) }
                    ResultPinLayers(pinsJson, selectedPlace, cameraState, colors, onPlace = { id -> results.firstOrNull { it.id == id }?.let { select(it, fly = false) } })
                }
                if (!resultsMode) CenterSelectionMarker()
                MapControls(cameraState, onRecenter = { requestLocate() }, modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp),
                    extra = MapControl(if (covered) AriciuIcons.Check else AriciuIcons.Download, if (covered) "Downloaded" else "Download area", "map_download",
                        enabled = downloadUi == DownloadUi.Idle, onClick = { openDownload() }))
                if (moved && (resultsMode || query.isNotBlank() || chips.isNotEmpty()) && !searching) FilledTonalButton(
                    onClick = { runSearch(centre(), query, chips) }, modifier = Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp, end = 92.dp)
                        .heightIn(min = 48.dp).testTag("search_this_area")) { Text("Search this area") }
                else if (!resultsMode && !compact && density.fontScale <= 1.3f) Surface(Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp, end = 92.dp), shape = MaterialTheme.shapes.small,
                    color = colors.surface, shadowElevation = 2.dp) {
                    Text(if (mapFailed) "Map unavailable here. Check internet or use a downloaded area." else "Move the map to choose your spot, or long-press to drop a pin", Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
                Surface(Modifier.align(Alignment.BottomStart).padding(8.dp).widthIn(max = 300.dp), shape = MaterialTheme.shapes.extraSmall, color = colors.surface) {
                    Text("© OpenMapTiles · © OpenStreetMap", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
        }
        }
        if (compact) Surface(color = colors.surface) { bottomPanel() }
    }
    }
    if (locationExplanation) AlertDialog(onDismissRequest = { locationExplanation = false }, title = { Text("Find your location") },
        text = { Text("Allow location while using Ariciu to centre the map. Approximate location also works for the map. Background access is a separate step for reminders.") },
        confirmButton = { TextButton(onClick = {
            locationExplanation = false
            if (permissionAsked) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
            else { permissionAsked = true; locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) }
        }) { Text(if (permissionAsked) "Open settings" else "Allow location") } },
        dismissButton = { TextButton(onClick = { locationExplanation = false }) { Text("Not now") } })
    offlineError?.let { error -> AlertDialog(onDismissRequest = { offlineMaps.error.value = null }, title = { Text("Offline maps") }, text = { Text(error) },
        confirmButton = { TextButton(onClick = { offlineMaps.error.value = null }) { Text("OK") } }) }
    message?.let { text -> AlertDialog(onDismissRequest = { message = null }, title = { Text("Map") }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } }) }
}

private const val MAX_CHAIN_AREAS = 40

/** One result. Selecting it highlights the pin; the row is also the TalkBack path to every pin. */
@Composable
private fun ResultRow(place: CatalogPlace, selected: Boolean, distance: Double?, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val category = PlaceCategory.of(place)
    Surface(shape = MaterialTheme.shapes.medium, color = if (selected) colors.primaryContainer else colors.surface,
        border = androidx.compose.foundation.BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
        modifier = Modifier.fillMaxWidth().selectable(selected, role = Role.RadioButton, onClick = onClick).testTag("result_${place.id}")) {
        Row(Modifier.heightIn(min = 56.dp).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(category.icon(), null, Modifier.size(24.dp), tint = if (selected) colors.onPrimaryContainer else colors.primary)
            Column(Modifier.weight(1f)) {
                Text(place.name, style = MaterialTheme.typography.titleSmall, color = if (selected) colors.onPrimaryContainer else colors.onSurface)
                Text(listOfNotNull(category.label, distance?.let { distanceText(it) }, place.address.takeIf { it.isNotBlank() }).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall, color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
            }
            if (selected) Icon(AriciuIcons.Check, null, Modifier.size(20.dp), tint = colors.onPrimaryContainer)
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
