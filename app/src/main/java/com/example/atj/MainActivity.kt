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

    // TextView della dashboard
    private lateinit var totalTradesText: TextView
    private lateinit var winRateText: TextView
    private lateinit var manualTradesText: TextView
    private lateinit var jsonTradesText: TextView

    // Launcher moderno per aprire AddTradeActivity e ricevere il risultato.
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

                // Creo il trade partendo dai dati della schermata manuale.
                val trade = Trade(
                    asset = asset,
                    type = type,
                    date = date,
                    session = session,
                    result = resultValue,
                    notes = notes,
                    source = "manual"
                )

                // Lo salvo nel database.
                val newId = database.tradeDao().insertTrade(trade)

                // Creo una copia con id valorizzato.
                val savedTrade = trade.copy(id = newId)

                // Lo aggiungo subito alla lista visibile.
                tradeAdapter.addTrade(savedTrade)

                // Aggiorno anche la dashboard.
                loadDashboard()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Collego i bottoni
        addTradeButton = findViewById(R.id.addTradeButton)
        simulateTradeButton = findViewById(R.id.simulateTradeButton)

        // Collego RecyclerView
        tradeRecyclerView = findViewById(R.id.tradeRecyclerView)

        // Collego TextView dashboard
        totalTradesText = findViewById(R.id.totalTradesText)
        winRateText = findViewById(R.id.winRateText)
        manualTradesText = findViewById(R.id.manualTradesText)
        jsonTradesText = findViewById(R.id.jsonTradesText)

        // Recupero istanza database
        database = AppDatabase.getDatabase(this)

        // Creo l'adapter e definisco cosa succede quando clicco un trade
        tradeAdapter = TradeAdapter(mutableListOf()) { trade ->
            val intent = Intent(this, TradeDetailActivity::class.java).apply {
                putExtra("trade_id", trade.id)
            }
            startActivity(intent)
        }

        // Imposto RecyclerView
        tradeRecyclerView.layoutManager = LinearLayoutManager(this)
        tradeRecyclerView.adapter = tradeAdapter

        // Carico i trade già presenti e la dashboard
        loadTrades()
        loadDashboard()

        // Apertura schermata di inserimento manuale
        addTradeButton.setOnClickListener {
            val intent = Intent(this, AddTradeActivity::class.java)
            addTradeLauncher.launch(intent)
        }

        // Simulazione di un evento esterno JSON
        simulateTradeButton.setOnClickListener {
            simulateTradeEvent()
        }
    }

    override fun onResume() {
        super.onResume()

        // Ogni volta che torniamo su questa schermata, ricarichiamo i dati.
        // Serve per esempio dopo un delete nel dettaglio.
        loadTrades()
        loadDashboard()
    }

    // Carica tutti i trade dal database e li mostra nella lista.
    private fun loadTrades() {
        val allTrades = database.tradeDao().getAllTrades()
        tradeAdapter.replaceTrades(allTrades)
    }

    // Aggiorna le statistiche della dashboard.
    private fun loadDashboard() {
        val allTrades = database.tradeDao().getAllTrades()

        val totalTrades = allTrades.size
        val manualTrades = allTrades.count { it.source.trim().lowercase() == "manual" }
        val jsonTrades = allTrades.count { it.source.trim().lowercase() == "json" }

        // Normalizziamo il risultato per evitare problemi tipo:
        // "win", "Win ", " WIN", ecc.
        val normalizedResults = allTrades.map { it.result.trim().lowercase() }

        // Consideriamo chiusi i trade con risultato:
        // win, loss, be
        val closedTradesCount = normalizedResults.count {
            it == "win" || it == "loss" || it == "be"
        }

        // Trade vincenti reali
        val winningTrades = normalizedResults.count { it == "win" }

        // Win rate = win / trade chiusi * 100
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

    // Simula l'arrivo di un JSON locale e crea un trade automatico.
    private fun simulateTradeEvent() {
        val sampleJson = """
            {
              "asset": "SPX500",
              "type": "Sell",
              "timestamp": 1713139200000
            }
        """.trimIndent()

        // Parsing del JSON con il metodo reale presente nella repo.
        val parsedTrade = TradeEventParser.parseTradeEvent(sampleJson)

        // Salvataggio nel database.
        val newId = database.tradeDao().insertTrade(parsedTrade)

        // Aggiorno l'id anche nell'oggetto in memoria.
        val savedTrade = parsedTrade.copy(id = newId)

        // Aggiungo subito alla lista.
        tradeAdapter.addTrade(savedTrade)

        // Aggiorno dashboard.
        loadDashboard()
    }
}