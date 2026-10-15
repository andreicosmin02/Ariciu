package ro.andrei.ariciu.feature.reminders.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.andrei.ariciu.feature.locationpicker.model.SelectedLocation
import ro.andrei.ariciu.feature.reminders.model.Reminder
import ro.andrei.ariciu.feature.reminders.model.ReminderRepeat
import ro.andrei.ariciu.feature.reminders.model.ReminderRepository
import ro.andrei.ariciu.feature.reminders.model.ReminderTransition

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val transition: String,
    val repeat: String,
) {
    fun toReminder() = Reminder(id, title, SelectedLocation(latitude, longitude, radiusMeters),
        ReminderTransition.valueOf(transition), ReminderRepeat.valueOf(repeat))
}

fun Reminder.toEntity() = ReminderEntity(id, title, location.latitude, location.longitude,
    location.radiusMeters, transition.name, repeat.name)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY title COLLATE NOCASE, id")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Upsert suspend fun upsert(reminder: ReminderEntity)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [ReminderEntity::class], version = 1, exportSchema = true)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile private var instance: ReminderDatabase? = null

        fun getInstance(context: Context): ReminderDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext,
                ReminderDatabase::class.java, "reminders.db").build().also { instance = it }
        }
    }
}

class RoomReminderRepository(private val dao: ReminderDao) : ReminderRepository {
    override fun observeReminders(): Flow<List<Reminder>> =
        dao.observeAll().map { rows -> rows.map { it.toReminder() } }

    override suspend fun save(reminder: Reminder) { dao.upsert(reminder.toEntity()) }
    override suspend fun delete(id: String) { dao.delete(id) }
}
