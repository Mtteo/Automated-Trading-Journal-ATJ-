package com.example.atj.utils

import org.json.JSONObject
import java.util.Calendar
import kotlin.random.Random

/*
 * Generatore di trade JSON simulati.
 * Serve a testare l'import automatico senza dipendere da API esterne reali.
 */
object JsonSimulationSamples {

    /*
     * Crea un payload JSON diverso a ogni chiamata.
     * Il formato simula dati ricevuti da un sistema esterno.
     */
    fun getNextSampleJson(indexSeed: Int): String {
        val random = Random(System.currentTimeMillis() + indexSeed)

        val asset = if (random.nextBoolean()) "NAS100" else "SPX500"
        val isNasdaq = asset == "NAS100"

        val isLong = random.nextBoolean()
        val direction = if (isLong) "Long" else "Short"
        val type = if (isLong) "Buy" else "Sell"

        val resultOptions = listOf("Win", "Loss", "Open")
        val result = resultOptions[random.nextInt(resultOptions.size)]

        val baseEntry = if (isNasdaq) {
            random.nextDouble(18100.0, 18850.0)
        } else {
            random.nextDouble(5240.0, 5410.0)
        }

        val stopDistance = if (isNasdaq) {
            random.nextDouble(25.0, 55.0)
        } else {
            random.nextDouble(8.0, 18.0)
        }

        val targetDistance = stopDistance * random.nextDouble(1.4, 2.8)

        val entryPrice = round2(baseEntry)

        val stopLoss = if (isLong) {
            entryPrice - stopDistance
        } else {
            entryPrice + stopDistance
        }

        val takeProfit = if (isLong) {
            entryPrice + targetDistance
        } else {
            entryPrice - targetDistance
        }

        val exitPrice = when (result) {
            "Win" -> takeProfit + random.nextDouble(-3.0, 3.0)
            "Loss" -> stopLoss + random.nextDouble(-2.0, 2.0)
            else -> 0.0
        }

        val accountValue = random.nextDouble(9500.0, 11200.0)
        val riskAmount = accountValue * random.nextDouble(0.006, 0.014)

        val pnlAmount = when (result) {
            "Win" -> riskAmount * random.nextDouble(1.3, 2.4)
            "Loss" -> -riskAmount
            else -> 0.0
        }

        val pnlPercent = if (accountValue != 0.0) {
            (pnlAmount / accountValue) * 100.0
        } else {
            0.0
        }

        val positionValue = accountValue * random.nextDouble(0.08, 0.20)
        val positionPercent = (positionValue / accountValue) * 100.0

        val rr = targetDistance / stopDistance

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -random.nextInt(0, 72))
        calendar.set(Calendar.MINUTE, listOf(0, 15, 30, 45).random())

        val externalId = "json_${asset.lowercase()}_${System.currentTimeMillis()}"

        val notes = when (result) {
            "Win" -> "Generated JSON trade. Setup respected: liquidity sweep, confirmation and clean expansion."
            "Loss" -> "Generated JSON trade. Loss accepted. Review: entry was slightly early."
            else -> "Generated JSON open trade. Waiting for confirmation or management."
        }

        /*
         * JSONObject costruisce una struttura key-value simile a quella ricevuta
         * da backend o integrazioni automatiche.
         */
        val jsonObject = JSONObject()
        jsonObject.put("source", "json")
        jsonObject.put("externalId", externalId)
        jsonObject.put("asset", asset)
        jsonObject.put("type", type)
        jsonObject.put("direction", direction)
        jsonObject.put("result", result)
        jsonObject.put("entryPrice", round2(entryPrice))
        jsonObject.put("exitPrice", round2(exitPrice))
        jsonObject.put("stopLoss", round2(stopLoss))
        jsonObject.put("takeProfit", round2(takeProfit))
        jsonObject.put("rr", round2(rr))
        jsonObject.put("positionValue", round2(positionValue))
        jsonObject.put("positionPercentOfAccount", round2(positionPercent))
        jsonObject.put("accountValue", round2(accountValue))
        jsonObject.put("pnlAmount", round2(pnlAmount))
        jsonObject.put("pnlPercent", round2(pnlPercent))
        jsonObject.put("timestamp", calendar.timeInMillis)
        jsonObject.put("notes", notes)

        return jsonObject.toString()
    }

    /*
     * Arrotonda i numeri a due decimali per simulare dati più leggibili.
     */
    private fun round2(value: Double): Double {
        return kotlin.math.round(value * 100.0) / 100.0
    }
}