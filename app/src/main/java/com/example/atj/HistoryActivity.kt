package com.example.atj

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.ui.HistoryTradeAdapter
import com.example.atj.utils.SessionManager
import com.example.atj.utils.TradeStateHelper

/*
 * Activity dello storico trade.
 * Mostra i trade divisi per stato e permette dettaglio o modifica.
 */
class HistoryActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var currentUserId: Long = -1L
    private var tradeBeingEdited: Trade? = null

    private lateinit var winningSectionTitle: TextView
    private lateinit var losingSectionTitle: TextView
    private lateinit var openSectionTitle: TextView

    private lateinit var winningRecyclerView: RecyclerView
    private lateinit var losingRecyclerView: RecyclerView
    private lateinit var openRecyclerView: RecyclerView

    private lateinit var winningEmptyText: TextView
    private lateinit var losingEmptyText: TextView
    private lateinit var openEmptyText: TextView

    private lateinit var winningAdapter: HistoryTradeAdapter
    private lateinit var losingAdapter: HistoryTradeAdapter
    private lateinit var openAdapter: HistoryTradeAdapter

    /*
     * Activity Result API per modificare un trade.
     * AddTradeActivity restituisce i dati aggiornati tramite Intent.
     */
    private val editTradeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val oldTrade = tradeBeingEdited ?: return@registerForActivityResult

                val updatedTrade = oldTrade.copy(
                    asset = data.getStringExtra("asset") ?: oldTrade.asset,
                    type = data.getStringExtra("type") ?: oldTrade.type,
                    direction = data.getStringExtra("direction") ?: oldTrade.direction,
                    result = data.getStringExtra("result") ?: oldTrade.result,
                    date = data.getStringExtra("date") ?: oldTrade.date,
                    session = data.getStringExtra("session") ?: oldTrade.session,
                    locationText = data.getStringExtra("locationText") ?: oldTrade.locationText,

                    entryPrice = data.getDoubleExtra("entryPrice", oldTrade.entryPrice),
                    exitPrice = data.getDoubleExtra("exitPrice", oldTrade.exitPrice),
                    stopLoss = data.getDoubleExtra("stopLoss", oldTrade.stopLoss),
                    takeProfit = data.getDoubleExtra("takeProfit", oldTrade.takeProfit),
                    rr = data.getDoubleExtra("rr", oldTrade.rr),
                    positionValue = data.getDoubleExtra("positionValue", oldTrade.positionValue),
                    positionPercentOfAccount = data.getDoubleExtra(
                        "positionPercentOfAccount",
                        oldTrade.positionPercentOfAccount
                    ),
                    accountValue = data.getDoubleExtra("accountValue", oldTrade.accountValue),
                    pnlAmount = data.getDoubleExtra("pnlAmount", oldTrade.pnlAmount),
                    pnlPercent = data.getDoubleExtra("pnlPercent", oldTrade.pnlPercent),

                    notes = data.getStringExtra("notes") ?: oldTrade.notes,
                    imagePath = data.getStringExtra("imagePath") ?: oldTrade.imagePath,
                    strategyName = data.getStringExtra("strategyName") ?: oldTrade.strategyName,
                    checkedConfluences = data.getStringExtra("checkedConfluences")
                        ?: oldTrade.checkedConfluences,
                    confluenceScore = data.getIntExtra(
                        "confluenceScore",
                        oldTrade.confluenceScore
                    )
                )

                val normalizedTrade = TradeStateHelper.normalizeTrade(updatedTrade)

                database.tradeDao().updateTrade(normalizedTrade)
                tradeBeingEdited = null
                loadTrades()

                Toast.makeText(this, "Trade updated", Toast.LENGTH_SHORT).show()
            }
        }

    /*
     * Controlla la sessione, carica layout e inizializza le liste.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionManager.isLoggedIn(this)) {
            finish()
            return
        }

        setContentView(R.layout.activity_history)

        database = AppDatabase.getDatabase(this)
        currentUserId = SessionManager.getLoggedInUserId(this)

        bindViews()
        setupLists()
        loadTrades()
    }

    /*
     * Aggiorna lo storico quando la Activity torna in primo piano.
     */
    override fun onResume() {
        super.onResume()
        loadTrades()
    }

    /*
     * Collega le View definite nel layout XML.
     */
    private fun bindViews() {
        winningSectionTitle = findViewById(R.id.winningSectionTitle)
        losingSectionTitle = findViewById(R.id.losingSectionTitle)
        openSectionTitle = findViewById(R.id.openSectionTitle)

        winningRecyclerView = findViewById(R.id.winningRecyclerView)
        losingRecyclerView = findViewById(R.id.losingRecyclerView)
        openRecyclerView = findViewById(R.id.openRecyclerView)

        winningEmptyText = findViewById(R.id.winningEmptyText)
        losingEmptyText = findViewById(R.id.losingEmptyText)
        openEmptyText = findViewById(R.id.openEmptyText)
    }

    /*
     * Configura tre RecyclerView: vincenti, perdenti e aperti/BE.
     */
    private fun setupLists() {
        winningAdapter = HistoryTradeAdapter(
            trades = mutableListOf(),
            onItemClick = { trade -> openTradeDetail(trade) },
            onItemLongClick = { trade -> openEditTrade(trade) }
        )

        losingAdapter = HistoryTradeAdapter(
            trades = mutableListOf(),
            onItemClick = { trade -> openTradeDetail(trade) },
            onItemLongClick = { trade -> openEditTrade(trade) }
        )

        openAdapter = HistoryTradeAdapter(
            trades = mutableListOf(),
            onItemClick = { trade -> openTradeDetail(trade) },
            onItemLongClick = { trade -> openEditTrade(trade) }
        )

        winningRecyclerView.layoutManager = LinearLayoutManager(this)
        losingRecyclerView.layoutManager = LinearLayoutManager(this)
        openRecyclerView.layoutManager = LinearLayoutManager(this)

        winningRecyclerView.adapter = winningAdapter
        losingRecyclerView.adapter = losingAdapter
        openRecyclerView.adapter = openAdapter
    }

    /*
     * Legge i trade dal database e li divide in sezioni.
     */
    private fun loadTrades() {
        val trades = database.tradeDao()
            .getTradesByUserId(currentUserId)
            .map { TradeStateHelper.normalizeTrade(it) }

        val winningTrades = trades.filter {
            TradeStateHelper.isWin(it)
        }

        val losingTrades = trades.filter {
            TradeStateHelper.isLoss(it)
        }

        val openOrOtherTrades = trades.filter {
            TradeStateHelper.isOpen(it) || TradeStateHelper.isBreakEven(it)
        }

        winningSectionTitle.text = "Winning Trades (${winningTrades.size})"
        losingSectionTitle.text = "Losing Trades (${losingTrades.size})"
        openSectionTitle.text = "Open / BE / Other Trades (${openOrOtherTrades.size})"

        winningAdapter.replaceTrades(winningTrades)
        losingAdapter.replaceTrades(losingTrades)
        openAdapter.replaceTrades(openOrOtherTrades)

        updateSectionVisibility(
            trades = winningTrades,
            recyclerView = winningRecyclerView,
            emptyText = winningEmptyText
        )

        updateSectionVisibility(
            trades = losingTrades,
            recyclerView = losingRecyclerView,
            emptyText = losingEmptyText
        )

        updateSectionVisibility(
            trades = openOrOtherTrades,
            recyclerView = openRecyclerView,
            emptyText = openEmptyText
        )
    }

    /*
     * Mostra il testo vuoto oppure la lista in base ai dati disponibili.
     */
    private fun updateSectionVisibility(
        trades: List<Trade>,
        recyclerView: RecyclerView,
        emptyText: TextView
    ) {
        val isEmpty = trades.isEmpty()

        emptyText.visibility = if (isEmpty) {
            View.VISIBLE
        } else {
            View.GONE
        }

        recyclerView.visibility = if (isEmpty) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    /*
     * Apre il dettaglio del trade tramite Intent esplicito.
     */
    private fun openTradeDetail(trade: Trade) {
        val intent = Intent(this, TradeDetailActivity::class.java).apply {
            putExtra("trade_id", trade.id)
        }

        startActivity(intent)
    }

    /*
     * Apre AddTradeActivity in modalità modifica.
     * I dati del trade vengono passati tramite extras dell'Intent.
     */
    private fun openEditTrade(trade: Trade) {
        tradeBeingEdited = trade

        val intent = Intent(this, AddTradeActivity::class.java).apply {
            putExtra("edit_mode", true)

            putExtra("asset", trade.asset)
            putExtra("type", trade.type)
            putExtra("direction", trade.direction)
            putExtra("date", trade.date)
            putExtra("session", trade.session)
            putExtra("result", TradeStateHelper.displayState(trade))
            putExtra("notes", trade.notes)
            putExtra("imagePath", trade.imagePath)
            putExtra("strategyName", trade.strategyName)
            putExtra("checkedConfluences", trade.checkedConfluences)
            putExtra("confluenceScore", trade.confluenceScore)
            putExtra("locationText", trade.locationText)

            putExtra("entryPrice", trade.entryPrice)
            putExtra("exitPrice", trade.exitPrice)
            putExtra("stopLoss", trade.stopLoss)
            putExtra("takeProfit", trade.takeProfit)
            putExtra("rr", trade.rr)
            putExtra("positionValue", trade.positionValue)
            putExtra("positionPercentOfAccount", trade.positionPercentOfAccount)
            putExtra("accountValue", trade.accountValue)
            putExtra("pnlAmount", trade.pnlAmount)
            putExtra("pnlPercent", trade.pnlPercent)
        }

        editTradeLauncher.launch(intent)
    }
}