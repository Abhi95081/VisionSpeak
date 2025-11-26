package com.example.visionspeak.model

import kotlinx.serialization.Serializable

@Serializable
data class Task(
    val id: String,
    val task_type: String, // "text_reading" | "image_description" | "photo_capture"
    val text: String? = null,
    val image_url: String? = null,
    val image_path: String? = null,
    val audio_path: String? = null,
    val duration_sec: Int,
    val timestamp: String // ISO-8601
)
