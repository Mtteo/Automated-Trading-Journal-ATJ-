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

        val monthlyPnl = trades.sumOf { it.pnlAmount }

        monthTitleText.text = getCurrentMonthLabel()
        monthSubtitleText.text = if (totalTrades == 0) {
            "No trades saved yet."
        } else {
            "$totalTrades trades tracked"
        }

        totalTradesValueText.text = totalTrades.toString()
        winRateValueText.text = "$winRate%"
        performanceBalanceValueText.text = String.format(Locale.getDefault(), "%.2f", monthlyPnl)
        performanceBalanceLabelText.text = "Net PnL"

        avgConfluenceValueText.text = "$avgConfluence%"
        bestAssetValueText.text = findBestAssetByPnl(trades)
        bestSessionValueText.text = findBestSessionByPnl(trades)
        sourceBreakdownValueText.text = buildSourceBreakdown(trades)
        sessionBreakdownValueText.text = buildSessionBreakdownText(trades)

        val calendarItems = buildCalendarItemsForCurrentMonth(trades)
        calendarAdapter.replaceItems(calendarItems)
    }

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

        repeat(mondayFirstOffset) {
            calendarItems.add(
                CalendarDayUiModel("", "", CalendarDayUiModel.Status.EMPTY)
            )
        }

        val monthTradesByDay = trades
            .filter { isTradeInCurrentMonth(it, year, month) }
            .groupBy { extractDayOfMonth(it) }

        for (day in 1..daysInMonth) {
            val dayTrades = monthTradesByDay[day].orEmpty()
            val status = determineDayStatus(dayTrades)
            val infoText = if (dayTrades.isEmpty()) "" else String.format(Locale.getDefault(), "%.0f", dayTrades.sumOf { it.pnlAmount })

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

    private fun determineDayStatus(dayTrades: List<Trade>): CalendarDayUiModel.Status {
        if (dayTrades.isEmpty()) return CalendarDayUiModel.Status.NONE

        val totalPnl = dayTrades.sumOf { it.pnlAmount }
        val hasOnlyOpen = dayTrades.all { it.result.equals("Open", ignoreCase = true) }

        return when {
            hasOnlyOpen -> CalendarDayUiModel.Status.OPEN
            totalPnl > 0 -> CalendarDayUiModel.Status.WIN
            totalPnl < 0 -> CalendarDayUiModel.Status.LOSS
            else -> CalendarDayUiModel.Status.MIXED
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

    private fun findBestAssetByPnl(trades: List<Trade>): String {
        if (trades.isEmpty()) return "No data"
        val grouped = trades.groupBy { it.asset }
        val best = grouped.maxByOrNull { entry -> entry.value.sumOf { it.pnlAmount } } ?: return "No data"
        val pnl = best.value.sumOf { it.pnlAmount }
        return "${best.key} (${String.format(Locale.getDefault(), "%.2f", pnl)})"
    }

    private fun findBestSessionByPnl(trades: List<Trade>): String {
        if (trades.isEmpty()) return "No data"
        val grouped = trades.groupBy { it.session }
        val best = grouped.maxByOrNull { entry -> entry.value.sumOf { it.pnlAmount } } ?: return "No data"
        val pnl = best.value.sumOf { it.pnlAmount }
        return "${best.key} (${String.format(Locale.getDefault(), "%.2f", pnl)})"
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
            "${entry.key} ${String.format(Locale.getDefault(), "%.2f", entry.value.sumOf { it.pnlAmount })}"
        }
    }
}