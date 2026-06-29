package com.example.atj

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import com.example.atj.utils.ImageDisplayHelper
import com.example.atj.utils.NotificationHelper
import com.example.atj.utils.SessionManager

class TradeDetailActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var currentTrade: Trade? = null

    private lateinit var assetTextView: TextView
    private lateinit var typeTextView: TextView
    private lateinit var dateTextView: TextView
    private lateinit var sessionTextView: TextView
    private lateinit var resultTextView: TextView
    private lateinit var sourceTextView: TextView
    private lateinit var notesTextView: TextView
    private lateinit var strategyNameTextView: TextView
    private lateinit var confluenceScoreTextView: TextView
    private lateinit var checkedConfluencesTextView: TextView
    private lateinit var locationTextView: TextView
    private lateinit var deleteTradeButton: Button
    private lateinit var tradeImageView: ImageView
    private lateinit var noImageTextView: TextView

    private val editTradeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data ?: return@registerForActivityResult
                val oldTrade = currentTrade ?: return@registerForActivityResult

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
                    imagePath = data.getStringExtra("imagePath"),
                    strategyName = data.getStringExtra("strategyName") ?: oldTrade.strategyName,
                    checkedConfluences = data.getStringExtra("checkedConfluences")
                        ?: oldTrade.checkedConfluences,
                    confluenceScore = data.getIntExtra(
                        "confluenceScore",
                        oldTrade.confluenceScore
                    )
                )

                database.tradeDao().updateTrade(updatedTrade)
                currentTrade = updatedTrade
                renderTrade(updatedTrade)

                Toast.makeText(this, "Trade updated", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!SessionManager.isLoggedIn(this)) {
            finish()
            return
        }

        setContentView(R.layout.activity_trade_detail)

        database = AppDatabase.getDatabase(this)

        bindViews()
        loadTrade()
        setupActions()
    }

    private fun bindViews() {
        assetTextView = findViewById(R.id.detailAssetText)
        typeTextView = findViewById(R.id.detailTypeText)
        dateTextView = findViewById(R.id.detailDateText)
        sessionTextView = findViewById(R.id.detailSessionText)
        resultTextView = findViewById(R.id.detailResultText)
        sourceTextView = findViewById(R.id.detailSourceText)
        notesTextView = findViewById(R.id.detailNotesText)
        strategyNameTextView = findViewById(R.id.detailStrategyNameText)
        confluenceScoreTextView = findViewById(R.id.detailConfluenceScoreText)
        checkedConfluencesTextView = findViewById(R.id.detailCheckedConfluencesText)
        locationTextView = findViewById(R.id.detailLocationText)
        deleteTradeButton = findViewById(R.id.deleteTradeButton)

        tradeImageView = findViewById(R.id.detailTradeImage)
        noImageTextView = findViewById(R.id.detailNoImageText)
    }

    private fun loadTrade() {
        val tradeId = intent.getLongExtra("trade_id", -1)

        if (tradeId == -1L) {
            finish()
            return
        }

        currentTrade = database.tradeDao().getTradeById(tradeId)

        if (currentTrade == null) {
            finish()
            return
        }

        renderTrade(currentTrade!!)
    }

    private fun setupActions() {
        /*
         * Modifica senza cambiare XML:
         * tieni premuto sul nome dell'asset per aprire la schermata edit.
         */
        assetTextView.setOnLongClickListener {
            openEditTrade()
            true
        }

        notesTextView.setOnLongClickListener {
            openEditTrade()
            true
        }

        deleteTradeButton.setOnClickListener {
            currentTrade?.let { trade ->
                database.tradeDao().deleteTrade(trade)

                NotificationHelper.showTradeDeletedNotification(
                    context = this,
                    asset = trade.asset,
                    type = trade.type
                )
            }
            finish()
        }
    }

    private fun renderTrade(trade: Trade) {
        assetTextView.text = trade.asset
        typeTextView.text = "${trade.type} • ${trade.direction.ifBlank { "N/A" }}"
        dateTextView.text = "${trade.date}\nEntry ${trade.entryPrice} • Exit ${trade.exitPrice}"
        sessionTextView.text = trade.session.ifBlank { "Unknown" }
        resultTextView.text =
            "${trade.result.ifBlank { "Open" }} • RR ${trade.rr} • PnL ${trade.pnlAmount} (${trade.pnlPercent}%)"

        sourceTextView.text =
            "${trade.source.replaceFirstChar { it.uppercase() }} • Account ${trade.accountValue}"

        notesTextView.text = trade.notes.ifBlank { "No notes added." }
        strategyNameTextView.text = trade.strategyName.ifBlank { "Not set" }
        confluenceScoreTextView.text = "${trade.confluenceScore}%"
        checkedConfluencesTextView.text = trade.checkedConfluences.ifBlank { "None" }
        locationTextView.text = trade.locationText.ifBlank { "Unknown" }

        val correctedBitmap = ImageDisplayHelper.loadCorrectlyOrientedBitmap(trade.imagePath)

        if (correctedBitmap != null) {
            tradeImageView.setImageBitmap(correctedBitmap)
            tradeImageView.visibility = View.VISIBLE
            noImageTextView.visibility = View.GONE
        } else {
            tradeImageView.setImageDrawable(null)
            tradeImageView.visibility = View.GONE
            noImageTextView.visibility = View.VISIBLE
        }
    }

    private fun openEditTrade() {
        val trade = currentTrade ?: return

        val intent = Intent(this, AddTradeActivity::class.java).apply {
            putExtra("edit_mode", true)

            putExtra("asset", trade.asset)
            putExtra("type", trade.type)
            putExtra("direction", trade.direction)
            putExtra("date", trade.date)
            putExtra("session", trade.session)
            putExtra("result", trade.result)
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