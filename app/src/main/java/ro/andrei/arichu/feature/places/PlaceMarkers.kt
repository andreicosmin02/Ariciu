package ro.andrei.arichu.feature.places

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.VectorPath
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
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
import ro.andrei.arichu.feature.locationpicker.geometry.buildRadiusBoundary
import ro.andrei.arichu.feature.locationpicker.map.toGeoJsonPolygon
import ro.andrei.arichu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.arichu.ui.components.ArichuIcons

private val MARKER_SIZE = 32.dp
private val SELECTED_SIZE = 40.dp

fun PlaceCategory.icon(): ImageVector = when (this) {
    PlaceCategory.SUPERMARKET -> ArichuIcons.Store; PlaceCategory.PHARMACY -> ArichuIcons.Pharmacy; PlaceCategory.FUEL -> ArichuIcons.Fuel
    PlaceCategory.BANK -> ArichuIcons.Bank; PlaceCategory.POST -> ArichuIcons.Post; PlaceCategory.MALL -> ArichuIcons.Mall
    PlaceCategory.FOOD -> ArichuIcons.Food; PlaceCategory.HEALTH -> ArichuIcons.Health; PlaceCategory.HARDWARE -> ArichuIcons.Hardware
    PlaceCategory.BAKERY -> ArichuIcons.Bakery; PlaceCategory.TOWN -> ArichuIcons.Town
}

/** Stroke path of a hand-drawn 24dp [ImageVector]. Drawing the path directly avoids rasterising a VectorPainter off-screen. */
internal fun ImageVector.strokePath(): Path {
    val path = Path()
    for (node in root) if (node is VectorPath) node.pathData.toPath(path)
    return path
}

/** Round badge + stroke icon, drawn with plain canvas calls. MapLibre rasterises the painter at screen density. */
internal class MarkerPainter(private val glyph: Path, private val fill: Color, private val ring: Color, private val glyphColor: Color,
    private val sizePx: Float) : Painter() {
    override val intrinsicSize = Size(sizePx, sizePx)
    override fun DrawScope.onDraw() {
        drawCircle(fill, radius = size.minDimension / 2)
        drawCircle(ring, radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(2.dp.toPx()))
        val box = size.minDimension * 0.6f
        val unit = box / 24f
        translate((size.width - box) / 2, (size.height - box) / 2) {
            scale(unit, unit, pivot = Offset.Zero) {
                drawPath(glyph, glyphColor, style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
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
        val glyph = remember(category) { category.icon().strokePath() }
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
    val selectedSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(placesGeoJson(listOfNotNull(selected))))
    val selectedGlyph = remember(selected?.id) { (selected?.let { PlaceCategory.of(it) } ?: PlaceCategory.TOWN).icon().strokePath() }
    val selectedPainter = remember(selectedGlyph, colors) { MarkerPainter(selectedGlyph, colors.primary, colors.surface, colors.onPrimary, with(density) { SELECTED_SIZE.toPx() }) }
    SymbolLayer(id = "res-selected", source = selectedSource, iconImage = image(selectedPainter, size = DpSize(SELECTED_SIZE, SELECTED_SIZE)),
        iconAllowOverlap = const(true))
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
