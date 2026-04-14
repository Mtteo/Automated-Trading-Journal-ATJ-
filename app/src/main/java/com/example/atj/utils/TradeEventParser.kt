package com.example.atj.utils

import com.example.atj.model.Trade
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Oggetto utility che si occupa di convertire un JSON simulato
// in un oggetto Trade pronto da salvare nel database.
object TradeEventParser {

    fun parseTradeEvent(json: String): Trade {
        // Converte la stringa JSON in un oggetto leggibile.
        val jsonObject = JSONObject(json)

        // Estrae i campi principali dal JSON.
        val asset = jsonObject.getString("asset")
        val type = jsonObject.getString("type")
        val timestamp = jsonObject.getLong("timestamp")

        // Converte il timestamp in una data leggibile.
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            .format(Date(timestamp))

        // Restituisce un Trade con alcuni campi valorizzati automaticamente.
        return Trade(
            asset = asset,
            type = type,
            date = formattedDate,
            session = "NY",
            result = "Open",
            notes = "Auto generated",
            source = "json"
        )
    }
}