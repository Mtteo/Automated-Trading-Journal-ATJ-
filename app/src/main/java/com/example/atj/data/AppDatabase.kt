package com.example.atj.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.atj.model.Trade

// Database principale dell'app.
@Database(entities = [Trade::class], version = 3)
abstract class AppDatabase : RoomDatabase() {

    abstract fun tradeDao(): TradeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "atj_database"
                )
                    // In fase prototipo, se il modello cambia, ricreiamo il DB.
                    .fallbackToDestructiveMigration()

                    // Manteniamo la stessa impostazione del progetto attuale.
                    .allowMainThreadQueries()
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}