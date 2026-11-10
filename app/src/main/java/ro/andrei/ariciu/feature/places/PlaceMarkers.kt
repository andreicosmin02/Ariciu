package ro.andrei.ariciu.feature.places

import android.util.Log
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import org.maplibre.compose.expressions.dsl.and
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.eq
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.format
import org.maplibre.compose.expressions.dsl.image
import org.maplibre.compose.expressions.dsl.not
import org.maplibre.compose.expressions.dsl.span
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import ro.andrei.ariciu.ui.components.AriciuIcons

private const val TAG = "AriciuMap"
private val MARKER_SIZE = 32.dp

private fun PlaceCategory.icon(): ImageVector = when (this) {
    PlaceCategory.SHOPS -> AriciuIcons.Store; PlaceCategory.MALLS -> AriciuIcons.Mall; PlaceCategory.TOWNS -> AriciuIcons.Town
}

/** Round badge + stroke icon. MapLibre rasterises the painter at screen density for the current theme. */
internal class MarkerPainter(private val icon: Painter, private val fill: Color, private val ring: Color, private val glyph: Color,
    private val sizePx: Float) : Painter() {
    override val intrinsicSize = Size(sizePx, sizePx)
    override fun DrawScope.onDraw() {
        drawCircle(fill, radius = size.minDimension / 2)
        drawCircle(ring, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
        val inset = size.minDimension * 0.2f
        translate(inset, inset) { with(icon) { draw(Size(size.width - inset * 2, size.height - inset * 2), colorFilter = ColorFilter.tint(glyph)) } }
    }
}

/** Viewport-filtered catalogue rows as GeoJSON. Recomputed only when the camera leaves the last padded box. */
@Composable
fun rememberViewportPlacesJson(cameraState: CameraState, catalog: List<CatalogPlace>, categories: Set<PlaceCategory>): State<String> {
    val json = remember { mutableStateOf(placesGeoJson(emptyList())) }
    LaunchedEffect(cameraState, catalog, categories) {
        var loaded: GeoBounds? = null
        json.value = placesGeoJson(emptyList())
        snapshotFlow { cameraState.isCameraMoving to cameraState.position.zoom }.collect { (moving, zoom) ->
            if (moving) return@collect
            if (!placeMarkersVisible(zoom)) { if (loaded != null) { loaded = null; json.value = placesGeoJson(emptyList()) }; return@collect }
            val box = cameraState.projection?.queryVisibleBoundingBox() ?: return@collect
            val view = GeoBounds(box.south, box.west, box.north, box.east)
            if (loaded?.let { it.contains(view.south, view.west) && it.contains(view.north, view.east) } == true) return@collect
            val padLat = (view.north - view.south) / 2; val padLon = (view.east - view.west) / 2
            val padded = GeoBounds(view.south - padLat, view.west - padLon, view.north + padLat, view.east + padLon)
            val start = System.nanoTime()
            val text = withContext(Dispatchers.Default) { placesGeoJson(placesInViewport(catalog, padded, categories)) }
            Log.i(TAG, "viewport markers: ${text.length / 1024} KB json in ${(System.nanoTime() - start) / 1_000_000} ms, zoom ${"%.1f".format(zoom)}")
            loaded = padded; json.value = text
        }
    }
    return json
}

/** Clustered catalogue markers. Must be called inside the MaplibreMap content block. Tap = choose that place. */
@MaplibreComposable
@Composable
fun PlaceMarkerLayers(json: String, cameraState: CameraState, colors: androidx.compose.material3.ColorScheme,
    onPlace: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val density = androidx.compose.ui.platform.LocalDensity.current
    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(json),
        options = GeoJsonOptions(cluster = true, clusterRadius = 48, clusterMaxZoom = 15))
    val clustered = feature.has("point_count")
    CircleLayer(id = "poi-cluster", source = source, filter = clustered, color = const(colors.primaryContainer),
        radius = const(18.dp), strokeColor = const(colors.primary), strokeWidth = const(2.dp),
        onClick = { features ->
            val at = (features.firstOrNull()?.geometry as? Point)?.coordinates
            if (at != null) scope.launch { cameraState.animateTo(CameraPosition(target = at, zoom = cameraState.position.zoom + 2.0)) }
            ClickResult.Consume
        })
    SymbolLayer(id = "poi-cluster-count", source = source, filter = clustered,
        textField = format(span(feature["point_count_abbreviated"].asString())),
        textFont = const(listOf("Noto Sans Bold")), textColor = const(colors.onPrimaryContainer),
        iconAllowOverlap = const(true), textAllowOverlap = const(true))
    PlaceCategory.entries.forEach { category ->
        val glyph = rememberVectorPainter(category.icon())
        val (fill, ring) = when (category) {
            PlaceCategory.SHOPS -> colors.primaryContainer to colors.primary
            PlaceCategory.MALLS -> colors.tertiaryContainer to colors.tertiary
            PlaceCategory.TOWNS -> colors.secondaryContainer to colors.secondary
        }
        val onFill = when (category) {
            PlaceCategory.SHOPS -> colors.onPrimaryContainer; PlaceCategory.MALLS -> colors.onTertiaryContainer; PlaceCategory.TOWNS -> colors.onSecondaryContainer
        }
        val painter = remember(glyph, fill, ring, onFill) { MarkerPainter(glyph, fill, ring, onFill, with(density) { MARKER_SIZE.toPx() }) }
        SymbolLayer(id = "poi-${category.name.lowercase()}", source = source,
            filter = !clustered and (feature["cat"].asString() eq const(category.name)),
            iconImage = image(painter, size = DpSize(MARKER_SIZE, MARKER_SIZE)), iconAllowOverlap = const(true),
            onClick = { features ->
                features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.content?.let(onPlace)
                ClickResult.Consume
            })
    }
}
