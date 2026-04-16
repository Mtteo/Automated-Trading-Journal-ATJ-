package com.example.atj

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.utils.SessionManager

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase

    private lateinit var totalTradesText: TextView
    private lateinit var closedTradesText: TextView
    private lateinit var openTradesText: TextView
    private lateinit var avgConfluenceScoreText: TextView
    private lateinit var bestAssetText: TextView
    private lateinit var bestSessionText: TextView
    private lateinit var sessionBreakdownText: TextView
    private lateinit var assetWinRatesText: TextView
    private lateinit var sessionWinRatesText: TextView
    private lateinit var sourceBreakdownText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionManager.isLoggedIn(this)) {
            finish()
            return
        }

        setContentView(R.layout.activity_analytics)

        database = AppDatabase.getDatabase(this)

        totalTradesText = findViewById(R.id.totalTradesAnalyticsText)
        closedTradesText = findViewById(R.id.closedTradesAnalyticsText)
        openTradesText = findViewById(R.id.openTradesAnalyticsText)
        avgConfluenceScoreText = findViewById(R.id.avgConfluenceScoreAnalyticsText)
        bestAssetText = findViewById(R.id.bestAssetAnalyticsText)
        bestSessionText = findViewById(R.id.bestSessionAnalyticsText)
        sessionBreakdownText = findViewById(R.id.sessionBreakdownAnalyticsText)
        assetWinRatesText = findViewById(R.id.assetWinRatesAnalyticsText)
        sessionWinRatesText = findViewById(R.id.sessionWinRatesAnalyticsText)
        sourceBreakdownText = findViewById(R.id.sourceBreakdownAnalyticsText)

        loadAnalytics()
    }

    private fun loadAnalytics() {
        val currentUserId = SessionManager.getLoggedInUserId(this)
        val trades = database.tradeDao().getTradesByUserId(currentUserId)

        val totalTrades = trades.size
        val closedTrades = trades.filter { isClosedTrade(it) }
        val openTrades = trades.filter { it.result.trim().lowercase() == "open" }

        val avgConfluenceScore = if (trades.isNotEmpty()) {
            trades.map { it.confluenceScore }.average().toInt()
        } else {
            0
        }

        totalTradesText.text = "Total Trades: $totalTrades"
        closedTradesText.text = "Closed Trades: ${closedTrades.size}"
        openTradesText.text = "Open Trades: ${openTrades.size}"
        avgConfluenceScoreText.text = "Average Confluence Score: $avgConfluenceScore%"

        bestAssetText.text = "Best Asset: ${getBestAssetByWinRate(trades)}"
        bestSessionText.text = "Best Session: ${getBestSessionByWinRate(trades)}"

        sessionBreakdownText.text = buildSessionBreakdown(trades)
        assetWinRatesText.text = buildAssetWinRates(trades)
        sessionWinRatesText.text = buildSessionWinRates(trades)
        sourceBreakdownText.text = buildSourceBreakdown(trades)
    }

    private fun isClosedTrade(trade: Trade): Boolean {
        val normalized = trade.result.trim().lowercase()
        return normalized == "win" || normalized == "loss" || normalized == "be"
    }

    private fun isWinningTrade(trade: Trade): Boolean {
        return trade.result.trim().lowercase() == "win"
    }

    private fun getBestAssetByWinRate(trades: List<Trade>): String {
        val closedTrades = trades.filter { isClosedTrade(it) }
        if (closedTrades.isEmpty()) return "No closed trades"

        val grouped = closedTrades.groupBy { it.asset.trim() }

        var bestAsset = "N/A"
        var bestRate = -1.0

        for ((asset, assetTrades) in grouped) {
            val wins = assetTrades.count { isWinningTrade(it) }
            val rate = wins.toDouble() / assetTrades.size.toDouble()

            if (rate > bestRate) {
                bestRate = rate
                bestAsset = "$asset (${(rate * 100).toInt()}%)"
            }
        }

        return bestAsset
    }

    private fun getBestSessionByWinRate(trades: List<Trade>): String {
        val closedTrades = trades.filter { isClosedTrade(it) }
        if (closedTrades.isEmpty()) return "No closed trades"

        val grouped = closedTrades.groupBy { it.session.trim() }

        var bestSession = "N/A"
        var bestRate = -1.0

        for ((session, sessionTrades) in grouped) {
            val wins = sessionTrades.count { isWinningTrade(it) }
            val rate = wins.toDouble() / sessionTrades.size.toDouble()

            if (rate > bestRate) {
                bestRate = rate
                bestSession = "$session (${(rate * 100).toInt()}%)"
            }
        }

        return bestSession
    }

    private fun buildSessionBreakdown(trades: List<Trade>): String {
        val sydney = trades.count { it.session.trim().equals("Sydney", ignoreCase = true) }
        val asia = trades.count { it.session.trim().equals("Asia", ignoreCase = true) }
        val london = trades.count { it.session.trim().equals("London", ignoreCase = true) }
        val ny = trades.count { it.session.trim().equals("NY", ignoreCase = true) }

        return """
            Session Breakdown:
            - Sydney: $sydney
            - Asia: $asia
            - London: $london
            - NY: $ny
        """.trimIndent()
    }

    private fun buildAssetWinRates(trades: List<Trade>): String {
        val closedTrades = trades.filter { isClosedTrade(it) }
        if (closedTrades.isEmpty()) return "Asset Win Rates:\nNo closed trades"

        val grouped = closedTrades.groupBy { it.asset.trim() }

        val lines = grouped.map { (asset, assetTrades) ->
            val wins = assetTrades.count { isWinningTrade(it) }
            val rate = (wins * 100) / assetTrades.size
            "- $asset: $rate% (${assetTrades.size} trades)"
        }.sorted()

        return "Asset Win Rates:\n" + lines.joinToString("\n")
    }

    private fun buildSessionWinRates(trades: List<Trade>): String {
        val closedTrades = trades.filter { isClosedTrade(it) }
        if (closedTrades.isEmpty()) return "Session Win Rates:\nNo closed trades"

        val grouped = closedTrades.groupBy { it.session.trim() }

        val lines = grouped.map { (session, sessionTrades) ->
            val wins = sessionTrades.count { isWinningTrade(it) }
            val rate = (wins * 100) / sessionTrades.size
            "- $session: $rate% (${sessionTrades.size} trades)"
        }.sorted()

        return "Session Win Rates:\n" + lines.joinToString("\n")
    }

    private fun buildSourceBreakdown(trades: List<Trade>): String {
        val manualTrades = trades.count { it.source.trim().equals("manual", ignoreCase = true) }
        val jsonTrades = trades.count { it.source.trim().equals("json", ignoreCase = true) }

        return """
            Source Breakdown:
            - Manual: $manualTrades
            - JSON: $jsonTrades
        """.trimIndent()
    }
}