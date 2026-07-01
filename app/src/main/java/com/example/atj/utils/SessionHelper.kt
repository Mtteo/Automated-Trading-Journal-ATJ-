package com.example.atj.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/*
 * Utility per ricavare la sessione di mercato dall'orario locale.
 * Centralizza questa logica invece di ripeterla nelle Activity.
 */
object SessionHelper {

    /*
     * Calcola la sessione partendo da un timestamp.
     * Calendar converte i millisecondi in ora e minuti locali.
     */
    fun getSessionFromTimestamp(timestampMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestampMillis

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return getSessionFromHourMinute(hour, minute)
    }

    /*
     * Calcola la sessione corrente usando l'orario del dispositivo.
     */
    fun getCurrentSession(): String {
        val now = Calendar.getInstance()
        val hour = now.get(Calendar.HOUR_OF_DAY)
        val minute = now.get(Calendar.MINUTE)

        return getSessionFromHourMinute(hour, minute)
    }

    /*
     * Regole orarie delle sessioni.
     * when rende chiara la scelta tra i diversi intervalli.
     */
    private fun getSessionFromHourMinute(hour: Int, minute: Int): String {
        return when {
            hour == 0 -> "Sydney"
            hour in 1..8 -> "Asia"
            hour in 9..13 -> "London"
            hour == 14 && minute < 30 -> "London"
            else -> "NY"
        }
    }

    /*
     * Formatta un timestamp in una data leggibile per la UI.
     */
    fun formatDateFromTimestamp(timestampMillis: Long): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return formatter.format(Date(timestampMillis))
    }
}