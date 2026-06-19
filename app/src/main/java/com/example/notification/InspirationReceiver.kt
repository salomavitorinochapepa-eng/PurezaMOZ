package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class InspirationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context != null) {
            if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
                val sharedPrefs = context.getSharedPreferences("vencer_prefs", Context.MODE_PRIVATE)
                val isEnabled = sharedPrefs.getBoolean("daily_notifications", false)
                if (isEnabled) {
                    InspirationNotificationHelper.scheduleDailyNotification(context)
                }
            } else {
                InspirationNotificationHelper.showInspirationNotification(context)
            }
        }
    }
}
