package com.example.atj

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
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
import com.example.atj.utils.NotificationHelper
import com.example.atj.utils.SessionManager
import com.example.atj.utils.TradeEventParser
import com.google.android.material.button.MaterialButton

/**
 * Dashboard principale.
 *
 * Nota:
 * per ora la lista trade resta ancora presente nel layout.
 * Nel prossimo step la togliamo dalla home e la spostiamo in una sezione History dedicata.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var addTradeButton: MaterialButton
    private lateinit var simulateTradeButton: MaterialButton
    private lateinit var openStrategyButton: MaterialButton
    private lateinit var openAnalyticsButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    private lateinit var welcomeTitleText: TextView
    private lateinit var welcomeSubtitleText: TextView

    private lateinit var totalTradesValueText: TextView
    private lateinit var totalTradesLabelText: TextView

    private lateinit var winRateValueText: TextView
    private lateinit var winRateLabelText: TextView

    private lateinit var sourceValueText: TextView
    private lateinit var sourceLabelText: TextView

    private lateinit var recentTradesCountText: TextView
    private lateinit var emptyStateText: TextView

    private lateinit var tradeRecyclerView: RecyclerView
    private lateinit var tradeAdapter: TradeAdapter

    private lateinit var database: AppDatabase

    private var currentUserId: Long = -1L
    private var currentUsername: String = ""

    /**
     * Launcher per il permesso notifiche su Android 13+.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                NotificationHelper.createNotificationChannel(this)
                NotificationHelper.scheduleAllSessionNotifications(this)
            }
        }

    /**
     * Launcher per AddTradeActivity.
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

                val imagePath = data.getStringExtra("imagePath")
                val strategyName = data.getStringExtra("strategyName") ?: ""
                val checkedConfluences = data.getStringExtra("checkedConfluences") ?: ""
                val confluenceScore = data.getIntExtra("confluenceScore", 0)
                val locationText = data.getStringExtra("locationText") ?: "Unknown"

                val trade = Trade(
                    userId = currentUserId,
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
                refreshDashboard()

                // Notifica trade salvato
                NotificationHelper.showTradeCreatedNotification(
                    context = this,
                    asset = trade.asset,
                    type = trade.type,
                    source = trade.source
                )
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        database = AppDatabase.getDatabase(this)

        currentUserId = SessionManager.getLoggedInUserId(this)
        currentUsername = SessionManager.getLoggedInUsername(this)

        if (currentUserId == -1L) {
            openLoginAndClose()
            return
        }

        // Setup notifiche sempre all'avvio della home
        setupNotifications()

        bindViews()
        setupRecyclerView()
        setupClickListeners()
        refreshDashboard()
    }

    override fun onResume() {
        super.onResume()

        currentUserId = SessionManager.getLoggedInUserId(this)
        currentUsername = SessionManager.getLoggedInUsername(this)

        if (currentUserId == -1L) {
            openLoginAndClose()
            return
        }

        refreshDashboard()
    }

    /**
     * Inizializza canale notifiche, richiede eventuale permesso
     * e programma le notifiche di sessione.
     */
    private fun setupNotifications() {
        NotificationHelper.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!NotificationHelper.hasNotificationPermission(this)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                NotificationHelper.scheduleAllSessionNotifications(this)
            }
        } else {
            NotificationHelper.scheduleAllSessionNotifications(this)
        }
    }

    private fun bindViews() {
        addTradeButton = findViewById(R.id.addTradeButton)
        simulateTradeButton = findViewById(R.id.simulateTradeButton)
        openStrategyButton = findViewById(R.id.openStrategyButton)
        openAnalyticsButton = findViewById(R.id.openAnalyticsButton)
        logoutButton = findViewById(R.id.logoutButton)

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

    private fun setupClickListeners() {
        addTradeButton.setOnClickListener {
            val intent = Intent(this, AddTradeActivity::class.java)
            addTradeLauncher.launch(intent)
        }

        simulateTradeButton.setOnClickListener {
            simulateTradeEvent()
        }

        openStrategyButton.setOnClickListener {
            startActivity(Intent(this, StrategyActivity::class.java))
        }

        openAnalyticsButton.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }

        logoutButton.setOnClickListener {
            SessionManager.logout(this)
            openLoginAndClose()
        }
    }

    private fun refreshDashboard() {
        val allTrades = database.tradeDao().getTradesByUserId(currentUserId)

        tradeAdapter.replaceTrades(allTrades)
        updateDashboardStats(allTrades)

        val isEmpty = allTrades.isEmpty()
        emptyStateText.visibility = if (isEmpty) View.VISIBLE else View.GONE
        tradeRecyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateDashboardStats(trades: List<Trade>) {
        val totalTrades = trades.size
        val closedTrades = trades.filter { !it.result.equals("Open", ignoreCase = true) }
        val openTradesCount = totalTrades - closedTrades.size
        val wins = closedTrades.count { it.result.equals("Win", ignoreCase = true) }

        val winRate = if (closedTrades.isNotEmpty()) {
            (wins * 100) / closedTrades.size
        } else {
            0
        }

        val manualCount = trades.count { it.source.equals("manual", ignoreCase = true) }
        val jsonCount = trades.count { it.source.equals("json", ignoreCase = true) }

        welcomeTitleText.text = if (currentUsername.isNotBlank()) {
            "Welcome, $currentUsername"
        } else {
            "ATJ Dashboard"
        }

        welcomeSubtitleText.text = if (totalTrades == 0) {
            "Track your execution with clean structure."
        } else {
            "${closedTrades.size} closed • $openTradesCount open • Stay consistent."
        }

        totalTradesValueText.text = totalTrades.toString()
        totalTradesLabelText.text = "Total trades"

        winRateValueText.text = "$winRate%"
        winRateLabelText.text = if (closedTrades.isNotEmpty()) {
            "$wins wins on ${closedTrades.size} closed"
        } else {
            "No closed trades yet"
        }

        sourceValueText.text = "$manualCount / $jsonCount"
        sourceLabelText.text = "Manual / JSON"

        recentTradesCountText.text = if (totalTrades == 0) {
            "0 trades"
        } else {
            "$totalTrades trade${if (totalTrades == 1) "" else "s"}"
        }
    }

    private fun simulateTradeEvent() {
        val sampleJson = """
            {
              "asset": "SPX500",
              "type": "Sell",
              "timestamp": 1713139200000
            }
        """.trimIndent()

        val parsedTrade = TradeEventParser.parseTradeEvent(sampleJson).copy(userId = currentUserId)

        val newId = database.tradeDao().insertTrade(parsedTrade)
        val savedTrade = parsedTrade.copy(id = newId)

        tradeAdapter.addTrade(savedTrade)
        refreshDashboard()

        // Notifica trade simulato salvato
        NotificationHelper.showTradeCreatedNotification(
            context = this,
            asset = parsedTrade.asset,
            type = parsedTrade.type,
            source = parsedTrade.source
        )
    }

    private fun openLoginAndClose() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}