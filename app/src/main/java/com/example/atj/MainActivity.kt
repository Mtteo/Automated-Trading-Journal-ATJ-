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
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.utils.DemoDataSeeder
import com.example.atj.utils.JsonSimulationSamples
import com.example.atj.utils.NotificationHelper
import com.example.atj.utils.SessionManager
import com.example.atj.utils.TradeEventParser
import com.example.atj.utils.TradeStateHelper
import com.google.android.material.button.MaterialButton
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var toggleAddMenuButton: MaterialButton
    private lateinit var addTradeButton: MaterialButton
    private lateinit var simulateTradeButton: MaterialButton

    private lateinit var openStrategyButton: MaterialButton
    private lateinit var openAnalyticsButton: MaterialButton
    private lateinit var openHistoryButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    private lateinit var addTradeMenuContainer: View

    private lateinit var welcomeTitleText: TextView
    private lateinit var welcomeSubtitleText: TextView

    private lateinit var totalTradesValueText: TextView
    private lateinit var totalTradesLabelText: TextView

    private lateinit var winRateValueText: TextView
    private lateinit var winRateLabelText: TextView

    private lateinit var sourceValueText: TextView
    private lateinit var sourceLabelText: TextView

    private lateinit var database: AppDatabase

    private var currentUserId: Long = -1L
    private var currentUsername: String = ""
    private var isAddMenuVisible: Boolean = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                NotificationHelper.createNotificationChannel(this)
                NotificationHelper.scheduleAllSessionNotifications(this)
            }
        }

    private val addTradeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult

                val asset = data.getStringExtra("asset") ?: return@registerForActivityResult
                val type = data.getStringExtra("type") ?: return@registerForActivityResult
                val direction = data.getStringExtra("direction") ?: ""
                val date = data.getStringExtra("date") ?: return@registerForActivityResult
                val session = data.getStringExtra("session") ?: "Unknown"
                val resultValue = data.getStringExtra("result") ?: "Open"
                val notes = data.getStringExtra("notes") ?: ""

                val imagePath = data.getStringExtra("imagePath")
                val strategyName = data.getStringExtra("strategyName") ?: ""
                val checkedConfluences = data.getStringExtra("checkedConfluences") ?: ""
                val confluenceScore = data.getIntExtra("confluenceScore", 0)
                val locationText = data.getStringExtra("locationText") ?: "Unknown"

                val entryPrice = data.getDoubleExtra("entryPrice", 0.0)
                val exitPrice = data.getDoubleExtra("exitPrice", 0.0)
                val stopLoss = data.getDoubleExtra("stopLoss", 0.0)
                val takeProfit = data.getDoubleExtra("takeProfit", 0.0)
                val rr = data.getDoubleExtra("rr", 0.0)
                val positionValue = data.getDoubleExtra("positionValue", 0.0)
                val positionPercentOfAccount = data.getDoubleExtra("positionPercentOfAccount", 0.0)
                val accountValue = data.getDoubleExtra("accountValue", 0.0)
                val pnlAmount = data.getDoubleExtra("pnlAmount", 0.0)
                val pnlPercent = data.getDoubleExtra("pnlPercent", 0.0)

                val trade = Trade(
                    userId = currentUserId,
                    source = "manual",
                    asset = asset,
                    type = type,
                    direction = direction,
                    result = resultValue.ifBlank { "Open" },
                    date = date,
                    session = session.ifBlank { "Unknown" },
                    locationText = locationText.ifBlank { "Unknown" },
                    entryPrice = entryPrice,
                    exitPrice = exitPrice,
                    stopLoss = stopLoss,
                    takeProfit = takeProfit,
                    rr = rr,
                    positionValue = positionValue,
                    positionPercentOfAccount = positionPercentOfAccount,
                    accountValue = accountValue,
                    pnlAmount = pnlAmount,
                    pnlPercent = pnlPercent,
                    notes = notes,
                    imagePath = imagePath,
                    strategyName = strategyName,
                    checkedConfluences = checkedConfluences,
                    confluenceScore = confluenceScore
                )

                val normalizedTrade = TradeStateHelper.normalizeTrade(trade)

                database.tradeDao().insertTrade(normalizedTrade)
                refreshDashboard()
                hideAddMenu()

                NotificationHelper.showTradeCreatedNotification(
                    context = this,
                    asset = normalizedTrade.asset,
                    type = normalizedTrade.type,
                    source = normalizedTrade.source
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

        DemoDataSeeder.prepareDemoDataForUser(
            context = this,
            database = database,
            userId = currentUserId,
            username = currentUsername
        )

        setupNotifications()
        bindViews()
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

        DemoDataSeeder.prepareDemoDataForUser(
            context = this,
            database = database,
            userId = currentUserId,
            username = currentUsername
        )

        refreshDashboard()
    }

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
        toggleAddMenuButton = findViewById(R.id.toggleAddMenuButton)
        addTradeButton = findViewById(R.id.addTradeButton)
        simulateTradeButton = findViewById(R.id.simulateTradeButton)

        openStrategyButton = findViewById(R.id.openStrategyButton)
        openAnalyticsButton = findViewById(R.id.openAnalyticsButton)
        openHistoryButton = findViewById(R.id.openHistoryButton)
        logoutButton = findViewById(R.id.logoutButton)

        addTradeMenuContainer = findViewById(R.id.addTradeMenuContainer)

        welcomeTitleText = findViewById(R.id.welcomeTitleText)
        welcomeSubtitleText = findViewById(R.id.welcomeSubtitleText)

        totalTradesValueText = findViewById(R.id.totalTradesValueText)
        totalTradesLabelText = findViewById(R.id.totalTradesLabelText)

        winRateValueText = findViewById(R.id.winRateValueText)
        winRateLabelText = findViewById(R.id.winRateLabelText)

        sourceValueText = findViewById(R.id.sourceValueText)
        sourceLabelText = findViewById(R.id.sourceLabelText)
    }

    private fun setupClickListeners() {
        toggleAddMenuButton.setOnClickListener {
            toggleAddMenu()
        }

        addTradeButton.setOnClickListener {
            val intent = Intent(this, AddTradeActivity::class.java)
            addTradeLauncher.launch(intent)
        }

        simulateTradeButton.setOnClickListener {
            simulateTradeEvent()
            hideAddMenu()
        }

        openStrategyButton.setOnClickListener {
            startActivity(Intent(this, StrategyActivity::class.java))
        }

        openAnalyticsButton.setOnClickListener {
            startActivity(Intent(this, AnalyticsActivity::class.java))
        }

        openHistoryButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }

        logoutButton.setOnClickListener {
            SessionManager.logout(this)
            openLoginAndClose()
        }
    }

    private fun refreshDashboard() {
        val allTrades = database.tradeDao().getTradesByUserId(currentUserId)
        updateDashboardStats(allTrades)
    }

    private fun updateDashboardStats(trades: List<Trade>) {
        val normalizedTrades = trades.map { TradeStateHelper.normalizeTrade(it) }

        val totalTrades = normalizedTrades.size

        val closedTrades = normalizedTrades.filter {
            TradeStateHelper.isClosed(it)
        }

        val openTradesCount = normalizedTrades.count {
            TradeStateHelper.isOpen(it)
        }

        val wins = closedTrades.count {
            TradeStateHelper.isWin(it)
        }

        val winRate = if (closedTrades.isNotEmpty()) {
            ((wins * 100.0) / closedTrades.size).toInt()
        } else {
            0
        }

        val manualCount = normalizedTrades.count {
            it.source.equals("manual", ignoreCase = true)
        }

        val jsonCount = normalizedTrades.count {
            it.source.equals("json", ignoreCase = true)
        }

        val demoCount = normalizedTrades.count {
            it.source.equals("demo", ignoreCase = true)
        }

        val latestTrade = normalizedTrades.firstOrNull()
        val latestAccountValue = latestTrade?.accountValue ?: 0.0
        val displayName = if (currentUsername.isNotBlank()) currentUsername else "Trader"
        val accountText = String.format(Locale.getDefault(), "%.2f", latestAccountValue)

        welcomeTitleText.text = if (DemoDataSeeder.isDemoProfile(currentUsername)) {
            "Demo profile, $displayName"
        } else {
            "Welcome, $displayName"
        }

        welcomeSubtitleText.text = buildWelcomeSubtitle(
            totalTrades = totalTrades,
            closedTrades = closedTrades.size,
            openTrades = openTradesCount,
            accountText = accountText,
            latestTrade = latestTrade
        )

        totalTradesValueText.text = totalTrades.toString()
        totalTradesLabelText.text = "Trades"

        winRateValueText.text = "$winRate%"
        winRateLabelText.text = if (closedTrades.isNotEmpty()) {
            "$wins wins"
        } else {
            "No closed trades"
        }

        sourceValueText.text = "$manualCount | $jsonCount | $demoCount"
        sourceLabelText.text = "Manual | JSON | Demo"
    }

    private fun buildWelcomeSubtitle(
        totalTrades: Int,
        closedTrades: Int,
        openTrades: Int,
        accountText: String,
        latestTrade: Trade?
    ): String {
        if (totalTrades == 0) {
            return "No trades yet. Start by adding your first trade."
        }

        val latestText = if (latestTrade != null) {
            "Latest: ${latestTrade.asset} • ${TradeStateHelper.displayState(latestTrade)} • ${latestTrade.date}"
        } else {
            "Latest: no trade"
        }

        return "$totalTrades trades • $closedTrades closed • $openTrades open • Account $accountText\n$latestText"
    }

    private fun simulateTradeEvent() {
        val sampleJson = JsonSimulationSamples.getNextSampleJson(System.currentTimeMillis().toInt())
        val parsedTrade = TradeEventParser.parseTradeEvent(sampleJson, currentUserId)
        val normalizedTrade = TradeStateHelper.normalizeTrade(parsedTrade)

        database.tradeDao().insertTrade(normalizedTrade)
        refreshDashboard()

        NotificationHelper.showTradeCreatedNotification(
            context = this,
            asset = normalizedTrade.asset,
            type = normalizedTrade.type,
            source = normalizedTrade.source
        )
    }

    private fun toggleAddMenu() {
        isAddMenuVisible = !isAddMenuVisible
        addTradeMenuContainer.visibility = if (isAddMenuVisible) View.VISIBLE else View.GONE
        toggleAddMenuButton.text = if (isAddMenuVisible) "×" else "+"
    }

    private fun hideAddMenu() {
        isAddMenuVisible = false
        addTradeMenuContainer.visibility = View.GONE
        toggleAddMenuButton.text = "+"
    }

    private fun openLoginAndClose() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}