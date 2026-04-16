package com.example.atj.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trades")
data class Trade(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Trade associato a uno specifico utente.
    val userId: Long,

    val asset: String,
    val type: String,
    val date: String,
    val session: String = "Unknown",
    val result: String = "Open",
    val notes: String = "",
    val source: String = "manual",
    val imagePath: String? = null,
    val strategyName: String = "",
    val checkedConfluences: String = "",
    val confluenceScore: Int = 0,
    val locationText: String = "Unknown"
)