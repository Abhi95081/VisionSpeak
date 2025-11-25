package com.example.visionspeak.audio

data class RecordingResult(val path: String, val durationSec: Int)

interface AudioRecorder {
    suspend fun startRecording(targetPath: String)
    suspend fun stopRecording(): RecordingResult
    suspend fun getAmplitude(): Float
}
