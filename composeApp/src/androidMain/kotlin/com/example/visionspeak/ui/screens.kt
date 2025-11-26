package com.example.visionspeak.ui

import android.content.Context
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.visionspeak.audio.AudioRecorder
import com.example.visionspeak.audio.RecordingResult
import com.example.visionspeak.camera.CameraHelper
import com.example.visionspeak.model.Task
import com.example.visionspeak.storage.TaskRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.io.File
import java.util.UUID

// Routes
sealed class Route {
    object Start : Route()
    object Noise : Route()
    object Select : Route()
    object TextRead : Route()
    object ImageDesc : Route()
    object PhotoCapture : Route()
    object History : Route()
}

@Composable
fun NavHostApp(audioRecorder: AudioRecorder, repo: TaskRepository) {
    var route by remember { mutableStateOf<Route>(Route.Start) }
    val context = LocalContext.current

    when (route) {
        Route.Start -> StartScreen { route = Route.Noise }
        Route.Noise -> NoiseTestScreen(onPass = { route = Route.Select })
        Route.Select -> TaskSelectionScreen(
            onText = { route = Route.TextRead },
            onImage = { route = Route.ImageDesc },
            onPhoto = { route = Route.PhotoCapture },
            onHistory = { route = Route.History }
        )
        Route.TextRead -> TextReadingScreen(audioRecorder, repo) { route = Route.Select }
        Route.ImageDesc -> ImageDescriptionScreen(audioRecorder, repo) { route = Route.Select }
        Route.PhotoCapture -> PhotoCaptureScreen(context, audioRecorder, repo) { route = Route.Select }
        Route.History -> TaskHistoryScreen(repo) { route = Route.Select }
    }
}

@Composable
fun StartScreen(onStart: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Let's start with a Sample Task for practice.", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text("Pehele hum ek sample task karte hain.")
        Spacer(Modifier.height(20.dp))
        Button(onClick = onStart) { Text("Start Sample Task") }
    }
}

@Composable
fun NoiseTestScreen(onPass: () -> Unit) {
    var dB by remember { mutableStateOf(10) }
    var running by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text("Noise Test", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(progress = dB / 60f, modifier = Modifier.fillMaxWidth().height(28.dp))
        Spacer(Modifier.height(8.dp))
        Text("$dB dB")
        Spacer(Modifier.height(12.dp))
        Button(enabled = !running, onClick = {
            running = true; status = null
            scope.launch {
                var total = 0
                repeat(6) {
                    val sample = (5..55).random()
                    total += sample
                    dB = sample
                    delay(350)
                }
                val avg = total / 6
                if (avg < 40) {
                    status = "Good to proceed"
                    delay(500)
                    onPass()
                } else {
                    status = "Please move to a quieter place"
                }
                running = false
            }
        }) { Text("Start Test") }
        Spacer(Modifier.height(8.dp))
        status?.let { Text(it) }
    }
}

@Composable
fun TaskSelectionScreen(onText: () -> Unit, onImage: () -> Unit, onPhoto: () -> Unit, onHistory: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Select a Task", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onText, modifier = Modifier.fillMaxWidth()) { Text("Text Reading Task") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onImage, modifier = Modifier.fillMaxWidth()) { Text("Image Description Task") }
        Spacer(Modifier.height(8.dp))
        Button(onClick = onPhoto, modifier = Modifier.fillMaxWidth()) { Text("Photo Capture Task") }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onHistory, modifier = Modifier.fillMaxWidth()) { Text("Task History") }
    }
}

@Composable
fun PressHoldRecorder(
    recorder: AudioRecorder,
    filePathProvider: () -> String,
    onResult: (RecordingResult) -> Unit
) {
    val scope = rememberCoroutineScope()

    var isPressed by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    // When pressed: start recording
    LaunchedEffect(isPressed) {
        if (isPressed) {
            val path = filePathProvider()
            scope.launch {
                try {
                    isRecording = true
                    recorder.startRecording(path)
                } catch (_: Throwable) { }
            }
        } else if (!isPressed && isRecording) {
            // When released: stop recording
            scope.launch {
                val res = try {
                    recorder.stopRecording()
                } catch (_: Throwable) {
                    RecordingResult("", 0)
                }
                isRecording = false

                // Validate length
                if (res.durationSec < 10)
                    message = "Recording too short (min 10 s)."
                else if (res.durationSec > 20)
                    message = "Recording too long (max 20 s)."
                else {
                    message = null
                    onResult(res)
                }
            }
        }
    }

    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            tonalElevation = 4.dp,
            modifier = Modifier
                .size(96.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.any { it.pressed }

                            if (pressed && !isPressed) {
                                isPressed = true
                            }
                            if (!pressed && isPressed) {
                                isPressed = false
                            }
                        }
                    }
                }
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isRecording) "Recording..." else "Hold to Record")
            }
        }
    }

    message?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Sample text reading screen
@Composable
fun TextReadingScreen(recorder: AudioRecorder, repo: TaskRepository, onDone: () -> Unit) {
    val sampleText = "Mega long lasting fragrance..." // replace with API fetch
    var audioPath by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableIntStateOf(0) }
    var c1 by remember { mutableStateOf(false) }
    var c2 by remember { mutableStateOf(false) }
    var c3 by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(sampleText)
        Spacer(Modifier.height(12.dp))
        Text("Read the passage aloud in your native language.")
        Spacer(Modifier.height(12.dp))
        PressHoldRecorder(recorder, filePathProvider = { "${android.os.Environment.getExternalStorageDirectory().absolutePath}/visionspeak_audio_${System.currentTimeMillis()}.mp4" }) { res ->
            audioPath = res.path; duration = res.durationSec
        }
        Spacer(Modifier.height(8.dp))
        audioPath?.let { Text("Recorded: $it (${duration}s)") }
        Spacer(Modifier.height(8.dp))
        Row { Checkbox(checked = c1, onCheckedChange = { c1 = it }); Spacer(Modifier.width(8.dp)); Text("No background noise") }
        Row { Checkbox(checked = c2, onCheckedChange = { c2 = it }); Spacer(Modifier.width(8.dp)); Text("No mistakes while reading") }
        Row { Checkbox(checked = c3, onCheckedChange = { c3 = it }); Spacer(Modifier.width(8.dp)); Text("Beech me koi galti nahi hai") }
        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = { audioPath = null; duration = 0 }) { Text("Record again") }
            Spacer(Modifier.width(8.dp))
            Button(enabled = audioPath != null && c1 && c2 && c3, onClick = {
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    task_type = "text_reading",
                    text = sampleText,
                    audio_path = audioPath,
                    duration_sec = duration,
                    timestamp = Clock.System.now().toString()
                )
                scope.launch { repo.saveTask(task); onDone() }
            }) { Text("Submit") }
        }
    }
}

// Image Description — uses sampleImageUrls
@Composable
fun ImageDescriptionScreen(recorder: AudioRecorder, repo: TaskRepository, onDone: () -> Unit) {
    val sampleImageUrls = listOf(
        "https://drive.google.com/open?id=1mog2XhYf5yifkHZxGXR3I3VjOKPiTL7q&usp=drive_copy",
        "https://drive.google.com/open?id=136A6wn0rTXQxY-sLBPrL8qekdzmsO65F&usp=drive_copy",
        "https://drive.google.com/open?id=18ECru540TAkLPGIo43dhsrWawOMxS0WM&usp=drive_copy",
        "https://drive.google.com/open?id=1Gf3NX_Pvrt3P2q8Z8GIB8jjKaltPbXCx&usp=drive_copy",
        "https://drive.google.com/open?id=1qJIL9KtDn3cbKFZprw17rkXDCxCfAfDh&usp=drive_copy",
        "https://drive.google.com/open?id=1K10BqY_xRnwbdIGWTtG-XmccPtJNgNJY&usp=drive_copy"
    )

    var selectedUrl by remember { mutableStateOf(sampleImageUrls.first()) }
    var audioPath by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // display image with Coil
        AsyncImage(
            model = ImageRequest.Builder(context).data(selectedUrl).crossfade(true).build(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentScale = ContentScale.Crop
        )

        Spacer(Modifier.height(8.dp))
        // Show sample selector
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            sampleImageUrls.forEach { url ->
                Button(onClick = { selectedUrl = url }) { Text("Image") }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Describe what you see in your native language.")
        Spacer(Modifier.height(12.dp))
        PressHoldRecorder(recorder, filePathProvider = { "${android.os.Environment.getExternalStorageDirectory().absolutePath}/visionspeak_desc_${System.currentTimeMillis()}.mp4" }) { res ->
            audioPath = res.path; duration = res.durationSec
        }

        Spacer(Modifier.height(12.dp))
        audioPath?.let { Text("Recorded: $it (${duration}s)") }
        Spacer(Modifier.height(12.dp))
        Button(enabled = audioPath != null, onClick = {
            val task = Task(
                id = UUID.randomUUID().toString(),
                task_type = "image_description",
                image_url = selectedUrl,
                audio_path = audioPath,
                duration_sec = duration,
                timestamp = Clock.System.now().toString()
            )
            scope.launch { repo.saveTask(task); onDone() }
        }) { Text("Submit") }
    }
}

// Photo capture screen
@Composable
fun PhotoCaptureScreen(context: Context, recorder: AudioRecorder, repo: TaskRepository, onDone: () -> Unit) {
    var photoFile by remember { mutableStateOf<File?>(null) }
    var previewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var description by remember { mutableStateOf("") }
    var audioPath by remember { mutableStateOf<String?>(null) }
    var duration by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && photoFile != null) {
            val bm = BitmapFactory.decodeFile(photoFile!!.absolutePath)
            previewBitmap = bm
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = {
            val f = CameraHelper.createImageFile(context)
            photoFile = f
            val uri = CameraHelper.fileToUri(context, f)
            cameraLauncher.launch(uri)
        }) { Text("Capture Image") }

        Spacer(Modifier.height(8.dp))
        previewBitmap?.let { Image(bitmap = it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth().height(240.dp), contentScale = ContentScale.Crop) }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Describe the photo") }, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        PressHoldRecorder(recorder, filePathProvider = { "${android.os.Environment.getExternalStorageDirectory().absolutePath}/visionspeak_photo_audio_${System.currentTimeMillis()}.mp4" }) { res ->
            audioPath = res.path; duration = res.durationSec
        }

        Spacer(Modifier.height(12.dp))
        Row {
            Button(onClick = { previewBitmap = null; photoFile = null }) { Text("Retake Photo") }
            Spacer(Modifier.width(8.dp))
            Button(enabled = photoFile != null, onClick = {
                val task = Task(
                    id = UUID.randomUUID().toString(),
                    task_type = "photo_capture",
                    image_path = photoFile?.absolutePath,
                    audio_path = audioPath,
                    duration_sec = duration,
                    timestamp = Clock.System.now().toString()
                )
                scope.launch { repo.saveTask(task); onDone() }
            }) { Text("Submit") }
        }
    }
}

@Composable
fun TaskHistoryScreen(repo: TaskRepository, onBack: () -> Unit) {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { tasks = repo.loadAll() }

    val totalTasks = tasks.size
    val totalDuration = tasks.sumOf { it.duration_sec }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Total Tasks: $totalTasks")
            Text("Total Duration: ${totalDuration}s")
        }
        Spacer(Modifier.height(12.dp))
        LazyColumn {
            items(tasks) { t ->
                Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("ID: ${t.id.take(8)}")
                            Text("Type: ${t.task_type}")
                            Text("Duration: ${t.duration_sec}s · ${t.timestamp}")
                        }
                        t.text?.let { Text(it.take(30)) }
                        t.image_path?.let {
                            val bm = BitmapFactory.decodeFile(it)
                            if (bm != null) Image(bitmap = bm.asImageBitmap(), contentDescription = null, modifier = Modifier.size(56.dp), contentScale = ContentScale.Crop)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onBack) { Text("Back") }
    }
}
