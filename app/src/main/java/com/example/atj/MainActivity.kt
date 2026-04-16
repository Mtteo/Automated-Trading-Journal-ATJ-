package com.example.atj

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.ui.TradeAdapter
import com.example.atj.utils.SessionManager
import com.example.atj.utils.TradeEventParser
import com.google.android.material.button.MaterialButton

/**
 * Dashboard principale dell'app.
 *
 * Questa versione è allineata con la tua base reale:
 * - usa SessionManager per recuperare l'utente loggato
 * - filtra i trade per userId
 * - crea trade manuali con userId obbligatorio
 * - evita riferimenti a view non presenti nel layout
 */
class MainActivity : AppCompatActivity() {

    // Pulsanti principali della dashboard
    private lateinit var addTradeButton: MaterialButton
    private lateinit var simulateTradeButton: MaterialButton
    private lateinit var openStrategyButton: MaterialButton

    // Testi header
    private lateinit var welcomeTitleText: TextView
    private lateinit var welcomeSubtitleText: TextView

    // Card statistiche
    private lateinit var totalTradesValueText: TextView
    private lateinit var totalTradesLabelText: TextView

    private lateinit var winRateValueText: TextView
    private lateinit var winRateLabelText: TextView

    private lateinit var sourceValueText: TextView
    private lateinit var sourceLabelText: TextView

    // Testi sezione lista
    private lateinit var recentTradesCountText: TextView
    private lateinit var emptyStateText: TextView

    // Lista trade
    private lateinit var tradeRecyclerView: RecyclerView
    private lateinit var tradeAdapter: TradeAdapter

    // Database
    private lateinit var database: AppDatabase

    // Utente loggato
    private var currentUserId: Long = -1L
    private var currentUsername: String = ""

    /**
     * Launcher moderno per AddTradeActivity.
     * Quando l'utente salva un trade, riceviamo i dati e lo inseriamo nel DB.
     */
    private val addTradeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val asset = data.getStringExtra("asset") ?: return@registerForActivityResult
                val type = data.getStringExtra("type") ?: return@registerForActivityResult
                val date = data.getStringExtra("date") ?: return@registerForActivityResult
                val session = data.getStringExtra("session") ?: "Unknown"
                val resultValue = data.getStringExtra("result") ?: "Open"
                val notes = data.getStringExtra("notes") ?: ""

                // Creazione del trade manuale associato all'utente loggato.
                val trade = Trade(
                    userId = currentUserId,
                    asset = asset,
                    type = type,
                    date = date,
                    session = session,
                    result = resultValue,
                    notes = notes,
                    source = "manual"
                )

                val newId = database.tradeDao().insertTrade(trade)
                val savedTrade = trade.copy(id = newId)

                tradeAdapter.addTrade(savedTrade)
                refreshDashboard()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Recupero database
        database = AppDatabase.getDatabase(this)

        // Recupero utente loggato dalla sessione
        currentUserId = SessionManager.getLoggedInUserId(this)
        currentUsername = SessionManager.getLoggedInUsername(this)

        // Sicurezza minima: se per qualche motivo non esiste una sessione valida,
        // evitiamo di lavorare con userId non valido.
        if (currentUserId == -1L) {
            finish()
            return
        }

        bindViews()
        setupRecyclerView()
        setupClickListeners()
        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()

        // Ricarichiamo i dati quando si torna sulla dashboard
        refreshDashboard()
    }

    /**
     * Collega tutte le view del layout.
     */
    private fun bindViews() {
        addTradeButton = findViewById(R.id.addTradeButton)
        simulateTradeButton = findViewById(R.id.simulateTradeButton)
        openStrategyButton = findViewById(R.id.openStrategyButton)

        welcomeTitleText = findViewById(R.id.welcomeTitleText)
        welcomeSubtitleText = findViewById(R.id.welcomeSubtitleText)

        totalTradesValueText = findViewById(R.id.totalTradesValueText)
        totalTradesLabelText = findViewById(R.id.totalTradesLabelText)

        winRateValueText = findViewById(R.id.winRateValueText)
        winRateLabelText = findViewById(R.id.winRateLabelText)

        sourceValueText = findViewById(R.id.sourceValueText)
        sourceLabelText = findViewById(R.id.sourceLabelText)

        recentTradesCountText = findViewById(R.id.recentTradesCountText)
        emptyStateText = findViewById(R.id.emptyStateText)

        tradeRecyclerView = findViewById(R.id.tradeRecyclerView)
    }

    /**
     * Configura la RecyclerView dei trade.
     */
    private fun setupRecyclerView() {
        tradeAdapter = TradeAdapter(mutableListOf()) { trade ->
            val intent = Intent(this, TradeDetailActivity::class.java).apply {
                putExtra("trade_id", trade.id)
            }
            startActivity(intent)
        }

        tradeRecyclerView.layoutManager = LinearLayoutManager(this)
        tradeRecyclerView.adapter = tradeAdapter
        tradeRecyclerView.setHasFixedSize(false)
    }

    /**
     * Listener di tutti i pulsanti della dashboard.
     */
    private fun setupClickListeners() {
        addTradeButton.setOnClickListener {
            val intent = Intent(this, AddTradeActivity::class.java)
            addTradeLauncher.launch(intent)
        }

        simulateTradeButton.setOnClickListener {
            simulateTradeEvent()
        }

        openStrategyButton.setOnClickListener {
            openStrategyScreen()
        }
    }

    /**
     * Ricarica lista + statistiche per l'utente loggato.
     */
    private fun refreshDashboard() {
        val allTrades = database.tradeDao().getTradesByUserId(currentUserId)

        tradeAdapter.replaceTrades(allTrades)
        updateDashboardStats(allTrades)

        val isEmpty = allTrades.isEmpty()
        emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        tradeRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    /**
     * Aggiorna i numeri mostrati nella dashboard.
     */
    private fun updateDashboardStats(trades: List<Trade>) {
        val totalTrades = trades.size

        // Trade chiusi = tutti quelli diversi da Open
        val closedTrades = trades.filter { !it.result.equals("Open", ignoreCase = true) }
        val openTradesCount = totalTrades - closedTrades.size

        // Conteggio dei win sui trade chiusi
        val wins = closedTrades.count { it.result.equals("Win", ignoreCase = true) }

        val winRate = if (closedTrades.isNotEmpty()) {
            (wins * 100) / closedTrades.size
        } else {
            0
        }

        val manualCount = trades.count { it.source.equals("manual", ignoreCase = true) }
        val jsonCount = trades.count { it.source.equals("json", ignoreCase = true) }

        // Header dashboard
        welcomeTitleText.text = if (currentUsername.isNotBlank()) {
            "Welcome, $currentUsername"
        } else {
            "ATJ Dashboard"
        }

        welcomeSubtitleText.text = if (totalTrades == 0) {
            "Start building your trading journal."
        } else {
            "${closedTrades.size} closed • $openTradesCount open • Stay consistent."
        }

        // Stat card 1
        totalTradesValueText.text = totalTrades.toString()
        totalTradesLabelText.text = "Total trades"

        // Stat card 2
        winRateValueText.text = "$winRate%"
        winRateLabelText.text = if (closedTrades.isNotEmpty()) {
            "$wins wins on ${closedTrades.size} closed"
        } else {
            "No closed trades yet"
        }

        // Stat card 3
        sourceValueText.text = "$manualCount / $jsonCount"
        sourceLabelText.text = "Manual / JSON"

        // Testo sopra la lista
        recentTradesCountText.text = if (totalTrades == 0) {
            "0 trades"
        } else {
            "$totalTrades trade${if (totalTrades == 1) "" else "s"}"
        }
    }

    /**
     * Apre la schermata strategie.
     */
    private fun openStrategyScreen() {
        val intent = Intent(this, StrategyActivity::class.java)
        startActivity(intent)
    }

    /**
     * Simula un trade JSON e lo salva per l'utente corrente.
     */
    private fun simulateTradeEvent() {
        val sampleJson = """
            {
              "asset": "SPX500",
              "type": "Sell",
              "timestamp": 1713139200000
            }
        """.trimIndent()

        // TradeEventParser restituisce un Trade;
        // qui forziamo l'associazione all'utente loggato.
        val parsedTrade = TradeEventParser.parseTradeEvent(sampleJson).copy(userId = currentUserId)

        val newId = database.tradeDao().insertTrade(parsedTrade)
        val savedTrade = parsedTrade.copy(id = newId)

        tradeAdapter.addTrade(savedTrade)
        refreshDashboard()
    }
}