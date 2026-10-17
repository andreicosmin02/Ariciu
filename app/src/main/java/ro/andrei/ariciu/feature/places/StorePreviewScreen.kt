package ro.andrei.ariciu.feature.places

import androidx.compose.foundation.layout.*
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ro.andrei.ariciu.feature.delivery.model.ChainArea
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.ariciu.feature.locationpicker.geometry.buildRadiusBoundary
import ro.andrei.ariciu.feature.locationpicker.map.toGeoJsonPolygon
import ro.andrei.ariciu.feature.offlinemap.OfflineMaps
import ro.andrei.ariciu.ui.theme.isAriciuDarkTheme
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.spatialk.geojson.Position
import java.util.Locale
import kotlin.math.log2

/** Shows the persisted snapshot, including older far-away selections until explicitly refreshed. */
@Composable
fun StorePreviewScreen(areas: List<ChainArea>, catalog: List<CatalogPlace>, anchor: SelectedLocation, onDismiss: () -> Unit) {
    var list by remember { mutableStateOf(false) }
    var mapLoaded by remember { mutableStateOf(false) }
    var mapFailed by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val dark = isAriciuDarkTheme()
    val minLat = minOf(anchor.latitude, areas.minOfOrNull { it.latitude } ?: anchor.latitude)
    val maxLat = maxOf(anchor.latitude, areas.maxOfOrNull { it.latitude } ?: anchor.latitude)
    val minLon = minOf(anchor.longitude, areas.minOfOrNull { it.longitude } ?: anchor.longitude)
    val maxLon = maxOf(anchor.longitude, areas.maxOfOrNull { it.longitude } ?: anchor.longitude)
    val span = maxOf(maxLat - minLat, maxLon - minLon, 0.008)
    val camera = rememberCameraState(firstPosition = CameraPosition(
        target = Position(latitude = (minLat + maxLat) / 2, longitude = (minLon + maxLon) / 2),
        zoom = log2(180.0 / span).coerceIn(5.0, 14.0)))
    BackHandler(onBack = onDismiss)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).testTag("store_preview_close")) { Text("Back") }
            Text("Selected stores · ${areas.size}", style = MaterialTheme.typography.titleMedium)
        }
        Text("Shaded circles show the selected reminder areas. Store data may be incomplete. Android monitors at most 100 areas across all reminders.",
            Modifier.padding(horizontal = 20.dp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Row(Modifier.padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(!list, { list = false }, label = { Text("Map") }, modifier = Modifier.heightIn(min = 48.dp))
            FilterChip(list, { list = true }, label = { Text("List") }, modifier = Modifier.heightIn(min = 48.dp))
        }
        if (areas.any { distanceMeters(anchor.latitude, anchor.longitude, it.latitude, it.longitude) > CHAIN_COVERAGE_METERS })
            Text("This saved selection includes distant stores. Refresh around your pin to limit it to 25 km.", Modifier.padding(horizontal = 20.dp), color = colors.error, style = MaterialTheme.typography.bodySmall)
        if (list) LazyColumn(Modifier.weight(1f).fillMaxWidth().testTag("store_preview_list"), contentPadding = PaddingValues(20.dp)) {
            itemsIndexed(areas, key = { _, area -> area.id }) { index, area ->
                val place = catalog.firstOrNull { it.id == area.id }
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(place?.name ?: "Store ${index + 1}", style = MaterialTheme.typography.titleMedium)
                    Text(place?.address?.ifBlank { "Address not listed" } ?: "Address not listed", style = MaterialTheme.typography.bodyMedium)
                    Text("%.1f km from your pin".format(Locale.ROOT, distanceMeters(anchor.latitude, anchor.longitude, area.latitude, area.longitude) / 1000), style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider(color = colors.outlineVariant)
            }
        } else {
            MaplibreMap(Modifier.fillMaxWidth().weight(1f).testTag("store_preview_map"),
                baseStyle = BaseStyle.Uri(OfflineMaps.styleUri(dark)), cameraState = camera,
                onMapLoadFailed = { mapFailed = true }, onMapLoadFinished = { mapFailed = false; mapLoaded = true },
                options = MapOptions(ornamentOptions = OrnamentOptions(isLogoEnabled = false, isScaleBarEnabled = false))) {
                areas.forEach { area ->
                    val polygon = remember(area, anchor.radiusMeters) {
                        buildRadiusBoundary(SelectedCoordinates(area.latitude, area.longitude), anchor.radiusMeters).toGeoJsonPolygon()
                    }
                    val source = rememberGeoJsonSource(data = GeoJsonData.Features(polygon))
                    FillLayer("preview-${area.id}", source, color = const(colors.primary), opacity = const(0.45f))
                    LineLayer("preview-line-${area.id}", source, color = const(colors.primary), width = const(3.dp))
                }
            }
            Text(if (mapFailed) "Map unavailable. Use List to review stores." else if (!mapLoaded) "Loading map detail… You can use List while it loads. © OpenMapTiles · © OpenStreetMap" else "Map detail may need internet. © OpenMapTiles · © OpenStreetMap",
                Modifier.padding(horizontal = 20.dp, vertical = 8.dp), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
    }
}
