package com.example.atj.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.atj.utils.NotificationHelper

/*
 * BroadcastReceiver usato per ricevere l'Intent programmato dall'AlarmManager.
 * Non ha interfaccia grafica: viene attivato dal sistema anche se l'Activity non è aperta.
 */
class SessionAlarmReceiver : BroadcastReceiver() {

    /*
     * Callback eseguita quando scatta l'allarme.
     * Recupera il nome della sessione dagli extra dell'Intent e mostra la notifica.
     */
    override fun onReceive(context: Context, intent: Intent) {
        val sessionName = intent.getStringExtra("sessionName") ?: "Session"

        // Il canale è necessario dalle versioni recenti di Android per mostrare notifiche.
        NotificationHelper.createNotificationChannel(context)
        NotificationHelper.showSessionNotification(context, sessionName)

        // Ripianifica le notifiche per mantenere attivi gli avvisi nei giorni successivi.
        NotificationHelper.scheduleAllSessionNotifications(context)
    }
}