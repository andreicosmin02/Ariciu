package ro.andrei.ariciu.feature.offlinemap
import java.io.IOException
import org.junit.Assert.*
import org.junit.Test
class DownloadBudgetTest {
    @Test fun resumedDownloadIncludesStoredBytesAndRejectsOversizeResponses() {
        val budget = DownloadBudget(44_999_990)
        budget.consume(10)
        assertEquals(45_000_000L, budget.bytes)
        try { budget.consume(1); fail("Expected byte limit") } catch (_: IOException) { }
    }
    @Test fun everyTileResponseIncludingRetriesConsumesBudget() {
        val budget = DownloadBudget()
        repeat(2500) { budget.consume(0, tile = true) }
        assertEquals(2500, budget.tiles)
        try { budget.consume(0, tile = true); fail("Expected tile limit") } catch (_: IOException) { }
    }
}
