package com.example.atj

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.utils.ImageStorageHelper

// Activity per inserire manualmente un nuovo trade.
// Qui raccogliamo i dati e li rimandiamo alla MainActivity.
class AddTradeActivity : AppCompatActivity() {

    private lateinit var assetEditText: EditText
    private lateinit var typeSpinner: Spinner
    private lateinit var dateEditText: EditText
    private lateinit var sessionInput: EditText
    private lateinit var resultInput: EditText
    private lateinit var notesInput: EditText
    private lateinit var saveTradeButton: Button

    // Nuove view per la parte immagini
    private lateinit var pickImageButton: Button
    private lateinit var takePhotoButton: Button
    private lateinit var imagePreview: ImageView

    // Percorso locale dell'immagine selezionata o scattata
    private var selectedImagePath: String? = null

    // Picker immagini da galleria / photo picker
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val savedPath = ImageStorageHelper.copyUriToInternalStorage(this, uri)
                selectedImagePath = savedPath

                val bitmap = BitmapFactory.decodeFile(savedPath)
                imagePreview.setImageBitmap(bitmap)
                imagePreview.visibility = ImageView.VISIBLE
            }
        }

    // Fotocamera rapida tramite preview bitmap
    private val takePicturePreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val savedPath = ImageStorageHelper.saveBitmapToInternalStorage(this, bitmap)
                selectedImagePath = savedPath

                val previewBitmap = BitmapFactory.decodeFile(savedPath)
                imagePreview.setImageBitmap(previewBitmap)
                imagePreview.visibility = ImageView.VISIBLE
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_trade)

        // Collego le view del layout
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

        // Spinner Buy / Sell
        val tradeTypes = listOf("Buy", "Sell")
        val spinnerAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            tradeTypes
        )
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        typeSpinner.adapter = spinnerAdapter

        // Apertura galleria / photo picker
        pickImageButton.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        // Apertura fotocamera rapida
        takePhotoButton.setOnClickListener {
            takePicturePreviewLauncher.launch(null)
        }

        // Salvataggio dati verso MainActivity
        saveTradeButton.setOnClickListener {
            val asset = assetEditText.text.toString().trim()
            val type = typeSpinner.selectedItem.toString()
            val date = dateEditText.text.toString().trim()
            val session = sessionInput.text.toString().trim()
            val result = resultInput.text.toString().trim()
            val notes = notesInput.text.toString().trim()

            val resultIntent = Intent().apply {
                putExtra("asset", asset)
                putExtra("type", type)
                putExtra("date", date)
                putExtra("session", session)
                putExtra("result", result)
                putExtra("notes", notes)
                putExtra("imagePath", selectedImagePath)
            }

            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
    }
}