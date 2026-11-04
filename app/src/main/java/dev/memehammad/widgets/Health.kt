package dev.memehammad.widgets

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Steps, distance, calories and workouts from Health Connect (which Google Fit, Fitbit,
 * Samsung Health, Strava and others can write to). Read in the background and cached for widgets.
 */
object Health {
    val STEPS = HealthPermission.getReadPermission(StepsRecord::class)
    val DISTANCE = HealthPermission.getReadPermission(DistanceRecord::class)
    val CALORIES = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    val EXERCISE = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    const val BACKGROUND = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

    val permissions = setOf(STEPS, DISTANCE, CALORIES, EXERCISE)

    data class Day(val steps: Long, val km: Double, val kcal: Double)

    data class Session(val title: String, val type: Int, val start: Long, val end: Long, val km: Double?, val app: String)

    data class Data(
        val today: Day,
        /** Steps for the last 7 days, oldest first, today last. */
        val week: List<Long>,
        val sessions: List<Session>,
        val weekCount: Int,
        val weekKm: Double,
        val fetchedAt: Long,
    )

    fun status(c: Context) = HealthConnectClient.getSdkStatus(c)
    fun available(c: Context) = status(c) == HealthConnectClient.SDK_AVAILABLE

    private fun client(c: Context) = HealthConnectClient.getOrCreate(c)

    suspend fun granted(c: Context): Set<String> =
        if (!available(c)) emptySet()
        else runCatching { client(c).permissionController.getGrantedPermissions() }.getOrDefault(emptySet())

    fun backgroundSupported(c: Context): Boolean = available(c) && runCatching {
        client(c).features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }.getOrDefault(false)

    /** What to ask for: all data types, plus background reads where the phone supports them. */
    fun request(c: Context): Set<String> = if (backgroundSupported(c)) permissions + BACKGROUND else permissions

    private fun prefs(c: Context) = c.getSharedPreferences("health", Context.MODE_PRIVATE)

    suspend fun refresh(c: Context, maxAgeMs: Long = 60_000L) {
        if (!available(c)) return
        if (System.currentTimeMillis() - prefs(c).getLong("fetched", 0L) < maxAgeMs) return
        val g = granted(c)
        if (STEPS !in g && EXERCISE !in g) return
        val hc = client(c)
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val now = Instant.now()
        try {
            // Today.
            val metrics = buildSet<AggregateMetric<*>> {
                if (STEPS in g) add(StepsRecord.COUNT_TOTAL)
                if (DISTANCE in g) add(DistanceRecord.DISTANCE_TOTAL)
                if (CALORIES in g) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            }
            var day = Day(0, 0.0, 0.0)
            if (metrics.isNotEmpty()) {
                val r = hc.aggregate(AggregateRequest(metrics, TimeRangeFilter.between(today.atStartOfDay(zone).toInstant(), now)))
                day = Day(
                    r[StepsRecord.COUNT_TOTAL] ?: 0L,
                    r[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0,
                    r[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0,
                )
            }
            // Last 7 days of steps.
            val week = if (STEPS in g) {
                val groups = hc.aggregateGroupByPeriod(
                    AggregateGroupByPeriodRequest(
                        setOf(StepsRecord.COUNT_TOTAL),
                        TimeRangeFilter.between(today.minusDays(6).atStartOfDay(), LocalDateTime.now()),
                        Period.ofDays(1),
                    ),
                )
                (0..6).map { i ->
                    val d = today.minusDays((6 - i).toLong())
                    groups.firstOrNull { it.startTime.toLocalDate() == d }?.result?.get(StepsRecord.COUNT_TOTAL) ?: 0L
                }
            } else List(7) { 0L }
            // Workouts (last 30 days, newest first).
            val sessions = mutableListOf<Session>()
            if (EXERCISE in g) {
                val recs = hc.readRecords(
                    ReadRecordsRequest(
                        ExerciseSessionRecord::class,
                        TimeRangeFilter.after(now.minusSeconds(30L * 24 * 3600)),
                        ascendingOrder = false,
                        pageSize = 30,
                    ),
                ).records
                for (r in recs.sortedByDescending { it.startTime }) {
                    val km = if (DISTANCE in g) runCatching {
                        hc.aggregate(
                            AggregateRequest(
                                setOf(DistanceRecord.DISTANCE_TOTAL),
                                TimeRangeFilter.between(r.startTime, r.endTime),
                                setOf(r.metadata.dataOrigin),
                            ),
                        )[DistanceRecord.DISTANCE_TOTAL]?.inKilometers
                    }.getOrNull()?.takeIf { it > 0.01 } else null
                    sessions += Session(
                        r.title?.takeIf { it.isNotBlank() } ?: typeLabel(r.exerciseType),
                        r.exerciseType, r.startTime.toEpochMilli(), r.endTime.toEpochMilli(), km,
                        r.metadata.dataOrigin.packageName,
                    )
                    if (sessions.size >= 20) break
                }
            }
            val weekStart = today.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek))
                .atStartOfDay(zone).toInstant().toEpochMilli()
            val thisWeek = sessions.filter { it.start >= weekStart }
            save(c, Data(day, week, sessions.take(5), thisWeek.size, thisWeek.sumOf { it.km ?: 0.0 }, System.currentTimeMillis()))
        } catch (_: Exception) {
            // Background reads not allowed, Health Connect busy, ... keep the last data.
        }
    }

    private fun save(c: Context, d: Data) {
        val o = JSONObject()
            .put("steps", d.today.steps).put("km", d.today.km).put("kcal", d.today.kcal)
            .put("week", JSONArray(d.week)).put("weekCount", d.weekCount).put("weekKm", d.weekKm)
            .put("sessions", JSONArray(d.sessions.map {
                JSONObject().put("title", it.title).put("type", it.type).put("start", it.start).put("end", it.end)
                    .put("km", it.km ?: -1.0).put("app", it.app)
            }))
            .put("day", LocalDate.now().toString())
        prefs(c).edit().putString("data", o.toString()).putLong("fetched", d.fetchedAt).apply()
    }

    fun cached(c: Context): Data? {
        val o = prefs(c).getString("data", null)?.let { runCatching { JSONObject(it) }.getOrNull() } ?: return null
        return runCatching {
            // Yesterday's numbers don't count for today.
            val sameDay = o.optString("day") == LocalDate.now().toString()
            val w = o.getJSONArray("week")
            val s = o.getJSONArray("sessions")
            Data(
                if (sameDay) Day(o.getLong("steps"), o.getDouble("km"), o.getDouble("kcal")) else Day(0, 0.0, 0.0),
                (0 until w.length()).map { w.getLong(it) },
                (0 until s.length()).map {
                    val x = s.getJSONObject(it)
                    Session(
                        x.getString("title"), x.getInt("type"), x.getLong("start"), x.getLong("end"),
                        x.getDouble("km").takeIf { k -> k >= 0 }, x.getString("app"),
                    )
                },
                o.getInt("weekCount"), o.getDouble("weekKm"), prefs(c).getLong("fetched", 0L),
            )
        }.getOrNull()
    }

    fun typeLabel(type: Int): String = when (type) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> "Run"
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING, ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY -> "Ride"
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Walk"
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> "Hike"
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL, ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> "Swim"
        ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING, ExerciseSessionRecord.EXERCISE_TYPE_WEIGHTLIFTING -> "Strength"
        ExerciseSessionRecord.EXERCISE_TYPE_YOGA, ExerciseSessionRecord.EXERCISE_TYPE_PILATES -> "Yoga"
        else -> "Workout"
    }

    /** Icon and shape for a workout type. */
    fun typeLook(type: Int): Pair<Int, String> = when (typeLabel(type)) {
        "Run" -> R.drawable.ic_run to "arrow"
        "Ride" -> R.drawable.ic_bike to "cookie12"
        "Walk" -> R.drawable.ic_walk to "pill"
        "Hike" -> R.drawable.ic_walk to "triangle"
        "Swim" -> R.drawable.ic_swim to "ghostish"
        "Strength" -> R.drawable.ic_fitness to "square"
        "Yoga" -> R.drawable.ic_yoga to "flower"
        else -> R.drawable.ic_bolt to "cookie9"
    }
}
