package com.example.atj

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
import com.example.atj.utils.TradeEventParser

// Activity principale dell'app.
// Qui gestiamo:
// - caricamento lista trade
// - aggiunta manuale
// - simulazione evento JSON
// - apertura dettaglio trade
// - dashboard con statistiche base
class MainActivity : AppCompatActivity() {

    private lateinit var addTradeButton: Button
    private lateinit var simulateTradeButton: Button
    private lateinit var tradeRecyclerView: RecyclerView
    private lateinit var tradeAdapter: TradeAdapter
    private lateinit var database: AppDatabase

    // TextView dashboard
    private lateinit var totalTradesText: TextView
    private lateinit var winRateText: TextView
    private lateinit var manualTradesText: TextView
    private lateinit var jsonTradesText: TextView

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

                val trade = Trade(
                    asset = asset,
                    type = type,
                    date = date,
                    session = session,
                    result = resultValue,
                    notes = notes,
                    source = "manual",
                    imagePath = imagePath
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

        loadTrades()
        loadDashboard()

        addTradeButton.setOnClickListener {
            val intent = Intent(this, AddTradeActivity::class.java)
            addTradeLauncher.launch(intent)
        }

        simulateTradeButton.setOnClickListener {
            simulateTradeEvent()
        }
    }

    override fun onResume() {
        super.onResume()
        loadTrades()
        loadDashboard()
    }

    // Carica tutti i trade dal database e li mostra nella lista.
    private fun loadTrades() {
        val allTrades = database.tradeDao().getAllTrades()
        tradeAdapter.replaceTrades(allTrades)
    }

    // Dashboard statistiche
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

    // Simula un evento JSON locale
    private fun simulateTradeEvent() {
        val sampleJson = """
            {
              "asset": "SPX500",
              "type": "Sell",
              "timestamp": 1713139200000
            }
        """.trimIndent()

        val parsedTrade = TradeEventParser.parseTradeEvent(sampleJson)
        val newId = database.tradeDao().insertTrade(parsedTrade)
        val savedTrade = parsedTrade.copy(id = newId)

        tradeAdapter.addTrade(savedTrade)
        loadDashboard()
    }
}