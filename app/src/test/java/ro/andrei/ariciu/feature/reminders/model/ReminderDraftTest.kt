package ro.andrei.ariciu.feature.reminders.model

import org.junit.Assert.*
import org.junit.Test
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation

class ReminderDraftTest {
    private val location = SelectedLocation(45.6579, 25.6012, 200)

    @Test fun titleAndLocationAreRequired() {
        assertFalse(ReminderDraft().canSave)
        assertFalse(ReminderDraft(title = "Buy bread").canSave)
        assertFalse(ReminderDraft(title = "  ", location = location).canSave)
        assertTrue(ReminderDraft(title = "Buy bread", location = location).canSave)
    }

    @Test fun rejectsInvalidCoordinatesAndRadius() {
        for (invalid in listOf(location.copy(latitude = Double.NaN), location.copy(latitude = 91.0),
            location.copy(longitude = -181.0), location.copy(radiusMeters = 0),
            location.copy(radiusMeters = 150), location.copy(radiusMeters = 1100))) {
            assertFalse(ReminderDraft(title = "Bread", location = invalid).canSave)
        }
    }

    @Test fun defaultsToArrivalAndOnce() {
        val draft = ReminderDraft()
        assertEquals(ReminderTransition.ENTER, draft.transition)
        assertEquals(ReminderRepeat.ONCE, draft.repeat)
    }

    @Test fun saveTrimsTitleAndPreservesChoicesAndIdentity() {
        val draft = ReminderDraft("  Buy bread  ", location, ReminderTransition.EXIT, ReminderRepeat.EVERY_VISIT)
        val reminder = draft.toReminder("existing-id")
        assertEquals("Buy bread", reminder.title)
        assertEquals("existing-id", reminder.id)
        assertEquals(location, reminder.location)
        assertEquals(draft, reminder.toDraft().copy(title = draft.title))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidDraftCannotBeSaved() { ReminderDraft().toReminder("id") }
}
