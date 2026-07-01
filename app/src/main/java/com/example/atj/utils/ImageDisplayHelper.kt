package com.example.atj.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface

/*
 * Utility per caricare immagini rispettando l'orientamento EXIF.
 * Alcune immagini salvano la rotazione nei metadati, non direttamente nel bitmap.
 */
object ImageDisplayHelper {

    /*
     * Decodifica il file immagine e, se necessario, ruota il Bitmap.
     * Ritorna null se il path è vuoto o il file non può essere letto.
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

            // Traduce il valore EXIF in un angolo di rotazione effettivo.
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
            // Se i metadati EXIF non sono leggibili, l'app mostra comunque l'immagine.
            originalBitmap
        }
    }
}