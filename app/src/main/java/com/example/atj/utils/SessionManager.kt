package com.example.atj.utils

import android.content.Context

// Gestisce la sessione utente loggato.
object SessionManager {

    private const val PREFS_NAME = "atj_session_prefs"
    private const val KEY_LOGGED_IN_USER_ID = "logged_in_user_id"
    private const val KEY_LOGGED_IN_USERNAME = "logged_in_username"

    fun saveLogin(context: Context, userId: Long, username: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LOGGED_IN_USER_ID, userId)
            .putString(KEY_LOGGED_IN_USERNAME, username)
            .apply()
    }

    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean {
        return getLoggedInUserId(context) != -1L
    }

    fun getLoggedInUserId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LOGGED_IN_USER_ID, -1L)
    }

    fun getLoggedInUsername(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LOGGED_IN_USERNAME, "") ?: ""
    }
}