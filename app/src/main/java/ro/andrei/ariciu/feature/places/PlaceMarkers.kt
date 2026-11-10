package ro.andrei.ariciu.feature.places

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
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
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.layers.SymbolLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.GeoJsonOptions
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.ClickResult
import org.maplibre.compose.util.MaplibreComposable
import org.maplibre.spatialk.geojson.Point
import ro.andrei.ariciu.feature.locationpicker.geometry.buildRadiusBoundary
import ro.andrei.ariciu.feature.locationpicker.map.toGeoJsonPolygon
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.ariciu.ui.components.AriciuIcons

private val MARKER_SIZE = 32.dp
private val SELECTED_SIZE = 44.dp

fun PlaceCategory.icon(): ImageVector = when (this) {
    PlaceCategory.SUPERMARKET -> AriciuIcons.Store; PlaceCategory.PHARMACY -> AriciuIcons.Pharmacy; PlaceCategory.FUEL -> AriciuIcons.Fuel
    PlaceCategory.BANK -> AriciuIcons.Bank; PlaceCategory.POST -> AriciuIcons.Post; PlaceCategory.MALL -> AriciuIcons.Mall
    PlaceCategory.FOOD -> AriciuIcons.Food; PlaceCategory.HEALTH -> AriciuIcons.Health; PlaceCategory.HARDWARE -> AriciuIcons.Hardware
    PlaceCategory.BAKERY -> AriciuIcons.Bakery; PlaceCategory.TOWN -> AriciuIcons.Town
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

private fun PlaceCategory.tones(c: ColorScheme) = when (ordinal % 3) {
    0 -> Triple(c.primaryContainer, c.primary, c.onPrimaryContainer)
    1 -> Triple(c.tertiaryContainer, c.tertiary, c.onTertiaryContainer)
    else -> Triple(c.secondaryContainer, c.secondary, c.onSecondaryContainer)
}

/**
 * Search results as clustered category pins plus one visually distinct selected pin. Call inside the MaplibreMap content.
 * [pinsJson] must not contain the selected place. Tapping a pin reports its id; tapping a cluster zooms in.
 */
@MaplibreComposable
@Composable
fun ResultPinLayers(pinsJson: String, selected: CatalogPlace?, cameraState: CameraState, colors: ColorScheme, onPlace: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val source = rememberGeoJsonSource(data = GeoJsonData.JsonString(pinsJson),
        options = GeoJsonOptions(cluster = true, clusterRadius = 44, clusterMaxZoom = 15))
    val clustered = feature.has("point_count")
    CircleLayer(id = "res-cluster", source = source, filter = clustered, color = const(colors.primaryContainer),
        radius = const(18.dp), strokeColor = const(colors.primary), strokeWidth = const(2.dp),
        onClick = { features ->
            val at = (features.firstOrNull()?.geometry as? Point)?.coordinates
            if (at != null) scope.launch { cameraState.animateTo(CameraPosition(target = at, zoom = cameraState.position.zoom + 2.0)) }
            ClickResult.Consume
        })
    SymbolLayer(id = "res-cluster-count", source = source, filter = clustered,
        textField = format(span(feature["point_count_abbreviated"].asString())),
        textFont = const(listOf("Noto Sans Bold")), textColor = const(colors.onPrimaryContainer),
        iconAllowOverlap = const(true), textAllowOverlap = const(true))
    PlaceCategory.entries.forEach { category ->
        val glyph = rememberVectorPainter(category.icon())
        val (fill, ring, onFill) = category.tones(colors)
        val painter = remember(glyph, fill, ring, onFill) { MarkerPainter(glyph, fill, ring, onFill, with(density) { MARKER_SIZE.toPx() }) }
        SymbolLayer(id = "res-${category.key}", source = source,
            filter = !clustered and (feature["cat"].asString() eq const(category.key)),
            iconImage = image(painter, size = DpSize(MARKER_SIZE, MARKER_SIZE)), iconAllowOverlap = const(true),
            onClick = { features ->
                features.firstOrNull()?.properties?.get("id")?.jsonPrimitive?.content?.let(onPlace)
                ClickResult.Consume
            })
    }
    if (selected != null) {
        val selectedSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(placesGeoJson(listOf(selected))))
        val glyph = rememberVectorPainter(PlaceCategory.of(selected).icon())
        val painter = remember(glyph, colors) { MarkerPainter(glyph, colors.primary, colors.surface, colors.onPrimary, with(density) { SELECTED_SIZE.toPx() }) }
        SymbolLayer(id = "res-selected", source = selectedSource, iconImage = image(painter, size = DpSize(SELECTED_SIZE, SELECTED_SIZE)),
            iconAllowOverlap = const(true))
    }
}

/** Blue dot plus an accuracy circle for the My location fix. Call inside the MaplibreMap content. */
@MaplibreComposable
@Composable
fun UserLocationLayers(position: SelectedCoordinates, accuracyMeters: Float, colors: ColorScheme) {
    val circle = remember(position, accuracyMeters) { buildRadiusBoundary(position, accuracyMeters.toInt().coerceIn(5, 2_000)).toGeoJsonPolygon() }
    val ring = rememberGeoJsonSource(data = GeoJsonData.Features(circle))
    FillLayer(id = "user-accuracy-fill", source = ring, color = const(colors.tertiary), opacity = const(0.12f))
    LineLayer(id = "user-accuracy-line", source = ring, color = const(colors.tertiary), width = const(1.dp))
    val dot = rememberGeoJsonSource(data = GeoJsonData.Features(Point(org.maplibre.spatialk.geojson.Position(position.longitude, position.latitude))))
    CircleLayer(id = "user-dot", source = dot, color = const(colors.tertiary), radius = const(7.dp), strokeColor = const(colors.surface), strokeWidth = const(2.dp))
}
