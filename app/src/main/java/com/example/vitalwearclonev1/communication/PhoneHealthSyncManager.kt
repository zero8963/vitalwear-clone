package com.example.vitalwearclonev1.communication

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.vitalwearclonev1.monster.PhoneMonsterManager
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class PhoneHealthSyncManager(private val context: Context) {

    companion object {
        // Samsung Health's package — used to read ITS numbers first so the app
        // matches the Samsung Health watch-face complication 1:1.
        private const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"
        private const val LOG_TAG = "VitalWear/Steps"

        /** Sessions shorter than this don't count as workouts — filters out
         *  auto-detected junk like a 3-minute walk. */
        private const val MIN_WORKOUT_MINUTES = 10L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

    /**
     * TEMPORARY diagnostic readout of the last getDailyStats() call.
     * Shown on the Workout screen while we verify Samsung step parity.
     * Remove once numbers are confirmed matching.
     */
    var lastDiagnostics: String = "no read yet"
        private set

    fun getSdkStatus(): Int {
        return try {
            HealthConnectClient.getSdkStatus(context)
        } catch (e: Exception) {
            HealthConnectClient.SDK_UNAVAILABLE
        }
    }

    private fun getClient(): HealthConnectClient? {
        return try {
            if (getSdkStatus() == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                Timber.tag(LOG_TAG).w("Health Connect not available (status=${getSdkStatus()})")
                null
            }
        } catch (e: Exception) {
            Timber.tag(LOG_TAG).e(e, "Failed to create Health Connect client")
            null
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        return try {
            getClient()?.permissionController?.getGrantedPermissions()?.containsAll(permissions) == true
        } catch (e: Exception) {
            Timber.tag(LOG_TAG).w(e, "Permission check threw")
            false
        }
    }

    fun startPeriodicSync() {
        scope.launch {
            while (true) {
                if (hasAllPermissions()) {
                    syncNow()
                } else {
                    Timber.w("Missing Health Connect permissions, sync skipped")
                }
                kotlinx.coroutines.delay(120000) // Sync every 2 minutes for better accuracy
            }
        }
    }

    suspend fun syncNow() {
        val stats = getDailyStats()

        try {
            Timber.d("Starting Health Sync with combined GPS/Health data...")
            val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()
            val endOfDay = Instant.now()

            val client = getClient()
            val startTime = Instant.now().minus(1, ChronoUnit.HOURS)
            var hasNewWorkout = false

            // NOTE: reads are attempted directly (try/catch) instead of gating
            // on hasAllPermissions() — the gate was suspected of silently
            // skipping reads on some devices. A denied read throws
            // SecurityException, which the surrounding try/catch handles.
            if (client != null) {
                val workoutResponse = client.readRecords(
                    ReadRecordsRequest(
                        ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endOfDay)
                    )
                )
                hasNewWorkout = workoutResponse.records.isNotEmpty()
            }

            // Samsung Health workout healing: every exercise session that lands
            // in Health Connect counts like one of the app's own workouts.
            creditNewExerciseSessions()

            // Fetch latest Weight for accurate calorie math on watch side if needed
            var weightKg = 75.0
            if (client != null) {
                val weightResponse = client.readRecords(
                    ReadRecordsRequest(
                        WeightRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(Instant.EPOCH, endOfDay),
                        ascendingOrder = false,
                        pageSize = 1
                    )
                )
                weightKg = weightResponse.records.firstOrNull()?.weight?.inKilograms ?: 75.0
            }

            sendToWatch(stats.first, stats.second, hasNewWorkout, startOfDay.toEpochMilli(), weightKg.toFloat())
        } catch (e: Exception) {
            Timber.e(e, "Error during Health Sync")
        }
    }

    /**
     * Samsung Health workout healing (2026-09-23): every exercise session that
     * lands in Health Connect (Samsung Health syncs its workouts there) counts
     * like one of the app's own workouts — each new session gets one call to
     * PhoneMonsterManager.recordExerciseCompleted(), shaving 15 min off the
     * critical timer. It's a no-op when the Digimon isn't critical, exactly
     * like the in-app workouts. Each session is credited at most once (tracked
     * by end time), so this is safe to run on every sync.
     */
    suspend fun creditNewExerciseSessions() {
        val client = getClient() ?: return
        try {
            val now = Instant.now()
            val prefs = context.getSharedPreferences("workout_heal_prefs", Context.MODE_PRIVATE)
            val lastCreditedEnd = prefs.getLong("last_credited_session_end", 0L)

            val response = client.readRecords(
                ReadRecordsRequest(
                    ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(
                        now.minus(48, ChronoUnit.HOURS),
                        now
                    ),
                    ascendingOrder = true
                )
            )

            var maxEnd = lastCreditedEnd
            val monsterManager = PhoneMonsterManager(context)
            for (session in response.records) {
                val endMs = session.endTime.toEpochMilli()
                if (endMs > maxEnd) maxEnd = endMs
                if (endMs <= lastCreditedEnd) continue
                val minutes = ChronoUnit.MINUTES.between(session.startTime, session.endTime)
                if (minutes < MIN_WORKOUT_MINUTES) continue
                val fullyHealed = monsterManager.recordExerciseCompleted()
                Timber.i("Credited ${minutes}min Health Connect workout (fully healed out of critical: $fullyHealed)")
            }
            prefs.edit().putLong("last_credited_session_end", maxEnd).apply()
        } catch (e: Exception) {
            Timber.w(e, "Exercise session credit check failed")
        }
    }

    suspend fun getDailyStats(): Pair<Long, Int> {
        var steps = 0L
        var calories = 0.0
        var samsungSteps = -1L   // -1 = read never attempted
        var allSteps = -1L       // -1 = read never attempted
        var path = "no_client"

        val client = getClient()
        val permOk = try {
            hasAllPermissions()
        } catch (e: Exception) {
            false
        }

        if (client != null) {
            // Attempt the read directly instead of gating on permOk: a truly
            // denied read throws SecurityException, which we catch below.
            // (The old gate was suspected of silently skipping reads.)
            val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()
            val endOfDay = Instant.now()

            // 1) Samsung Health first — matches the Samsung Health watch-face
            //    complication 1:1 instead of summing every writer on the phone.
            try {
                samsungSteps = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                        dataOriginFilter = setOf(DataOrigin(SAMSUNG_HEALTH_PACKAGE))
                    )
                )[StepsRecord.COUNT_TOTAL] ?: 0L
                val samsungCals = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay),
                        dataOriginFilter = setOf(DataOrigin(SAMSUNG_HEALTH_PACKAGE))
                    )
                )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                path = "samsung_ok"
                steps = samsungSteps
                calories = samsungCals
            } catch (e: Exception) {
                path = "samsung_fail:${e.javaClass.simpleName}"
                Timber.tag(LOG_TAG).w(e, "Samsung-origin read failed")
            }

            // 2) Fallback: all writers, for when Samsung Health shares nothing.
            if (steps == 0L) {
                try {
                    allSteps = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                        )
                    )[StepsRecord.COUNT_TOTAL] ?: 0L
                    val allCals = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                        )
                    )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                    path += "+fallback_ok"
                    steps = allSteps
                    calories = allCals
                } catch (e: Exception) {
                    path += "+fallback_fail:${e.javaClass.simpleName}"
                    Timber.tag(LOG_TAG).w(e, "Fallback aggregate failed")
                }
            }
        } else {
            Timber.tag(LOG_TAG).w("Health Connect client null, skipping read")
        }

        // GPS Fetch — only wins when Health Connect has zero steps
        // (broken-sensor fallback). Never overrides real Health Connect data,
        // otherwise the app and the Samsung-based watch face drift apart.
        val gpsPrefs = context.getSharedPreferences("gps_tracking_prefs", Context.MODE_PRIVATE)
        val gpsSteps = gpsPrefs.getInt("steps", 0).toLong()
        val gpsCals = gpsPrefs.getFloat("calories", 0f).toDouble()

        val finalSteps = if (steps == 0L && gpsSteps > 0) gpsSteps else steps
        val finalCals = if (calories == 0.0 && gpsCals > 0) gpsCals else calories
        if (finalSteps != steps) path += "+gps_won"

        lastDiagnostics = "perm=$permOk | samsung=$samsungSteps | all=$allSteps | gps=$gpsSteps | final=$finalSteps | path=$path"
        Timber.tag(LOG_TAG).d(lastDiagnostics)

        return Pair(finalSteps, finalCals.toInt())
    }

    suspend fun sendWorkoutSession(routineName: String, caloriesBurned: Int) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val messageClient = Wearable.getMessageClient(context)

            val payload = ByteArrayOutputStream().use { bos ->
                val dos = DataOutputStream(bos)
                dos.writeUTF(routineName)
                dos.writeInt(caloriesBurned)
                dos.flush()
                bos.toByteArray()
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/WORKOUT_SESSION", payload).await()
            }
            Timber.i("Sent workout session to watch: $routineName, $caloriesBurned kcal")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send workout session to watch")
        }
    }

    private suspend fun sendToWatch(steps: Long, calories: Int, hasWorkout: Boolean, startOfDayMillis: Long, weightKg: Float = 75f) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val messageClient = Wearable.getMessageClient(context)

            val payload = ByteArrayOutputStream().use { bos ->
                val dos = DataOutputStream(bos)
                dos.writeLong(steps)
                dos.writeInt(calories)
                dos.writeBoolean(hasWorkout)
                dos.writeLong(startOfDayMillis)
                dos.writeFloat(weightKg)
                dos.flush()
                bos.toByteArray()
            }

            if (nodes.isEmpty()) {
                Timber.w("No connected Wear OS nodes found for health sync")
                return
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/HEALTH_SYNC", payload).await()
            }
            Timber.d("Sent health sync to watch: Steps=$steps, Cals=$calories, WeightKg=$weightKg")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send health sync message")
        }
    }
}
