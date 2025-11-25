package com.example.visionspeak.storage

interface FileSystem {
    suspend fun readText(filename: String): String?
    suspend fun writeText(filename: String, content: String)
    fun resolvePath(filename: String): String
}
