package com.example.atj.utils

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.atj.MainActivity
import com.example.atj.R
import com.example.atj.receiver.SessionAlarmReceiver
import java.util.Calendar

/*
 * Helper centrale per notifiche e promemoria.
 * Raggruppa la logica legata a NotificationManager e AlarmManager.
 */
object NotificationHelper {

    const val CHANNEL_ID = "atj_notifications_channel"
    private const val CHANNEL_NAME = "ATJ Notifications"
    private const val CHANNEL_DESCRIPTION = "Trade confirmations and session reminders"

    /*
     * Crea il canale notifiche richiesto da Android
     * Senza canale, le notifiche non vengono mostrate sulle versioni recenti.
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION
            }

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)
        }
    }

    /*
     * Controlla il permesso runtime per le notifiche.
     * Da Android 13 POST_NOTIFICATIONS deve essere concesso dall'utente.
     */
    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /*
     * Mostra una notifica quando un trade viene salvato.
     * Il PendingIntent riapre la MainActivity al click sulla notifica.
     */
    fun showTradeCreatedNotification(
        context: Context,
        asset: String,
        type: String,
        source: String
    ) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Trade saved")
            .setContentText("$asset • $type saved successfully ($source)")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    /*
     * Mostra una notifica quando un trade viene cancellato.
     */
    fun showTradeDeletedNotification(
        context: Context,
        asset: String,
        type: String
    ) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Trade deleted")
            .setContentText("$asset • $type removed from your journal")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context)
            .notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    /*
     * Notifica l'inizio di una sessione di mercato.
     * Viene chiamata dal BroadcastReceiver quando scatta l'allarme.
     */
    fun showSessionNotification(context: Context, sessionName: String) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            sessionName.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("$sessionName session started")
            .setContentText("Time to journal or prepare your session.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(sessionName.hashCode(), notification)
    }

    /*
     * Programma le notifiche giornaliere delle sessioni.
     * AlarmManager serve per eseguire un Intent in un momento futuro.
     */
    fun scheduleAllSessionNotifications(context: Context) {
        scheduleDailySessionNotification(context, "Sydney", 0, 0, 2001)
        scheduleDailySessionNotification(context, "Asia", 1, 0, 2002)
        scheduleDailySessionNotification(context, "London", 9, 0, 2003)
        scheduleDailySessionNotification(context, "NY", 14, 30, 2004)
    }

    /*
     * Programma una singola notifica giornaliera.
     * Il PendingIntent punta al BroadcastReceiver, non direttamente a una Activity.
     */
    private fun scheduleDailySessionNotification(
        context: Context,
        sessionName: String,
        hour: Int,
        minute: Int,
        requestCode: Int
    ) {
        val intent = Intent(context, SessionAlarmReceiver::class.java).apply {
            putExtra("sessionName", sessionName)
            putExtra("hour", hour)
            putExtra("minute", minute)
            putExtra("requestCode", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerTime = getNextTriggerTime(hour, minute)

        try {
            // Allarme più preciso, anche con dispositivo in idle quando consentito.
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // Fallback se l'app non può usare allarmi esatti.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: Exception) {
            // Ultimo fallback: allarme standard.
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /*
     * Calcola il prossimo orario valido.
     * Se l'orario di oggi è già passato, programma quello del giorno dopo.
     */
    private fun getNextTriggerTime(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (trigger.before(now)) {
            trigger.add(Calendar.DAY_OF_YEAR, 1)
        }

        return trigger.timeInMillis
    }
}