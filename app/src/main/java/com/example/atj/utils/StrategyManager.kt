package com.example.atj.utils

import android.content.Context

// Manager semplice basato su SharedPreferences.
// Serve per salvare una strategia unica definita dall'utente.
object StrategyManager {

    private const val PREFS_NAME = "atj_strategy_prefs"
    private const val KEY_STRATEGY_NAME = "strategy_name"
    private const val KEY_STRATEGY_DESCRIPTION = "strategy_description"
    private const val KEY_STRATEGY_IMAGE_PATH = "strategy_image_path"
    private const val KEY_STRATEGY_ITEMS = "strategy_items"

    data class StrategyData(
        val name: String,
        val description: String,
        val imagePath: String?,
        val checklistItems: List<String>
    )

    fun saveStrategy(
        context: Context,
        name: String,
        description: String,
        imagePath: String?,
        checklistItems: List<String>
    ) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_STRATEGY_NAME, name)
            .putString(KEY_STRATEGY_DESCRIPTION, description)
            .putString(KEY_STRATEGY_IMAGE_PATH, imagePath)
            .putString(KEY_STRATEGY_ITEMS, checklistItems.joinToString("|||"))
            .apply()
    }

    fun getStrategy(context: Context): StrategyData {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val name = prefs.getString(KEY_STRATEGY_NAME, "") ?: ""
        val description = prefs.getString(KEY_STRATEGY_DESCRIPTION, "") ?: ""
        val imagePath = prefs.getString(KEY_STRATEGY_IMAGE_PATH, null)
        val itemsRaw = prefs.getString(KEY_STRATEGY_ITEMS, "") ?: ""

        val checklistItems = if (itemsRaw.isBlank()) {
            emptyList()
        } else {
            itemsRaw.split("|||").map { it.trim() }.filter { it.isNotBlank() }
        }

        return StrategyData(
            name = name,
            description = description,
            imagePath = imagePath,
            checklistItems = checklistItems
        )
    }

    fun hasStrategy(context: Context): Boolean {
        val strategy = getStrategy(context)
        return strategy.name.isNotBlank() || strategy.checklistItems.isNotEmpty()
    }
}