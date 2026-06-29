package com.example.atj.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.atj.model.Trade

@Dao
interface TradeDao {

    @Query("SELECT * FROM trades WHERE userId = :userId ORDER BY id DESC")
    fun getTradesByUserId(userId: Long): MutableList<Trade>

    @Query("SELECT * FROM trades WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    fun getLatestTradeByUserId(userId: Long): Trade?

    @Query("SELECT * FROM trades WHERE id = :tradeId LIMIT 1")
    fun getTradeById(tradeId: Long): Trade?

    @Query("SELECT * FROM trades WHERE externalId = :externalId AND userId = :userId LIMIT 1")
    fun getTradeByExternalId(externalId: String, userId: Long): Trade?

    @Query("DELETE FROM trades WHERE userId = :userId AND source = 'demo'")
    fun deleteDemoTradesByUserId(userId: Long)

    @Insert
    fun insertTrade(trade: Trade): Long

    @Update
    fun updateTrade(trade: Trade)

    @Delete
    fun deleteTrade(trade: Trade)
}