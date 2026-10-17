package ro.andrei.ariciu.feature.reminders.data

import android.content.Context
import androidx.room.Transaction
import androidx.room.ColumnInfo
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.withTransaction
import java.util.UUID
import ro.andrei.ariciu.feature.delivery.model.*
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
import ro.andrei.ariciu.feature.reminders.model.NotificationIntensity
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
    @ColumnInfo(defaultValue = "''") val generation: String = "",
    @ColumnInfo(defaultValue = "'WAITING'") val phase: String = "WAITING",
    @ColumnInfo(defaultValue = "'UNKNOWN'") val position: String = "UNKNOWN",
    @ColumnInfo(defaultValue = "0") val occurrence: Int = 0,
    @ColumnInfo(defaultValue = "0") val notificationVisible: Boolean = false,
    @ColumnInfo(defaultValue = "0") val notificationPending: Boolean = false,
    @ColumnInfo(defaultValue = "-1") val lastEventElapsedMs: Long = -1,
    @ColumnInfo(defaultValue = "-1") val lastEventBoot: Int = -1,
    @ColumnInfo(defaultValue = "'PENDING'") val registration: String = "PENDING",
    val deliveryError: String? = null,
    @ColumnInfo(defaultValue = "'NORMAL'") val intensity: String = "NORMAL",
    @ColumnInfo(defaultValue = "0") val quiet: Boolean = false,
    @ColumnInfo(defaultValue = "0") val snoozeUntilMs: Long = 0,
    val chainName: String? = null,
    @ColumnInfo(defaultValue = "''") val chainAreas: String = "",
    @ColumnInfo(defaultValue = "0") val urgentRetryAtMs: Long = 0,
    @ColumnInfo(defaultValue = "0") val notificationAttempt: Int = 0,
) {
    fun toReminder() = Reminder(id, title, SelectedLocation(latitude, longitude, radiusMeters),
        ReminderTransition.valueOf(transition), ReminderRepeat.valueOf(repeat),
        DeliveryState(DeliveryPhase.valueOf(phase), AreaPosition.valueOf(position), occurrence,
            notificationVisible, notificationPending, lastEventElapsedMs, lastEventBoot, quiet, snoozeUntilMs, urgentRetryAtMs, notificationAttempt),
        RegistrationState.valueOf(registration), deliveryError, NotificationIntensity.valueOf(intensity), chainName, decodeChainAreas(chainAreas))
}

fun Reminder.toEntity(generation: String = "") = ReminderEntity(id, title, location.latitude, location.longitude,
    location.radiusMeters, transition.name, repeat.name, generation, delivery.phase.name, delivery.position.name,
    delivery.occurrence, delivery.notificationVisible, delivery.notificationPending, delivery.lastEventElapsedMs,
    delivery.lastEventBoot, registration.name, deliveryError, intensity.name, delivery.quiet, delivery.snoozeUntilMs, chainName, encodeChainAreas(chainAreas), delivery.urgentRetryAtMs, delivery.notificationAttempt)

@Entity(tableName = "registered_geofences")
data class RegisteredGeofence(@PrimaryKey val requestId: String)

@Dao
interface ReminderDao {
    @Query("SELECT * FROM reminders ORDER BY title COLLATE NOCASE, id")
    fun observeAll(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM reminders WHERE id = :id") suspend fun find(id: String): ReminderEntity?
    @Transaction
    suspend fun complete(id: String, occurrence: Int) {
        val old = find(id) ?: return
        val current = old.toReminder()
        if (old.occurrence != occurrence || !current.delivery.active) return
        // Rotate identity so an interrupted worker cannot undo completion.
        upsert(current.copy(delivery = current.delivery.completed(),
            registration = RegistrationState.INACTIVE, deliveryError = null).toEntity(UUID.randomUUID().toString()))
    }

    @Transaction
    suspend fun nextTime(id: String, occurrence: Int) {
        val old = find(id) ?: return
        val current = old.toReminder()
        if (!current.unresolvedUrgent || old.occurrence != occurrence) return
        val next = current.delivery.onAction(OccurrenceAction.NEXT_TIME,occurrence,current.transition,allowUnresolvedUrgent = true)
        if (next == current.delivery) return
        upsert(current.copy(delivery = next,registration = RegistrationState.PENDING,deliveryError = null)
            .toEntity(UUID.randomUUID().toString()))
    }

    @Query("SELECT * FROM reminders") suspend fun getAll(): List<ReminderEntity>
    @Query("SELECT * FROM registered_geofences") suspend fun registrations(): List<RegisteredGeofence>
    @Upsert suspend fun track(registrations: List<RegisteredGeofence>)
    @Query("DELETE FROM registered_geofences WHERE requestId IN (:ids)") suspend fun untrack(ids: List<String>)
    @Upsert suspend fun upsert(reminder: ReminderEntity)

    @Transaction
    suspend fun saveEdited(reminder: ReminderEntity) {
        val old = find(reminder.id)
        val moved = old != null && (old.latitude != reminder.latitude || old.longitude != reminder.longitude || old.radiusMeters != reminder.radiusMeters || old.chainName != reminder.chainName ||
            decodeChainAreas(old.chainAreas).map { it.id } != decodeChainAreas(reminder.chainAreas).map { it.id })
        val preserved = if (old == null) reminder else reminder.copy(
            phase = old.phase, position = if (moved) AreaPosition.UNKNOWN.name else old.position,
            occurrence = old.occurrence, notificationVisible = old.notificationVisible,
            notificationPending = old.notificationVisible, lastEventElapsedMs = old.lastEventElapsedMs,
            lastEventBoot = old.lastEventBoot, quiet = old.quiet, snoozeUntilMs = old.snoozeUntilMs,
            urgentRetryAtMs = old.urgentRetryAtMs, notificationAttempt = old.notificationAttempt,
            chainAreas = encodeChainAreas(decodeChainAreas(reminder.chainAreas).map { area ->
                if (old.chainName != reminder.chainName || moved) area else decodeChainAreas(old.chainAreas).firstOrNull {
                    it.id == area.id && it.latitude == area.latitude && it.longitude == area.longitude } ?: area
            }),
        )
        val converted = when {
            old == null || old.intensity == reminder.intensity -> preserved
            reminder.intensity == NotificationIntensity.URGENT.name && old.phase != DeliveryPhase.COMPLETED.name ->
                preserved.copy(phase = if (old.notificationVisible || old.snoozeUntilMs > 0) DeliveryPhase.TRIGGERED.name else old.phase,
                    urgentRetryAtMs = old.snoozeUntilMs, snoozeUntilMs = 0, quiet = false)
            else -> preserved.copy(urgentRetryAtMs = 0,
                notificationVisible = old.notificationVisible || old.urgentRetryAtMs > 0,
                notificationPending = old.notificationVisible || old.urgentRetryAtMs > 0)
        }
        upsert(converted)
    }


    @Query("""UPDATE reminders SET phase=:phase, position=:position, occurrence=:occurrence,
        notificationVisible=:visible, notificationPending=:pending, lastEventElapsedMs=:elapsed,
        lastEventBoot=:boot, registration=:registration, deliveryError=:error, quiet=:quiet, snoozeUntilMs=:snoozeUntilMs, chainAreas=:chainAreas, urgentRetryAtMs=:urgentRetryAtMs, notificationAttempt=:notificationAttempt
        WHERE id=:id AND generation=:generation""")
    suspend fun updateDelivery(id: String, generation: String, phase: String, position: String,
        occurrence: Int, visible: Boolean, pending: Boolean, elapsed: Long, boot: Int,
        registration: String, error: String?, quiet: Boolean, snoozeUntilMs: Long, chainAreas: String, urgentRetryAtMs: Long, notificationAttempt: Int)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [ReminderEntity::class, RegisteredGeofence::class], version = 5, exportSchema = true)
abstract class ReminderDatabase : RoomDatabase() {
    abstract fun reminderDao(): ReminderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN generation TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE reminders ADD COLUMN phase TEXT NOT NULL DEFAULT 'WAITING'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN position TEXT NOT NULL DEFAULT 'UNKNOWN'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN occurrence INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN notificationVisible INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN notificationPending INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN lastEventElapsedMs INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE reminders ADD COLUMN lastEventBoot INTEGER NOT NULL DEFAULT -1")
                db.execSQL("ALTER TABLE reminders ADD COLUMN registration TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN deliveryError TEXT")
                db.execSQL("UPDATE reminders SET generation = lower(hex(randomblob(16)))")
                db.execSQL("CREATE TABLE IF NOT EXISTS registered_geofences (requestId TEXT NOT NULL PRIMARY KEY)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN intensity TEXT NOT NULL DEFAULT 'NORMAL'")
                db.execSQL("ALTER TABLE reminders ADD COLUMN quiet INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN snoozeUntilMs INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN chainName TEXT")
                db.execSQL("ALTER TABLE reminders ADD COLUMN chainAreas TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reminders ADD COLUMN urgentRetryAtMs INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reminders ADD COLUMN notificationAttempt INTEGER NOT NULL DEFAULT 0")
                // rc3 EVERY_VISIT could rearm while its Urgent alert remained unanswered.
                db.execSQL("UPDATE reminders SET phase='TRIGGERED' WHERE intensity='URGENT' AND phase!='COMPLETED' AND notificationVisible=1 AND occurrence>0")
            }
        }

        @Volatile private var instance: ReminderDatabase? = null

        fun getInstance(context: Context): ReminderDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext,
                ReminderDatabase::class.java, "reminders.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build().also { instance = it }
        }
    }
}

class RoomReminderRepository(private val dao: ReminderDao, private val onChanged: () -> Unit = {}) : ReminderRepository {
    override fun observeReminders(): Flow<List<Reminder>> =
        dao.observeAll().map { rows -> rows.map { it.toReminder() } }

    override suspend fun save(reminder: Reminder): String {
        // Editing replaces the monitoring generation, preserving occurrence/completion semantics.
        val generation = UUID.randomUUID().toString()
        dao.saveEdited(reminder.toEntity(generation))
        onChanged()
        return generation
    }
    override suspend fun complete(id: String, occurrence: Int) { dao.complete(id, occurrence); onChanged() }
    override suspend fun nextTime(id: String, occurrence: Int) { dao.nextTime(id, occurrence); onChanged() }
    override suspend fun delete(id: String) { dao.delete(id); onChanged() }
}

class RoomDeliveryStore(private val db: ReminderDatabase) : DeliveryStore {
    private val dao get() = db.reminderDao()
    override suspend fun all() = dao.getAll().map { DeliveryRecord(it.toReminder(), it.generation) }
    override suspend fun put(record: DeliveryRecord) {
        val r = record.reminder
        val d = r.delivery
        dao.updateDelivery(r.id, record.generation, d.phase.name, d.position.name, d.occurrence,
            d.notificationVisible, d.notificationPending, d.lastEventElapsedMs, d.lastEventBoot,
            r.registration.name, r.deliveryError, d.quiet, d.snoozeUntilMs, encodeChainAreas(r.chainAreas), d.urgentRetryAtMs, d.notificationAttempt)
    }
    override suspend fun registeredIds() = dao.registrations().map { it.requestId }.toSet()
    override suspend fun track(ids: Set<String>) { dao.track(ids.map { RegisteredGeofence(it) }) }
    override suspend fun untrack(ids: Set<String>) { dao.untrack(ids.toList()) }
    override suspend fun <T> transaction(block: suspend () -> T): T = db.withTransaction { block() }
}
