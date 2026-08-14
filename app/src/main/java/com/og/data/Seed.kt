package com.og.data

import java.time.LocalDate

/**
 * One-off backfill of sessions trained before the app could record them.
 *
 * Guarded on the day already being empty, so it runs once and never doubles up a session
 * the user has since edited.
 */
object Seed {

    private data class Entry(val exerciseId: String, val weightKg: Double, val reps: Int)

    /** 13 August 2026 — back and arms, reconstructed from the session as logged. */
    private val aug13 = listOf(
        Entry("lat_pulldown", 45.0, 8),
        Entry("lat_pulldown", 45.0, 8),
        Entry("lat_pulldown", 40.0, 5),
        Entry("seated_cable_row", 35.0, 8),
        Entry("seated_cable_row", 35.0, 8),
        Entry("seated_cable_row", 35.0, 8),
        Entry("shrug", 12.5, 8),
        Entry("straight_arm_pulldown", 35.0, 8),
        Entry("straight_arm_pulldown", 35.0, 8),
        Entry("hammer_curl", 10.0, 8),
        Entry("hammer_curl", 10.0, 8),
        Entry("preacher_curl", 45.0, 8),
        Entry("dumbbell_curl", 10.0, 8),
    )

    suspend fun backfill(dao: OgDao) {
        val day = LocalDate.of(2026, 8, 13).toEpochDay()
        if (dao.setCountOn(day) > 0) return

        // Ordered timestamps so the "last set" prefill picks the right weight.
        val base = day * 86_400_000L + 19 * 3_600_000L
        dao.putSets(
            aug13.mapIndexed { i, e ->
                SetLog(
                    exerciseId = e.exerciseId,
                    day = day,
                    weightKg = e.weightKg,
                    reps = e.reps,
                    createdAt = base + i * 60_000L,
                )
            },
        )
    }
}
