package ro.andrei.ariciu.feature.reminders.model

import kotlinx.coroutines.flow.Flow

interface ReminderRepository {
    fun observeReminders(): Flow<List<Reminder>>
    /** Returns the saved generation so an optional foreground action cannot target an edited row. */
    suspend fun save(reminder: Reminder): String
    suspend fun complete(id: String, occurrence: Int)
    suspend fun delete(id: String)
}
