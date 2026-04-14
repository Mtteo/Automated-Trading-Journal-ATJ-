package com.example.atj

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity

// Activity dedicata all'inserimento manuale di un nuovo trade.
class AddTradeActivity : AppCompatActivity() {

    private lateinit var assetEditText: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var dateEditText: EditText
    private lateinit var sessionInput: EditText
    private lateinit var resultInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var saveTradeButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trade)

        // Collego le view del layout al codice Kotlin.
        assetEditText = findViewById(R.id.assetEditText)
        typeSpinner = findViewById(R.id.typeSpinner)
        dateEditText = findViewById(R.id.dateEditText)
        sessionInput = findViewById(R.id.sessionInput)
        resultInput = findViewById(R.id.resultInput)
        notesInput = findViewById(R.id.notesInput)
        saveTradeButton = findViewById(R.id.saveTradeButton)

        // Popolo lo spinner con le due opzioni base.
        val types = arrayOf("Buy", "Sell")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, types)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = adapter

        // Alla pressione del bottone raccogliamo i dati
        // e li rimandiamo alla MainActivity.
        saveTradeButton.setOnClickListener {
            val asset = assetEditText.text.toString().trim()
            val type = typeSpinner.selectedItem.toString()
            val date = dateEditText.text.toString().trim()
            val session = sessionInput.text.toString().trim()
            val result = resultInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            // Salviamo solo se almeno asset e date sono compilati.
            if (asset.isNotEmpty() && date.isNotEmpty()) {
                val resultIntent = Intent().apply {
                    putExtra("asset", asset)
                    putExtra("type", type)
                    putExtra("date", date)
                    putExtra("session", session)
                    putExtra("result", result)
                    putExtra("notes", notes)
                }

                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }
}