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
}

@Database(
    entities = [Profile::class, SetLog::class, MealLog::class, Measurement::class, ExtraIntake::class],
    version = 2,
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

        @Volatile private var instance: OgDatabase? = null

        fun get(context: Context): OgDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                OgDatabase::class.java,
                "og.db",
            ).addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
