package com.example.atj.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.atj.model.Trade

/*
 * DAO dei trade.
 * Contiene le query Room per leggere e modificare la tabella trades.
 */
@Dao
interface TradeDao {

    /*
     * Restituisce tutti i trade di un utente, dal più recente al più vecchio.
     */
    @Query("SELECT * FROM trades WHERE userId = :userId ORDER BY id DESC")
    fun getTradesByUserId(userId: Long): MutableList<Trade>

    /*
     * Recupera l'ultimo trade inserito da un utente.
     * Trade? indica che può non esistere alcun risultato.
     */
    @Query("SELECT * FROM trades WHERE userId = :userId ORDER BY id DESC LIMIT 1")
    fun getLatestTradeByUserId(userId: Long): Trade?

    /*
     * Cerca un trade tramite id locale.
     */
    @Query("SELECT * FROM trades WHERE id = :tradeId LIMIT 1")
    fun getTradeById(tradeId: Long): Trade?

    /*
     * Cerca un trade importato/simulato tramite externalId.
     * userId evita conflitti tra utenti diversi.
     */
    @Query("SELECT * FROM trades WHERE externalId = :externalId AND userId = :userId LIMIT 1")
    fun getTradeByExternalId(externalId: String, userId: Long): Trade?

    /*
     * Elimina solo i trade demo dell'utente corrente.
     */
    @Query("DELETE FROM trades WHERE userId = :userId AND source = 'demo'")
    fun deleteDemoTradesByUserId(userId: Long)

    /*
     * Inserisce un trade e restituisce l'id generato dal database.
     */
    @Insert
    fun insertTrade(trade: Trade): Long

    @Update
    fun updateTrade(trade: Trade)

    @Delete
    fun deleteTrade(trade: Trade)
}