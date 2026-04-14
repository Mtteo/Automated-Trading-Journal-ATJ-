package com.example.atj.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.atj.model.Trade

// Il DAO contiene tutte le operazioni sul database relative ai trade.
@Dao
interface TradeDao {

    // Restituisce tutti i trade ordinati dal più recente al più vecchio.
    @Query("SELECT * FROM trades ORDER BY id DESC")
    fun getAllTrades(): MutableList<Trade>

    // Inserisce un trade nel database e restituisce l'id generato.
    @Insert
    fun insertTrade(trade: Trade): Long

    // Elimina un trade dal database.
    @Delete
    fun deleteTrade(trade: Trade)

    // Cerca un singolo trade per id.
    @Query("SELECT * FROM trades WHERE id = :tradeId LIMIT 1")
    fun getTradeById(tradeId: Long): Trade?
}