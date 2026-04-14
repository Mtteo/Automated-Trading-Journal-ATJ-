package com.example.atj

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.data.AppDatabase
import com.example.atj.model.Trade

// Activity che mostra il dettaglio completo di un singolo trade.
class TradeDetailActivity : AppCompatActivity() {

    private lateinit var database: AppDatabase
    private var currentTrade: Trade? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trade_detail)

        // Collego tutte le view.
        val assetTextView: TextView = findViewById(R.id.detailAssetText)
        val typeTextView: TextView = findViewById(R.id.detailTypeText)
        val dateTextView: TextView = findViewById(R.id.detailDateText)
        val sessionTextView: TextView = findViewById(R.id.detailSessionText)
        val resultTextView: TextView = findViewById(R.id.detailResultText)
        val sourceTextView: TextView = findViewById(R.id.detailSourceText)
        val notesTextView: TextView = findViewById(R.id.detailNotesText)
        val deleteTradeButton: Button = findViewById(R.id.deleteTradeButton)

        database = AppDatabase.getDatabase(this)

        // Leggo l'id del trade passato dalla MainActivity.
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
            }
        }

        // Elimina il trade dal database e chiude la schermata.
        deleteTradeButton.setOnClickListener {
            currentTrade?.let { trade ->
                database.tradeDao().deleteTrade(trade)
            }
            finish()
        }
    }
}