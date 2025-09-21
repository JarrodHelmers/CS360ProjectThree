package com.example.weight_trackerapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val channelId = "weight_reminders"

        // Create channel on Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Weight Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mgr.createNotificationChannel(channel)
        }

        // Build and show the notification
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setContentTitle("Log your weight")
            .setContentText("Quick reminder to record today’s weight.")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(1001, notif)
    }
}