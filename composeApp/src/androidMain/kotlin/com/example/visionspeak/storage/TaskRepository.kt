package com.example.visionspeak.storage

import com.example.visionspeak.model.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TaskRepository(private val fs: FileSystem) {
    private val json = Json { prettyPrint = true }
    private val filename = "tasks.json"

    suspend fun loadAll(): List<Task> = withContext(Dispatchers.IO) {
        val txt = fs.readText(filename) ?: return@withContext emptyList()
        try { json.decodeFromString(txt) } catch (_: Exception) { emptyList() }
    }

    suspend fun saveTask(task: Task) = withContext(Dispatchers.IO) {
        val current = loadAll().toMutableList()
        current.add(0, task)
        fs.writeText(filename, json.encodeToString(current))
    }
}
