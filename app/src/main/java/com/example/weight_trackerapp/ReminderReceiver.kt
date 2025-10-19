package com.example.weight_trackerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat.IMPORTANCE_DEFAULT

/**
 * Receives the daily alarm and posts a simple notification.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ensureNotificationChannel(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Weight reminder")
            .setContentText("Don’t forget to log your weight today.")
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2001, notification)
    }

    companion object {
        private const val CHANNEL_ID = "weight_reminders"

        fun ensureNotificationChannel(context: Context) {
            val mgr = NotificationManagerCompat.from(context)
            val channel = NotificationChannelCompat.Builder(CHANNEL_ID, IMPORTANCE_DEFAULT)
                .setName("Weight Reminders")
                .setDescription("Daily reminder to log your weight")
                .build()
            mgr.createNotificationChannel(channel)
        }
    }
}
