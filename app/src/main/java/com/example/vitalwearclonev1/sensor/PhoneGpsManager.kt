package com.example.vitalwearclonev1.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import timber.log.Timber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

class PhoneGpsManager(private val context: Context) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private var lastLocation: Location? = null

    private val prefs = context.getSharedPreferences("gps_tracking_prefs", Context.MODE_PRIVATE)

    private val _totalDistance = MutableStateFlow(0f)
    val totalDistance: StateFlow<Float> = _totalDistance

    private val _stepCount = MutableStateFlow(prefs.getInt("steps", 0))
    val stepCount: StateFlow<Int> = _stepCount

    private val _calories = MutableStateFlow(prefs.getFloat("calories", 0f))
    val calories: StateFlow<Float> = _calories

    private val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
        .setMinUpdateIntervalMillis(2000)
        .build()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                updateTracking(location)
            }
        }
    }

    /**
     * Public so the UI can trigger the midnight rollover check WITHOUT
     * registering for location updates. Never call startTracking() here —
     * that leaks a new high-accuracy GPS callback on every call.
     */
    fun checkDayReset() {
        val lastReset = prefs.getLong("last_reset", 0L)
        val startOfDay = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS).toInstant().toEpochMilli()

        if (lastReset < startOfDay) {
            Timber.i("GPS Tracking Reset: lastReset=$lastReset, startOfDay=$startOfDay")
            _totalDistance.value = 0f
            _stepCount.value = 0
            _calories.value = 0f
            prefs.edit()
                .putLong("last_reset", System.currentTimeMillis())
                .putFloat("total_distance", 0f)
                .putInt("steps", 0)
                .putFloat("calories", 0f)
                .apply()
        }
    }

    private fun updateTracking(location: Location) {
        checkDayReset()

        val currentLastLocation = lastLocation
        if (currentLastLocation != null) {
            val distance = currentLastLocation.distanceTo(location)

            // Use System.currentTimeMillis() as a robust fallback for elapsed time
            val currentTime = System.currentTimeMillis()
            val lastTime = if (currentLastLocation.time > 0) currentLastLocation.time else currentTime - 2000
            val timeDeltaMillis = currentTime - lastTime
            val timeDeltaSecs = timeDeltaMillis / 1000.0

            val calculatedSpeedKmh: Float = if (timeDeltaSecs > 0) ((distance / timeDeltaSecs) * 3.6).toFloat() else 0f
            val currentSpeedKmh: Float = if (location.hasSpeed()) location.speed * 3.6f else calculatedSpeedKmh

            val isAccurate = location.accuracy < 30.0f
            val isSignificant = distance > 2.0f
            val isHumanSpeed = currentSpeedKmh < 35.0f

            if (isAccurate && isSignificant && isHumanSpeed) {
                _totalDistance.value += distance

                val newSteps = (distance / 0.762f).roundToInt()
                _stepCount.value += newSteps

                val met = when {
                    currentSpeedKmh < 1f -> 0f
                    currentSpeedKmh < 4.5f -> 3.0f
                    currentSpeedKmh < 6.5f -> 4.5f
                    currentSpeedKmh < 9.5f -> 8.0f
                    currentSpeedKmh < 13f -> 12.0f
                    currentSpeedKmh < 20f -> 16.0f
                    else -> 20.0f
                }

                val weight = 75f
                val calBurned = (met * weight * (timeDeltaSecs / 3600.0)).toFloat()
                _calories.value += calBurned

                prefs.edit()
                    .putFloat("total_distance", _totalDistance.value)
                    .putInt("steps", _stepCount.value)
                    .putFloat("calories", _calories.value)
                    .apply()

                Timber.d("GPS Tracking: Dist=$distance, Steps=$newSteps, SpeedKmh=$currentSpeedKmh, Cal=$calBurned")
            }
        }
        lastLocation = location
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        checkDayReset()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, context.mainLooper)
        Timber.i("GPS Tracking Started")
    }

    fun stopTracking() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        lastLocation = null
        Timber.i("GPS Tracking Stopped")
    }

    fun resetToday() {
        _totalDistance.value = 0f
        _stepCount.value = 0
        _calories.value = 0f
        prefs.edit().clear().apply()
    }

    companion object
}