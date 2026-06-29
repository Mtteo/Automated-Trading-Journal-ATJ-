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
    private lateinit var directionSpinner: Spinner
    private lateinit var dateEditText: EditText
    private lateinit var sessionInput: EditText
    private lateinit var resultInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var saveTradeButton: Button

    private lateinit var entryPriceInput: EditText
    private lateinit var exitPriceInput: EditText
    private lateinit var stopLossInput: EditText
    private lateinit var takeProfitInput: EditText
    private lateinit var rrInput: EditText
    private lateinit var positionValueInput: EditText
    private lateinit var positionPercentInput: EditText
    private lateinit var accountValueInput: EditText
    private lateinit var pnlAmountInput: EditText
    private lateinit var pnlPercentInput: EditText

    private lateinit var pickImageButton: Button
    private lateinit var takePhotoButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var voiceReflectionButton: Button

    private lateinit var activeStrategyText: TextView
    private lateinit var checklistContainer: LinearLayout
    private lateinit var noChecklistText: TextView
    private lateinit var locationInput: EditText

    private var selectedImagePath: String? = null
    private var isEditMode: Boolean = false
    private val checklistCheckBoxes = mutableListOf<CheckBox>()

    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val savedPath = ImageStorageHelper.copyUriToInternalStorage(this, uri)
                selectedImagePath = savedPath
                showPreviewImage(savedPath)
            }
        }

    private val takePicturePreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val savedPath = ImageStorageHelper.saveBitmapToInternalStorage(this, bitmap)
                selectedImagePath = savedPath
                showPreviewImage(savedPath)
            }
        }

    private val speechToTextLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val spokenResults =
                    result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)

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

        isEditMode = intent.getBooleanExtra("edit_mode", false)

        bindViews()
        setupSpinners()
        prefillAutomaticFields()
        renderStrategyChecklist()

        if (isEditMode) {
            populateFieldsFromIntent()
            saveTradeButton.text = "Update Trade"
        } else {
            saveTradeButton.text = "Save Trade"
        }

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

    private fun bindViews() {
        assetEditText = findViewById(R.id.assetEditText)
        typeSpinner = findViewById(R.id.typeSpinner)
        directionSpinner = findViewById(R.id.directionSpinner)
        dateEditText = findViewById(R.id.dateEditText)
        sessionInput = findViewById(R.id.sessionInput)
        resultInput = findViewById(R.id.resultInput)
        notesInput = findViewById(R.id.notesInput)
        saveTradeButton = findViewById(R.id.saveTradeButton)

        entryPriceInput = findViewById(R.id.entryPriceInput)
        exitPriceInput = findViewById(R.id.exitPriceInput)
        stopLossInput = findViewById(R.id.stopLossInput)
        takeProfitInput = findViewById(R.id.takeProfitInput)
        rrInput = findViewById(R.id.rrInput)
        positionValueInput = findViewById(R.id.positionValueInput)
        positionPercentInput = findViewById(R.id.positionPercentInput)
        accountValueInput = findViewById(R.id.accountValueInput)
        pnlAmountInput = findViewById(R.id.pnlAmountInput)
        pnlPercentInput = findViewById(R.id.pnlPercentInput)

        pickImageButton = findViewById(R.id.pickImageButton)
        takePhotoButton = findViewById(R.id.takePhotoButton)
        imagePreview = findViewById(R.id.imagePreview)
        voiceReflectionButton = findViewById(R.id.voiceReflectionButton)

        activeStrategyText = findViewById(R.id.activeStrategyText)
        checklistContainer = findViewById(R.id.checklistContainer)
        noChecklistText = findViewById(R.id.noChecklistText)
        locationInput = findViewById(R.id.locationInput)
    }

    private fun setupSpinners() {
        val tradeTypes = listOf("Buy", "Sell")
        val directionTypes = listOf("Long", "Short")

        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tradeTypes
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        val directionAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            directionTypes
        )
        directionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        directionSpinner.adapter = directionAdapter
    }

    private fun prefillAutomaticFields() {
        val now = System.currentTimeMillis()

        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        dateEditText.setText(formatter.format(Date(now)))

        sessionInput.setText(SessionHelper.getSessionFromTimestamp(now))
        locationInput.setText(LocationHelper.getCurrentLocationText(this))
    }

    private fun populateFieldsFromIntent() {
        assetEditText.setText(intent.getStringExtra("asset") ?: "")
        dateEditText.setText(intent.getStringExtra("date") ?: "")
        sessionInput.setText(intent.getStringExtra("session") ?: "")
        resultInput.setText(intent.getStringExtra("result") ?: "Open")
        notesInput.setText(intent.getStringExtra("notes") ?: "")
        locationInput.setText(intent.getStringExtra("locationText") ?: "Unknown")

        entryPriceInput.setText(doubleToInput(intent.getDoubleExtra("entryPrice", 0.0)))
        exitPriceInput.setText(doubleToInput(intent.getDoubleExtra("exitPrice", 0.0)))
        stopLossInput.setText(doubleToInput(intent.getDoubleExtra("stopLoss", 0.0)))
        takeProfitInput.setText(doubleToInput(intent.getDoubleExtra("takeProfit", 0.0)))
        rrInput.setText(doubleToInput(intent.getDoubleExtra("rr", 0.0)))
        positionValueInput.setText(doubleToInput(intent.getDoubleExtra("positionValue", 0.0)))
        positionPercentInput.setText(
            doubleToInput(intent.getDoubleExtra("positionPercentOfAccount", 0.0))
        )
        accountValueInput.setText(doubleToInput(intent.getDoubleExtra("accountValue", 0.0)))
        pnlAmountInput.setText(doubleToInput(intent.getDoubleExtra("pnlAmount", 0.0)))
        pnlPercentInput.setText(doubleToInput(intent.getDoubleExtra("pnlPercent", 0.0)))

        val type = intent.getStringExtra("type") ?: "Buy"
        val direction = intent.getStringExtra("direction") ?: "Long"

        setSpinnerSelection(typeSpinner, type)
        setSpinnerSelection(directionSpinner, direction)

        selectedImagePath = intent.getStringExtra("imagePath")
        showPreviewImage(selectedImagePath)

        val checkedConfluences = intent.getStringExtra("checkedConfluences") ?: ""
        restoreChecklistChecks(checkedConfluences)
    }

    private fun setSpinnerSelection(spinner: Spinner, value: String) {
        for (i in 0 until spinner.count) {
            val item = spinner.getItemAtPosition(i).toString()
            if (item.equals(value, ignoreCase = true)) {
                spinner.setSelection(i)
                return
            }
        }
    }

    private fun restoreChecklistChecks(checkedConfluences: String) {
        if (checkedConfluences.isBlank()) return

        val checkedItems = checkedConfluences
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        checklistCheckBoxes.forEach { checkBox ->
            checkBox.isChecked = checkedItems.any {
                it.equals(checkBox.text.toString(), ignoreCase = true)
            }
        }
    }

    private fun doubleToInput(value: Double): String {
        return if (value == 0.0) "" else value.toString()
    }

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

    private fun saveTrade() {
        val asset = assetEditText.text.toString().trim()
        val type = typeSpinner.selectedItem.toString()
        val direction = directionSpinner.selectedItem.toString()
        val date = dateEditText.text.toString().trim()
        val session = sessionInput.text.toString().trim()
        val result = resultInput.text.toString().trim().ifBlank { "Open" }
        val notes = notesInput.text.toString().trim()
        val locationText = locationInput.text.toString().trim().ifBlank { "Unknown" }

        if (asset.isBlank()) {
            Toast.makeText(this, "Insert asset", Toast.LENGTH_SHORT).show()
            return
        }

        if (date.isBlank()) {
            Toast.makeText(this, "Insert date", Toast.LENGTH_SHORT).show()
            return
        }

        val strategy = StrategyManager.getStrategy(this)

        val checkedItems = checklistCheckBoxes
            .filter { it.isChecked }
            .map { it.text.toString() }

        val oldStrategyName = intent.getStringExtra("strategyName") ?: ""
        val oldCheckedConfluences = intent.getStringExtra("checkedConfluences") ?: ""
        val oldConfluenceScore = intent.getIntExtra("confluenceScore", 0)

        val finalStrategyName = if (strategy.name.isNotBlank()) {
            strategy.name
        } else {
            oldStrategyName
        }

        val finalCheckedConfluences = if (checkedItems.isNotEmpty()) {
            checkedItems.joinToString(", ")
        } else {
            oldCheckedConfluences
        }

        val totalItems = checklistCheckBoxes.size
        val finalConfluenceScore = if (totalItems > 0) {
            (checkedItems.size * 100) / totalItems
        } else {
            oldConfluenceScore
        }

        val resultIntent = Intent().apply {
            putExtra("asset", asset)
            putExtra("type", type)
            putExtra("direction", direction)
            putExtra("date", date)
            putExtra("session", session)
            putExtra("result", result)
            putExtra("notes", notes)
            putExtra("imagePath", selectedImagePath)
            putExtra("strategyName", finalStrategyName)
            putExtra("checkedConfluences", finalCheckedConfluences)
            putExtra("confluenceScore", finalConfluenceScore)
            putExtra("locationText", locationText)

            putExtra("entryPrice", entryPriceInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("exitPrice", exitPriceInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("stopLoss", stopLossInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("takeProfit", takeProfitInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("rr", rrInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("positionValue", positionValueInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("positionPercentOfAccount", positionPercentInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("accountValue", accountValueInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("pnlAmount", pnlAmountInput.text.toString().toDoubleOrNull() ?: 0.0)
            putExtra("pnlPercent", pnlPercentInput.text.toString().toDoubleOrNull() ?: 0.0)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

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