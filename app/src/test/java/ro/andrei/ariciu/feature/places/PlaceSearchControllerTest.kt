package ro.andrei.ariciu.feature.places

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceSearchControllerTest {
    private val place = CatalogPlace("n1", "AFI Brașov", "mall", "", 45.0, 25.0)

    @Test fun loadingSearchingAndEmptyAreDistinctAndCurrent() = runTest {
        val catalogue = CompletableDeferred<List<CatalogPlace>>()
        val result = CompletableDeferred<List<CatalogPlace>>()
        val search = PlaceSearchController(backgroundScope, { catalogue.await() }, { _, _ -> result.await() })
        assertEquals(PlaceSearchPhase.LOADING_CATALOGUE, search.state.value.phase)
        search.query("afi"); runCurrent()
        catalogue.complete(listOf(place)); runCurrent()
        assertEquals(PlaceSearchPhase.SEARCHING, search.state.value.phase)
        assertTrue(search.state.value.matches.isEmpty())
        result.complete(emptyList()); runCurrent()
        assertEquals(PlaceSearchPhase.RESULTS, search.state.value.phase)
        assertEquals("afi", search.state.value.query)
        assertTrue(search.state.value.matches.isEmpty())
    }

    @Test fun rapidTypingCancelsPreviousSearchAndClearsItsResults() = runTest {
        val queries = mutableListOf<String>(); val cancelled = mutableListOf<String>()
        val second = CompletableDeferred<List<CatalogPlace>>()
        val search = PlaceSearchController(backgroundScope, { listOf(place) }) { _, query ->
            queries += query
            if (query == "a") try { awaitCancellation() } finally { cancelled += query }
            else second.await()
        }
        runCurrent(); search.query("a"); runCurrent(); search.query("afi"); runCurrent()
        assertEquals(listOf("a"), cancelled)
        assertEquals(listOf("a", "afi"), queries)
        assertEquals(PlaceSearchPhase.SEARCHING, search.state.value.phase)
        second.complete(listOf(place)); runCurrent()
        assertEquals(listOf(place), search.state.value.matches)
        search.query("missing")
        assertEquals(PlaceSearchPhase.SEARCHING, search.state.value.phase)
        assertTrue(search.state.value.matches.isEmpty()) // Old AFI results cannot masquerade as final.
        search.query("")
        assertEquals(PlaceSearchPhase.IDLE, search.state.value.phase)
        assertTrue(search.state.value.matches.isEmpty())
        runCurrent()
        assertFalse("missing" in queries)
    }

    @Test fun catalogueFailureCanRetryWithoutLosingQuery() = runTest {
        var fails = true
        val search = PlaceSearchController(backgroundScope, { if (fails) error("asset unreadable") else listOf(place) }) { data, _ -> data }
        search.query("afi"); runCurrent()
        assertEquals(PlaceSearchPhase.CATALOGUE_FAILED, search.state.value.phase)
        fails = false; search.retry(); runCurrent()
        assertEquals(PlaceSearchPhase.RESULTS, search.state.value.phase)
        assertEquals("afi", search.state.value.query)
    }

    @Test fun cancelledOwnerStopsLoadAndSearchWithoutReportingFailure() = runTest {
        val scope = CoroutineScope(coroutineContext + Job())
        var cancelled = false
        val search = PlaceSearchController(scope, { listOf(place) }) { _, _ ->
            try { awaitCancellation() } finally { cancelled = true }
        }
        runCurrent(); search.query("afi"); runCurrent(); scope.cancel(); runCurrent()
        assertTrue(cancelled)
        assertNotEquals(PlaceSearchPhase.SEARCH_FAILED, search.state.value.phase)
    }

    @Test fun searchFailureIsNotAnEmptyResult() = runTest {
        val search = PlaceSearchController(backgroundScope, { listOf(place) }) { _, _ -> error("search failure") }
        runCurrent(); search.query("afi"); runCurrent()
        assertEquals(PlaceSearchPhase.SEARCH_FAILED, search.state.value.phase)
    }
}
