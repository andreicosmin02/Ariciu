package ro.andrei.ariciu.feature.reminders.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Position
import ro.andrei.ariciu.feature.delivery.model.DeliveryCapabilities
import ro.andrei.ariciu.feature.locationpicker.ui.MapControls
import ro.andrei.ariciu.feature.offlinemap.OfflineMaps
import ro.andrei.ariciu.feature.places.MarkerPainter
import ro.andrei.ariciu.feature.reminders.model.Reminder
import ro.andrei.ariciu.ui.components.AriciuIcons
import ro.andrei.ariciu.ui.theme.isAriciuDarkTheme

private data class ToneStyle(val icon: ImageVector, val fill: Color, val ring: Color, val glyph: Color)

private fun MapTone.style(c: ColorScheme) = when (this) {
    MapTone.READY -> ToneStyle(AriciuIcons.Pin, c.primaryContainer, c.primary, c.onPrimaryContainer)
    MapTone.WAITING -> ToneStyle(AriciuIcons.Clock, c.secondaryContainer, c.secondary, c.onSecondaryContainer)
    MapTone.ATTENTION -> ToneStyle(AriciuIcons.BellUrgent, c.errorContainer, c.error, c.onErrorContainer)
    MapTone.DONE -> ToneStyle(AriciuIcons.Check, c.surfaceVariant, c.outline, c.onSurfaceVariant)
}

/** Active reminders as pins and radius circles. Colour and glyph both encode status; a text legend names each one. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RemindersMap(reminders: List<Reminder>, capabilities: DeliveryCapabilities, onOpen: (Reminder) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val context = androidx.compose.ui.platform.LocalContext.current
    val dark = isAriciuDarkTheme()
    val density = LocalDensity.current
    val baseStyle = remember(dark) { BaseStyle.Uri(OfflineMaps.styleUri(dark)) }
    val cameraState = rememberCameraState(firstPosition = CameraPosition(target = Position(longitude = 25.0, latitude = 45.9), zoom = 5.5))
    val pins = remember(reminders, capabilities) { reminderPinsGeoJson(reminders, capabilities) }
    val circles = remember(reminders, capabilities) { reminderCirclesGeoJson(reminders, capabilities) }
    val bounds = remember(reminders) { reminderMapBounds(reminders) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = reminders.firstOrNull { it.id == selectedId && it.delivery.active }
    val scope = rememberCoroutineScope()
    suspend fun fitAll() {
        val b = bounds ?: return
        if (b.north - b.south < 0.002 && b.east - b.west < 0.002)
            cameraState.animateTo(CameraPosition(target = Position(longitude = (b.west + b.east) / 2, latitude = (b.south + b.north) / 2), zoom = 15.0))
        else cameraState.animateTo(BoundingBox(b.west, b.south, b.east, b.north), padding = PaddingValues(56.dp))
    }
    LaunchedEffect(bounds) { fitAll() }
    val tones = remember(reminders, capabilities) { reminders.filter { it.delivery.active }.map { it.mapTone(capabilities) }.distinct() }
    Column(modifier.fillMaxWidth().testTag("reminders_map"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            MaplibreMap(Modifier.fillMaxSize(), baseStyle = baseStyle, cameraState = cameraState,
                options = MapOptions(ornamentOptions = OrnamentOptions(isLogoEnabled = false, isScaleBarEnabled = false))) {
                val circleSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(circles))
                val pinSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(pins))
                MapTone.entries.forEach { tone ->
                    val style = tone.style(colors)
                    val isTone = feature["tone"].asString() eq const(tone.name)
                    FillLayer(id = "rem-fill-${tone.name}", source = circleSource, filter = isTone, color = const(style.ring), opacity = const(0.16f))
                    LineLayer(id = "rem-line-${tone.name}", source = circleSource, filter = isTone, color = const(style.ring), width = const(2.dp))
                    val glyph = rememberVectorPainter(style.icon)
                    val painter = remember(glyph, style) { MarkerPainter(glyph, style.fill, style.ring, style.glyph, with(density) { 36.dp.toPx() }) }
                    SymbolLayer(id = "rem-pin-${tone.name}", source = pinSource, filter = isTone,
                        iconImage = image(painter, size = DpSize(36.dp, 36.dp)), iconAllowOverlap = const(true),
                        onClick = { features ->
                            selectedId = features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.content
                            ClickResult.Consume
                        })
                }
            }
            MapControls(cameraState, onRecenter = { scope.launch { fitAll() } }, recenterLabel = "Show all",
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp))
            if (bounds == null) Surface(Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp, end = 92.dp),
                shape = MaterialTheme.shapes.small, color = colors.surface, shadowElevation = 2.dp) {
                Text("No active reminders to show yet.", Modifier.padding(horizontal = 12.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium)
            }
            Surface(Modifier.align(Alignment.BottomStart).padding(8.dp).widthIn(max = 300.dp), shape = MaterialTheme.shapes.extraSmall, color = colors.surface) {
                Text("© OpenMapTiles · © OpenStreetMap", Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
        if (tones.isNotEmpty()) FlowRow(Modifier.fillMaxWidth().testTag("reminders_map_legend"), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            tones.forEach { tone ->
                val style = tone.style(colors)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(style.icon, null, Modifier.size(16.dp), tint = style.ring)
                    Text(tone.label, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
                }
            }
        }
        if (selected != null) Surface(shape = MaterialTheme.shapes.medium, color = colors.surface, border = BorderStroke(1.dp, colors.primary),
            modifier = Modifier.fillMaxWidth().testTag("reminders_map_selected")) {
            Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(selected.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                    Text("${selected.mapTone(capabilities).label} · ${selected.location.radiusMeters} m", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                }
                TextButton(onClick = { onOpen(selected) }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Open") }
            }
        }
    }
}
