package ro.andrei.ariciu.feature.places

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlaceSearchPhase { LOADING_CATALOGUE, IDLE, SEARCHING, RESULTS, CATALOGUE_FAILED, SEARCH_FAILED }
data class PlaceSearchState(val query: String = "", val phase: PlaceSearchPhase = PlaceSearchPhase.LOADING_CATALOGUE,
    val matches: List<CatalogPlace> = emptyList())

/** The current query owns its results. The dialog's scope cancels work when it closes. */
class PlaceSearchController(private val scope: CoroutineScope,
    private val load: suspend () -> List<CatalogPlace>,
    private val search: suspend (List<CatalogPlace>, String) -> List<CatalogPlace>) {
    private val mutableState = MutableStateFlow(PlaceSearchState())
    val state = mutableState.asStateFlow()
    private var catalogue: List<CatalogPlace>? = null
    private var loadJob: Job? = null
    private var searchJob: Job? = null
    private var revision = 0
    init { retry() }

    fun query(value: String) {
        if (value == state.value.query) return
        mutableState.value = state.value.copy(query = value, matches = emptyList())
        searchCurrent()
    }

    fun retry() {
        loadJob?.cancel(); searchJob?.cancel(); revision++
        catalogue = null
        mutableState.value = PlaceSearchState(state.value.query)
        loadJob = scope.launch {
            try {
                val loaded = load()
                currentCoroutineContext().ensureActive()
                catalogue = loaded
                searchCurrent()
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                mutableState.value = PlaceSearchState(state.value.query, PlaceSearchPhase.CATALOGUE_FAILED)
            }
        }
    }

    private fun searchCurrent() {
        searchJob?.cancel()
        val version = ++revision
        val query = state.value.query
        val data = catalogue
        val phase = when {
            data == null && state.value.phase == PlaceSearchPhase.CATALOGUE_FAILED -> PlaceSearchPhase.CATALOGUE_FAILED
            data == null -> PlaceSearchPhase.LOADING_CATALOGUE
            query.isBlank() -> PlaceSearchPhase.IDLE
            else -> PlaceSearchPhase.SEARCHING
        }
        mutableState.value = PlaceSearchState(query, phase)
        if (phase != PlaceSearchPhase.SEARCHING || data == null) return
        searchJob = scope.launch {
            try {
                val matches = search(data, query)
                currentCoroutineContext().ensureActive()
                if (version == revision) mutableState.value = PlaceSearchState(query, PlaceSearchPhase.RESULTS, matches)
            } catch (e: CancellationException) { throw e
            } catch (_: Exception) {
                if (version == revision) mutableState.value = PlaceSearchState(query, PlaceSearchPhase.SEARCH_FAILED)
            }
        }
    }
}
