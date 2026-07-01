package com.example.atj.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.atj.model.User

/*
 * DAO degli utenti.
 * Gestisce registrazione, ricerca utente e login nel database locale.
 */
@Dao
interface UserDao {

    /*
     * Inserisce un nuovo utente e restituisce l'id generato.
     */
    @Insert
    fun insertUser(user: User): Long

    /*
     * Cerca un utente tramite username.
     * User? gestisce il caso in cui non venga trovato nulla.
     */
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    fun getUserByUsername(username: String): User?

    /*
     * Controlla username e password per il login locale.
     */
    @Query("SELECT * FROM users WHERE username = :username AND password = :password LIMIT 1")
    fun login(username: String, password: String): User?
}