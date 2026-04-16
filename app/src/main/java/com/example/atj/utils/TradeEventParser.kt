package com.example.atj.utils

import com.example.atj.model.Trade
import org.json.JSONObject

// Parser semplice per simulare l'arrivo di un trade da evento JSON.
object TradeEventParser {

    fun parseTradeEvent(json: String): Trade {
        val jsonObject = JSONObject(json)

        val asset = jsonObject.getString("asset")
        val type = jsonObject.getString("type")
        val timestamp = jsonObject.getLong("timestamp")

        val formattedDate = SessionHelper.formatDateFromTimestamp(timestamp)
        val session = SessionHelper.getSessionFromTimestamp(timestamp)

        return Trade(
            userId = 0L, // verrà poi sostituito in MainActivity con l'utente loggato
            asset = asset,
            type = type,
            date = formattedDate,
            session = session,
            result = "Open",
            notes = "",
            source = "json",
            imagePath = null,
            strategyName = "",
            checkedConfluences = "",
            confluenceScore = 0,
            locationText = "Unknown"
        )
    }
}