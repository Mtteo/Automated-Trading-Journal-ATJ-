package com.example.atj.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Questa classe rappresenta un trade salvato nel database.
@Entity(tableName = "trades")
data class Trade(

    // ID univoco generato automaticamente dal database.
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Asset tradato, ad esempio NAS100 o SPX500.
    val asset: String,

    // Tipo di operazione: Buy oppure Sell.
    val type: String,

    // Data del trade in formato testuale semplice.
    val date: String,

    // Sessione di mercato, ad esempio NY / London / Asia.
    val session: String = "Unknown",

    // Esito del trade: Win / Loss / BE / Open.
    val result: String = "Open",

    // Note libere inserite dall’utente.
    val notes: String = "",

    // Origine del trade: manual oppure json.
    val source: String = "manual",

    // Percorso locale dell'immagine associata al trade.
    // Può essere null se il trade non ha screenshot.
    val imagePath: String? = null
)