package com.example.atj.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val userId: Long,

    val source: String = "manual",
    val externalId: String = "",

    val asset: String,
    val type: String,
    val direction: String = "",

    /*
     * Campo mantenuto per compatibilità con la UI attuale.
     *
     * Valori usati:
     * - Open
     * - Win
     * - Loss
     * - BE
     */
    val result: String = "Open",

    /*
     * Stato interno del ciclo di vita del trade.
     *
     * OPEN   = trade ancora attivo
     * CLOSED = trade concluso
     */
    val tradeStatus: String = "OPEN",

    /*
     * Esito finale del trade.
     *
     * NONE = nessun esito finale, usato quando il trade è OPEN
     * WIN  = trade chiuso in profitto
     * LOSS = trade chiuso in perdita
     * BE   = trade chiuso a break-even
     */
    val tradeOutcome: String = "NONE",

    val date: String,

    val session: String = "Unknown",
    val locationText: String = "Unknown",

    val entryPrice: Double = 0.0,
    val exitPrice: Double = 0.0,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,
    val rr: Double = 0.0,

    val positionValue: Double = 0.0,
    val positionPercentOfAccount: Double = 0.0,
    val accountValue: Double = 0.0,

    val pnlAmount: Double = 0.0,
    val pnlPercent: Double = 0.0,

    val notes: String = "",
    val imagePath: String? = null,

    val strategyName: String = "",
    val checkedConfluences: String = "",
    val confluenceScore: Int = 0
)