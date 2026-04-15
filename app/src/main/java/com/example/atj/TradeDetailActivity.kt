package com.example.atj

import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade
import java.io.File

class TradeDetailActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var currentTrade: Trade? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade_detail)

        val assetTextView: TextView = findViewById(R.id.detailAssetText)
        val typeTextView: TextView = findViewById(R.id.detailTypeText)
        val dateTextView: TextView = findViewById(R.id.detailDateText)
        val sessionTextView: TextView = findViewById(R.id.detailSessionText)
        val resultTextView: TextView = findViewById(R.id.detailResultText)
        val sourceTextView: TextView = findViewById(R.id.detailSourceText)
        val notesTextView: TextView = findViewById(R.id.detailNotesText)
        val strategyNameTextView: TextView = findViewById(R.id.detailStrategyNameText)
        val confluenceScoreTextView: TextView = findViewById(R.id.detailConfluenceScoreText)
        val checkedConfluencesTextView: TextView = findViewById(R.id.detailCheckedConfluencesText)
        val locationTextView: TextView = findViewById(R.id.detailLocationText)
        val deleteTradeButton: Button = findViewById(R.id.deleteTradeButton)

        val tradeImageView: ImageView = findViewById(R.id.detailTradeImage)
        val noImageTextView: TextView = findViewById(R.id.detailNoImageText)

        database = AppDatabase.getDatabase(this)

        val tradeId = intent.getLongExtra("trade_id", -1)

        if (tradeId != -1L) {
            currentTrade = database.tradeDao().getTradeById(tradeId)

            currentTrade?.let { trade ->
                assetTextView.text = "Asset: ${trade.asset}"
                typeTextView.text = "Type: ${trade.type}"
                dateTextView.text = "Date: ${trade.date}"
                sessionTextView.text = "Session: ${trade.session}"
                resultTextView.text = "Result: ${trade.result}"
                sourceTextView.text = "Source: ${trade.source}"
                notesTextView.text = "Notes: ${trade.notes}"
                strategyNameTextView.text = "Strategy: ${trade.strategyName.ifBlank { "Not set" }}"
                confluenceScoreTextView.text = "Confluence Score: ${trade.confluenceScore}%"
                checkedConfluencesTextView.text =
                    "Checked Confluences: ${trade.checkedConfluences.ifBlank { "None" }}"
                locationTextView.text = "Location: ${trade.locationText}"

                if (!trade.imagePath.isNullOrBlank()) {
                    val imageFile = File(trade.imagePath)
                    if (imageFile.exists()) {
                        val bitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                        tradeImageView.setImageBitmap(bitmap)
                        tradeImageView.visibility = View.VISIBLE
                        noImageTextView.visibility = View.GONE
                    } else {
                        tradeImageView.visibility = View.GONE
                        noImageTextView.visibility = View.VISIBLE
                    }
                } else {
                    tradeImageView.visibility = View.GONE
                    noImageTextView.visibility = View.VISIBLE
                }
            }
        }

        deleteTradeButton.setOnClickListener {
            currentTrade?.let { trade ->
                database.tradeDao().deleteTrade(trade)
            }
            finish()
        }
    }
}