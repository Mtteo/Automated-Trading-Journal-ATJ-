package com.example.atj.utils

import com.example.atj.model.Trade

/*
 * Utility per normalizzare e leggere lo stato dei trade.
 * Centralizza le regole invece di ripeterle in Activity, Adapter e statistiche.
 */
object TradeStateHelper {

    const val STATUS_OPEN = "OPEN"
    const val STATUS_CLOSED = "CLOSED"

    const val OUTCOME_NONE = "NONE"
    const val OUTCOME_WIN = "WIN"
    const val OUTCOME_LOSS = "LOSS"
    const val OUTCOME_BE = "BE"

    /*
     * Normalizza result e aggiorna i campi tecnici tradeStatus/tradeOutcome.
     * copy() mantiene immutabilità pratica: crea un Trade modificato senza alterare l'originale.
     */
    fun normalizeTrade(trade: Trade): Trade {
        val normalizedResult = normalizeResult(trade.result)

        val status = when (normalizedResult) {
            "Win", "Loss", "BE" -> STATUS_CLOSED
            else -> STATUS_OPEN
        }

        val outcome = when (normalizedResult) {
            "Win" -> OUTCOME_WIN
            "Loss" -> OUTCOME_LOSS
            "BE" -> OUTCOME_BE
            else -> OUTCOME_NONE
        }

        return trade.copy(
            result = normalizedResult,
            tradeStatus = status,
            tradeOutcome = outcome
        )
    }

    /*
     * Verifica se il trade è ancora aperto.
     */
    fun isOpen(trade: Trade): Boolean {
        return trade.tradeStatus.equals(STATUS_OPEN, ignoreCase = true)
    }

    /*
     * Verifica se il trade è chiuso.
     */
    fun isClosed(trade: Trade): Boolean {
        return trade.tradeStatus.equals(STATUS_CLOSED, ignoreCase = true)
    }

    /*
     * Verifica se il trade è vincente.
     */
    fun isWin(trade: Trade): Boolean {
        return trade.tradeOutcome.equals(OUTCOME_WIN, ignoreCase = true)
    }

    /*
     * Verifica se il trade è perdente.
     */
    fun isLoss(trade: Trade): Boolean {
        return trade.tradeOutcome.equals(OUTCOME_LOSS, ignoreCase = true)
    }

    /*
     * Verifica se il trade è chiuso a pareggio.
     */
    fun isBreakEven(trade: Trade): Boolean {
        return trade.tradeOutcome.equals(OUTCOME_BE, ignoreCase = true)
    }

    /*
     * Restituisce una label pronta per la UI.
     */
    fun displayState(trade: Trade): String {
        return if (isOpen(trade)) {
            "Open"
        } else {
            when {
                isWin(trade) -> "Win"
                isLoss(trade) -> "Loss"
                isBreakEven(trade) -> "BE"
                else -> "Closed"
            }
        }
    }

    /*
     * Uniforma possibili valori testuali dello stesso risultato.
     * Utile perché i dati possono arrivare da input manuale o JSON simulato.
     */
    fun normalizeResult(rawResult: String): String {
        val clean = rawResult.trim()

        return when {
            clean.equals("Win", ignoreCase = true) -> "Win"
            clean.equals("Winner", ignoreCase = true) -> "Win"
            clean.equals("Profit", ignoreCase = true) -> "Win"
            clean.equals("Profitable", ignoreCase = true) -> "Win"

            clean.equals("Loss", ignoreCase = true) -> "Loss"
            clean.equals("Lose", ignoreCase = true) -> "Loss"
            clean.equals("Lost", ignoreCase = true) -> "Loss"

            clean.equals("BE", ignoreCase = true) -> "BE"
            clean.equals("Break Even", ignoreCase = true) -> "BE"
            clean.equals("Break-even", ignoreCase = true) -> "BE"
            clean.equals("Breakeven", ignoreCase = true) -> "BE"

            clean.equals("Open", ignoreCase = true) -> "Open"
            clean.isBlank() -> "Open"

            else -> "Open"
        }
    }
}