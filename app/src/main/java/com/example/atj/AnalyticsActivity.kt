package com.example.atj

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.utils.SessionManager

/**
 * Schermata Analytics semplice ma leggibile.
 * Calcola statistiche direttamente dai trade dell'utente.
 */
class AnalyticsActivity : AppCompatActivity() {

    private lateinit var totalTradesValueText: TextView
    private lateinit var openTradesValueText: TextView
    private lateinit var closedTradesValueText: TextView
    private lateinit var winRateValueText: TextView
    private lateinit var avgConfluenceValueText: TextView
    private lateinit var bestAssetValueText: TextView
    private lateinit var bestSessionValueText: TextView
    private lateinit var sourceBreakdownValueText: TextView
    private lateinit var assetWinRateValueText: TextView
    private lateinit var sessionBreakdownValueText: TextView

    private lateinit var database: AppDatabase
    private var currentUserId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        database = AppDatabase.getDatabase(this)
        currentUserId = SessionManager.getLoggedInUserId(this)

        bindViews()
        loadAnalytics()
    }

    private fun bindViews() {
        totalTradesValueText = findViewById(R.id.totalTradesValueText)
        openTradesValueText = findViewById(R.id.openTradesValueText)
        closedTradesValueText = findViewById(R.id.closedTradesValueText)
        winRateValueText = findViewById(R.id.winRateValueText)
        avgConfluenceValueText = findViewById(R.id.avgConfluenceValueText)
        bestAssetValueText = findViewById(R.id.bestAssetValueText)
        bestSessionValueText = findViewById(R.id.bestSessionValueText)
        sourceBreakdownValueText = findViewById(R.id.sourceBreakdownValueText)
        assetWinRateValueText = findViewById(R.id.assetWinRateValueText)
        sessionBreakdownValueText = findViewById(R.id.sessionBreakdownValueText)
    }

    private fun loadAnalytics() {
        val trades = database.tradeDao().getTradesByUserId(currentUserId)

        val totalTrades = trades.size
        val openTrades = trades.count { it.result.equals("Open", ignoreCase = true) }
        val closedTrades = trades.filter { !it.result.equals("Open", ignoreCase = true) }
        val wins = closedTrades.count { it.result.equals("Win", ignoreCase = true) }

        val winRate = if (closedTrades.isNotEmpty()) {
            (wins * 100) / closedTrades.size
        } else {
            0
        }

        val avgConfluence = if (trades.isNotEmpty()) {
            trades.sumOf { it.confluenceScore } / trades.size
        } else {
            0
        }

        totalTradesValueText.text = totalTrades.toString()
        openTradesValueText.text = openTrades.toString()
        closedTradesValueText.text = closedTrades.size.toString()
        winRateValueText.text = "$winRate%"
        avgConfluenceValueText.text = "$avgConfluence%"

        bestAssetValueText.text = findBestAsset(trades)
        bestSessionValueText.text = findBestSession(trades)
        sourceBreakdownValueText.text = buildSourceBreakdown(trades)
        assetWinRateValueText.text = buildAssetWinRateText(trades)
        sessionBreakdownValueText.text = buildSessionBreakdownText(trades)
    }

    private fun findBestAsset(trades: List<Trade>): String {
        val closedTrades = trades.filter { !it.result.equals("Open", ignoreCase = true) }
        if (closedTrades.isEmpty()) return "No data"

        val grouped = closedTrades.groupBy { it.asset }
        val scored = grouped.mapValues { entry ->
            val total = entry.value.size
            val wins = entry.value.count { it.result.equals("Win", ignoreCase = true) }
            if (total > 0) (wins * 100) / total else 0
        }

        val best = scored.maxByOrNull { it.value } ?: return "No data"
        return "${best.key} (${best.value}%)"
    }

    private fun findBestSession(trades: List<Trade>): String {
        val closedTrades = trades.filter { !it.result.equals("Open", ignoreCase = true) }
        if (closedTrades.isEmpty()) return "No data"

        val grouped = closedTrades.groupBy { it.session }
        val scored = grouped.mapValues { entry ->
            val total = entry.value.size
            val wins = entry.value.count { it.result.equals("Win", ignoreCase = true) }
            if (total > 0) (wins * 100) / total else 0
        }

        val best = scored.maxByOrNull { it.value } ?: return "No data"
        return "${best.key} (${best.value}%)"
    }

    private fun buildSourceBreakdown(trades: List<Trade>): String {
        val manual = trades.count { it.source.equals("manual", ignoreCase = true) }
        val json = trades.count { it.source.equals("json", ignoreCase = true) }
        return "Manual: $manual\nJSON: $json"
    }

    private fun buildAssetWinRateText(trades: List<Trade>): String {
        val closedTrades = trades.filter { !it.result.equals("Open", ignoreCase = true) }
        if (closedTrades.isEmpty()) return "No data"

        val grouped = closedTrades.groupBy { it.asset }

        return grouped.entries.joinToString("\n") { entry ->
            val total = entry.value.size
            val wins = entry.value.count { it.result.equals("Win", ignoreCase = true) }
            val rate = if (total > 0) (wins * 100) / total else 0
            "${entry.key}: $rate% ($wins/$total)"
        }
    }

    private fun buildSessionBreakdownText(trades: List<Trade>): String {
        if (trades.isEmpty()) return "No data"

        val grouped = trades.groupBy { it.session }

        return grouped.entries.joinToString("\n") { entry ->
            "${entry.key}: ${entry.value.size}"
        }
    }
}