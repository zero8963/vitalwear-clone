package com.example.vitalwearclonev1.communication

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
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
            
            if (client != null && hasAllPermissions()) {
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
            if (client != null && hasAllPermissions()) {
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

    suspend fun getDailyStats(): Pair<Long, Int> {
        var steps = 0L
        var calories = 0.0

        val client = getClient()
        if (client != null && hasAllPermissions()) {
            try {
                val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant()
                val endOfDay = Instant.now()
                
                val stepsResponse = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
                steps = stepsResponse[StepsRecord.COUNT_TOTAL] ?: 0L

                val calsResponse = client.aggregate(
                    AggregateRequest(
                        metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                    )
                )
                calories = calsResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
            } catch (e: Exception) {
                Timber.w(e, "Health Connect aggregate failed")
            }
        }

        // GPS Fetch
        val gpsPrefs = context.getSharedPreferences("gps_tracking_prefs", Context.MODE_PRIVATE)
        val gpsSteps = gpsPrefs.getInt("steps", 0).toLong()
        val gpsCals = gpsPrefs.getFloat("calories", 0f).toDouble()
        
        // Use GPS if it has more data (for phones with broken sensors)
        val finalSteps = if (gpsSteps > steps) gpsSteps else steps
        val finalCals = if (gpsCals > calories) gpsCals else calories
        
        Timber.d("Combined Stats: HealthSteps=$steps, GpsSteps=$gpsSteps, FinalSteps=$finalSteps")
        
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
