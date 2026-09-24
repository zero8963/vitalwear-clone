package com.example.vitalwearclonev1.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.vitalwearclonev1.MainActivity
import com.example.vitalwearclonev1.monster.MonsterManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber

import android.content.pm.ServiceInfo

import android.content.BroadcastReceiver
import android.content.IntentFilter

class VitalForegroundService : Service() {

    val binder = VitalBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    lateinit var sensorManager: VitalSensorManager
    private lateinit var monsterManager: MonsterManager
    
    private var lastAgingTimestamp = 0L
    private val _workoutCountdown = MutableStateFlow(0)
    val workoutCountdown: StateFlow<Int> = _workoutCountdown
    
    private var activeExercise: String? = null
    private var isWorkoutActive = false

    private var lastStepCount = 0
    private var lastCalories = 0
    private var lastEvoCheck = 0L

    private val healthSyncReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.vitalwearclonev1.HEALTH_SYNCED") {
                val steps = intent.getLongExtra("steps", 0L)
                val calories = intent.getIntExtra("calories", 0)
                val startOfDay = intent.getLongExtra("startOfDay", 0L)
                val weight = intent.getFloatExtra("weight", 165f)
                
                sensorManager.updateBaseHealthData(steps, calories, startOfDay, weight)
                monsterManager.syncHealthData(sensorManager.stepCount.value, sensorManager.calories.value)
                
                // Sync service's local counters to new state to avoid immediate delta jumps
                lastStepCount = sensorManager.stepCount.value
                lastCalories = sensorManager.calories.value
                Timber.d("Service handled health sync. New Total: ${lastStepCount}")
                
                updateNotification("Health Synced: $steps steps")
            }
        }
    }

    inner class VitalBinder : Binder() {
        fun getService(): VitalForegroundService = this@VitalForegroundService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        sensorManager = VitalSensorManager.getInstance(this)
        monsterManager = MonsterManager(this)
        
        // Initialize sensor manager with current monster stats
        monsterManager.getCurrentMonster()?.let {
            sensorManager.updateBaseHealthData(it.steps.toLong(), it.calories)
        }
        
        val filter = IntentFilter("com.example.vitalwearclonev1.HEALTH_SYNCED")
        registerReceiver(healthSyncReceiver, filter, Context.RECEIVER_EXPORTED)

        createNotificationChannel()
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID, 
                createNotification("Vital Tracking Active"),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification("Vital Tracking Active"))
        }
        
        sensorManager.startTracking()
        lastAgingTimestamp = System.currentTimeMillis()
        
        // Initialize baseline from current monster state to avoid double counting on start
        val initialMonsterState = monsterManager.getCurrentMonster()
        lastStepCount = initialMonsterState?.steps ?: sensorManager.stepCount.value
        lastCalories = initialMonsterState?.calories ?: sensorManager.calories.value

        startTrackingLoop()
    }

    private fun startTrackingLoop() {
        scope.launch {
            while (true) {
                delay(5000) // Poll every 5 seconds
                
                val currentSteps = sensorManager.stepCount.value
                val currentCals = sensorManager.calories.value
                val now = System.currentTimeMillis()
                val elapsedSeconds = (now - lastAgingTimestamp) / 1000
                
                val deltaSteps = (currentSteps - lastStepCount).coerceAtLeast(0)
                val deltaCals = (currentCals - lastCalories).coerceAtLeast(0)
                val addSeconds = if (elapsedSeconds >= 10) {
                    val s = elapsedSeconds
                    lastAgingTimestamp = now
                    s
                } else 0
                
                if (deltaSteps > 0 || deltaCals > 0 || addSeconds > 0) {
                    val state = monsterManager.getCurrentMonster()
                    if (state != null) {
                        monsterManager.updateStats(deltaSteps, deltaCals, addSeconds)
                        
                        if (state.isAdventureMode && deltaSteps > 0) {
                            monsterManager.updateAdventureSteps(deltaSteps)
                        }
                        
                        // Steps feed Vital Points; hitting the daily step goal counts
                        // as "looked after" for the care system.
                        monsterManager.syncStepsToVitalPoints(currentSteps)

                        // Care clock: lifespan burns in real time; poor care
                        // can self-delete the Digimon.
                        val died = monsterManager.tickCare()
                        if (died) {
                            Timber.w("Digimon died of poor care")
                            sendBroadcast(refreshIntent())
                        } else if (!state.isEvolutionPaused) {
                            // Evolve along the card's REAL tree when requirements
                            // are met. Throttled and off the main thread: card
                            // parsing is expensive and requirements move slowly.
                            val now = System.currentTimeMillis()
                            if (now - lastEvoCheck >= 60000) {
                                lastEvoCheck = now
                                scope.launch(Dispatchers.IO) {
                                    val available = monsterManager.getEvolutionCandidates()
                                        .filter { it.requirementsMet }
                                    if (available.isNotEmpty()) {
                                        val pick = available.first()
                                        monsterManager.evolveTo(pick.path.toIndex, pick.path.hoursUntilEvolution)
                                        Timber.i("Background evolution: ${state.characterId} -> ${pick.path.toIndex}")
                                        sendBroadcast(refreshIntent())
                                    }
                                }
                            }
                        }
                        
                        lastStepCount = currentSteps
                        lastCalories = currentCals
                        Timber.d("Background Update: Steps+$deltaSteps, Cals+$deltaCals, Time+$addSeconds")
                    }
                }
                
                if (isWorkoutActive && _workoutCountdown.value > 0) {
                    _workoutCountdown.value--
                    if (_workoutCountdown.value <= 0) {
                        completeWorkout()
                    } else {
                        updateNotification("Training: $activeExercise (${_workoutCountdown.value} s)")
                    }
                }
            }
        }
    }

    fun startWorkout(exercise: String, durationSeconds: Int) {
        activeExercise = exercise
        _workoutCountdown.value = durationSeconds
        isWorkoutActive = true
        updateNotification("Training: $exercise (${_workoutCountdown.value} s)")
    }

    private fun refreshIntent() = Intent("com.example.vitalwearclonev1.CARD_IMPORTED").apply {
        putExtra("timestamp", System.currentTimeMillis())
        setPackage(packageName)
    }

    private fun completeWorkout() {
        isWorkoutActive = false
        activeExercise?.let {
            monsterManager.addTrainingBonus(it)
            monsterManager.addVitalPoints(2) // placeholder: each workout earns VP
            Timber.i("Workout completed in background: $it")
        }
        updateNotification("Workout Complete! Digimon Powered Up.")
        
        val intent = Intent("com.example.vitalwearclonev1.WORKOUT_COMPLETE").apply {
            putExtra("exercise", activeExercise)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Vital Wear Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(text: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Vital Wear")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation) // Use a proper icon in real app
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, createNotification(text))
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.stopTracking()
        scope.cancel()
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "vital_wear_channel"
    }
}
