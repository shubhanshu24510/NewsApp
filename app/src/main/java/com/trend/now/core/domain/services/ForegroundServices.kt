package com.trend.now.core.domain.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.trend.now.R

class NewsForegroundService : Service() {
    override fun onCreate() {
        super.onCreate()
        startForeground(1, createNotification())
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Fetch news updates periodically
        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "news_foreground_service"
        val channel = NotificationChannel(
            channelId, "News Updates", NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("News Updates")
            .setContentText("Fetching latest news in the background...")
            .setContentInfo("Tap to view")
            .setSmallIcon(R.drawable.news_icon)
            .build()
    }
}
