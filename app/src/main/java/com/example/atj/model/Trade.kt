package com.example.atj.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Modello trade più serio, pensato per:
 * - inserimento manuale
 * - import simulato via JSON
 * - future analytics su PnL e account
 */
@Entity(tableName = "trades")
data class Trade(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Trade associato a uno specifico utente
    val userId: Long,

    // Provenienza del trade: manual / json
    val source: String = "manual",

    // Id esterno simulato (futuro: alert / broker / TradingView / webhook)
    val externalId: String = "",

    // Asset / simbolo
    val asset: String,

    // Buy / Sell
    val type: String,

    // Long / Short
    val direction: String = "",

    // Open / Win / Loss / BE
    val result: String = "Open",

    // Dati temporali e di contesto
    val date: String,
    val session: String = "Unknown",
    val locationText: String = "Unknown",

    // Prezzi operativi
    val entryPrice: Double = 0.0,
    val exitPrice: Double = 0.0,
    val stopLoss: Double = 0.0,
    val takeProfit: Double = 0.0,

    // Risk / performance
    val rr: Double = 0.0,
    val positionValue: Double = 0.0,
    val positionPercentOfAccount: Double = 0.0,
    val accountValue: Double = 0.0,
    val pnlAmount: Double = 0.0,
    val pnlPercent: Double = 0.0,

    // Note / immagine / strategia
    val notes: String = "",
    val imagePath: String? = null,
    val strategyName: String = "",
    val checkedConfluences: String = "",
    val confluenceScore: Int = 0
)