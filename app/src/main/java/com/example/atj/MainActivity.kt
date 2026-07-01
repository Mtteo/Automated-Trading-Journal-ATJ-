package com.example.atj

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.utils.DemoDataSeeder
import com.example.atj.utils.DemoStrategySeeder
import com.example.atj.utils.JsonSimulationSamples
import com.example.atj.utils.NotificationHelper
import com.example.atj.utils.SessionManager
import com.example.atj.utils.TradeEventParser
import com.example.atj.utils.TradeStateHelper
import com.google.android.material.button.MaterialButton
import java.util.Locale

/*
 * Activity principale dell'app.
 * Funziona da dashboard: mostra statistiche, gestisce navigazione e inserimento trade.
 */
class MainActivity : AppCompatActivity() {

    /*
     * Database Room usato per leggere e salvare i trade dell'utente corrente.
     */
    private lateinit var database: AppDatabase

    /*
     * Dati della sessione utente salvati localmente tramite SessionManager.
     */
    private var currentUserId: Long = -1L
    private var username: String = ""

    /*
     * View della dashboard collegate dal layout XML.
     */
    private lateinit var welcomeTitleText: TextView
    private lateinit var welcomeSubtitleText: TextView

    private lateinit var totalTradesValueText: TextView
    private lateinit var totalTradesLabelText: TextView

    private lateinit var winRateValueText: TextView
    private lateinit var winRateLabelText: TextView

    private lateinit var sourceValueText: TextView
    private lateinit var sourceLabelText: TextView

    private lateinit var toggleAddMenuButton: MaterialButton
    private lateinit var addTradeButton: MaterialButton
    private lateinit var simulateTradeButton: MaterialButton
    private lateinit var openStrategyButton: MaterialButton
    private lateinit var openAnalyticsButton: MaterialButton
    private lateinit var openHistoryButton: MaterialButton
    private lateinit var logoutButton: MaterialButton

    private lateinit var addTradeMenuContainer: View

    /*
     * Stato locale usato per mostrare o nascondere il menu di aggiunta.
     */
    private var isAddMenuVisible = false

    /*
     * Activity Result API per ricevere un trade creato da AddTradeActivity.
     * È il metodo moderno rispetto a startActivityForResult deprecato.
     */
    private val addTradeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != RESULT_OK) return@registerForActivityResult

            val data = result.data ?: return@registerForActivityResult

            /*
             * Ricostruisce l'Entity Trade dai dati restituiti tramite Intent.
             * La MainActivity si occupa poi del salvataggio nel database.
             */
            val trade = Trade(
                userId = currentUserId,
                source = "manual",
                externalId = "",
                asset = data.getStringExtra("asset") ?: "",
                type = data.getStringExtra("type") ?: "",
                direction = data.getStringExtra("direction") ?: "",
                result = data.getStringExtra("result") ?: "Open",
                date = data.getStringExtra("date") ?: "",
                session = data.getStringExtra("session") ?: "Unknown",
                locationText = data.getStringExtra("locationText") ?: "Unknown",
                entryPrice = data.getDoubleExtra("entryPrice", 0.0),
                exitPrice = data.getDoubleExtra("exitPrice", 0.0),
                stopLoss = data.getDoubleExtra("stopLoss", 0.0),
                takeProfit = data.getDoubleExtra("takeProfit", 0.0),
                rr = data.getDoubleExtra("rr", 0.0),
                positionValue = data.getDoubleExtra("positionValue", 0.0),
                positionPercentOfAccount = data.getDoubleExtra("positionPercentOfAccount", 0.0),
                accountValue = data.getDoubleExtra("accountValue", 0.0),
                pnlAmount = data.getDoubleExtra("pnlAmount", 0.0),
                pnlPercent = data.getDoubleExtra("pnlPercent", 0.0),
                notes = data.getStringExtra("notes") ?: "",
                imagePath = data.getStringExtra("imagePath"),
                strategyName = data.getStringExtra("strategyName") ?: "",
                checkedConfluences = data.getStringExtra("checkedConfluences") ?: "",
                confluenceScore = data.getIntExtra("confluenceScore", 0)
            )

            val normalizedTrade = TradeStateHelper.normalizeTrade(trade)
            database.tradeDao().insertTrade(normalizedTrade)

            NotificationHelper.showTradeCreatedNotification(
                context = this,
                asset = normalizedTrade.asset,
                type = normalizedTrade.type,
                source = normalizedTrade.source
            )

            hideAddMenu()
            updateDashboardStats()

            Toast.makeText(this, "Trade saved", Toast.LENGTH_SHORT).show()
        }

    /*
     * Launcher per il permesso runtime delle notifiche.
     * Da Android 13 il permesso POST_NOTIFICATIONS va richiesto all'utente.
     */
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show()
            }
        }

    /*
     * onCreate inizializza database, sessione, layout, listener e statistiche.
     * Se non c'è login, l'utente viene rimandato alla LoginActivity.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        database = AppDatabase.getDatabase(this)

        currentUserId = SessionManager.getLoggedInUserId(this)
        username = SessionManager.getLoggedInUsername(this)

        if (currentUserId == -1L) {
            openLoginAndClose()
            return
        }

        prepareDemoContentIfNeeded()

        setContentView(R.layout.activity_main)

        bindViews()
        setupNotifications()
        setupClickListeners()
        updateDashboardStats()
    }

    /*
     * onResume aggiorna i dati quando la dashboard torna in primo piano.
     * Utile dopo inserimenti, modifiche o cambiamenti di sessione.
     */
    override fun onResume() {
        super.onResume()

        if (::database.isInitialized) {
            currentUserId = SessionManager.getLoggedInUserId(this)
            username = SessionManager.getLoggedInUsername(this)

            if (currentUserId == -1L) {
                openLoginAndClose()
                return
            }

            prepareDemoContentIfNeeded()

            if (::welcomeTitleText.isInitialized) {
                updateDashboardStats()
            }
        }
    }

    /*
     * Collega le View XML agli oggetti Kotlin tramite findViewById.
     */
    private fun bindViews() {
        welcomeTitleText = findViewById(R.id.welcomeTitleText)
        welcomeSubtitleText = findViewById(R.id.welcomeSubtitleText)

        totalTradesValueText = findViewById(R.id.totalTradesValueText)
        totalTradesLabelText = findViewById(R.id.totalTradesLabelText)

        winRateValueText = findViewById(R.id.winRateValueText)
        winRateLabelText = findViewById(R.id.winRateLabelText)

        sourceValueText = findViewById(R.id.sourceValueText)
        sourceLabelText = findViewById(R.id.sourceLabelText)

        toggleAddMenuButton = findViewById(R.id.toggleAddMenuButton)
        addTradeButton = findViewById(R.id.addTradeButton)
        simulateTradeButton = findViewById(R.id.simulateTradeButton)

        openStrategyButton = findViewById(R.id.openStrategyButton)
        openAnalyticsButton = findViewById(R.id.openAnalyticsButton)
        openHistoryButton = findViewById(R.id.openHistoryButton)

        logoutButton = findViewById(R.id.logoutButton)

        addTradeMenuContainer = findViewById(R.id.addTradeMenuContainer)
    }

    /*
     * Configura canale notifiche, allarmi giornalieri e permesso runtime.
     */
    private fun setupNotifications() {
        NotificationHelper.createNotificationChannel(this)
        NotificationHelper.scheduleAllSessionNotifications(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissionGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    /*
     * Registra i listener dei pulsanti.
     * Ogni click attiva una navigazione o una funzione della dashboard.
     */
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

    /*
     * Prepara dati demo se l'utente corrente è un profilo demo.
     * Serve per avere una presentazione già popolata e testabile.
     */
    private fun prepareDemoContentIfNeeded() {
        DemoDataSeeder.prepareDemoDataForUser(
            context = this,
            database = database,
            userId = currentUserId,
            username = username
        )

        if (DemoDataSeeder.isDemoProfile(username)) {
            DemoStrategySeeder.seedDemoStrategy(this)
        }
    }

    /*
     * Legge i trade dal database e aggiorna le metriche della dashboard.
     */
    private fun updateDashboardStats() {
        val trades = database.tradeDao()
            .getTradesByUserId(currentUserId)
            .map { TradeStateHelper.normalizeTrade(it) }

        val totalTrades = trades.size
        val closedTrades = trades.filter { TradeStateHelper.isClosed(it) }
        val winningTrades = trades.filter { TradeStateHelper.isWin(it) }
        val openTrades = trades.filter { TradeStateHelper.isOpen(it) }

        val winRate = if (closedTrades.isNotEmpty()) {
            (winningTrades.size.toDouble() / closedTrades.size.toDouble()) * 100.0
        } else {
            0.0
        }

        val manualTrades = trades.count { it.source.equals("manual", ignoreCase = true) }
        val jsonTrades = trades.count { it.source.equals("json", ignoreCase = true) }
        val demoTrades = trades.count { it.source.equals("demo", ignoreCase = true) }

        welcomeTitleText.text = "Welcome, $username"
        welcomeSubtitleText.text = buildWelcomeSubtitle(trades)

        totalTradesValueText.text = totalTrades.toString()
        totalTradesLabelText.text = if (totalTrades == 1) {
            "Trade"
        } else {
            "Trades"
        }

        winRateValueText.text = String.format(Locale.getDefault(), "%.0f%%", winRate)
        winRateLabelText.text = if (closedTrades.isEmpty()) {
            "No closed trades"
        } else {
            "${winningTrades.size}/${closedTrades.size} closed"
        }

        sourceValueText.text = "$manualTrades | $jsonTrades | $demoTrades"
        sourceLabelText.text = "Manual | JSON | Demo"

        if (openTrades.isNotEmpty()) {
            welcomeSubtitleText.append("\nOpen trades: ${openTrades.size}")
        }
    }

    /*
     * Costruisce il sottotitolo della dashboard in base allo stato del journal.
     */
    private fun buildWelcomeSubtitle(trades: List<Trade>): String {
        if (trades.isEmpty()) {
            return if (DemoDataSeeder.isDemoProfile(username)) {
                "Demo profile ready. Trade history and strategy are prepared."
            } else {
                "No trades yet. Start by adding your first trade."
            }
        }

        val latestTrade = trades.maxByOrNull { it.id }

        return if (latestTrade != null) {
            val state = TradeStateHelper.displayState(latestTrade)
            "Latest trade: ${latestTrade.asset} • $state • ${latestTrade.date}"
        } else {
            "Journal ready."
        }
    }

    /*
     * Simula un evento JSON esterno.
     * Il payload viene parsato e salvato come Trade nel database Room.
     */
    private fun simulateTradeEvent() {
        val json = JsonSimulationSamples.getNextSampleJson(
            database.tradeDao().getTradesByUserId(currentUserId).size
        )

        val parsedTrade = TradeEventParser.parseTradeEvent(json, currentUserId)

        if (parsedTrade == null) {
            Toast.makeText(this, "Invalid JSON trade event", Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedTrade = TradeStateHelper.normalizeTrade(parsedTrade)
        database.tradeDao().insertTrade(normalizedTrade)

        NotificationHelper.showTradeCreatedNotification(
            context = this,
            asset = normalizedTrade.asset,
            type = normalizedTrade.type,
            source = normalizedTrade.source
        )

        hideAddMenu()
        updateDashboardStats()

        Toast.makeText(this, "Simulated JSON trade saved", Toast.LENGTH_SHORT).show()
    }

    /*
     * Mostra o nasconde il menu di aggiunta trade.
     */
    private fun toggleAddMenu() {
        isAddMenuVisible = !isAddMenuVisible

        addTradeMenuContainer.visibility = if (isAddMenuVisible) {
            View.VISIBLE
        } else {
            View.GONE
        }

        toggleAddMenuButton.text = if (isAddMenuVisible) {
            "×"
        } else {
            "+"
        }
    }

    /*
     * Chiude il menu di aggiunta dopo salvataggio o simulazione.
     */
    private fun hideAddMenu() {
        isAddMenuVisible = false
        addTradeMenuContainer.visibility = View.GONE
        toggleAddMenuButton.text = "+"
    }

    /*
     * Torna al login cancellando il back stack.
     * Così l'utente non può rientrare nella dashboard con il tasto Back dopo il logout.
     */
    private fun openLoginAndClose() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}