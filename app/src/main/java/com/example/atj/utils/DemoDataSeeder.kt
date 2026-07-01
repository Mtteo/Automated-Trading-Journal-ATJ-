package com.example.atj.utils

import android.content.Context
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.random.Random

/*
 * Seeder dei dati demo.
 * Serve a popolare il database locale con trade realistici per test e presentazione.
 */
object DemoDataSeeder {

    private const val DEMO_PREFS_NAME = "atj_demo_data_prefs"

    private val demoUsernames = listOf(
        "demo",
        "esame",
        "atjdemo"
    )

    private val demoConfluences = listOf(
        "Liquidity sweep",
        "BOS High Timeframe",
        "OB retracement",
        "BOS OB",
        "FVG respected",
        "FVG inverted",
        "Equilibrium",
        "SMT",
        "75% closure"
    )

    /*
     * Crea una chiave diversa per ogni utente.
     * Così il caricamento demo resta separato per profilo.
     */
    private fun buildDemoKey(userId: Long): String {
        return "demo_loaded_user_$userId"
    }

    /*
     * Verifica se l'utente corrente è un profilo demo.
     */
    fun isDemoProfile(username: String): Boolean {
        return demoUsernames.any { demoName ->
            username.equals(demoName, ignoreCase = true)
        }
    }

    /*
     * Punto di ingresso per preparare i dati demo.
     * Usa Room per i trade e SharedPreferences per ricordare lo stato del seed.
     */
    fun prepareDemoDataForUser(
        context: Context,
        database: AppDatabase,
        userId: Long,
        username: String
    ) {
        if (userId == -1L) return

        if (!isDemoProfile(username)) {
            database.tradeDao().deleteDemoTradesByUserId(userId)
            return
        }

        seedDemoMonthIfNeeded(context, database, userId)
    }

    /*
     * Inserisce i trade demo solo se non sono già presenti.
     * Evita duplicati nella tabella trades.
     */
    private fun seedDemoMonthIfNeeded(
        context: Context,
        database: AppDatabase,
        userId: Long
    ) {
        val existingTrades = database.tradeDao().getTradesByUserId(userId)

        val existingDemoTrades = existingTrades.filter {
            it.source.equals("demo", ignoreCase = true)
        }

        if (existingDemoTrades.isNotEmpty()) {
            context.getSharedPreferences(DEMO_PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(buildDemoKey(userId), true)
                .apply()

            return
        }

        seedDemoMonth(database, userId)

        context.getSharedPreferences(DEMO_PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(buildDemoKey(userId), true)
            .apply()
    }

    /*
     * Costruisce i trade demo e li salva tramite DAO.
     * Prima dell'inserimento normalizza lo stato del trade.
     */
    private fun seedDemoMonth(database: AppDatabase, userId: Long) {
        val demoTrades = buildDemoTrades(userId)

        demoTrades.forEach { trade ->
            val normalizedTrade = TradeStateHelper.normalizeTrade(trade)
            database.tradeDao().insertTrade(normalizedTrade)
        }
    }

    /*
     * Genera una lista di trade demo.
     * I dati sono fittizi ma coerenti con il dominio dell'app.
     */
    private fun buildDemoTrades(userId: Long): List<Trade> {
        val trades = mutableListOf<Trade>()

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 15)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        /*
         * Template base dei trade.
         * Vengono riutilizzati nel ciclo per creare un mese demo vario.
         */
        val templates = listOf(
            DemoTemplate(
                asset = "NAS100",
                type = "Buy",
                direction = "Long",
                result = "Win",
                setupName = "Liquidity sweep + bullish displacement",
                baseEntry = 18420.0,
                pointsMove = 72.0,
                stopDistance = 32.0
            ),
            DemoTemplate(
                asset = "NAS100",
                type = "Sell",
                direction = "Short",
                result = "Loss",
                setupName = "Failed bearish continuation after sweep",
                baseEntry = 18580.0,
                pointsMove = -38.0,
                stopDistance = 34.0
            ),
            DemoTemplate(
                asset = "SPX500",
                type = "Buy",
                direction = "Long",
                result = "Win",
                setupName = "OB retracement with FVG respected",
                baseEntry = 5325.0,
                pointsMove = 24.0,
                stopDistance = 11.0
            ),
            DemoTemplate(
                asset = "SPX500",
                type = "Sell",
                direction = "Short",
                result = "Loss",
                setupName = "FVG inverted but confirmation was weak",
                baseEntry = 5350.0,
                pointsMove = -13.0,
                stopDistance = 10.0
            )
        )

        var accountValue = 10000.0

        /*
         * Genera 22 trade chiusi distribuiti sul mese.
         * Calendar e SimpleDateFormat servono per creare date leggibili e timestamp.
         */
        for (i in 0 until 22) {
            val template = templates[i % templates.size]

            val hour = when (i % 4) {
                0 -> 9
                1 -> 10
                2 -> 14
                else -> 15
            }

            val minute = when (i % 3) {
                0 -> 15
                1 -> 30
                else -> 45
            }

            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.add(Calendar.DAY_OF_MONTH, i)
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)

            val timestamp = calendar.timeInMillis
            val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                .format(calendar.time)

            // Piccola variazione casuale per rendere i dati demo meno artificiali.
            val randomNoise = Random.nextDouble(-18.0, 18.0)
            val entry = template.baseEntry + randomNoise

            val isWin = template.result.equals("Win", ignoreCase = true)
            val isLong = template.direction.equals("Long", ignoreCase = true)

            val move = abs(template.pointsMove) + Random.nextDouble(-6.0, 10.0)
            val stopDistance = template.stopDistance + Random.nextDouble(-3.0, 4.0)

            val exit = when {
                isWin && isLong -> entry + move
                isWin && !isLong -> entry - move
                !isWin && isLong -> entry - stopDistance
                else -> entry + stopDistance
            }

            val stopLoss = if (isLong) {
                entry - stopDistance
            } else {
                entry + stopDistance
            }

            val takeProfit = if (isLong) {
                entry + move
            } else {
                entry - move
            }

            val riskAmount = Random.nextDouble(70.0, 130.0)

            val pnlAmount = if (isWin) {
                riskAmount * Random.nextDouble(1.35, 2.35)
            } else {
                -riskAmount
            }

            accountValue += pnlAmount

            val pnlPercent = if (accountValue != 0.0) {
                (pnlAmount / accountValue) * 100.0
            } else {
                0.0
            }

            val rr = if (riskAmount != 0.0 && isWin) {
                pnlAmount / riskAmount
            } else {
                Random.nextDouble(1.0, 2.2)
            }

            val positionValue = accountValue * Random.nextDouble(0.08, 0.18)
            val positionPercent = (positionValue / accountValue) * 100.0

            val checkedConfluences = buildCheckedConfluences(i)

            val confluenceScore = if (isWin) {
                Random.nextInt(72, 96)
            } else {
                Random.nextInt(38, 68)
            }

            /*
             * Entity Trade pronta per Room.
             * Ogni oggetto diventerà una riga nella tabella trades.
             */
            val trade = Trade(
                userId = userId,
                source = "demo",
                externalId = "demo_${template.asset.lowercase()}_${i + 1}",
                asset = template.asset,
                type = template.type,
                direction = template.direction,
                result = template.result,
                date = date,
                session = SessionHelper.getSessionFromTimestamp(timestamp),
                locationText = "Demo Location",
                entryPrice = round2(entry),
                exitPrice = round2(exit),
                stopLoss = round2(stopLoss),
                takeProfit = round2(takeProfit),
                rr = round2(rr),
                positionValue = round2(positionValue),
                positionPercentOfAccount = round2(positionPercent),
                accountValue = round2(accountValue),
                pnlAmount = round2(pnlAmount),
                pnlPercent = round2(pnlPercent),
                notes = buildDemoNotes(template, isWin),
                imagePath = null,
                strategyName = "Liquidity ICT Strategy",
                checkedConfluences = checkedConfluences,
                confluenceScore = confluenceScore
            )

            trades.add(trade)
        }

        // Aggiunge un trade aperto per mostrare anche questo stato nella UI.
        trades.add(
            buildOpenTrade(
                userId = userId,
                asset = "NAS100",
                type = "Buy",
                direction = "Long",
                dayOfMonth = 27,
                entry = 18620.0,
                accountValue = accountValue
            )
        )

        // Aggiunge un trade break-even per coprire un altro caso possibile.
        trades.add(
            buildBreakEvenTrade(
                userId = userId,
                asset = "SPX500",
                type = "Sell",
                direction = "Short",
                dayOfMonth = 28,
                entry = 5364.0,
                accountValue = accountValue
            )
        )

        return trades
    }

    /*
     * Costruisce un trade demo ancora aperto.
     */
    private fun buildOpenTrade(
        userId: Long,
        asset: String,
        type: String,
        direction: String,
        dayOfMonth: Int,
        entry: Double,
        accountValue: Double
    ): Trade {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        calendar.set(Calendar.HOUR_OF_DAY, 14)
        calendar.set(Calendar.MINUTE, 30)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(calendar.time)

        return Trade(
            userId = userId,
            source = "demo",
            externalId = "demo_open_${asset.lowercase()}",
            asset = asset,
            type = type,
            direction = direction,
            result = "Open",
            date = date,
            session = SessionHelper.getSessionFromTimestamp(calendar.timeInMillis),
            locationText = "Demo Location",
            entryPrice = round2(entry),
            exitPrice = 0.0,
            stopLoss = round2(entry - 35.0),
            takeProfit = round2(entry + 80.0),
            rr = 2.28,
            positionValue = 1350.0,
            positionPercentOfAccount = 13.5,
            accountValue = round2(accountValue),
            pnlAmount = 0.0,
            pnlPercent = 0.0,
            notes = "Open demo trade based on Liquidity ICT Strategy. Waiting for continuation after liquidity sweep and BOS High Timeframe confirmation.",
            imagePath = null,
            strategyName = "Liquidity ICT Strategy",
            checkedConfluences = "Liquidity sweep, BOS High Timeframe, OB retracement, FVG respected, Equilibrium",
            confluenceScore = 75
        )
    }

    /*
     * Costruisce un trade chiuso a pareggio.
     */
    private fun buildBreakEvenTrade(
        userId: Long,
        asset: String,
        type: String,
        direction: String,
        dayOfMonth: Int,
        entry: Double,
        accountValue: Double
    ): Trade {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 45)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            .format(calendar.time)

        return Trade(
            userId = userId,
            source = "demo",
            externalId = "demo_be_${asset.lowercase()}",
            asset = asset,
            type = type,
            direction = direction,
            result = "BE",
            date = date,
            session = SessionHelper.getSessionFromTimestamp(calendar.timeInMillis),
            locationText = "Demo Location",
            entryPrice = round2(entry),
            exitPrice = round2(entry),
            stopLoss = round2(entry + 10.0),
            takeProfit = round2(entry - 22.0),
            rr = 2.2,
            positionValue = 900.0,
            positionPercentOfAccount = 9.0,
            accountValue = round2(accountValue),
            pnlAmount = 0.0,
            pnlPercent = 0.0,
            notes = "Break-even demo trade. Good idea based on SMT and FVG respected, but price did not expand enough after entry.",
            imagePath = null,
            strategyName = "Liquidity ICT Strategy",
            checkedConfluences = "Liquidity sweep, SMT, FVG respected, Equilibrium",
            confluenceScore = 58
        )
    }

    /*
     * Seleziona un sottoinsieme casuale di confluence.
     * Serve a simulare checklist diverse tra trade.
     */
    private fun buildCheckedConfluences(index: Int): String {
        val count = 4 + (index % 4)

        return demoConfluences
            .shuffled()
            .take(count)
            .joinToString(", ")
    }

    /*
     * Genera note demo diverse per trade vinti e persi.
     */
    private fun buildDemoNotes(template: DemoTemplate, isWin: Boolean): String {
        return if (isWin) {
            "Demo setup: ${template.setupName}. Execution followed Liquidity ICT Strategy. Main confirmations: liquidity sweep, structure confirmation and fair value gap reaction."
        } else {
            "Demo setup: ${template.setupName}. Loss accepted according to plan. Main review point: wait for stronger confirmation before entering after liquidity sweep."
        }
    }

    /*
     * Arrotonda i valori numerici a due decimali per una visualizzazione più pulita.
     */
    private fun round2(value: Double): Double {
        return kotlin.math.round(value * 100.0) / 100.0
    }

    /*
     * Template interno per creare trade demo.
     * data class usata solo come modello di supporto, non come Entity Room.
     */
    private data class DemoTemplate(
        val asset: String,
        val type: String,
        val direction: String,
        val result: String,
        val setupName: String,
        val baseEntry: Double,
        val pointsMove: Double,
        val stopDistance: Double
    )
}