package com.example.vitalwearclonev1.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages low-power sensors to reduce battery drain and lag.
 */
class VitalSensorManager private constructor(context: Context) : SensorEventListener {

    companion object {
        @Volatile
        private var instance: VitalSensorManager? = null

        fun getInstance(context: Context): VitalSensorManager {
            return instance ?: synchronized(this) {
                instance ?: VitalSensorManager(context.applicationContext).also { 
                    instance = it
                    // Pre-initialize from database if possible to ensure immediate baseline
                    try {
                        val monsterManager = com.example.vitalwearclonev1.monster.MonsterManager(context.applicationContext)
                        monsterManager.getCurrentMonster()?.let { m ->
                            it.updateBaseHealthData(m.steps.toLong(), m.calories)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to pre-initialize sensor manager")
                    }
                }
            }
        }
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    private val _stepCount = MutableStateFlow(0)
    val stepCount: StateFlow<Int> = _stepCount

    private val _calories = MutableStateFlow(0)
    val calories: StateFlow<Int> = _calories

    @Volatile private var initialStepCount = -1
    @Volatile private var lastRecordedSessionSteps = 0
    @Volatile private var baseSteps = 0L
    @Volatile private var baseCalories = 0.0
    @Volatile private var lastStartOfDayMillis = 0L
    @Volatile private var userWeightKg = 75.0

    // Formula: Weight accounts for ~60% of variance.
    // 0.57 cal/lb/mile * 2.2 lb/kg = 1.254 cal/kg per mile
    // 1.254 / 2200 steps/mile = 0.00057 cal/kg per step
    private fun getCaloriesPerStep(): Double {
        return 0.00057 * userWeightKg
    }

    fun updateBaseHealthData(steps: Long, calories: Int, startOfDayMillis: Long = 0L, weightKg: Float = -1f) {
        if (weightKg > 0) {
            userWeightKg = weightKg.toDouble()
        }

        // Handle Day Crossover: If startOfDay has changed, reset everything
        if (startOfDayMillis != 0L && startOfDayMillis > lastStartOfDayMillis) {
            Timber.i("Day crossover detected. Resetting health baseline for new day.")
            baseSteps = steps
            baseCalories = calories.toDouble()
            initialStepCount = -1
            lastRecordedSessionSteps = 0
            lastStartOfDayMillis = startOfDayMillis
            
            _stepCount.value = baseSteps.toInt()
            _calories.value = baseCalories.toInt()
            return
        }

        // Absolute update from Phone/Health app
        Timber.d("Syncing health base from phone: Steps=$steps, Cals=$calories, WeightKg=$userWeightKg")
        
        baseSteps = steps
        baseCalories = calories.toDouble()
        
        // Reset the live session tracker to avoid adding old watch steps to the new phone baseline
        // Setting to -1 ensures onSensorChanged will re-baseline against the hardware counter
        initialStepCount = -1 
        lastRecordedSessionSteps = 0
        
        _stepCount.value = baseSteps.toInt()
        _calories.value = baseCalories.toInt()
        
        Timber.i("Health sync completed. Baseline: Steps=$baseSteps, Cals=$baseCalories")
    }

    fun startTracking() {
        val stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor != null) {
            Timber.d("Step Counter found, starting tracking...")
            // Use SENSOR_DELAY_NORMAL for background battery efficiency
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            Timber.e("Step Counter sensor NOT found!")
        }
    }

    fun stopTracking() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_STEP_COUNTER) {
            val totalSteps = event.values[0].toInt()
            
            // Handle Sensor Reset (e.g. Reboot or Overflow)
            if (initialStepCount != -1 && totalSteps < initialStepCount) {
                Timber.w("Hardware step counter reset detected (Current: $totalSteps, Prev: $initialStepCount). Resetting session baseline.")
                initialStepCount = totalSteps
                lastRecordedSessionSteps = 0
            }

            // CRITICAL FIX: If we just synced, initialStepCount is -1.
            // We MUST set it to current totalSteps and WAIT until the next event
            // to avoid using -1 in the sessionSteps calculation which causes huge jumps.
            if (initialStepCount == -1) {
                initialStepCount = totalSteps
                lastRecordedSessionSteps = 0
                Timber.i("Step Counter Initialized: HardwareTotal=$totalSteps, Base=$baseSteps")
                return 
            }

            val sessionSteps = totalSteps - initialStepCount
            val delta = sessionSteps - lastRecordedSessionSteps
            
            if (delta > 0) {
                _stepCount.value = baseSteps.toInt() + sessionSteps
                _calories.value = baseCalories.toInt() + (sessionSteps * getCaloriesPerStep()).toInt()
                lastRecordedSessionSteps = sessionSteps
                
                Timber.d("Step increment: +$delta, SessionTotal=$sessionSteps, AppTotal=${_stepCount.value}")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
