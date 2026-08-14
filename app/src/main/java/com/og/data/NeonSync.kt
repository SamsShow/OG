package com.og.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

/**
 * Backup and restore against a Neon Postgres database.
 *
 * The whole local database goes up as a single JSON document in one row, keyed by device.
 * That is deliberate: this is a personal backup, not multi-device collaboration, so
 * last-write-wins on a blob avoids a per-table merge that would be far more code and far
 * more ways to lose data. Restore replaces local rows by primary key.
 *
 * The connection string is entered in the app and stored locally. It is never compiled in,
 * so no credential reaches the repository or the APK. See [NeonSync.isConfigured].
 *
 * Talks to Neon's SQL-over-HTTP endpoint, so there is no Postgres driver dependency.
 */
object NeonSync {

    class SyncError(message: String) : Exception(message)

    fun isConfigured(url: String) = url.startsWith("postgres://") || url.startsWith("postgresql://")

    /**
     * Neon's HTTP endpoint lives on the same host as the connection string, under /sql.
     * Pooled hosts (`-pooler`) do not serve it, so that suffix is stripped.
     */
    private fun endpointOf(connectionString: String): URL {
        val uri = URI(connectionString)
        val host = uri.host?.replace("-pooler", "")
            ?: throw SyncError("That connection string has no host in it.")
        return URI("https", host, "/sql", null).toURL()
    }

    private fun query(connectionString: String, sql: String, params: List<Any?>): JSONObject {
        val conn = (endpointOf(connectionString).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Neon-Connection-String", connectionString)
            setRequestProperty("Neon-Raw-Text-Output", "true")
            setRequestProperty("Neon-Array-Mode", "false")
        }
        val body = JSONObject()
            .put("query", sql)
            .put("params", JSONArray(params))
            .toString()
        conn.outputStream.use { it.write(body.toByteArray()) }

        val code = conn.responseCode
        val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
            ?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
        if (code !in 200..299) {
            throw SyncError("Neon returned $code. ${text.take(300)}")
        }
        return JSONObject(text)
    }

    private const val CREATE = """
        CREATE TABLE IF NOT EXISTS og_backup (
            device_id TEXT PRIMARY KEY,
            payload   TEXT NOT NULL,
            updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
        )
    """

    suspend fun push(dao: OgDao, connectionString: String, deviceId: String): Int =
        withContext(Dispatchers.IO) {
            if (!isConfigured(connectionString)) throw SyncError("Add your Neon connection string first.")
            val payload = snapshot(dao).toString()
            query(connectionString, CREATE, emptyList())
            query(
                connectionString,
                """
                INSERT INTO og_backup (device_id, payload, updated_at)
                VALUES ($1, $2, now())
                ON CONFLICT (device_id)
                DO UPDATE SET payload = EXCLUDED.payload, updated_at = now()
                """.trimIndent(),
                listOf(deviceId, payload),
            )
            payload.length
        }

    /** Returns the number of rows written back, or throws if there is no backup to pull. */
    suspend fun pull(dao: OgDao, connectionString: String, deviceId: String): Int =
        withContext(Dispatchers.IO) {
            if (!isConfigured(connectionString)) throw SyncError("Add your Neon connection string first.")
            query(connectionString, CREATE, emptyList())
            val res = query(
                connectionString,
                "SELECT payload FROM og_backup WHERE device_id = $1",
                listOf(deviceId),
            )
            val rows = res.optJSONArray("rows") ?: JSONArray()
            if (rows.length() == 0) throw SyncError("No backup found for this device yet.")
            val payload = rows.getJSONObject(0).optString("payload")
            if (payload.isBlank()) throw SyncError("The stored backup is empty.")
            restore(dao, JSONObject(payload))
        }

    // ---------------------------------------------------------------- snapshot

    private suspend fun snapshot(dao: OgDao): JSONObject {
        val o = JSONObject()
        o.put("version", 1)

        dao.profileOnce()?.let { p ->
            o.put(
                "profile",
                JSONObject()
                    .put("heightCm", p.heightCm)
                    .put("startWeightKg", p.startWeightKg)
                    .put("startBodyFatPct", p.startBodyFatPct)
                    .put("startSmmKg", p.startSmmKg)
                    .put("startedOnDay", p.startedOnDay)
                    .put("creatineStartDay", p.creatineStartDay ?: JSONObject.NULL)
                    .put("weeklyWorkoutTarget", p.weeklyWorkoutTarget)
                    .put("proteinMin", p.proteinMin)
                    .put("proteinMax", p.proteinMax)
                    .put("onboarded", p.onboarded),
            )
        }

        o.put(
            "sets",
            JSONArray().also { arr ->
                dao.allSets().forEach {
                    arr.put(
                        JSONObject()
                            .put("exerciseId", it.exerciseId)
                            .put("day", it.day)
                            .put("weightKg", it.weightKg)
                            .put("reps", it.reps)
                            .put("createdAt", it.createdAt),
                    )
                }
            },
        )
        o.put(
            "meals",
            JSONArray().also { arr ->
                dao.allMeals().forEach {
                    arr.put(
                        JSONObject()
                            .put("day", it.day)
                            .put("mealId", it.mealId)
                            .put("servings", it.servings)
                            .put("completed", it.completed),
                    )
                }
            },
        )
        o.put(
            "extras",
            JSONArray().also { arr ->
                dao.allExtras().forEach {
                    arr.put(
                        JSONObject()
                            .put("day", it.day)
                            .put("label", it.label)
                            .put("proteinG", it.proteinG)
                            .put("kcal", it.kcal)
                            .put("createdAt", it.createdAt),
                    )
                }
            },
        )
        o.put(
            "measurements",
            JSONArray().also { arr ->
                dao.allMeasurements().forEach {
                    arr.put(
                        JSONObject()
                            .put("day", it.day)
                            .put("weightKg", it.weightKg ?: JSONObject.NULL)
                            .put("bodyFatPct", it.bodyFatPct ?: JSONObject.NULL)
                            .put("smmKg", it.smmKg ?: JSONObject.NULL)
                            .put("shouldersCm", it.shouldersCm ?: JSONObject.NULL)
                            .put("chestCm", it.chestCm ?: JSONObject.NULL)
                            .put("waistCm", it.waistCm ?: JSONObject.NULL)
                            .put("hipsCm", it.hipsCm ?: JSONObject.NULL)
                            .put("armCm", it.armCm ?: JSONObject.NULL)
                            .put("thighCm", it.thighCm ?: JSONObject.NULL),
                    )
                }
            },
        )
        o.put(
            "dayLogs",
            JSONArray().also { arr ->
                dao.allDayLogs().forEach {
                    arr.put(
                        JSONObject()
                            .put("day", it.day)
                            .put("groups", it.groups)
                            .put("note", it.note),
                    )
                }
            },
        )
        o.put(
            "customExercises",
            JSONArray().also { arr ->
                dao.customExercisesOnce().forEach {
                    arr.put(
                        JSONObject()
                            .put("id", it.id)
                            .put("name", it.name)
                            .put("muscleGroup", it.muscleGroup)
                            .put("primaryMuscles", it.primaryMuscles)
                            .put("equipment", it.equipment),
                    )
                }
            },
        )
        return o
    }

    private fun JSONObject.dbl(key: String): Double? =
        if (isNull(key)) null else optDouble(key)

    private suspend fun restore(dao: OgDao, o: JSONObject): Int {
        var written = 0

        o.optJSONObject("profile")?.let { p ->
            // The connection string and reminder flag stay local — they are device settings,
            // not training data, and overwriting them from a backup would be surprising.
            val existing = dao.profileOnce()
            dao.saveProfile(
                Profile(
                    heightCm = p.optDouble("heightCm", 178.0),
                    startWeightKg = p.optDouble("startWeightKg", 69.0),
                    startBodyFatPct = p.optDouble("startBodyFatPct", 19.0),
                    startSmmKg = p.optDouble("startSmmKg", 32.5),
                    startedOnDay = p.optLong("startedOnDay", 0L),
                    creatineStartDay = if (p.isNull("creatineStartDay")) null else p.optLong("creatineStartDay"),
                    weeklyWorkoutTarget = p.optInt("weeklyWorkoutTarget", 5),
                    proteinMin = p.optInt("proteinMin", 100),
                    proteinMax = p.optInt("proteinMax", 140),
                    onboarded = p.optBoolean("onboarded", true),
                    neonUrl = existing?.neonUrl.orEmpty(),
                    reminderOn = existing?.reminderOn ?: true,
                ),
            )
            written++
        }

        o.optJSONArray("sets")?.let { arr ->
            val rows = (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                SetLog(
                    exerciseId = j.optString("exerciseId"),
                    day = j.optLong("day"),
                    weightKg = j.optDouble("weightKg", 0.0),
                    reps = j.optInt("reps"),
                    createdAt = j.optLong("createdAt"),
                )
            }
            dao.putSets(rows); written += rows.size
        }
        o.optJSONArray("meals")?.let { arr ->
            val rows = (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                MealLog(
                    day = j.optLong("day"),
                    mealId = j.optString("mealId"),
                    servings = j.optDouble("servings", 1.0),
                    completed = j.optBoolean("completed"),
                )
            }
            dao.putMeals(rows); written += rows.size
        }
        o.optJSONArray("extras")?.let { arr ->
            val rows = (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                ExtraIntake(
                    day = j.optLong("day"),
                    label = j.optString("label"),
                    proteinG = j.optDouble("proteinG", 0.0),
                    kcal = j.optDouble("kcal", 0.0),
                    createdAt = j.optLong("createdAt"),
                )
            }
            dao.putExtras(rows); written += rows.size
        }
        o.optJSONArray("measurements")?.let { arr ->
            val rows = (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                Measurement(
                    day = j.optLong("day"),
                    weightKg = j.dbl("weightKg"),
                    bodyFatPct = j.dbl("bodyFatPct"),
                    smmKg = j.dbl("smmKg"),
                    shouldersCm = j.dbl("shouldersCm"),
                    chestCm = j.dbl("chestCm"),
                    waistCm = j.dbl("waistCm"),
                    hipsCm = j.dbl("hipsCm"),
                    armCm = j.dbl("armCm"),
                    thighCm = j.dbl("thighCm"),
                )
            }
            dao.putMeasurements(rows); written += rows.size
        }
        o.optJSONArray("dayLogs")?.let { arr ->
            val rows = (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                DayLog(day = j.optLong("day"), groups = j.optString("groups"), note = j.optString("note"))
            }
            dao.putDayLogs(rows); written += rows.size
        }
        o.optJSONArray("customExercises")?.let { arr ->
            val rows = (0 until arr.length()).map { i ->
                val j = arr.getJSONObject(i)
                CustomExercise(
                    id = j.optString("id"),
                    name = j.optString("name"),
                    muscleGroup = j.optString("muscleGroup"),
                    primaryMuscles = j.optString("primaryMuscles"),
                    equipment = j.optString("equipment"),
                )
            }
            dao.putCustomExercises(rows); written += rows.size
        }
        return written
    }
}
