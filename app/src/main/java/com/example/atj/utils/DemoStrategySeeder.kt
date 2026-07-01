package com.example.atj.utils

import android.content.Context

/*
 * Utility singleton che inserisce una strategia demo nelle SharedPreferences.
 * È utile per avere dati iniziali già pronti senza usare una tabella Room.
 */
object DemoStrategySeeder {

    private const val PREFS_NAME = "strategy_prefs"

    private const val STRATEGY_NAME_KEY = "strategy_name"
    private const val STRATEGY_DESCRIPTION_KEY = "strategy_description"
    private const val STRATEGY_CHECKLIST_KEY = "strategy_checklist"
    private const val STRATEGY_IMAGE_PATH_KEY = "strategy_image_path"

    /*
     * Salva una strategia predefinita in modalità key-value.
     * SharedPreferences è adatto per dati semplici di configurazione o preferenze.
     */
    fun seedDemoStrategy(context: Context) {
        val checklist = listOf(
            "Liquidity sweep",
            "BOS High Timeframe",
            "OB retracement",
            "BOS OB",
            "FVG respected",
            "FVG inverted",
            "Equilibrium",
            "SMT",
            "75% closure"
        ).joinToString("\n")

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(STRATEGY_NAME_KEY, "Liquidity ICT Strategy")
            .putString(
                STRATEGY_DESCRIPTION_KEY,
                "Demo strategy based on ICT concepts. The model focuses on liquidity sweeps, high timeframe break of structure, order block retracement, fair value gap reaction, equilibrium, SMT confirmation and 75% closure management."
            )
            .putString(STRATEGY_CHECKLIST_KEY, checklist)
            .putString(STRATEGY_IMAGE_PATH_KEY, "")
            .apply()
    }
}