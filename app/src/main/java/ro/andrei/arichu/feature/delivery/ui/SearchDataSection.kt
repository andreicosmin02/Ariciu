package ro.andrei.arichu.feature.delivery.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ro.andrei.arichu.feature.places.PackStatus
import ro.andrei.arichu.feature.places.PlacePackManager
import ro.andrei.arichu.feature.places.packStatus
import ro.andrei.arichu.ui.components.ExpandableSection
import ro.andrei.arichu.ui.components.SectionLabel
import java.util.Locale

private fun mb(bytes: Long) = "%.1f MB".format(Locale.ROOT, bytes / 1_000_000.0)

/** Settings -> Search data: download, update and remove the per-country place catalogues used by map search. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.SearchDataSection() {
    val context = LocalContext.current
    val manager = remember(context) { PlacePackManager.get(context) }
    val catalog by manager.catalog.collectAsState()
    val installed by manager.installed.collectAsState()
    val work by manager.work.collectAsState()
    val message by manager.message.collectAsState()
    val refreshing by manager.refreshing.collectAsState()
    var expanded by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val updates = catalog.filter { packStatus(it, installed[it.code]) == PackStatus.UPDATE }
    SectionLabel("Search data")
    Text("Map search works offline with place lists you download per country (shops, pharmacies, fuel, banks, food, hospitals, towns and more). " +
        "Romania is already included. Downloading uses mobile data or Wi-Fi once per country; searching never uses the internet. " +
        "Lists come from OpenStreetMap and are updated when the app's data release is.", style = MaterialTheme.typography.bodyMedium)
    Text(if (installed.isEmpty()) "No country downloaded yet (Romania is built in)." else "Downloaded: ${installed.keys.sorted().mapNotNull { c -> catalog.firstOrNull { it.code == c }?.name ?: c.uppercase() }.joinToString(", ")}" +
        if (updates.isNotEmpty()) ". ${updates.size} update${if (updates.size == 1) "" else "s"} available." else ".", color = colors.onSurfaceVariant)
    message?.let { Text(it, color = colors.error, modifier = Modifier.testTag("search_data_message")) }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { manager.refresh() }, enabled = !refreshing && work == null, modifier = Modifier.testTag("search_data_refresh")) {
            Text(if (refreshing) "Loading…" else if (catalog.isEmpty()) "Load country list" else "Check for updates")
        }
        if (updates.isNotEmpty()) Button(onClick = { manager.install(updates) }, enabled = work == null, modifier = Modifier.testTag("search_data_update_all")) {
            Text("Update all (${mb(updates.sumOf { it.bytes })})")
        }
        if (work != null) TextButton(onClick = { manager.cancel() }) { Text("Cancel") }
    }
    work?.let { w ->
        val name = catalog.firstOrNull { it.code == w.code }?.name ?: w.code
        Text("${w.label} $name… ${(w.progress * 100).toInt()}%", modifier = Modifier.testTag("search_data_progress"))
        LinearProgressIndicator(progress = { w.progress }, modifier = Modifier.fillMaxWidth())
    }
    if (catalog.isNotEmpty()) ExpandableSection("Countries (${catalog.size})", expanded, { expanded = !expanded }) {
        catalog.forEach { pack ->
            val status = packStatus(pack, installed[pack.code])
            Surface(color = colors.surfaceContainer, shape = MaterialTheme.shapes.medium, modifier = Modifier.testTag("search_data_${pack.code}")) {
                Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(pack.name, style = MaterialTheme.typography.titleSmall)
                        Text("${mb(pack.bytes)} · ${"%,d".format(Locale.ROOT, pack.rows)} places · " + when (status) {
                            PackStatus.NOT_INSTALLED -> "not downloaded"; PackStatus.INSTALLED -> "downloaded ${pack.generated}"; PackStatus.UPDATE -> "update available"
                        }, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                    }
                    if (status != PackStatus.INSTALLED) TextButton(onClick = { manager.install(listOf(pack)) }, enabled = work == null,
                        modifier = Modifier.testTag("search_data_get_${pack.code}")) { Text(if (status == PackStatus.UPDATE) "Update" else "Download") }
                    if (status != PackStatus.NOT_INSTALLED) TextButton(onClick = { manager.remove(pack.code) }, enabled = work == null) { Text("Remove") }
                }
            }
        }
    }
    Text("Place data © OpenStreetMap contributors, ODbL 1.0, from Geofabrik country extracts; files are hosted as public release files on GitHub. " +
        "Shops and addresses may be missing or outdated. Searches stay on this phone.", style = MaterialTheme.typography.bodySmall)
}
