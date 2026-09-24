package com.example.vitalwearclonev1.sensor

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.vitalwearclonev1.PhoneMainActivity
import timber.log.Timber

class PhoneTrackingService : Service() {
    private lateinit var gpsManager: PhoneGpsManager

    companion object {
        var isServiceRunning = false

        fun start(context: Context) {
            val intent = Intent(context, PhoneTrackingService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PhoneTrackingService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        gpsManager = PhoneGpsManager(this)
        startForeground(1, createNotification())
        gpsManager.startTracking()
        Timber.i("Phone Tracking Service Started")
    }

    private fun createNotification(): Notification {
        val channelId = "tracking_channel"
        val channel = NotificationChannel(channelId, "Activity Tracking", NotificationManager.IMPORTANCE_LOW)
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)

        val intent = Intent(this, PhoneMainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Vital Tracking Active")
            .setContentText("Tracking steps and calories via GPS")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        gpsManager.stopTracking()
        isServiceRunning = false
        super.onDestroy()
        Timber.i("Phone Tracking Service Stopped")
    }
}
