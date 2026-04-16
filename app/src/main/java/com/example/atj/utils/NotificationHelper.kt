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

/**
 * Helper centrale per tutte le notifiche dell'app.
 *
 * Gestisce:
 * - creazione canale notifiche
 * - verifica permesso runtime
 * - notifiche trade saved / deleted
 * - notifiche sessione
 * - scheduling giornaliero delle sessioni
 */
object NotificationHelper {

    const val CHANNEL_ID = "atj_notifications_channel"
    private const val CHANNEL_NAME = "ATJ Notifications"
    private const val CHANNEL_DESCRIPTION = "Trade confirmations and session reminders"

    /**
     * Crea il canale notifiche richiesto da Android 8+.
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

    /**
     * Controlla se l'app ha il permesso per mostrare notifiche.
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

    /**
     * Notifica immediata quando viene salvato un trade.
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

    /**
     * Notifica immediata quando viene cancellato un trade.
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

    /**
     * Notifica sessione.
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

    /**
     * Programma tutte le notifiche giornaliere delle sessioni.
     *
     * Orari attuali:
     * - Sydney 00:00
     * - Asia 01:00
     * - London 09:00
     * - NY 14:30
     */
    fun scheduleAllSessionNotifications(context: Context) {
        scheduleDailySessionNotification(context, "Sydney", 0, 0, 2001)
        scheduleDailySessionNotification(context, "Asia", 1, 0, 2002)
        scheduleDailySessionNotification(context, "London", 9, 0, 2003)
        scheduleDailySessionNotification(context, "NY", 14, 30, 2004)
    }

    /**
     * Programma una notifica giornaliera per una sessione.
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
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    /**
     * Calcola il prossimo orario valido per una notifica giornaliera.
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