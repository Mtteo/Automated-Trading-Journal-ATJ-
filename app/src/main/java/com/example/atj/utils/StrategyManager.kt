package com.example.atj.utils

import android.content.Context

/*
 * Gestisce la strategia personale dell'utente.
 * Usa SharedPreferences perché salva pochi dati testuali e un path immagine.
 */
object StrategyManager {

    private const val PREFS_NAME = "atj_strategy_prefs"

    /*
     * Modello dati usato per riportare alla UI la strategia salvata.
     */
    data class StrategyData(
        val name: String,
        val description: String,
        val imagePath: String?,
        val checklistItems: List<String>
    )

    /*
     * Crea chiavi diverse per ogni utente.
     * Evita che strategie di account diversi si sovrascrivano.
     */
    private fun buildKey(userId: Long, field: String): String {
        return "user_${userId}_$field"
    }

    /*
     * Salva la strategia dell'utente loggato.
     * apply() scrive le preferenze senza bloccare il thread principale.
     */
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

    /*
     * Recupera la strategia dell'utente corrente.
     * Se non c'è login, restituisce un oggetto vuoto.
     */
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

        /*
         * La checklist viene salvata come stringa unica e poi ricostruita in lista.
         */
        val checklistItems = if (itemsRaw.isBlank()) {
            emptyList()
        } else {
            itemsRaw.split("|||").map { it.trim() }.filter { it.isNotBlank() }
        }

        return StrategyData(name, description, imagePath, checklistItems)
    }

    /*
     * Controlla se l'utente ha già configurato una strategia.
     */
    fun hasStrategy(context: Context): Boolean {
        val strategy = getStrategy(context)
        return strategy.name.isNotBlank() || strategy.checklistItems.isNotEmpty()
    }
}