package com.example.atj.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.atj.utils.NotificationHelper

// Receiver che scatta all'inizio di una sessione.
// Invia la notifica e poi riprogramma quella del giorno dopo.
class SessionAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val sessionName = intent.getStringExtra("sessionName") ?: "Session"

        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showSessionNotification(context, sessionName)

        // Ripianifica tutte le sessioni per garantire continuità.
        NotificationHelper.scheduleAllSessionNotifications(context)
    }
}