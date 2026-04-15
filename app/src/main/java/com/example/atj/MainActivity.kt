package com.example.atj

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.ui.TradeAdapter
import com.example.atj.utils.LocationHelper
import com.example.atj.utils.SessionHelper
import com.example.atj.utils.StrategyManager
import com.example.atj.utils.TradeEventParser

class MainActivity : AppCompatActivity() {

    private lateinit var addTradeButton: Button
    private lateinit var simulateTradeButton: Button
    private lateinit var strategyButton: Button
    private lateinit var tradeRecyclerView: RecyclerView
    private lateinit var tradeAdapter: TradeAdapter
    private lateinit var database: AppDatabase

    private lateinit var totalTradesText: TextView
    private lateinit var winRateText: TextView
    private lateinit var manualTradesText: TextView
    private lateinit var jsonTradesText: TextView

    private val requestLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            // Non facciamo nulla qui: la location verrà letta quando serve.
        }

    private val addTradeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data

                val asset = data?.getStringExtra("asset") ?: return@registerForActivityResult
                val type = data.getStringExtra("type") ?: return@registerForActivityResult
                val date = data.getStringExtra("date") ?: return@registerForActivityResult
                val session = data.getStringExtra("session") ?: "Unknown"
                val resultValue = data.getStringExtra("result") ?: "Open"
                val notes = data.getStringExtra("notes") ?: ""
                val imagePath = data.getStringExtra("imagePath")
                val strategyName = data.getStringExtra("strategyName") ?: ""
                val checkedConfluences = data.getStringExtra("checkedConfluences") ?: ""
                val confluenceScore = data.getIntExtra("confluenceScore", 0)
                val locationText = data.getStringExtra("locationText") ?: "Unknown"

                val trade = Trade(
                    asset = asset,
                    type = type,
                    date = date,
                    session = session,
                    result = resultValue,
                    notes = notes,
                    source = "manual",
                    imagePath = imagePath,
                    strategyName = strategyName,
                    checkedConfluences = checkedConfluences,
                    confluenceScore = confluenceScore,
                    locationText = locationText
                )

                val newId = database.tradeDao().insertTrade(trade)
                val savedTrade = trade.copy(id = newId)

                tradeAdapter.addTrade(savedTrade)
                loadDashboard()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addTradeButton = findViewById(R.id.addTradeButton)
        simulateTradeButton = findViewById(R.id.simulateTradeButton)
        strategyButton = findViewById(R.id.strategyButton)
        tradeRecyclerView = findViewById(R.id.tradeRecyclerView)

        totalTradesText = findViewById(R.id.totalTradesText)
        winRateText = findViewById(R.id.winRateText)
        manualTradesText = findViewById(R.id.manualTradesText)
        jsonTradesText = findViewById(R.id.jsonTradesText)

        database = AppDatabase.getDatabase(this)

        tradeAdapter = TradeAdapter(mutableListOf()) { trade ->
            val intent = Intent(this, TradeDetailActivity::class.java).apply {
                putExtra("trade_id", trade.id)
            }
            startActivity(intent)
        }

        tradeRecyclerView.layoutManager = LinearLayoutManager(this)
        tradeRecyclerView.adapter = tradeAdapter

        requestLocationPermissionIfNeeded()
        loadTrades()
        loadDashboard()

        addTradeButton.setOnClickListener {
            val intent = Intent(this, AddTradeActivity::class.java)
            addTradeLauncher.launch(intent)
        }

        simulateTradeButton.setOnClickListener {
            simulateTradeEvent()
        }

        strategyButton.setOnClickListener {
            startActivity(Intent(this, StrategyActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadTrades()
        loadDashboard()
    }

    private fun requestLocationPermissionIfNeeded() {
        if (!LocationHelper.hasLocationPermission(this)) {
            requestLocationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun loadTrades() {
        val allTrades = database.tradeDao().getAllTrades()
        tradeAdapter.replaceTrades(allTrades)
    }

    private fun loadDashboard() {
        val allTrades = database.tradeDao().getAllTrades()

        val totalTrades = allTrades.size
        val manualTrades = allTrades.count { it.source.trim().lowercase() == "manual" }
        val jsonTrades = allTrades.count { it.source.trim().lowercase() == "json" }

        val normalizedResults = allTrades.map { it.result.trim().lowercase() }

        val closedTradesCount = normalizedResults.count {
            it == "win" || it == "loss" || it == "be"
        }

        val winningTrades = normalizedResults.count { it == "win" }

        val winRate = if (closedTradesCount > 0) {
            (winningTrades * 100) / closedTradesCount
        } else {
            0
        }

        totalTradesText.text = "Total Trades: $totalTrades"
        winRateText.text = "Win Rate: $winRate%"
        manualTradesText.text = "Manual Trades: $manualTrades"
        jsonTradesText.text = "JSON Trades: $jsonTrades"
    }

    private fun simulateTradeEvent() {
        val sampleJson = """
            {
              "asset": "SPX500",
              "type": "Sell",
              "timestamp": 1713139200000
            }
        """.trimIndent()

        val parsedTrade = TradeEventParser.parseTradeEvent(sampleJson)
        val activeStrategy = StrategyManager.getStrategy(this)

        // Per il trade JSON simulato compiliamo automaticamente:
        // - sessione da timestamp del trade
        // - data leggibile da timestamp
        // - luogo dal device
        val timestamp = 1713139200000L
        val autoSession = SessionHelper.getSessionFromTimestamp(timestamp)
        val formattedDate = SessionHelper.formatDateFromTimestamp(timestamp)
        val autoLocation = LocationHelper.getCurrentLocationText(this)

        val enrichedTrade = parsedTrade.copy(
            date = formattedDate,
            session = autoSession,
            strategyName = activeStrategy.name,
            checkedConfluences = "",
            confluenceScore = 0,
            locationText = autoLocation
        )

        val newId = database.tradeDao().insertTrade(enrichedTrade)
        val savedTrade = enrichedTrade.copy(id = newId)

        tradeAdapter.addTrade(savedTrade)
        loadDashboard()
    }
}