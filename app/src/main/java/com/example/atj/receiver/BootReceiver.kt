package com.example.atj.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.atj.utils.NotificationHelper

// Quando il telefono si riavvia, riprogrammiamo le notifiche.
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            NotificationHelper.createNotificationChannel(context)
            NotificationHelper.scheduleAllSessionNotifications(context)
        }
    }
}