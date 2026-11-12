package ro.andrei.arichu.feature.locationpicker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.CameraState
import ro.andrei.arichu.ui.components.ArichuIcons

private const val MIN_ZOOM = 3.0
private const val MAX_ZOOM = 19.0

/** An extra labelled control below the standard ones, for example "Download area". */
data class MapControl(val icon: ImageVector, val label: String, val tag: String, val enabled: Boolean = true, val onClick: () -> Unit)

/** Zoom, compass, recenter and an optional extra control. Each target is at least 48dp and carries a visible label. */
@Composable
fun MapControls(cameraState: CameraState, onRecenter: () -> Unit, modifier: Modifier = Modifier, recenterLabel: String = "Recenter", extra: MapControl? = null) {
    val scope = rememberCoroutineScope()
    fun zoomBy(delta: Double) = scope.launch {
        val p = cameraState.position
        cameraState.animateTo(p.copy(zoom = (p.zoom + delta).coerceIn(MIN_ZOOM, MAX_ZOOM)))
    }
    val bearing = cameraState.position.bearing
    Surface(modifier.testTag("map_controls"), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shadowElevation = 2.dp) {
        Column(Modifier.verticalScroll(rememberScrollState()).widthIn(min = 68.dp).width(IntrinsicSize.Max)) {
            ControlButton(ArichuIcons.ZoomIn, "Zoom in", "map_zoom_in") { zoomBy(1.0) }
            ControlButton(ArichuIcons.ZoomOut, "Zoom out", "map_zoom_out") { zoomBy(-1.0) }
            ControlButton(ArichuIcons.Compass, if (bearing == 0.0) "North" else "Reset north", "map_compass", iconRotation = (-bearing).toFloat()) {
                scope.launch { cameraState.animateTo(cameraState.position.copy(bearing = 0.0, tilt = 0.0)) }
            }
            ControlButton(ArichuIcons.Locate, recenterLabel, "map_recenter", onClick = onRecenter)
            extra?.let { ControlButton(it.icon, it.label, it.tag, enabled = it.enabled, onClick = it.onClick) }
        }
    }
}

@Composable
private fun ControlButton(icon: ImageVector, label: String, tag: String, iconRotation: Float = 0f, enabled: Boolean = true, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(vertical = 6.dp).testTag(tag).semantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, Modifier.size(22.dp).rotate(iconRotation), tint = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, softWrap = false,
            modifier = Modifier.padding(horizontal = 4.dp))
    }
}
