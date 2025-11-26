package com.example.visionspeak.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileSystemAndroid(private val context: Context) : FileSystem {
    override suspend fun readText(filename: String): String? = withContext(Dispatchers.IO) {
        val f = File(context.filesDir, filename)
        if (!f.exists()) return@withContext null
        return@withContext f.readText()
    }

    override suspend fun writeText(filename: String, content: String) = withContext(Dispatchers.IO) {
        val f = File(context.filesDir, filename)
        f.writeText(content)
    }

    override fun resolvePath(filename: String): String {
        return File(context.filesDir, filename).absolutePath
    }
}
