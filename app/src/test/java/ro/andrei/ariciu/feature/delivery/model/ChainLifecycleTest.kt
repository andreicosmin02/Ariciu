package ro.andrei.ariciu.feature.delivery.model
import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.reminders.model.*
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation

class ChainLifecycleTest {
    private val r = Reminder("id", "Bread", SelectedLocation(45.0,25.0,200), ReminderTransition.ENTER, ReminderRepeat.EVERY_VISIT,
        chainName = "Kaufland", chainAreas = listOf(ChainArea("n1",45.0,25.0), ChainArea("n2",45.1,25.1)))
    @Test fun duplicatesAndOverlappingStoresDoNotCreateMultipleOccurrences() {
        val first = r.crossChain("n1", ReminderTransition.ENTER, 10, 1)
        assertEquals(1, first.delivery.occurrence)
        assertEquals(first, first.crossChain("n1", ReminderTransition.ENTER, 10, 1))
        val overlap = first.crossChain("n2", ReminderTransition.ENTER, 20, 1)
        assertEquals(1, overlap.delivery.occurrence)
        val stillInside = overlap.crossChain("n1", ReminderTransition.EXIT, 30, 1)
        assertEquals(AreaPosition.INSIDE, stillInside.delivery.position)
        val outside = stillInside.crossChain("n2", ReminderTransition.EXIT, 40, 1)
        assertEquals(2, outside.crossChain("n2", ReminderTransition.ENTER, 50, 1).delivery.occurrence)
        assertEquals(outside, outside.crossChain("n2", ReminderTransition.ENTER, 30, 1))
    }
    @Test fun onceAndDoneApplyAcrossAllStores() {
        val once = r.copy(repeat = ReminderRepeat.ONCE).crossChain("n1", ReminderTransition.ENTER, 10, 1)
            .crossChain("n1", ReminderTransition.EXIT, 20, 1).crossChain("n2", ReminderTransition.ENTER, 30, 1)
        assertEquals(1, once.delivery.occurrence)
        val done = once.copy(delivery = once.delivery.onAction(OccurrenceAction.DONE,1,ReminderTransition.ENTER))
        assertEquals(done, done.crossChain("n2",ReminderTransition.EXIT,40,1))
    }
    @Test fun allocationSharesCapacityAndNeverExceeds100() {
        val records = (0..9).map { DeliveryRecord(r.copy(id = "r$it"), "g") }
        val allocated = allocateAreas(records, 12)
        assertEquals(12, allocated.size)
        assertEquals(10, allocated.map { it.reminder.id }.distinct().size)
    }
    @Test fun unresolvedUrgentChainKeepsItsOccurrenceAcrossStoreExitAndReentry() {
        val reminder = ro.andrei.ariciu.feature.reminders.model.Reminder("urgent","Chain",ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation(45.0,25.0,200),
            ReminderTransition.ENTER,ReminderRepeat.EVERY_VISIT,intensity = NotificationIntensity.URGENT,chainName = "Kaufland",chainAreas = listOf(ChainArea("n1",45.0,25.0)))
        val first = reminder.crossChain("n1",ReminderTransition.ENTER,10,1)
        val swiped = first.copy(delivery = first.delivery.onUrgentDismiss(1,0,1_000))
        val next = swiped.crossChain("n1",ReminderTransition.EXIT,20,1).crossChain("n1",ReminderTransition.ENTER,30,1)
        assertEquals(1,next.delivery.occurrence)
        assertEquals(DeliveryPhase.TRIGGERED,next.delivery.phase)
        assertEquals(swiped.delivery.urgentRetryAtMs,next.delivery.urgentRetryAtMs)
    }

}
