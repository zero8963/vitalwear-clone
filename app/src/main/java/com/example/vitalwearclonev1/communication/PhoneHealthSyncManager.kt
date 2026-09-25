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
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class PhoneHealthSyncManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class)
    )

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
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun hasAllPermissions(): Boolean {
        return try {
            getClient()?.permissionController?.getGrantedPermissions()?.containsAll(permissions) == true
        } catch (e: Exception) {
            false
        }
    }

    fun startPeriodicSync() {
        scope.launch {
            while (true) {
                // TEMP DIAG (2026-09-24): don't gate on the permission pre-check.
                // Attempt the reads directly; exceptions are caught inside syncNow().
                syncNow()
                // Battery fix (2026-09-25): was every 2 min; each sync does HC
                // reads + a BLE send to the watch. 15 min is plenty for a
                // virtual-pet step counter.
                kotlinx.coroutines.delay(900000)
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
            
            if (client != null) {
                val workoutResponse = client.readRecords(
                    ReadRecordsRequest(
                        ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startTime, endOfDay)
                    )
                )
                hasNewWorkout = workoutResponse.records.isNotEmpty()
            }

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

            // Samsung Health workouts count as healing workouts too.
            creditNewExerciseSessions()

            val samsungWorkouts = getSamsungWorkoutsToday()
            sendToWatch(stats.first, stats.second, hasNewWorkout, startOfDay.toEpochMilli(), weightKg.toFloat(),
                samsungWorkouts.count, samsungWorkouts.caloriesKcal)
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

    /** Counts today's Health Connect exercise sessions (Samsung Health syncs
     *  its workouts here) and the active calories burned inside them.
     *  Diagnostic companion to the workout healing credit — if this shows 0
     *  sessions, Samsung Health isn't sharing with Health Connect. */
    data class SamsungWorkoutSummary(
        val count: Int,
        val caloriesKcal: Int,
        val diag: String
    )

    suspend fun getSamsungWorkoutsToday(): SamsungWorkoutSummary {
        val client = getClient()
            ?: return SamsungWorkoutSummary(0, 0, "samsung workouts: Health Connect not available")
        return try {
            val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()
            val now = Instant.now()
            val sessions = mutableListOf<ExerciseSessionRecord>()
            var pageToken: String? = null
            do {
                val page = client.readRecords(
                    ReadRecordsRequest(
                        ExerciseSessionRecord::class,
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, now),
                        pageToken = pageToken
                    )
                )
                sessions += page.records
                pageToken = page.pageToken
            } while (pageToken != null)

            val origins = mutableMapOf<String, Int>()
            var counted = 0
            var totalKcal = 0.0
            for (s in sessions) {
                val pkg = s.metadata.dataOrigin.packageName
                origins[pkg] = (origins[pkg] ?: 0) + 1
                val mins = ChronoUnit.MINUTES.between(s.startTime, s.endTime)
                if (mins < MIN_WORKOUT_MINUTES) continue
                counted++
                var calToken: String? = null
                do {
                    val calPage = client.readRecords(
                        ReadRecordsRequest(
                            ActiveCaloriesBurnedRecord::class,
                            timeRangeFilter = TimeRangeFilter.between(s.startTime, s.endTime),
                            pageToken = calToken
                        )
                    )
                    for (r in calPage.records) totalKcal += r.energy.inKilocalories
                    calToken = calPage.pageToken
                } while (calToken != null)
            }
            val diag = if (sessions.isEmpty()) {
                "samsung workouts: no exercise sessions in Health Connect today — " +
                    "check Samsung Health > Settings > Health Connect sharing is ON"
            } else {
                val originBits = origins.entries.joinToString(", ") {
                    "${it.key.substringAfterLast('.')} x${it.value}"
                }
                "samsung workouts: ${sessions.size} session(s) today [$originBits], " +
                    "counted $counted (10+ min)"
            }
            SamsungWorkoutSummary(counted, totalKcal.toInt(), diag)
        } catch (e: Exception) {
            Timber.w(e, "Samsung workout summary read failed")
            SamsungWorkoutSummary(0, 0, "samsung workouts: read failed (${e.message})")
        }
    }

    companion object {
        /** Samsung Health's package name. Filtering Health Connect reads to this
         *  origin makes our numbers match the Samsung Health app 1:1, instead of
         *  summing every app that ever wrote steps. Requires "Share with Health
         *  Connect" turned ON in Samsung Health settings. */
        private const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"

        /** Sessions shorter than this don't count as workouts — filters out
         *  auto-detected junk like a 3-minute walk. */
        private const val MIN_WORKOUT_MINUTES = 10L
    }

    // TEMP DIAGNOSTIC (2026-09-24): one-line summary of where today's step
    // number came from. Shown under the Daily Activity card in the Workout
    // screen. REMOVE once Samsung parity is confirmed.
    var lastDiagString: String = "diag: not run yet"
        private set

    suspend fun getDailyStats(): Pair<Long, Int> = getDailyStatsWithDiag(includeOrigins = false).first

    /**
     * Samsung parity (2026-09-24): sum the raw step records for the given
     * origins instead of using aggregate(). Health Connect's aggregate merges
     * overlapping phone+watch intervals and undercounts vs the Samsung Health
     * app, which displays the straight sum of its own records (verified
     * on-device: raw sum 10,898 vs Samsung display 10,883, aggregate 7,991).
     */
    private suspend fun sumStepsForOrigins(
        client: HealthConnectClient,
        range: TimeRangeFilter,
        origins: Set<DataOrigin>
    ): Long {
        var total = 0L
        try {
            var pageToken: String? = null
            do {
                val page = client.readRecords(
                    ReadRecordsRequest(
                        StepsRecord::class,
                        timeRangeFilter = range,
                        dataOriginFilter = origins,
                        pageToken = pageToken
                    )
                )
                for (rec in page.records) total += rec.count
                pageToken = page.pageToken
            } while (pageToken != null)
        } catch (e: Exception) {
            Timber.w(e, "Raw Samsung step sum failed")
        }
        return total
    }

    /**
     * Battery note (2026-09-25): the per-origin breakdown pages through ALL of
     * today's step records — expensive. Only the Workout screen sets
     * includeOrigins=true; background syncs skip it.
     */
    suspend fun getDailyStatsWithDiag(includeOrigins: Boolean = false): Pair<Pair<Long, Int>, String> {
        var steps = 0L
        var calories = 0.0
        var stepsSource = "none"
        var permCheck = false
        var samsungSteps = -1L
        var allSteps = -1L
        var readError = "none"
        var originsDiag = ""

        val client = getClient()
        // TEMP DIAG: attempt the reads directly instead of gating on
        // hasAllPermissions(). We still record what the pre-check says.
        try {
            permCheck = hasAllPermissions()
        } catch (e: Exception) {
            readError = "permCheck threw ${e.javaClass.simpleName}"
        }
        if (client != null) {
            try {
                val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()
                val endOfDay = Instant.now()
                val range = TimeRangeFilter.between(startOfDay, endOfDay)
                val samsungOrigin = setOf(DataOrigin(SAMSUNG_HEALTH_PACKAGE))

                // 1) Prefer Samsung Health's own records -> matches the Samsung
                //    Health app exactly (raw sum, NOT aggregate: aggregate
                //    merges overlaps and undercounts vs Samsung's display).
                samsungSteps = sumStepsForOrigins(client, range, samsungOrigin)

                steps = if (samsungSteps > 0) {
                    stepsSource = "samsung"
                    samsungSteps
                } else {
                    // 2) Fallback: whatever Health Connect has (Samsung sharing
                    //    is off, or this is not a Samsung phone).
                    allSteps = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = range
                        )
                    )[StepsRecord.COUNT_TOTAL] ?: 0L
                    stepsSource = if (allSteps > 0) "health-connect" else "none"
                    allSteps
                }

                // Same origin policy for calories so the two stay consistent.
                val calsOrigin = if (stepsSource == "samsung") samsungOrigin else emptySet()
                calories = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                        timeRangeFilter = range,
                        dataOriginFilter = calsOrigin
                    )
                )[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0

                // TEMP DIAG (2026-09-24): per-package raw step sums for today, to
                // find which writer holds Samsung Health's full total. Raw sums
                // can overlap; this is only for diagnosis, not the final number.
                // Skipped for background syncs (battery).
                val originSteps = mutableMapOf<String, Long>()
                if (!includeOrigins) originsDiag = "skipped" else {
                try {
                    var pageToken: String? = null
                    do {
                        val page = client.readRecords(
                            ReadRecordsRequest(
                                StepsRecord::class,
                                timeRangeFilter = range,
                                pageToken = pageToken
                            )
                        )
                        for (rec in page.records) {
                            val pkg = rec.metadata.dataOrigin.packageName
                            originSteps[pkg] = (originSteps[pkg] ?: 0L) + rec.count
                        }
                        pageToken = page.pageToken
                    } while (pageToken != null)
                } catch (e: Exception) {
                    Timber.w(e, "Per-origin step read failed")
                }
                val topOrigins = originSteps.entries
                    .sortedByDescending { it.value }
                    .take(4)
                    .joinToString(",") { "${it.key.substringAfterLast('.')}=${it.value}" }
                originsDiag = topOrigins.ifEmpty { "none" }
                }
            } catch (e: Exception) {
                readError = "${e.javaClass.simpleName}: ${e.message}"
                Timber.w(e, "Health Connect aggregate failed")
            }
        } else {
            readError = "HealthConnectClient null (SDK unavailable?)"
        }

        // GPS fallback: only for phones whose sensors feed nothing into Health
        // Connect at all (broken sensor case). A real Health Connect number
        // always wins over the GPS estimate.
        val gpsPrefs = context.getSharedPreferences("gps_tracking_prefs", Context.MODE_PRIVATE)
        val gpsSteps = gpsPrefs.getInt("steps", 0).toLong()
        val gpsCals = gpsPrefs.getFloat("calories", 0f).toDouble()

        val finalSteps = if (steps > 0) steps else gpsSteps
        val finalCals = if (calories > 0) calories else gpsCals
        val path = when {
            steps > 0 -> stepsSource
            gpsSteps > 0 -> "gps_won"
            else -> "all_zero"
        }

        lastDiagString =
            "perm=$permCheck | samsung=$samsungSteps | all=$allSteps | gps=$gpsSteps | " +
            "final=$finalSteps | path=$path | err=$readError | origins=$originsDiag"
        Timber.tag("VitalWear/Steps").d(lastDiagString)
        Timber.d("Combined Stats: source=$stepsSource HealthSteps=$steps, GpsSteps=$gpsSteps, FinalSteps=$finalSteps")

        return Pair(Pair(finalSteps, finalCals.toInt()), lastDiagString)
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

    private suspend fun sendToWatch(steps: Long, calories: Int, hasWorkout: Boolean, startOfDayMillis: Long, weightKg: Float = 75f,
                               samsungWorkouts: Int = 0, samsungWorkoutCals: Int = 0) {
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
                // 2026-09-25: Samsung Health workout counter for the watch's
                // workout screen. Older watch builds stop reading after
                // weightKg, so appending is backward compatible.
                dos.writeInt(samsungWorkouts)
                dos.writeInt(samsungWorkoutCals)
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
