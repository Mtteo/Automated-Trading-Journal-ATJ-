package com.example.atj.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.example.atj.model.Trade

@Dao
interface TradeDao {

    @Query("SELECT * FROM trades WHERE userId = :userId ORDER BY id DESC")
    fun getTradesByUserId(userId: Long): MutableList<Trade>

    @Insert
    fun insertTrade(trade: Trade): Long

    @Delete
    fun deleteTrade(trade: Trade)

    @Query("SELECT * FROM trades WHERE id = :tradeId LIMIT 1")
    fun getTradeById(tradeId: Long): Trade?
}