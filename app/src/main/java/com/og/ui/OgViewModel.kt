package com.og.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import com.og.OgApp
import com.og.data.ExtraIntake
import com.og.data.MealLog
import com.og.data.Measurement
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
    val sets: List<SetLog> = emptyList(),
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
) {
    val onboarded: Boolean get() = profile?.onboarded == true
    val todaySession: Session?
        get() = TrainingSplit.forDay(LocalDate.ofEpochDay(today).dayOfWeek)
    val trainedToday: Boolean get() = sets.any { it.day == today }
    val proteinTarget: Int get() = profile?.proteinMin ?: 100
    val proteinRemaining: Double get() = (proteinTarget - proteinToday).coerceAtLeast(0.0)
}

class OgViewModel(private val dao: OgDao) : ViewModel() {

    private val today = LocalDate.now().toEpochDay()

    val state: StateFlow<UiState> = combine(
        dao.profile(),
        dao.setsSince(today - 120),
        dao.mealsSince(today - 60),
        dao.measurements(),
        dao.extrasSince(today - 60),
    ) { profile, sets, meals, measurements, extras ->
        val p = profile ?: return@combine UiState(loading = false, today = today)
        val recency = Analytics.muscleRecency(sets, today)
        UiState(
            loading = false,
            profile = p,
            today = today,
            sets = sets,
            meals = meals,
            extras = extras,
            measurements = measurements,
            score = Analytics.score(today, p, sets, meals, measurements, extras),
            heat = Muscle.entries.associateWith { Analytics.heat(recency[it]) },
            proteinToday = Analytics.proteinOn(today, meals, extras),
            kcalToday = Analytics.kcalOn(today, meals, extras),
            streak = Analytics.streak(sets.map { it.day }.toSet(), today, p.startedOnDay),
            groupSessions = Analytics.groupSessions(sets, today - 7),
            suggestTopUp = Analytics.shouldSuggestTopUp(today, meals, p, extras),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState())

    fun history(exerciseId: String): Flow<List<SetLog>> = dao.setsFor(exerciseId)

    fun addSet(exerciseId: String, weightKg: Double, reps: Int) = viewModelScope.launch {
        dao.addSet(
            SetLog(
                exerciseId = exerciseId,
                day = today,
                weightKg = weightKg,
                reps = reps,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun deleteSet(id: Long) = viewModelScope.launch { dao.deleteSet(id) }

    fun setMeal(mealId: String, servings: Double, completed: Boolean) = viewModelScope.launch {
        dao.saveMeal(MealLog(day = today, mealId = mealId, servings = servings, completed = completed))
    }

    fun addExtra(label: String, proteinG: Double, kcal: Double) = viewModelScope.launch {
        dao.addExtra(
            ExtraIntake(
                day = today,
                label = label.ifBlank { "Extra" },
                proteinG = proteinG,
                kcal = kcal,
                createdAt = System.currentTimeMillis(),
            ),
        )
    }

    fun deleteExtra(id: Long) = viewModelScope.launch { dao.deleteExtra(id) }

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
