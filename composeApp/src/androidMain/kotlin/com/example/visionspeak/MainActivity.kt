package com.example.visionspeak

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import com.example.visionspeak.audio.AudioRecorderAndroid
import com.example.visionspeak.storage.FileSystemAndroid
import com.example.visionspeak.storage.TaskRepository
import com.example.visionspeak.ui.NavHostApp

class MainActivity : ComponentActivity() {

    private lateinit var repo: TaskRepository
    private lateinit var recorder: AudioRecorderAndroid

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Request runtime permissions (improve later: request on demand)
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            ),
            1001
        )

        val fs = FileSystemAndroid(this)
        repo = TaskRepository(fs)
        recorder = AudioRecorderAndroid()

        setContent {
            NavHostApp(audioRecorder = recorder, repo = repo)
        }
    }
}
