package ro.andrei.ariciu.feature.locationpicker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.layers.FillLayer
import org.maplibre.compose.layers.LineLayer
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.util.MaplibreComposable
import ro.andrei.ariciu.feature.offlinemap.DownloadPlan
import ro.andrei.ariciu.feature.offlinemap.OfflineArea
import ro.andrei.ariciu.feature.offlinemap.OfflineLimits
import ro.andrei.ariciu.feature.offlinemap.boundsGeoJson
import ro.andrei.ariciu.feature.places.GeoBounds

/** What the inline download panel shows. Idle shows the normal place actions instead. */
sealed interface DownloadUi {
    data object Idle : DownloadUi
    data class Plan(val plan: DownloadPlan) : DownloadUi
    data object Covered : DownloadUi
    data object Starting : DownloadUi
    data class Tracking(val id: Long) : DownloadUi
}

class DownloadActions(
    val start: (DownloadPlan.Ready) -> Unit, val close: () -> Unit,
    val cancel: (Long) -> Unit, val resume: (Long) -> Unit, val remove: (Long) -> Unit,
)

private fun mb(bytes: Long) = String.format(java.util.Locale.ROOT, "%.1f MB", bytes / 1_000_000.0)

/** Thin outlines of saved areas for the current theme, and the highlighted box of the plan being offered. */
@MaplibreComposable
@Composable
fun OfflineOutlineLayers(saved: List<GeoBounds>, planned: GeoBounds?, colors: ColorScheme) {
    val savedSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(boundsGeoJson(saved)))
    // Saved areas are quiet: a faint tint and a thin dashed edge, so overlapping packs do not turn into a grid of boxes.
    FillLayer(id = "offline-saved-fill", source = savedSource, color = const(colors.tertiary), opacity = const(0.06f))
    LineLayer(id = "offline-saved-line", source = savedSource, color = const(colors.onSurfaceVariant), width = const(1.dp),
        opacity = const(0.55f), dasharray = const(listOf(3f, 3f)))
    // The plan is the only strong outline.
    val planSource = rememberGeoJsonSource(data = GeoJsonData.JsonString(boundsGeoJson(listOfNotNull(planned))))
    FillLayer(id = "offline-plan-fill", source = planSource, color = const(colors.tertiary), opacity = const(0.10f))
    LineLayer(id = "offline-plan-line", source = planSource, color = const(colors.tertiary), width = const(3.dp))
}

@Composable
fun DownloadPanel(state: DownloadUi, areas: List<OfflineArea>, dark: Boolean, actions: DownloadActions, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val theme = if (dark) "dark" else "light"
    Surface(modifier.fillMaxWidth().testTag("download_panel"), shape = MaterialTheme.shapes.medium, color = colors.surfaceContainer) {
        Column(Modifier.fillMaxWidth().heightIn(max = 260.dp).verticalScroll(rememberScrollState()).padding(16.dp).semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (state) {
                DownloadUi.Idle -> Unit
                is DownloadUi.Plan -> when (val plan = state.plan) {
                    is DownloadPlan.Ready -> {
                        Text("Download the outlined area", style = MaterialTheme.typography.titleSmall)
                        Text("The map moved to the largest area that can be saved: about ${plan.tiles} map tiles and ${mb(plan.estimatedBytes)} including fonts and map styling. This is an estimate; downloads stop at ${mb(OfflineLimits.SOFT_BYTES)}.",
                            style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("download_estimate"))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { actions.start(plan) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("download_confirm")) { Text("Download") }
                            OutlinedButton(onClick = actions.close, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("download_cancel")) { Text("Cancel") }
                        }
                        Text("Saves the $theme map only. If you switch theme, download again for that theme. Uses internet. Keep Create open while downloading. Map requests are sent to OpenFreeMap and its CDN.",
                            style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    DownloadPlan.NothingFits -> Notice("Map fonts and styling alone use up the download limit, so no area can be saved right now.", actions.close)
                    DownloadPlan.Unsupported -> Notice("Offline downloads are available away from polar regions and the date line.", actions.close)
                }
                DownloadUi.Covered -> Notice("This view is already saved offline for the $theme map. Manage or delete areas in Settings.", actions.close, "Done")
                DownloadUi.Starting -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp); Text("Starting download…", style = MaterialTheme.typography.bodyMedium)
                }
                is DownloadUi.Tracking -> {
                    val area = areas.firstOrNull { it.id == state.id }
                    when {
                        area == null -> Notice("This download was removed.", actions.close, "Done")
                        area.complete -> Notice("Saved offline (${mb(area.bytes)}). Manage or delete areas in Settings.", actions.close, "Done")
                        area.error != null -> {
                            Text(area.error, style = MaterialTheme.typography.bodyMedium, color = colors.error, modifier = Modifier.testTag("download_error"))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { actions.resume(area.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Try again") }
                                OutlinedButton(onClick = { actions.remove(area.id); actions.close() }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Remove") }
                            }
                        }
                        area.active -> {
                            Text("Downloading… ${(area.progress * 100).toInt()}% · ${mb(area.bytes)} so far", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.testTag("download_progress"))
                            LinearProgressIndicator(progress = { area.progress }, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { actions.cancel(area.id); actions.close() }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("download_stop")) { Text("Cancel and remove") }
                        }
                        else -> {
                            Text("Download paused (${(area.progress * 100).toInt()}%). It continues only while Create is open.", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Button(onClick = { actions.resume(area.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Resume") }
                                OutlinedButton(onClick = { actions.remove(area.id); actions.close() }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Remove") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(text: String, onClose: () -> Unit, label: String = "Close") {
    Text(text, style = MaterialTheme.typography.bodyMedium)
    OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("download_close")) { Text(label) }
}
