package com.example.atj.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.atj.model.User

@Dao
interface UserDao {

    @Insert
    fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    fun login(username: String, password: String): User?
}