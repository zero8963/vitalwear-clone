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
        // Steps -> vitals (2026-09-30): feed the step counter into Vital
        // Points like the real bracelet does. Delta-only, safe to call often.
        try {
            PhoneMonsterManager(context).syncStepsToVitalPoints(stats.first.toInt())
        } catch (e: Exception) {
            Timber.e(e, "Step->VP sync failed")
        }
        
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
                // 2026-09-25: block-aware — a real workout is a block with 10+
                // active minutes, not any single short auto-detected session.
                hasNewWorkout = clusterSessions(workoutResponse.records).any {
                    it.isRealWorkout()
                }
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
                samsungWorkouts.count, samsungWorkouts.caloriesKcal, samsungWorkouts.totalSessions)
        } catch (e: Exception) {
            Timber.e(e, "Error during Health Sync")
        }
    }

    /** 2026-09-25: Samsung Health writes each exercise of a routine
     *  (push-ups, pull-ups, ...) as its own short ExerciseSessionRecord —
     *  the user's real 21-min workout arrived as SIX sub-10-min sessions.
     *  Sessions starting within BLOCK_GAP_MINUTES of the previous session's
     *  end belong to the same workout block; the block's total active
     *  minutes is what counts as a workout. */
    data class WorkoutBlock(
        val sessions: List<ExerciseSessionRecord>,
        /** Total active time in seconds — compared against the 10-min bar. */
        val activeSeconds: Long,
        val startTime: Instant,
        val endTime: Instant
    )

    private fun clusterSessions(sessions: List<ExerciseSessionRecord>): List<WorkoutBlock> {
        val sorted = sessions.sortedBy { it.startTime }
        val blocks = mutableListOf<WorkoutBlock>()
        var cur = mutableListOf<ExerciseSessionRecord>()
        for (s in sorted) {
            val prev = cur.lastOrNull()
            if (prev != null &&
                ChronoUnit.MINUTES.between(prev.endTime, s.startTime) > BLOCK_GAP_MINUTES) {
                blocks += buildBlock(cur)
                cur = mutableListOf()
            }
            cur += s
        }
        if (cur.isNotEmpty()) blocks += buildBlock(cur)
        return blocks
    }

    private fun buildBlock(sessions: List<ExerciseSessionRecord>): WorkoutBlock {
        val active = sessions.sumOf { ChronoUnit.SECONDS.between(it.startTime, it.endTime) }
        return WorkoutBlock(
            sessions = sessions,
            activeSeconds = active,
            startTime = sessions.first().startTime,
            endTime = sessions.maxOf { it.endTime }
        )
    }

    private fun WorkoutBlock.isRealWorkout(): Boolean =
        activeSeconds >= MIN_WORKOUT_MINUTES * 60

    /**
     * Samsung Health workout healing + training (2026-09-23, block-aware
     * 2026-09-25, stat bonuses 2026-09-26): every workout BLOCK that lands in
     * Health Connect (Samsung Health syncs its workouts there) counts like
     * one of the app's own workouts —
     *   1. each newly-qualifying block gets one call to
     *      PhoneMonsterManager.recordExerciseCompleted(), shaving 15 min off
     *      the critical timer and resetting the overwork counter (no-op when
     *      the Digimon isn't critical, exactly like the in-app workouts);
     *   2. each newly-qualifying block ALSO grants training stat bonuses via
     *      applyWorkoutPowerUp(), with the completion ratio scaled by block
     *      length (a 20+ min block = a fully completed in-app routine), and
     *      the EXACT deltas are forwarded to the watch through
     *      sendWorkoutSession() so both Digimon gain identically.
     * Each block is credited at most once per purpose (tracked by block start
     * time), so this is safe to run on every sync. The bonus marker starts at
     * 0 on first run, so blocks already healing-credited by older builds still
     * get their stat bonuses in one catch-up pass over the 48h window.
     */
    suspend fun creditNewExerciseSessions() {
        val client = getClient() ?: return
        try {
            val now = Instant.now()
            val prefs = context.getSharedPreferences("workout_heal_prefs", Context.MODE_PRIVATE)
            val lastCreditedStart = prefs.getLong("last_credited_block_start", 0L)
            val lastBonusStart = prefs.getLong("last_bonus_block_start", 0L)

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

            var maxCreditedStart = lastCreditedStart
            var maxBonusStart = lastBonusStart
            val monsterManager = PhoneMonsterManager(context)
            for (block in clusterSessions(response.records)) {
                val startMs = block.startTime.toEpochMilli()
                if (!block.isRealWorkout()) continue
                if (startMs > lastCreditedStart) {
                    val fullyHealed = monsterManager.recordExerciseCompleted()
                    Timber.i("Credited ${block.activeSeconds / 60}min Health Connect workout block " +
                        "(${block.sessions.size} sessions, fully healed out of critical: $fullyHealed)")
                    if (startMs > maxCreditedStart) maxCreditedStart = startMs
                }
                if (startMs > lastBonusStart) {
                    val ratio = (block.activeSeconds / (FULL_BONUS_MINUTES * 60).toFloat())
                        .coerceIn(0f, 1f)
                    val deltas = monsterManager.applyWorkoutPowerUp(ratio)
                    val mins = block.activeSeconds / 60
                    if (deltas.sum() > 0) {
                        sendWorkoutSession("Samsung workout (${mins}m)", deltas[0], deltas[1], deltas[2], deltas[3])
                        Timber.i("Granted Samsung workout training bonus +${deltas[0]}/+${deltas[1]}/" +
                            "+${deltas[2]}/+${deltas[3]} for ${mins}min block (ratio $ratio)")
                    }
                    if (startMs > maxBonusStart) maxBonusStart = startMs
                }
            }
            prefs.edit()
                .putLong("last_credited_block_start", maxCreditedStart)
                .putLong("last_bonus_block_start", maxBonusStart)
                .apply()
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
        val diag: String,
        /** Every exercise session found today, counted or not — proves detection works. */
        val totalSessions: Int
    )

    suspend fun getSamsungWorkoutsToday(): SamsungWorkoutSummary {
        val client = getClient()
            ?: return SamsungWorkoutSummary(0, 0, "samsung workouts: Health Connect not available", 0)
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
            val zone = java.time.ZoneId.systemDefault()
            val timeFmt = java.time.format.DateTimeFormatter.ofPattern("h:mma")
            // 2026-09-25: per-session detail (duration + start time) so the user can
            // see exactly what was detected — Samsung splits one routine into a
            // short session per exercise, so sessions are clustered into blocks.
            val sessionBits = mutableListOf<String>()
            for (s in sessions.sortedBy { it.startTime }) {
                val pkg = s.metadata.dataOrigin.packageName
                origins[pkg] = (origins[pkg] ?: 0) + 1
                val mins = ChronoUnit.MINUTES.between(s.startTime, s.endTime)
                val t = ZonedDateTime.ofInstant(s.startTime, zone).format(timeFmt).lowercase()
                sessionBits += "${mins}m@$t"
            }
            var counted = 0
            var totalKcal = 0.0
            val blockBits = mutableListOf<String>()
            for (b in clusterSessions(sessions)) {
                val t = ZonedDateTime.ofInstant(b.startTime, zone).format(timeFmt).lowercase()
                blockBits += "$t ${b.activeSeconds / 60}m x${b.sessions.size}"
                if (!b.isRealWorkout()) continue
                counted++
                for (s in b.sessions) {
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
            }
            val diag = if (sessions.isEmpty()) {
                "samsung workouts: no exercise sessions in Health Connect today — " +
                    "check Samsung Health > Settings > Health Connect sharing is ON"
            } else {
                val originBits = origins.entries.joinToString(", ") {
                    "${it.key.substringAfterLast('.')} x${it.value}"
                }
                val detail = sessionBits.take(8).joinToString(", ") +
                    if (sessionBits.size > 8) " +${sessionBits.size - 8} more" else ""
                "samsung workouts: ${sessions.size} session(s) in ${blockBits.size} block(s) " +
                    "[$originBits]: $detail — counted $counted (10+ min active, each earns training stats)"
            }
            SamsungWorkoutSummary(counted, totalKcal.toInt(), diag, sessions.size)
        } catch (e: Exception) {
            Timber.w(e, "Samsung workout summary read failed")
            SamsungWorkoutSummary(0, 0, "samsung workouts: read failed (${e.message})", 0)
        }
    }

    companion object {
        /** Samsung Health's package name. Filtering Health Connect reads to this
         *  origin makes our numbers match the Samsung Health app 1:1, instead of
         *  summing every app that ever wrote steps. Requires "Share with Health
         *  Connect" turned ON in Samsung Health settings. */
        private const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.shealth"

        /** Sessions shorter than this don't count as workouts — filters out
         *  auto-detected junk like a 3-minute walk. Applied to a workout
         *  BLOCK's total active minutes (2026-09-25), not single sessions. */
        private const val MIN_WORKOUT_MINUTES = 10L

        /** Sessions starting within this many minutes of the previous
         *  session's end are clustered into one workout block (2026-09-25). */
        private const val BLOCK_GAP_MINUTES = 5L

        /** A workout block this long (or longer) earns the full training
         *  bonus of a completed in-app routine; shorter qualifying blocks
         *  scale down proportionally (2026-09-26). */
        private const val FULL_BONUS_MINUTES = 20L
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

    /** 2026-09-25: sends the EXACT stat deltas the phone's Digimon just gained,
     *  so the watch's Digimon gets the identical training bonus instead of
     *  rolling its own different random numbers. Payload: UTF routine name +
     *  4 ints (atk, hp, spd, def). Old watch builds read the name + first int
     *  and ignore the rest, so this degrades gracefully. */
    suspend fun sendWorkoutSession(routineName: String, atkDelta: Int, hpDelta: Int, spdDelta: Int, defDelta: Int) {
        try {
            val nodes = Wearable.getNodeClient(context).connectedNodes.await()
            val messageClient = Wearable.getMessageClient(context)

            val payload = ByteArrayOutputStream().use { bos ->
                val dos = DataOutputStream(bos)
                dos.writeUTF(routineName)
                dos.writeInt(atkDelta)
                dos.writeInt(hpDelta)
                dos.writeInt(spdDelta)
                dos.writeInt(defDelta)
                dos.flush()
                bos.toByteArray()
            }

            for (node in nodes) {
                messageClient.sendMessage(node.id, "/WORKOUT_SESSION", payload).await()
            }
            Timber.i("Sent workout session to watch: $routineName (+$atkDelta/+$hpDelta/+$spdDelta/+$defDelta)")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send workout session to watch")
        }
    }

    private suspend fun sendToWatch(steps: Long, calories: Int, hasWorkout: Boolean, startOfDayMillis: Long, weightKg: Float = 75f,
                               samsungWorkouts: Int = 0, samsungWorkoutCals: Int = 0, samsungSessions: Int = 0) {
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
                // 2026-09-25: total detected sessions (counted or not) so the
                // watch can show "6 sessions, 0 counted" instead of just 0.
                dos.writeInt(samsungSessions)
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
