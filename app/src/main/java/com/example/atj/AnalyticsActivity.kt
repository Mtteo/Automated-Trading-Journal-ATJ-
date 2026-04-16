package com.example.atj

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.ui.CalendarDayAdapter
import com.example.atj.ui.CalendarDayUiModel
import com.example.atj.utils.SessionManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Nuova schermata Analytics / Journal Workspace.
 *
 * Obiettivi:
 * - rendere la seconda schermata più simile a una workspace web app
 * - mostrare metriche importanti in alto
 * - mostrare il calendario mensile con esiti giornalieri
 * - ridurre l'effetto "lista lunga e brutta"
 */
class AnalyticsActivity : AppCompatActivity() {

    private lateinit var monthTitleText: TextView
    private lateinit var monthSubtitleText: TextView

    private lateinit var totalTradesValueText: TextView
    private lateinit var winRateValueText: TextView
    private lateinit var performanceBalanceValueText: TextView
    private lateinit var performanceBalanceLabelText: TextView

    private lateinit var bestAssetValueText: TextView
    private lateinit var bestSessionValueText: TextView
    private lateinit var avgConfluenceValueText: TextView
    private lateinit var sourceBreakdownValueText: TextView
    private lateinit var sessionBreakdownValueText: TextView

    private lateinit var calendarRecyclerView: RecyclerView
    private lateinit var calendarAdapter: CalendarDayAdapter

    private lateinit var database: AppDatabase
    private var currentUserId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        database = AppDatabase.getDatabase(this)
        currentUserId = SessionManager.getLoggedInUserId(this)

        bindViews()
        setupCalendar()
        loadAnalytics()
    }

    private fun bindViews() {
        monthTitleText = findViewById(R.id.monthTitleText)
        monthSubtitleText = findViewById(R.id.monthSubtitleText)

        totalTradesValueText = findViewById(R.id.totalTradesValueText)
        winRateValueText = findViewById(R.id.winRateValueText)
        performanceBalanceValueText = findViewById(R.id.performanceBalanceValueText)
        performanceBalanceLabelText = findViewById(R.id.performanceBalanceLabelText)

        bestAssetValueText = findViewById(R.id.bestAssetValueText)
        bestSessionValueText = findViewById(R.id.bestSessionValueText)
        avgConfluenceValueText = findViewById(R.id.avgConfluenceValueText)
        sourceBreakdownValueText = findViewById(R.id.sourceBreakdownValueText)
        sessionBreakdownValueText = findViewById(R.id.sessionBreakdownValueText)

        calendarRecyclerView = findViewById(R.id.calendarRecyclerView)
    }

    private fun setupCalendar() {
        calendarAdapter = CalendarDayAdapter(mutableListOf())
        calendarRecyclerView.layoutManager = GridLayoutManager(this, 7)
        calendarRecyclerView.adapter = calendarAdapter
    }

    private fun loadAnalytics() {
        val trades = database.tradeDao().getTradesByUserId(currentUserId)

        val totalTrades = trades.size
        val openTrades = trades.count { it.result.equals("Open", ignoreCase = true) }
        val closedTrades = trades.filter { !it.result.equals("Open", ignoreCase = true) }
        val wins = closedTrades.count { it.result.equals("Win", ignoreCase = true) }
        val losses = closedTrades.count { it.result.equals("Loss", ignoreCase = true) }

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

        val performanceBalance = wins - losses

        monthTitleText.text = getCurrentMonthLabel()
        monthSubtitleText.text = if (totalTrades == 0) {
            "No trades saved yet."
        } else {
            "$totalTrades total • $openTrades open • ${closedTrades.size} closed"
        }

        totalTradesValueText.text = totalTrades.toString()
        winRateValueText.text = "$winRate%"

        performanceBalanceValueText.text = when {
            performanceBalance > 0 -> "+$performanceBalance"
            else -> performanceBalance.toString()
        }

        performanceBalanceLabelText.text = "Win/Loss balance"

        avgConfluenceValueText.text = "$avgConfluence%"
        bestAssetValueText.text = findBestAsset(trades)
        bestSessionValueText.text = findBestSession(trades)
        sourceBreakdownValueText.text = buildSourceBreakdown(trades)
        sessionBreakdownValueText.text = buildSessionBreakdownText(trades)

        val calendarItems = buildCalendarItemsForCurrentMonth(trades)
        calendarAdapter.replaceItems(calendarItems)
    }

    /**
     * Costruisce il calendario del mese corrente.
     * Ogni cella rappresenta un giorno del mese.
     */
    private fun buildCalendarItemsForCurrentMonth(trades: List<Trade>): List<CalendarDayUiModel> {
        val calendarItems = mutableListOf<CalendarDayUiModel>()

        val currentCalendar = Calendar.getInstance()
        val year = currentCalendar.get(Calendar.YEAR)
        val month = currentCalendar.get(Calendar.MONTH)

        val firstDayCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val daysInMonth = firstDayCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        // Convertiamo la settimana in formato Monday-first:
        // Calendar.SUNDAY = 1 ... SATURDAY = 7
        val dayOfWeek = firstDayCalendar.get(Calendar.DAY_OF_WEEK)
        val mondayFirstOffset = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }

        // Celle vuote iniziali
        repeat(mondayFirstOffset) {
            calendarItems.add(
                CalendarDayUiModel(
                    dayNumber = "",
                    dayInfo = "",
                    status = CalendarDayUiModel.Status.EMPTY
                )
            )
        }

        // Raggruppiamo i trade del mese corrente per giorno
        val monthTradesByDay = trades
            .filter { isTradeInCurrentMonth(it, year, month) }
            .groupBy { extractDayOfMonth(it) }

        for (day in 1..daysInMonth) {
            val dayTrades = monthTradesByDay[day].orEmpty()
            val status = determineDayStatus(dayTrades)

            val infoText = when {
                dayTrades.isEmpty() -> ""
                else -> "${dayTrades.size}T"
            }

            calendarItems.add(
                CalendarDayUiModel(
                    dayNumber = day.toString(),
                    dayInfo = infoText,
                    status = status
                )
            )
        }

        return calendarItems
    }

    /**
     * Determina lo stato visivo del giorno:
     * - WIN: solo win
     * - LOSS: solo loss
     * - MIXED: win e loss insieme
     * - OPEN: solo open
     * - NONE: nessun trade
     */
    private fun determineDayStatus(dayTrades: List<Trade>): CalendarDayUiModel.Status {
        if (dayTrades.isEmpty()) return CalendarDayUiModel.Status.NONE

        val hasWin = dayTrades.any { it.result.equals("Win", ignoreCase = true) }
        val hasLoss = dayTrades.any { it.result.equals("Loss", ignoreCase = true) }
        val hasOnlyOpen = dayTrades.all { it.result.equals("Open", ignoreCase = true) }

        return when {
            hasOnlyOpen -> CalendarDayUiModel.Status.OPEN
            hasWin && hasLoss -> CalendarDayUiModel.Status.MIXED
            hasWin -> CalendarDayUiModel.Status.WIN
            hasLoss -> CalendarDayUiModel.Status.LOSS
            else -> CalendarDayUiModel.Status.NONE
        }
    }

    private fun isTradeInCurrentMonth(trade: Trade, targetYear: Int, targetMonth: Int): Boolean {
        val calendar = parseTradeDateToCalendar(trade.date) ?: return false
        return calendar.get(Calendar.YEAR) == targetYear &&
                calendar.get(Calendar.MONTH) == targetMonth
    }

    private fun extractDayOfMonth(trade: Trade): Int {
        val calendar = parseTradeDateToCalendar(trade.date) ?: return -1
        return calendar.get(Calendar.DAY_OF_MONTH)
    }

    /**
     * Supporta i formati più probabili del progetto:
     * - yyyy-MM-dd HH:mm
     * - yyyy-MM-dd
     */
    private fun parseTradeDateToCalendar(dateText: String): Calendar? {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        )

        for (format in formats) {
            try {
                val date = format.parse(dateText)
                if (date != null) {
                    return Calendar.getInstance().apply { time = date }
                }
            } catch (_: Exception) {
            }
        }

        return null
    }

    private fun getCurrentMonthLabel(): String {
        val formatter = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        return formatter.format(Calendar.getInstance().time).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        }
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
        return "Manual $manual • JSON $json"
    }

    private fun buildSessionBreakdownText(trades: List<Trade>): String {
        if (trades.isEmpty()) return "No data"

        val grouped = trades.groupBy { it.session }
        return grouped.entries.joinToString(" • ") { entry ->
            "${entry.key} ${entry.value.size}"
        }
    }
}