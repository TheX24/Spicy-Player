package com.tx24.spicyplayer.library.store

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object ScanNotificationHelper {
    private const val CHANNEL_ID = "scanning_channel"
    private const val CHANNEL_NAME = "Library Scanning"
    const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows progress of music library scanning"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun buildScanNotification(context: Context, content: String, progress: Int = -1, total: Int = -1): Notification {
        val intent = Intent(context, Class.forName("com.tx24.spicyplayer.MainActivity")).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Library Synchronizing")
            .setContentText(content)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)

        if (progress >= 0 && total > 0) {
            builder.setProgress(total, progress, false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    fun updateNotification(context: Context, content: String, progress: Int = -1, total: Int = -1) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildScanNotification(context, content, progress, total))
    }
}
