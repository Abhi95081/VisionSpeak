package com.example.visionspeak.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CameraHelper {
    fun createImageFile(context: Context): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_smis", Locale.US).format(Date())
        val filename = "JPEG_${timeStamp}_"
        val storageDir = context.cacheDir
        return File.createTempFile(filename, ".jpg", storageDir)
    }

    fun fileToUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.packageName}.file provider", file)
    }

    fun openCameraIntent(outputUri: Uri): Intent {
        return Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(android.provider.MediaStore.EXTRA_OUTPUT, outputUri)
        }
    }
}
