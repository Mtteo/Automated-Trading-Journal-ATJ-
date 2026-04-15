package com.example.atj.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.atj.model.Trade

// Questo è il database principale dell'app.
// Contiene la tabella Trade e il relativo DAO.
@Database(entities = [Trade::class], version = 2)
abstract class AppDatabase : RoomDatabase() {

    // Punto di accesso alle query sui trade.
    abstract fun tradeDao(): TradeDao

    companion object {
        // INSTANCE serve per avere un solo database aperto nell'app.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atj_database"
                )
                    // Se il modello cambia e non esiste una migration,
                    // Room ricrea il database da zero.
                    .fallbackToDestructiveMigration()

                    // Per semplicità lasciamo le query nel main thread.
                    // In produzione sarebbe meglio evitarlo.
                    .allowMainThreadQueries()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}