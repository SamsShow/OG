package com.og.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import kotlinx.coroutines.flow.Flow

/** Single-row settings/baseline record. Always id = 1. */
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val heightCm: Double = 178.0,
    val startWeightKg: Double = 69.0,
    val startBodyFatPct: Double = 19.0,
    val startSmmKg: Double = 32.5,
    val startedOnDay: Long = 0L,
    /** Set when creatine begins; the weight trend treats the following weeks as water-confounded. */
    val creatineStartDay: Long? = null,
    val weeklyWorkoutTarget: Int = 5,
    val proteinMin: Int = 100,
    val proteinMax: Int = 140,
    val onboarded: Boolean = false,
    /**
     * Neon connection string, entered in-app and never compiled in. Empty disables sync.
     * Kept out of the build so no credential can end up in the repository or the APK.
     */
    val neonUrl: String = "",
    /** Set once the 11pm reminder has been scheduled, so it is not rescheduled every launch. */
    val reminderOn: Boolean = true,
)

@Entity(tableName = "set_log")
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: String,
    val day: Long,
    val weightKg: Double,
    val reps: Int,
    val createdAt: Long,
)

@Entity(tableName = "meal_log", primaryKeys = ["day", "mealId"])
data class MealLog(
    val day: Long,
    val mealId: String,
    /** 1.0 = the planned portion. Scales protein and calories linearly. */
    val servings: Double,
    val completed: Boolean,
)

/** A lift the user added because the built-in library was missing it. */
@Entity(tableName = "custom_exercise")
data class CustomExercise(
    @PrimaryKey val id: String,
    val name: String,
    /** [MuscleGroup] name. */
    val muscleGroup: String,
    /** Comma-separated [Muscle] names. */
    val primaryMuscles: String,
    /** [Equipment] name. */
    val equipment: String,
) {
    fun toExercise(): Exercise = Exercise(
        id = id,
        name = name,
        group = MuscleGroup.entries.firstOrNull { it.name == muscleGroup } ?: MuscleGroup.BACK,
        primary = primaryMuscles.split(',')
            .mapNotNull { n -> Muscle.entries.firstOrNull { it.name == n.trim() } },
        equipment = Equipment.entries.firstOrNull { it.name == equipment } ?: Equipment.MACHINE,
    )
}

/**
 * What you say you trained on a day, independent of logged sets.
 *
 * Sets are the precise record, but they are not always the whole record — you can finish a
 * back session and never type in the numbers. Tagging the day keeps the calendar and the
 * coverage stats honest without forcing you to log every set.
 */
@Entity(tableName = "day_log")
data class DayLog(
    @PrimaryKey val day: Long,
    /** Comma-separated [MuscleGroup] names. Empty string means no manual tag. */
    val groups: String,
    val note: String,
)

/** Anything eaten outside the plan — snacks, meals out, a second helping. */
@Entity(tableName = "extra_intake")
data class ExtraIntake(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val day: Long,
    val label: String,
    val proteinG: Double,
    val kcal: Double,
    val createdAt: Long,
)

@Entity(tableName = "measurement")
data class Measurement(
    @PrimaryKey val day: Long,
    val weightKg: Double?,
    val bodyFatPct: Double?,
    val smmKg: Double?,
    val shouldersCm: Double?,
    val chestCm: Double?,
    val waistCm: Double?,
    val hipsCm: Double?,
    val armCm: Double?,
    val thighCm: Double?,
)

@Dao
interface OgDao {

    @Query("SELECT * FROM profile WHERE id = 1")
    fun profile(): Flow<Profile?>

    @Upsert
    suspend fun saveProfile(profile: Profile)

    @Query("SELECT * FROM set_log WHERE day >= :fromDay ORDER BY createdAt ASC")
    fun setsSince(fromDay: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_log WHERE day = :day ORDER BY createdAt ASC")
    fun setsOn(day: Long): Flow<List<SetLog>>

    @Query("SELECT * FROM set_log WHERE exerciseId = :exerciseId ORDER BY day DESC, createdAt DESC")
    fun setsFor(exerciseId: String): Flow<List<SetLog>>

    @Insert
    suspend fun addSet(set: SetLog)

    @Query("DELETE FROM set_log WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("SELECT * FROM meal_log WHERE day = :day")
    fun mealsOn(day: Long): Flow<List<MealLog>>

    @Query("SELECT * FROM meal_log WHERE day >= :fromDay")
    fun mealsSince(fromDay: Long): Flow<List<MealLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMeal(log: MealLog)

    @Query("SELECT * FROM custom_exercise ORDER BY name ASC")
    fun customExercises(): Flow<List<CustomExercise>>

    @Query("SELECT * FROM custom_exercise")
    suspend fun customExercisesOnce(): List<CustomExercise>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomExercise(exercise: CustomExercise)

    @Query("DELETE FROM custom_exercise WHERE id = :id")
    suspend fun deleteCustomExercise(id: String)

    @Query("SELECT * FROM day_log WHERE day >= :fromDay ORDER BY day ASC")
    fun dayLogsSince(fromDay: Long): Flow<List<DayLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDayLog(log: DayLog)

    @Query("DELETE FROM day_log WHERE day = :day")
    suspend fun deleteDayLog(day: Long)

    @Query("SELECT * FROM extra_intake WHERE day >= :fromDay ORDER BY createdAt ASC")
    fun extrasSince(fromDay: Long): Flow<List<ExtraIntake>>

    @Insert
    suspend fun addExtra(extra: ExtraIntake)

    @Query("DELETE FROM extra_intake WHERE id = :id")
    suspend fun deleteExtra(id: Long)

    @Query("SELECT * FROM measurement ORDER BY day ASC")
    fun measurements(): Flow<List<Measurement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveMeasurement(measurement: Measurement)

    @Query("SELECT DISTINCT day FROM set_log ORDER BY day DESC")
    fun trainingDays(): Flow<List<Long>>

    // ---- whole-state read/write, used by backup and restore ----

    @Query("SELECT * FROM profile WHERE id = 1") suspend fun profileOnce(): Profile?
    @Query("SELECT * FROM set_log") suspend fun allSets(): List<SetLog>
    @Query("SELECT * FROM meal_log") suspend fun allMeals(): List<MealLog>
    @Query("SELECT * FROM extra_intake") suspend fun allExtras(): List<ExtraIntake>
    @Query("SELECT * FROM measurement") suspend fun allMeasurements(): List<Measurement>
    @Query("SELECT * FROM day_log") suspend fun allDayLogs(): List<DayLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putSets(rows: List<SetLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeals(rows: List<MealLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putExtras(rows: List<ExtraIntake>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putMeasurements(rows: List<Measurement>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putDayLogs(rows: List<DayLog>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putCustomExercises(rows: List<CustomExercise>)

    @Query("SELECT COUNT(*) FROM set_log WHERE day = :day") suspend fun setCountOn(day: Long): Int
}

@Database(
    entities = [
        Profile::class, SetLog::class, MealLog::class, Measurement::class,
        ExtraIntake::class, DayLog::class, CustomExercise::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class OgDatabase : RoomDatabase() {
    abstract fun dao(): OgDao

    companion object {
        /** Adds ad-hoc intake. A real migration, not a wipe — there is logged data to keep. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `extra_intake` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `day` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `proteinG` REAL NOT NULL,
                        `kcal` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /** Adds hand-tagged training days for the calendar. */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `day_log` (
                        `day` INTEGER NOT NULL PRIMARY KEY,
                        `groups` TEXT NOT NULL,
                        `note` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        /** Adds user-added lifts, the Neon connection string and the reminder toggle. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `custom_exercise` (
                        `id` TEXT NOT NULL PRIMARY KEY,
                        `name` TEXT NOT NULL,
                        `muscleGroup` TEXT NOT NULL,
                        `primaryMuscles` TEXT NOT NULL,
                        `equipment` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                connection.execSQL("ALTER TABLE `profile` ADD COLUMN `neonUrl` TEXT NOT NULL DEFAULT ''")
                connection.execSQL("ALTER TABLE `profile` ADD COLUMN `reminderOn` INTEGER NOT NULL DEFAULT 1")
            }
        }

        @Volatile private var instance: OgDatabase? = null

        fun get(context: Context): OgDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                OgDatabase::class.java,
                "og.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { instance = it }
        }
    }
}
