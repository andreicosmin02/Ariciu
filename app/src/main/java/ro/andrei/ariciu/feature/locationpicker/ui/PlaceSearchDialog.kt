package ro.andrei.ariciu.feature.locationpicker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ro.andrei.ariciu.feature.locationpicker.model.SelectedCoordinates
import ro.andrei.ariciu.feature.places.*

@Composable
internal fun PlaceSearchDialog(centre: SelectedCoordinates, onDismiss: () -> Unit, onSelect: (CatalogPlace) -> Unit,
    load: (suspend () -> List<CatalogPlace>)? = null,
    search: (suspend (List<CatalogPlace>, String) -> List<CatalogPlace>)? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var restoredQuery by rememberSaveable { mutableStateOf("") }
    val controller = remember(context, centre) { PlaceSearchController(scope, load ?: { LocalPlaces.load(context) },
        search ?: { places, query -> withContext(Dispatchers.Default) { searchPlaces(places, query, centre.latitude, centre.longitude) } }).also { it.query(restoredQuery) } }
    val state by controller.state.collectAsState()
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Find a place") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(state.query, { restoredQuery = it; controller.query(it) }, label = { Text("Town, mall, store or store address") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("place_search"))
            Text("Romania · offline · OpenStreetMap. Selected malls, shops and towns, not every address. OSM source: 15 Oct 2026.", style = MaterialTheme.typography.bodySmall)
            when (state.phase) {
                PlaceSearchPhase.LOADING_CATALOGUE, PlaceSearchPhase.SEARCHING -> Row(
                    Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(24.dp).testTag("place_search_progress"), strokeWidth = 2.dp)
                    Text(if (state.phase == PlaceSearchPhase.LOADING_CATALOGUE) "Opening local places…" else "Searching places…", style = MaterialTheme.typography.bodyMedium)
                }
                PlaceSearchPhase.CATALOGUE_FAILED, PlaceSearchPhase.SEARCH_FAILED -> Column {
                    Text(if (state.phase == PlaceSearchPhase.CATALOGUE_FAILED) "Could not open the local catalogue. You can still choose a pin on the map." else "Could not finish the search. Please retry.")
                    TextButton(onClick = controller::retry) { Text("Retry") }
                }
                PlaceSearchPhase.IDLE -> Text("Try Brașov or Kaufland Brașov.")
                PlaceSearchPhase.RESULTS -> if (state.matches.isEmpty()) Text("No places found. Try a nearby town, then move the map.",
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                else LazyColumn(Modifier.heightIn(max = 300.dp)) { items(state.matches, key = { it.id }) { place ->
                    TextButton(onClick = { onSelect(place) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Column(Modifier.fillMaxWidth()) {
                            Text(place.name, style = MaterialTheme.typography.titleMedium)
                            Text(place.address.ifBlank { if (place.kind == "town") "Town or village in Romania" else "${if (place.kind == "mall") "Shopping centre" else "Shop"} · Address not listed" }, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                } }
            }
        }
    }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
