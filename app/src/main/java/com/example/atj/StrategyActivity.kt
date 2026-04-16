package com.example.atj

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.atj.utils.ImageDisplayHelper
import com.example.atj.utils.ImageStorageHelper
import com.example.atj.utils.StrategyManager

// Schermata separata dove l'utente definisce la propria strategia.
class StrategyActivity : AppCompatActivity() {

    private lateinit var strategyNameInput: EditText
    private lateinit var strategyDescriptionInput: EditText
    private lateinit var strategyChecklistInput: EditText
    private lateinit var strategyImagePreview: ImageView
    private lateinit var pickStrategyImageButton: Button
    private lateinit var takeStrategyPhotoButton: Button
    private lateinit var saveStrategyButton: Button

    private var selectedStrategyImagePath: String? = null

    /**
     * Selezione immagine da galleria.
     */
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                val savedPath = ImageStorageHelper.copyUriToInternalStorage(this, uri)
                selectedStrategyImagePath = savedPath

                showPreviewImage(savedPath)
            }
        }

    /**
     * Scatto rapido foto da camera.
     */
    private val takePicturePreviewLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            if (bitmap != null) {
                val savedPath = ImageStorageHelper.saveBitmapToInternalStorage(this, bitmap)
                selectedStrategyImagePath = savedPath

                showPreviewImage(savedPath)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_strategy)

        strategyNameInput = findViewById(R.id.strategyNameInput)
        strategyDescriptionInput = findViewById(R.id.strategyDescriptionInput)
        strategyChecklistInput = findViewById(R.id.strategyChecklistInput)
        strategyImagePreview = findViewById(R.id.strategyImagePreview)
        pickStrategyImageButton = findViewById(R.id.pickStrategyImageButton)
        takeStrategyPhotoButton = findViewById(R.id.takeStrategyPhotoButton)
        saveStrategyButton = findViewById(R.id.saveStrategyButton)

        loadExistingStrategy()

        pickStrategyImageButton.setOnClickListener {
            pickMediaLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        takeStrategyPhotoButton.setOnClickListener {
            takePicturePreviewLauncher.launch(null)
        }

        saveStrategyButton.setOnClickListener {
            saveStrategy()
        }
    }

    /**
     * Carica la strategia già salvata, se presente.
     */
    private fun loadExistingStrategy() {
        val strategy = StrategyManager.getStrategy(this)

        strategyNameInput.setText(strategy.name)
        strategyDescriptionInput.setText(strategy.description)
        strategyChecklistInput.setText(strategy.checklistItems.joinToString("\n"))
        selectedStrategyImagePath = strategy.imagePath

        showPreviewImage(strategy.imagePath)
    }

    /**
     * Mostra preview immagine con orientamento corretto.
     */
    private fun showPreviewImage(imagePath: String?) {
        val correctedBitmap = ImageDisplayHelper.loadCorrectlyOrientedBitmap(imagePath)

        if (correctedBitmap != null) {
            strategyImagePreview.setImageBitmap(correctedBitmap)
            strategyImagePreview.visibility = ImageView.VISIBLE
        } else {
            strategyImagePreview.setImageDrawable(null)
            strategyImagePreview.visibility = ImageView.GONE
        }
    }

    /**
     * Salva la strategia nei preferences tramite StrategyManager.
     */
    private fun saveStrategy() {
        val name = strategyNameInput.text.toString().trim()
        val description = strategyDescriptionInput.text.toString().trim()
        val checklistItems = strategyChecklistInput.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        StrategyManager.saveStrategy(
            context = this,
            name = name,
            description = description,
            imagePath = selectedStrategyImagePath,
            checklistItems = checklistItems
        )

        Toast.makeText(this, "Strategy saved", Toast.LENGTH_SHORT).show()
        finish()
    }
}