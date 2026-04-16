package com.example.atj.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

/**
 * Utility per mostrare immagini con orientamento corretto.
 *
 * Problema risolto:
 * alcune immagini prese dalla galleria hanno metadati EXIF
 * che indicano una rotazione, ma BitmapFactory.decodeFile()
 * non la applica automaticamente.
 *
 * Questa utility:
 * - legge il file salvato internamente
 * - controlla l'orientamento EXIF
 * - ruota il bitmap se necessario
 */
object ImageDisplayHelper {

    /**
     * Decodifica un file immagine e restituisce un bitmap già ruotato correttamente.
     *
     * @param imagePath percorso assoluto del file immagine
     * @return bitmap corretto oppure null se il file non è leggibile
     */
    fun loadCorrectlyOrientedBitmap(imagePath: String?): Bitmap? {
        if (imagePath.isNullOrBlank()) return null

        val originalBitmap = BitmapFactory.decodeFile(imagePath) ?: return null

        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            if (rotationAngle == 0f) {
                originalBitmap
            } else {
                val matrix = Matrix().apply {
                    postRotate(rotationAngle)
                }

                Bitmap.createBitmap(
                    originalBitmap,
                    0,
                    0,
                    originalBitmap.width,
                    originalBitmap.height,
                    matrix,
                    true
                )
            }
        } catch (e: Exception) {
            // Se per qualsiasi motivo EXIF non è leggibile,
            // mostriamo comunque il bitmap originale.
            originalBitmap
        }
    }
}