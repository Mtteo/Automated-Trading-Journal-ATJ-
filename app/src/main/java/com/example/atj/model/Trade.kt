package com.example.atj.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Questa classe rappresenta un trade salvato nel database.
@Entity(tableName = "trades")
data class Trade(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val asset: String,
    val type: String,
    val date: String,
    val session: String = "Unknown",
    val result: String = "Open",
    val notes: String = "",
    val source: String = "manual"
)