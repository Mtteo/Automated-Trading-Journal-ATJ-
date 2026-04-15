package com.example.atj.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Modello principale del trade salvato nel database.
@Entity(tableName = "trades")
data class Trade(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val asset: String,
    val type: String,
    val date: String,

    // Sessione di mercato calcolata automaticamente o inserita manualmente.
    val session: String = "Unknown",

    val result: String = "Open",
    val notes: String = "",
    val source: String = "manual",
    val imagePath: String? = null,

    // Strategia attiva al momento del trade.
    val strategyName: String = "",

    // Confluenze selezionate nel trade.
    val checkedConfluences: String = "",

    // Percentuale di match della checklist.
    val confluenceScore: Int = 0,

    // Luogo leggibile del trade, ad esempio "Bologna, Italy".
    val locationText: String = "Unknown"
)