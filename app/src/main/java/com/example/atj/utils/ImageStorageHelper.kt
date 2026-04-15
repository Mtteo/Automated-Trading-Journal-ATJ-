package com.example.atj.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

// Utility per salvare immagini in memoria interna.
// In questo modo il trade conserva un percorso stabile locale.
object ImageStorageHelper {

    // Salva un Bitmap (ad esempio dalla fotocamera preview) in memoria interna.
    fun saveBitmapToInternalStorage(context: Context, bitmap: Bitmap): String {
        val fileName = "trade_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
        }

        return file.absolutePath
    }

    // Copia un'immagine scelta dalla galleria in memoria interna.
    fun copyUriToInternalStorage(context: Context, uri: Uri): String {
        val fileName = "trade_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, fileName)

        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        FileOutputStream(file).use { outputStream ->
            inputStream?.copyTo(outputStream)
            outputStream.flush()
        }
        inputStream?.close()

        return file.absolutePath
    }
}