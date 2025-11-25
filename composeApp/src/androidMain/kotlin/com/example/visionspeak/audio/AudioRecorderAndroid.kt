package com.example.visionspeak.audio

import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10

class AudioRecorderAndroid : AudioRecorder {
    private var recorder: MediaRecorder? = null
    private var startTime: Long = 0L
    private var filePath: String = ""

    override suspend fun startRecording(targetPath: String) = withContext(Dispatchers.Main) {
        recorder?.release()
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(targetPath)
            try {
                prepare()
                start()
                startTime = System.currentTimeMillis()
                filePath = targetPath
            } catch (t: Throwable) {
                release()
                recorder = null
                throw t
            }
        }
    }

    override suspend fun stopRecording(): RecordingResult = withContext(Dispatchers.Main) {
        val r = recorder ?: return@withContext RecordingResult(path = filePath, durationSec = 0)
        try {
            r.stop()
        } catch (_: Exception) { /* ignore */ }
        r.release()
        recorder = null
        val dur = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        return@withContext RecordingResult(path = filePath, durationSec = dur)
    }

    override suspend fun getAmplitude(): Float = withContext(Dispatchers.Main) {
        val r = recorder ?: return@withContext 0f
        return@withContext try {
            val amp = r.maxAmplitude // 0..32767
            val db = 20 * log10(amp.toDouble() / 32768.0 + 1e-6).toFloat()
            ((db + 60) / 60).coerceIn(0f, 1f)
        } catch (_: Exception) { 0f }
    }
}
