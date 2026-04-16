package com.example.atj.utils

import android.content.Context

object StrategyManager {

    private const val PREFS_NAME = "atj_strategy_prefs"

    data class StrategyData(
        val name: String,
        val description: String,
        val imagePath: String?,
        val checklistItems: List<String>
    )

    private fun buildKey(userId: Long, field: String): String {
        return "user_${userId}_$field"
    }

    fun saveStrategy(
        context: Context,
        name: String,
        description: String,
        imagePath: String?,
        checklistItems: List<String>
    ) {
        val userId = SessionManager.getLoggedInUserId(context)
        if (userId == -1L) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(buildKey(userId, "strategy_name"), name)
            .putString(buildKey(userId, "strategy_description"), description)
            .putString(buildKey(userId, "strategy_image_path"), imagePath)
            .putString(buildKey(userId, "strategy_items"), checklistItems.joinToString("|||"))
            .apply()
    }

    fun getStrategy(context: Context): StrategyData {
        val userId = SessionManager.getLoggedInUserId(context)
        if (userId == -1L) {
            return StrategyData("", "", null, emptyList())
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val name = prefs.getString(buildKey(userId, "strategy_name"), "") ?: ""
        val description = prefs.getString(buildKey(userId, "strategy_description"), "") ?: ""
        val imagePath = prefs.getString(buildKey(userId, "strategy_image_path"), null)
        val itemsRaw = prefs.getString(buildKey(userId, "strategy_items"), "") ?: ""

        val checklistItems = if (itemsRaw.isBlank()) {
            emptyList()
        } else {
            itemsRaw.split("|||").map { it.trim() }.filter { it.isNotBlank() }
        }

        return StrategyData(name, description, imagePath, checklistItems)
    }

    fun hasStrategy(context: Context): Boolean {
        val strategy = getStrategy(context)
        return strategy.name.isNotBlank() || strategy.checklistItems.isNotEmpty()
    }
}