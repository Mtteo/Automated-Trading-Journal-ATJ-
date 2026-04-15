package com.example.atj.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Utility per calcolare la sessione di mercato in base all'orario locale.
object SessionHelper {

    // Calcola la sessione partendo da un timestamp in millisecondi.
    fun getSessionFromTimestamp(timestampMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestampMillis

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return getSessionFromHourMinute(hour, minute)
    }

    // Calcola la sessione dall'orario corrente locale.
    fun getCurrentSession(): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        return getSessionFromHourMinute(hour, minute)
    }

    // Regole definite da te:
    // 01:00 - 08:59 -> Asia
    // 09:00 - 14:29 -> London
    // 14:30 - 23:59 -> NY
    // 00:00 - 00:59 -> Sydney
    private fun getSessionFromHourMinute(hour: Int, minute: Int): String {
        return when {
            hour == 0 -> "Sydney"
            hour in 1..8 -> "Asia"
            hour in 9..13 -> "London"
            hour == 14 && minute < 30 -> "London"
            else -> "NY"
        }
    }

    // Formatta la data da timestamp in forma leggibile.
    fun formatDateFromTimestamp(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestampMillis))
    }
}