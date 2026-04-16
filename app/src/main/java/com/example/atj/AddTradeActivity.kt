package com.example.atj

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.utils.ImageDisplayHelper
import com.example.atj.utils.ImageStorageHelper
import com.example.atj.utils.LocationHelper
import com.example.atj.utils.SessionHelper
import com.example.atj.utils.StrategyManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddTradeActivity : AppCompatActivity() {

    private lateinit var assetEditText: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var dateEditText: EditText
    private lateinit var sessionInput: EditText
    private lateinit var resultInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var saveTradeButton: Button

    private lateinit var pickImageButton: Button
    private lateinit var takePhotoButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var voiceReflectionButton: Button

    private lateinit var activeStrategyText: TextView
    private lateinit var checklistContainer: LinearLayout
    private lateinit var noChecklistText: TextView

    // Campo visuale per il luogo auto-rilevato
    private lateinit var locationInput: EditText

    private var selectedImagePath: String? = null
    private val checklistCheckBoxes = mutableListOf<CheckBox>()

    /**
     * Selezione immagine dalla galleria.
     * Dopo il salvataggio interno, mostriamo l'immagine con orientamento corretto.
     */
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val savedPath = ImageStorageHelper.copyUriToInternalStorage(this, uri)
                selectedImagePath = savedPath

                showPreviewImage(savedPath)
            }
        }

    /**
     * Scatto rapido foto da camera.
     * Il bitmap ottenuto viene salvato internamente e poi ricaricato
     * usando la utility di orientamento.
     */
    private val takePicturePreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val savedPath = ImageStorageHelper.saveBitmapToInternalStorage(this, bitmap)
                selectedImagePath = savedPath

                showPreviewImage(savedPath)
            }
        }

    /**
     * Launcher per il riconoscimento vocale.
     */
    private val speechToTextLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenResults = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

                if (!spokenResults.isNullOrEmpty()) {
                    val recognizedText = spokenResults[0].trim()

                    if (recognizedText.isNotBlank()) {
                        appendVoiceReflectionToNotes(recognizedText)
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trade)

        assetEditText = findViewById(R.id.assetEditText)
        typeSpinner = findViewById(R.id.typeSpinner)
        dateEditText = findViewById(R.id.dateEditText)
        sessionInput = findViewById(R.id.sessionInput)
        resultInput = findViewById(R.id.resultInput)
        notesInput = findViewById(R.id.notesInput)
        saveTradeButton = findViewById(R.id.saveTradeButton)

        pickImageButton = findViewById(R.id.pickImageButton)
        takePhotoButton = findViewById(R.id.takePhotoButton)
        imagePreview = findViewById(R.id.imagePreview)
        voiceReflectionButton = findViewById(R.id.voiceReflectionButton)

        activeStrategyText = findViewById(R.id.activeStrategyText)
        checklistContainer = findViewById(R.id.checklistContainer)
        noChecklistText = findViewById(R.id.noChecklistText)
        locationInput = findViewById(R.id.locationInput)

        val tradeTypes = listOf("Buy", "Sell")
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tradeTypes
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = spinnerAdapter

        prefillAutomaticFields()
        renderStrategyChecklist()

        pickImageButton.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        takePhotoButton.setOnClickListener {
            takePicturePreviewLauncher.launch(null)
        }

        voiceReflectionButton.setOnClickListener {
            startVoiceRecognition()
        }

        saveTradeButton.setOnClickListener {
            saveTrade()
        }
    }

    /**
     * Prefill automatico dei campi data, sessione e location.
     */
    private fun prefillAutomaticFields() {
        val now = System.currentTimeMillis()

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dateEditText.setText(formatter.format(Date(now)))

        sessionInput.setText(SessionHelper.getSessionFromTimestamp(now))
        locationInput.setText(LocationHelper.getCurrentLocationText(this))
    }

    /**
     * Disegna la checklist della strategia attiva.
     */
    private fun renderStrategyChecklist() {
        val strategy = StrategyManager.getStrategy(this)

        activeStrategyText.text = if (strategy.name.isBlank()) {
            "Active Strategy: Not defined"
        } else {
            "Active Strategy: ${strategy.name}"
        }

        checklistContainer.removeAllViews()
        checklistCheckBoxes.clear()

        if (strategy.checklistItems.isEmpty()) {
            noChecklistText.visibility = View.VISIBLE
            return
        }

        noChecklistText.visibility = View.GONE

        strategy.checklistItems.forEach { item ->
            val checkBox = CheckBox(this)
            checkBox.text = item
            checkBox.textSize = 16f

            checklistContainer.addView(checkBox)
            checklistCheckBoxes.add(checkBox)
        }
    }

    /**
     * Mostra la preview dell'immagine con orientamento corretto.
     */
    private fun showPreviewImage(imagePath: String?) {
        val correctedBitmap = ImageDisplayHelper.loadCorrectlyOrientedBitmap(imagePath)

        if (correctedBitmap != null) {
            imagePreview.setImageBitmap(correctedBitmap)
            imagePreview.visibility = ImageView.VISIBLE
        } else {
            imagePreview.setImageDrawable(null)
            imagePreview.visibility = ImageView.GONE
        }
    }

    /**
     * Salva i dati del trade e li restituisce alla MainActivity.
     */
    private fun saveTrade() {
        val asset = assetEditText.text.toString().trim()
        val type = typeSpinner.selectedItem.toString()
        val date = dateEditText.text.toString().trim()
        val session = sessionInput.text.toString().trim()
        val result = resultInput.text.toString().trim()
        val notes = notesInput.text.toString().trim()
        val locationText = locationInput.text.toString().trim()

        val strategy = StrategyManager.getStrategy(this)
        val checkedItems = checklistCheckBoxes
            .filter { it.isChecked }
            .map { it.text.toString() }

        val totalItems = checklistCheckBoxes.size
        val confluenceScore = if (totalItems > 0) {
            (checkedItems.size * 100) / totalItems
        } else {
            0
        }

        val resultIntent = Intent().apply {
            putExtra("asset", asset)
            putExtra("type", type)
            putExtra("date", date)
            putExtra("session", session)
            putExtra("result", result)
            putExtra("notes", notes)
            putExtra("imagePath", selectedImagePath)
            putExtra("strategyName", strategy.name)
            putExtra("checkedConfluences", checkedItems.joinToString(", "))
            putExtra("confluenceScore", confluenceScore)
            putExtra("locationText", locationText)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Avvia il voice-to-text.
     */
    private fun startVoiceRecognition() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your voice reflection")
            }

            speechToTextLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Speech recognition not available on this device",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Appende il testo riconosciuto dentro il campo note.
     */
    private fun appendVoiceReflectionToNotes(recognizedText: String) {
        val currentNotes = notesInput.text.toString().trim()

        val updatedNotes = if (currentNotes.isBlank()) {
            "Voice Reflection: $recognizedText"
        } else {
            "$currentNotes\n\nVoice Reflection: $recognizedText"
        }

        notesInput.setText(updatedNotes)
        notesInput.setSelection(notesInput.text.length)
    }
}