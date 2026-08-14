package com.og.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.og.OgApp
import android.app.Application
import android.provider.Settings
import com.og.data.CustomExercise
import com.og.data.DayLog
import com.og.data.ExerciseLibrary
import com.og.data.ExtraIntake
import com.og.data.NeonSync
import com.og.data.Seed
import com.og.data.MealLog
import com.og.data.Measurement
import com.og.data.Equipment
import com.og.data.Muscle
import com.og.data.MuscleGroup
import com.og.data.OgDao
import com.og.data.Profile
import com.og.data.Session
import com.og.data.SetLog
import com.og.data.TrainingSplit
import com.og.domain.Analytics
import com.og.domain.Score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiState(
    val loading: Boolean = true,
    val profile: Profile? = null,
    val today: Long = LocalDate.now().toEpochDay(),
    /** The day new sets are written to. Usually today; the calendar can point it elsewhere. */
    val selectedDay: Long = LocalDate.now().toEpochDay(),
    val sets: List<SetLog> = emptyList(),
    val dayLogs: List<DayLog> = emptyList(),
    val customExercises: List<CustomExercise> = emptyList(),
    val meals: List<MealLog> = emptyList(),
    val extras: List<ExtraIntake> = emptyList(),
    val measurements: List<Measurement> = emptyList(),
    val score: Score? = null,
    val heat: Map<Muscle, Float> = emptyMap(),
    val proteinToday: Double = 0.0,
    val kcalToday: Double = 0.0,
    val streak: Int = 0,
    val groupSessions: Map<MuscleGroup, Int> = emptyMap(),
    val suggestTopUp: Boolean = false,
    /** Protein and calories for the day the Fuel screen is showing. */
    val proteinSelected: Double = 0.0,
    val kcalSelected: Double = 0.0,
) {
    val onboarded: Boolean get() = profile?.onboarded == true

    /** The suggested split for the selected day — a suggestion, never a constraint. */
    val todaySession: Session?
        get() = TrainingSplit.forDay(LocalDate.ofEpochDay(selectedDay).dayOfWeek)

    val trainedToday: Boolean get() = sets.any { it.day == today }
    val loggingForToday: Boolean get() = selectedDay == today
    val proteinTarget: Int get() = profile?.proteinMin ?: 100
    val proteinRemaining: Double get() = (proteinTarget - proteinToday).coerceAtLeast(0.0)

    /** Every day with training on it, mapped to the muscle groups worked. */
    val calendar: Map<Long, Set<MuscleGroup>>
        get() = Analytics.trainingCalendar(sets, dayLogs)

    fun groupsOn(day: Long): Set<MuscleGroup> = Analytics.groupsOn(day, sets, dayLogs)
}

/** The five nutrition/training flows, bundled so the outer combine stays under the limit. */
private data class Core(
    val profile: Profile?,
    val sets: List<SetLog>,
    val meals: List<MealLog>,
    val measurements: List<Measurement>,
    val extras: List<ExtraIntake>,
)

class OgViewModel(private val dao: OgDao) : ViewModel() {

    private val today = LocalDate.now().toEpochDay()

    private val selectedDay = MutableStateFlow(today)

    init {
        viewModelScope.launch { Seed.backfill(dao) }
    }

    private val core = combine(
        dao.profile(),
        dao.setsSince(today - 400),
        dao.mealsSince(today - 60),
        dao.measurements(),
        dao.extrasSince(today - 60),
    ) { profile, sets, meals, measurements, extras -> Core(profile, sets, meals, measurements, extras) }

    val state: StateFlow<UiState> = combine(
        core,
        dao.dayLogsSince(today - 400),
        selectedDay,
        dao.customExercises(),
    ) { c, dayLogs, chosen, customs ->
        // Mirror user-added lifts into the static library so every lookup resolves them.
        ExerciseLibrary.setCustom(customs.map { it.toExercise() })
        val p = c.profile ?: return@combine UiState(loading = false, today = today, selectedDay = chosen)
        val recency = Analytics.muscleRecency(c.sets, today)
        // Hand-tagged days count as trained, so the streak does not break on a session
        // you did but never logged set-by-set.
        val trainedDays = c.sets.map { it.day }.toSet() +
            dayLogs.filter { it.groups.isNotBlank() }.map { it.day }
        UiState(
            loading = false,
            profile = p,
            today = today,
            selectedDay = chosen,
            sets = c.sets,
            dayLogs = dayLogs,
            customExercises = customs,
            meals = c.meals,
            extras = c.extras,
            measurements = c.measurements,
            score = Analytics.score(today, p, c.sets, c.meals, c.measurements, c.extras),
            heat = Muscle.entries.associateWith { Analytics.heat(recency[it]) },
            proteinToday = Analytics.proteinOn(today, c.meals, c.extras),
            kcalToday = Analytics.kcalOn(today, c.meals, c.extras),
            streak = Analytics.streak(trainedDays, today, p.startedOnDay),
            groupSessions = Analytics.groupSessions(c.sets, today - 7, dayLogs),
            suggestTopUp = Analytics.shouldSuggestTopUp(today, c.meals, p, c.extras),
            proteinSelected = Analytics.proteinOn(chosen, c.meals, c.extras),
            kcalSelected = Analytics.kcalOn(chosen, c.meals, c.extras),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun history(exerciseId: String): Flow<List<SetLog>> = dao.setsFor(exerciseId)

    fun selectDay(day: Long) { selectedDay.value = day }

    fun resetToToday() { selectedDay.value = today }

    /** Writes to the selected day, so a session can be filled in after the fact. */
    fun addSet(exerciseId: String, weightKg: Double, reps: Int) = viewModelScope.launch {
        dao.addSet(
            SetLog(
                exerciseId = exerciseId,
                day = selectedDay.value,
                weightKg = weightKg,
                reps = reps,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    /** Tags or untags a muscle group on a day without needing set-level detail. */
    fun toggleDayGroup(day: Long, group: MuscleGroup) = viewModelScope.launch {
        val current = Analytics.tagsOf(state.value.dayLogs.firstOrNull { it.day == day })
        val next = if (group in current) current - group else current + group
        val existing = state.value.dayLogs.firstOrNull { it.day == day }
        if (next.isEmpty() && existing?.note.isNullOrBlank()) {
            dao.deleteDayLog(day)
        } else {
            dao.saveDayLog(
                DayLog(
                    day = day,
                    groups = next.joinToString(",") { it.name },
                    note = existing?.note.orEmpty(),
                ),
            )
        }
    }

    fun deleteSet(id: Long) = viewModelScope.launch { dao.deleteSet(id) }

    fun setMeal(mealId: String, servings: Double, completed: Boolean) = viewModelScope.launch {
        dao.saveMeal(
            MealLog(
                day = selectedDay.value,
                mealId = mealId,
                servings = servings,
                completed = completed,
            ),
        )
    }

    fun addExtra(label: String, proteinG: Double, kcal: Double) = viewModelScope.launch {
        dao.addExtra(
            ExtraIntake(
                day = selectedDay.value,
                label = label.ifBlank { "Extra" },
                proteinG = proteinG,
                kcal = kcal,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun deleteExtra(id: Long) = viewModelScope.launch { dao.deleteExtra(id) }

    // ---------------------------------------------------------------- custom lifts

    fun addCustomExercise(
        name: String,
        group: MuscleGroup,
        muscles: List<Muscle>,
        equipment: Equipment,
    ) = viewModelScope.launch {
        val clean = name.trim()
        if (clean.isEmpty()) return@launch
        // Slug plus a timestamp so two lifts with the same name cannot collide.
        val slug = clean.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')
        dao.saveCustomExercise(
            CustomExercise(
                id = "custom_${slug}_${System.currentTimeMillis() % 100000}",
                name = clean,
                muscleGroup = group.name,
                primaryMuscles = muscles.ifEmpty { listOf(group.defaultMuscle) }.joinToString(",") { it.name },
                equipment = equipment.name,
            ),
        )
    }

    fun deleteCustomExercise(id: String) = viewModelScope.launch { dao.deleteCustomExercise(id) }

    // ---------------------------------------------------------------- backup

    private val _sync = MutableStateFlow<String?>(null)

    /** Last sync outcome, shown in Settings. Null while idle. */
    val syncStatus: StateFlow<String?> = _sync.asStateFlow()

    fun saveNeonUrl(url: String) = viewModelScope.launch {
        state.value.profile?.let { dao.saveProfile(it.copy(neonUrl = url.trim())) }
    }

    fun setReminder(on: Boolean) = viewModelScope.launch {
        state.value.profile?.let { dao.saveProfile(it.copy(reminderOn = on)) }
    }

    fun backupNow(deviceId: String) = viewModelScope.launch {
        _sync.value = "Backing up…"
        _sync.value = try {
            val bytes = NeonSync.push(dao, state.value.profile?.neonUrl.orEmpty(), deviceId)
            "Backed up ${bytes / 1024 + 1} KB"
        } catch (e: Exception) {
            e.message ?: "Backup failed"
        }
    }

    fun restoreNow(deviceId: String) = viewModelScope.launch {
        _sync.value = "Restoring…"
        _sync.value = try {
            val rows = NeonSync.pull(dao, state.value.profile?.neonUrl.orEmpty(), deviceId)
            "Restored $rows rows"
        } catch (e: Exception) {
            e.message ?: "Restore failed"
        }
    }

    fun saveMeasurement(m: Measurement) = viewModelScope.launch { dao.saveMeasurement(m) }

    fun saveProfile(profile: Profile) = viewModelScope.launch { dao.saveProfile(profile) }

    fun startCreatineToday() = viewModelScope.launch {
        state.value.profile?.let { dao.saveProfile(it.copy(creatineStartDay = today)) }
    }

    /** Seeds the baseline the whole projection engine measures against. */
    fun completeOnboarding(
        heightCm: Double,
        weightKg: Double,
        bodyFatPct: Double,
        smmKg: Double,
        shouldersCm: Double?,
        waistCm: Double?,
        chestCm: Double?,
        armCm: Double?,
        onCreatine: Boolean,
    ) = viewModelScope.launch {
        dao.saveProfile(
            Profile(
                heightCm = heightCm,
                startWeightKg = weightKg,
                startBodyFatPct = bodyFatPct,
                startSmmKg = smmKg,
                startedOnDay = today,
                creatineStartDay = if (onCreatine) today else null,
                onboarded = true,
            ),
        )
        dao.saveMeasurement(
            Measurement(
                day = today,
                weightKg = weightKg,
                bodyFatPct = bodyFatPct,
                smmKg = smmKg,
                shouldersCm = shouldersCm,
                chestCm = chestCm,
                waistCm = waistCm,
                hipsCm = null,
                armCm = armCm,
                thighCm = null,
            ),
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as OgApp
                OgViewModel(app.db.dao())
            }
        }
    }
}
