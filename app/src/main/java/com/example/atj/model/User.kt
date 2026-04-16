package com.example.atj.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Utente locale dell'app.
@Entity(tableName = "users")
data class User(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val username: String,
    val password: String
)