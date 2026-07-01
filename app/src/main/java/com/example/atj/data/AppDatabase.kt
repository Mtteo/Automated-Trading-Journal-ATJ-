package com.example.atj.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.atj.model.Trade
import com.example.atj.model.User

/*
 * Database Room principale dell'app.
 * Room fa da livello di astrazione sopra SQLite e collega Entity e DAO.
 */
@Database(
    entities = [
        Trade::class,
        User::class
    ],
    version = 8
)
abstract class AppDatabase : RoomDatabase() {

    /*
     * DAO usato per accedere ai dati dei trade e utenti
     */
    abstract fun tradeDao(): TradeDao

    abstract fun userDao(): UserDao

    companion object {

        /*
         * Istanza unica del database.
         * @Volatile rende il riferimento sicuro anche con accessi da più thread.
         */
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /*
         * Restituisce il database già creato oppure lo inizializza.
         * Si usa applicationContext per non legare il database a una singola Activity.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atj_database"
                )
                    // Scelta semplice per progetto didattico: ricrea il DB se cambia versione.
                    .fallbackToDestructiveMigration()

                    // Consentito qui per semplicità; in app grandi meglio thread separati.
                    .allowMainThreadQueries()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}