package com.example.atj.utils

import com.example.atj.model.Trade
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Parser del payload JSON simulato.
 *
 * Trasforma il JSON in un Trade ricco di informazioni.
 */
object TradeEventParser {

    fun parseTradeEvent(json: String, userId: Long): Trade {
        val jsonObject = JSONObject(json)

        val asset = jsonObject.optString("asset", "UNKNOWN")
        val type = jsonObject.optString("type", "Buy")
        val direction = jsonObject.optString("direction", "")
        val result = jsonObject.optString("result", "Open")
        val source = jsonObject.optString("source", "json")
        val externalId = jsonObject.optString("externalId", "")

        val timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis())
        val formattedDate = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(Date(timestamp))

        val session = SessionHelper.getSessionFromTimestamp(timestamp)

        return Trade(
            userId = userId,
            source = source,
            externalId = externalId,
            asset = asset,
            type = type,
            direction = direction,
            result = result,
            date = formattedDate,
            session = session,
            locationText = "Auto",
            entryPrice = jsonObject.optDouble("entryPrice", 0.0),
            exitPrice = jsonObject.optDouble("exitPrice", 0.0),
            stopLoss = jsonObject.optDouble("stopLoss", 0.0),
            takeProfit = jsonObject.optDouble("takeProfit", 0.0),
            rr = jsonObject.optDouble("rr", 0.0),
            positionValue = jsonObject.optDouble("positionValue", 0.0),
            positionPercentOfAccount = jsonObject.optDouble("positionPercentOfAccount", 0.0),
            accountValue = jsonObject.optDouble("accountValue", 0.0),
            pnlAmount = jsonObject.optDouble("pnlAmount", 0.0),
            pnlPercent = jsonObject.optDouble("pnlPercent", 0.0),
            notes = jsonObject.optString("notes", "Auto imported from simulated JSON")
        )
    }
}