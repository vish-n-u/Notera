# Conversational AI Call Feature Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a full-screen "Talk to AI" call feature that streams mic audio to Gemini Live API over WebSocket and plays back AI audio in real time, with an option to save the transcript as a note.

**Architecture:** The app opens an OkHttp WebSocket directly to Gemini Live API. `AICallViewModel` owns the WebSocket client, `AudioRecord` for mic input, and `AudioTrack` for playback. `AICallScreen` is a full-screen Compose UI that mirrors call state. On end, the user is prompted to save the transcript as a Room DB note.

**Tech Stack:** OkHttp WebSocket (already a transitive dep via Retrofit), AudioRecord, AudioTrack, Gson, Jetpack Compose, Canvas animation, Room DB (existing).

---

## Chunk 1: Foundation — API Key, WebSocket Client, Call State

### Task 1: Add Gemini API Key to Build Config

**Files:**
- Modify: `local.properties` (root of project)
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add placeholder API key to `local.properties`**

Open `local.properties` (root of project, already in `.gitignore`) and add:

```properties
GEMINI_API_KEY=YOUR_KEY_HERE
```

Replace `YOUR_KEY_HERE` with your actual Gemini API key from https://aistudio.google.com/app/apikey

- [ ] **Step 2: Expose the key via BuildConfig in `app/build.gradle.kts`**

Inside the `android { defaultConfig { ... } }` block, add:

```kotlin
val localProperties = java.util.Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}
buildConfigField(
    "String",
    "GEMINI_API_KEY",
    "\"${localProperties.getProperty("GEMINI_API_KEY", "")}\""
)
```

Also ensure `buildFeatures { buildConfig = true }` is set inside the `android { }` block:

```kotlin
buildFeatures {
    compose = true
    buildConfig = true
}
```

- [ ] **Step 3: Sync Gradle**

In Android Studio: File → Sync Project with Gradle Files. Confirm build succeeds with no errors.

---

### Task 2: Create `GeminiLiveClient`

**Files:**
- Create: `app/src/main/java/com/example/devaudioreccordings/pages/AICall/GeminiLiveClient.kt`
- Create: `app/src/test/java/com/example/devaudioreccordings/GeminiLiveClientTest.kt`

- [ ] **Step 1: Write the failing unit test first**

Create `app/src/test/java/com/example/devaudioreccordings/GeminiLiveClientTest.kt`:

```kotlin
package com.example.devaudioreccordings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveClientTest {

    @Test
    fun `buildSetupMessage returns valid JSON with model and system instruction`() {
        val json = GeminiLiveClient.buildSetupMessage("Be helpful.")
        assertTrue(json.contains("\"setup\""))
        assertTrue(json.contains("models/gemini-2.0-flash-live"))
        assertTrue(json.contains("Be helpful."))
        assertTrue(json.contains("\"AUDIO\""))
    }

    @Test
    fun `buildAudioChunkMessage encodes bytes as base64 in expected JSON structure`() {
        val bytes = byteArrayOf(0x00, 0x01, 0x02)
        val json = GeminiLiveClient.buildAudioChunkMessage(bytes)
        assertTrue(json.contains("\"realtimeInput\""))
        assertTrue(json.contains("\"mediaChunks\""))
        assertTrue(json.contains("audio/pcm;rate=16000"))
        // base64 of [0x00, 0x01, 0x02] is "AAEC"
        assertTrue(json.contains("AAEC"))
    }

    @Test
    fun `parseServerMessage extracts audio data when present`() {
        val json = """
        {
          "serverContent": {
            "modelTurn": {
              "parts": [
                {"inlineData": {"mimeType": "audio/pcm;rate=24000", "data": "AAEC"}}
              ]
            }
          }
        }
        """.trimIndent()
        val result = GeminiLiveClient.parseServerMessage(json)
        assertEquals("AAEC", result.audioBase64)
        assertNull(result.transcript)
        assertTrue(!result.turnComplete)
        assertTrue(!result.interrupted)
    }

    @Test
    fun `parseServerMessage detects turnComplete`() {
        val json = """
        {
          "serverContent": {
            "turnComplete": true
          }
        }
        """.trimIndent()
        val result = GeminiLiveClient.parseServerMessage(json)
        assertTrue(result.turnComplete)
        assertNull(result.audioBase64)
    }

    @Test
    fun `parseServerMessage detects interrupted`() {
        val json = """{"serverContent": {"interrupted": true}}"""
        val result = GeminiLiveClient.parseServerMessage(json)
        assertTrue(result.interrupted)
    }

    @Test
    fun `parseServerMessage extracts transcript text when present`() {
        val json = """
        {
          "serverContent": {
            "modelTurn": {
              "parts": [
                {"text": "Hello, how can I help you?"}
              ]
            }
          }
        }
        """.trimIndent()
        val result = GeminiLiveClient.parseServerMessage(json)
        assertEquals("Hello, how can I help you?", result.transcript)
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

In Android Studio terminal:
```bash
./gradlew :app:test --tests "com.example.devaudioreccordings.GeminiLiveClientTest"
```
Expected: FAIL — `GeminiLiveClient` class not found.

- [ ] **Step 3: Create `GeminiLiveClient.kt`**

Create `app/src/main/java/com/example/devaudioreccordings/pages/AICall/GeminiLiveClient.kt`:

```kotlin
package com.example.devaudioreccordings.pages.AICall

import android.util.Base64
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit

data class ServerMessage(
    val audioBase64: String? = null,
    val transcript: String? = null,
    val turnComplete: Boolean = false,
    val interrupted: Boolean = false
)

class GeminiLiveClient(
    private val apiKey: String,
    private val onMessage: (ServerMessage) -> Unit,
    private val onOpen: () -> Unit,
    private val onClosed: () -> Unit,
    private val onError: (Throwable) -> Unit
) {

    private var webSocket: WebSocket? = null

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // no timeout for persistent WebSocket
        .build()

    fun connect() {
        val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage" +
                ".v1alpha.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                // Send setup message immediately after connection opens
                webSocket.send(buildSetupMessage("You are a helpful AI assistant. Keep your responses conversational, warm, and concise as if talking on a phone call."))
                onOpen()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val parsed = parseServerMessage(text)
                onMessage(parsed)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                onError(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                onClosed()
            }
        })
    }

    fun sendAudioChunk(pcmBytes: ByteArray) {
        val json = buildAudioChunkMessage(pcmBytes)
        webSocket?.send(json)
    }

    fun disconnect() {
        webSocket?.close(1000, "Call ended")
        webSocket = null
    }

    companion object {
        fun buildSetupMessage(systemPrompt: String): String {
            return """
                {
                  "setup": {
                    "model": "models/gemini-2.0-flash-live-001",
                    "generationConfig": {
                      "responseModalities": ["AUDIO"]
                    },
                    "systemInstruction": {
                      "parts": [{"text": "$systemPrompt"}]
                    }
                  }
                }
            """.trimIndent()
        }

        fun buildAudioChunkMessage(pcmBytes: ByteArray): String {
            val base64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
            return """
                {
                  "realtimeInput": {
                    "mediaChunks": [
                      {"mimeType": "audio/pcm;rate=16000", "data": "$base64"}
                    ]
                  }
                }
            """.trimIndent()
        }

        fun parseServerMessage(json: String): ServerMessage {
            return try {
                val root = JsonParser.parseString(json).asJsonObject
                val serverContent = root.getAsJsonObject("serverContent")
                    ?: return ServerMessage()

                val interrupted = serverContent.get("interrupted")?.asBoolean ?: false
                if (interrupted) return ServerMessage(interrupted = true)

                val turnComplete = serverContent.get("turnComplete")?.asBoolean ?: false

                val modelTurn = serverContent.getAsJsonObject("modelTurn")
                    ?: return ServerMessage(turnComplete = turnComplete)

                val parts = modelTurn.getAsJsonArray("parts")
                    ?: return ServerMessage(turnComplete = turnComplete)

                var audioBase64: String? = null
                var transcript: String? = null

                for (partElement in parts) {
                    val part = partElement.asJsonObject
                    val inlineData = part.getAsJsonObject("inlineData")
                    if (inlineData != null) {
                        audioBase64 = inlineData.get("data")?.asString
                    }
                    val text = part.get("text")?.asString
                    if (text != null) {
                        transcript = (transcript ?: "") + text
                    }
                }

                ServerMessage(
                    audioBase64 = audioBase64,
                    transcript = transcript,
                    turnComplete = turnComplete,
                    interrupted = false
                )
            } catch (e: Exception) {
                ServerMessage()
            }
        }
    }
}
```

**Note:** The unit test imports `GeminiLiveClient` directly. Because `buildSetupMessage`, `buildAudioChunkMessage`, and `parseServerMessage` are in the `companion object`, they're testable without Android framework. However, `android.util.Base64` is not available in JUnit (it's Android-only). To fix this for testing, split the Base64 logic:

In the test file, the `buildAudioChunkMessage` test uses `"AAEC"` as expected output. In the actual `GeminiLiveClient`, `android.util.Base64` is used. For the unit test to work, replace `android.util.Base64` in the `companion object` with `java.util.Base64`:

```kotlin
// In companion object, change:
val base64 = Base64.encodeToString(pcmBytes, Base64.NO_WRAP)
// To:
val base64 = java.util.Base64.getEncoder().encodeToString(pcmBytes)
```

And remove the `import android.util.Base64` line. Keep the `android.util.Base64` usage only in the instance method `sendAudioChunk` where it can stay as `java.util.Base64` too (it works on Android API 26+, which is above the minSdk of 29).

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :app:test --tests "com.example.devaudioreccordings.GeminiLiveClientTest"
```
Expected: All 6 tests PASS.

---

### Task 3: Create `CallState` and `AICallViewModel` skeleton

**Files:**
- Create: `app/src/main/java/com/example/devaudioreccordings/pages/AICall/AICallViewModel.kt`
- Create: `app/src/test/java/com/example/devaudioreccordings/AICallViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/example/devaudioreccordings/AICallViewModelTest.kt`:

```kotlin
package com.example.devaudioreccordings

import com.example.devaudioreccordings.pages.AICall.CallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AICallViewModelTest {

    @Test
    fun `Ended state holds transcript string`() {
        val state = CallState.Ended("AI: Hello\nYou: Hi")
        assertEquals("AI: Hello\nYou: Hi", state.transcript)
    }

    @Test
    fun `CallState values are distinct types`() {
        val connecting: CallState = CallState.Connecting
        val listening: CallState = CallState.Listening
        val speaking: CallState = CallState.AISpeaking
        val muted: CallState = CallState.Muted
        val ended: CallState = CallState.Ended("")

        assertTrue(connecting is CallState.Connecting)
        assertTrue(listening is CallState.Listening)
        assertTrue(speaking is CallState.AISpeaking)
        assertTrue(muted is CallState.Muted)
        assertTrue(ended is CallState.Ended)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
./gradlew :app:test --tests "com.example.devaudioreccordings.AICallViewModelTest"
```
Expected: FAIL — `CallState` not found.

- [ ] **Step 3: Create `AICallViewModel.kt` with `CallState` and skeleton**

Create `app/src/main/java/com/example/devaudioreccordings/pages/AICall/AICallViewModel.kt`:

```kotlin
package com.example.devaudioreccordings.pages.AICall

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.devaudioreccordings.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class CallState {
    object Connecting : CallState()
    object Listening : CallState()
    object AISpeaking : CallState()
    object Muted : CallState()
    data class Ended(val transcript: String) : CallState()
}

class AICallViewModel(application: Application) : AndroidViewModel(application) {

    private val _callState = MutableStateFlow<CallState>(CallState.Connecting)
    val callState: StateFlow<CallState> = _callState

    private val _callDurationSeconds = MutableStateFlow(0)
    val callDurationSeconds: StateFlow<Int> = _callDurationSeconds

    // Mic input config (Gemini requires 16kHz mono PCM)
    private val MIC_SAMPLE_RATE = 16000
    private val CHUNK_SIZE_BYTES = 640 // 20ms of audio at 16kHz mono 16-bit

    // Playback config (Gemini outputs 24kHz mono PCM)
    private val PLAYBACK_SAMPLE_RATE = 24000

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null
    private var micCaptureJob: Job? = null
    private var timerJob: Job? = null
    private var isMuted = false

    private val transcriptLines = mutableListOf<String>()

    private var geminiClient: GeminiLiveClient? = null

    fun startCall() {
        _callState.value = CallState.Connecting
        transcriptLines.clear()

        geminiClient = GeminiLiveClient(
            apiKey = BuildConfig.GEMINI_API_KEY,
            onOpen = {
                _callState.value = CallState.Listening
                startMicCapture()
                startTimer()
            },
            onMessage = { msg ->
                handleServerMessage(msg)
            },
            onClosed = {
                if (_callState.value !is CallState.Ended) {
                    endCall()
                }
            },
            onError = { error ->
                android.util.Log.e("AICallViewModel", "WebSocket error: ${error.message}")
                if (_callState.value !is CallState.Ended) {
                    endCall()
                }
            }
        )
        geminiClient?.connect()
    }

    private fun handleServerMessage(msg: ServerMessage) {
        when {
            msg.interrupted -> {
                stopPlayback()
                _callState.value = CallState.Listening
            }
            msg.audioBase64 != null -> {
                _callState.value = CallState.AISpeaking
                playAudioChunk(msg.audioBase64)
            }
            msg.turnComplete -> {
                _callState.value = CallState.Listening
            }
        }
        msg.transcript?.let { transcriptLines.add("AI: $it") }
    }

    fun toggleMute() {
        isMuted = !isMuted
        _callState.value = if (isMuted) CallState.Muted else CallState.Listening
    }

    fun endCall() {
        micCaptureJob?.cancel()
        timerJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        stopPlayback()
        geminiClient?.disconnect()
        geminiClient = null
        val transcript = transcriptLines.joinToString("\n")
        _callState.value = CallState.Ended(transcript)
    }

    private fun startMicCapture() {
        val minBufferSize = AudioRecord.getMinBufferSize(
            MIC_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            MIC_SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufferSize, CHUNK_SIZE_BYTES * 2)
        )
        audioRecord?.startRecording()

        micCaptureJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(CHUNK_SIZE_BYTES)
            while (true) {
                if (kotlinx.coroutines.isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                    if (read > 0 && !isMuted) {
                        geminiClient?.sendAudioChunk(buffer.copyOf(read))
                    }
                } else break
            }
        }
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    private fun playAudioChunk(base64Audio: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val pcmBytes = java.util.Base64.getDecoder().decode(base64Audio)
            if (audioTrack == null || audioTrack?.state == AudioTrack.STATE_UNINITIALIZED) {
                initAudioTrack()
            }
            audioTrack?.write(pcmBytes, 0, pcmBytes.size)
        }
    }

    private fun initAudioTrack() {
        val minBufferSize = AudioTrack.getMinBufferSize(
            PLAYBACK_SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
            AudioFormat.Builder()
                .setSampleRate(PLAYBACK_SAMPLE_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build(),
            minBufferSize * 2,
            AudioTrack.MODE_STREAM,
            android.media.AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        audioTrack?.play()
    }

    private fun stopPlayback() {
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
    }

    override fun onCleared() {
        super.onCleared()
        if (_callState.value !is CallState.Ended) {
            endCall()
        }
    }

    fun formatDuration(seconds: Int): String {
        val m = seconds / 60
        val s = seconds % 60
        return "%d:%02d".format(m, s)
    }
}
```

- [ ] **Step 4: Run tests to confirm they pass**

```bash
./gradlew :app:test --tests "com.example.devaudioreccordings.AICallViewModelTest"
```
Expected: Both tests PASS.

---

## Chunk 2: UI — Full-Screen Call Screen

### Task 4: Create the Pulsing Animation Composable

**Files:**
- Create: `app/src/main/java/com/example/devaudioreccordings/pages/AICall/PulsingCircle.kt`

- [ ] **Step 1: Create `PulsingCircle.kt`**

```kotlin
package com.example.devaudioreccordings.pages.AICall

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PulsingCircle(
    isSpeaking: Boolean,
    size: Dp = 160.dp,
    baseColor: Color = MaterialTheme.colorScheme.primary
) {
    val duration = if (isSpeaking) 600 else 1400

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Canvas(modifier = Modifier.size(size)) {
        val radius = (this.size.minDimension / 2) * scale
        // Outer glow ring
        drawCircle(
            color = baseColor.copy(alpha = alpha * 0.3f),
            radius = radius * 1.3f
        )
        // Inner ring
        drawCircle(
            color = baseColor.copy(alpha = alpha * 0.6f),
            radius = radius * 1.1f
        )
        // Core circle
        drawCircle(
            color = baseColor.copy(alpha = alpha),
            radius = radius * 0.8f
        )
    }
}
```

- [ ] **Step 2: Build the project to verify no compile errors**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 5: Create `AICallScreen`

**Files:**
- Create: `app/src/main/java/com/example/devaudioreccordings/pages/AICall/AICallScreen.kt`

- [ ] **Step 1: Create `AICallScreen.kt`**

```kotlin
package com.example.devaudioreccordings.pages.AICall

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun AICallScreen(
    navController: NavController,
    onSaveTranscript: (String) -> Unit
) {
    val viewModel: AICallViewModel = viewModel()
    val callState by viewModel.callState.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }
    var pendingTranscript by remember { mutableStateOf("") }

    // Start the call when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.startCall()
    }

    // When state becomes Ended, show the save dialog
    LaunchedEffect(callState) {
        if (callState is CallState.Ended) {
            pendingTranscript = (callState as CallState.Ended).transcript
            showSaveDialog = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Timer top right
        Text(
            text = viewModel.formatDuration(durationSeconds),
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(20.dp)
        )

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Notera AI",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = when (callState) {
                    is CallState.Connecting -> "Connecting..."
                    is CallState.Listening -> "Listening..."
                    is CallState.AISpeaking -> "Speaking..."
                    is CallState.Muted -> "Muted"
                    is CallState.Ended -> "Call ended"
                },
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 15.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (callState is CallState.Connecting) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(160.dp),
                    strokeWidth = 3.dp
                )
            } else {
                PulsingCircle(
                    isSpeaking = callState is CallState.AISpeaking,
                    baseColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(64.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Mute button
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.15f),
                modifier = Modifier.size(64.dp),
                onClick = { viewModel.toggleMute() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (callState is CallState.Muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // End call button (red)
            Surface(
                shape = CircleShape,
                color = Color(0xFFD32F2F),
                modifier = Modifier.size(72.dp),
                onClick = { viewModel.endCall() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.CallEnd,
                        contentDescription = "End call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // Save dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { /* don't dismiss on outside tap */ },
            title = { Text("Save this conversation?") },
            text = { Text("Save the transcript as a note you can edit and revisit later.") },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    onSaveTranscript(pendingTranscript)
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    navController.popBackStack()
                }) {
                    Text("Discard")
                }
            }
        )
    }
}
```

- [ ] **Step 2: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

## Chunk 3: Integration — Route, FAB, Save Flow

### Task 6: Add `AICall` Route to Navigation

**Files:**
- Modify: `app/src/main/java/com/example/devaudioreccordings/Routes.kt`

- [ ] **Step 1: Add `AICall` to the `Routes` enum**

In `Routes.kt`, find:
```kotlin
enum class Routes {
    Homepage, ListRecordings, EditPage, AddMediaPage, AIGeneratedText, Settings, PrivacyPolicy, FloatingUI
}
```

Change to:
```kotlin
enum class Routes {
    Homepage, ListRecordings, EditPage, AddMediaPage, AIGeneratedText, Settings, PrivacyPolicy, FloatingUI, AICall
}
```

- [ ] **Step 2: Add the `AICallScreen` composable to the `NavHost` in `Navigation()`**

In `Routes.kt`, inside the `NavHost { }` block, add after the last `composable(...)` call (before the closing `}`):

```kotlin
composable(route = Routes.AICall.name) {
    AICallScreen(
        navController = navigationController,
        onSaveTranscript = { transcript ->
            CoroutineScope(Dispatchers.IO).launch {
                val id = appViewModel.addInitialTextData(
                    header = "AI Conversation",
                    text = transcript
                )
                withContext(Dispatchers.Main) {
                    navigationController.navigate(
                        Routes.EditPage.name + "?id=$id&flow=${Flows.AddText.name}"
                    )
                }
            }
        }
    )
}
```

Add the required imports at the top of `Routes.kt`:
```kotlin
import com.example.devaudioreccordings.pages.AICall.AICallScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
```

- [ ] **Step 3: Check `addInitialTextData` signature in `AppViewModel`**

Open `app/src/main/java/com/example/devaudioreccordings/viewModals/AppViewModel.kt` and find `addInitialTextData`. It may currently take no parameters (creating a blank note). You need a version that accepts `header` and `text`. If the existing method doesn't support this, add an overload:

```kotlin
suspend fun addInitialTextData(header: String, text: String): Int {
    val audioText = AudioText(
        text = text,
        audioFileName = null,
        header = header,
        subHeader = null,
        flowType = FlowType.AddText,
        isApiCallRequired = false,
        imageCollection = null,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    val dbData = convertAudioTextToAudioTextDbData(audioText)
    audioTextDao.insertData(dbData)
    return audioTextDao.getLatestCreatedId()
}
```

Add this method inside `AppViewModel` if the overload doesn't already exist.

- [ ] **Step 4: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 7: Add "Talk to AI" FAB Option

**Files:**
- Modify: `app/src/main/java/com/example/devaudioreccordings/pages/Homepage/ExtendableFAB.kt`

- [ ] **Step 1: Add the new FAB option to the `items` list in `ExpandableFAB`**

In `ExtendableFAB.kt`, find the `items` list (the `listOf(FabOption(...), ...)` block). After the last item (Floating Clipboard), add:

```kotlin
,
FabOption(
    icon = Icons.Default.Call,
    label = "Talk to AI",
    color = Color(0xFF1A6B8A),
    actionToExecute = {
        appViewModel.isNewTextCreated.value = false
        expanded = false
        navController.navigate(Routes.AICall.name)
    },
    content = {
        TalkToAIContent()
    }
)
```

Add the import for the Call icon at the top of the file:
```kotlin
import androidx.compose.material.icons.filled.Call
```

- [ ] **Step 2: Add the `TalkToAIContent` composable at the bottom of `ExtendableFAB.kt`**

```kotlin
@Composable
fun TalkToAIContent() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp)
            .verticalScroll(rememberScrollState(0))
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Talk to AI",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Live voice conversation with AI",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Text(
                text = "Start a real-time voice call with an AI assistant. Ask questions, brainstorm ideas, or just have a conversation. Optionally save the transcript as a note.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}
```

- [ ] **Step 3: Build to verify no compile errors**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 8: Add RECORD_AUDIO Permission Check Before Navigating to AICall

**Files:**
- Modify: `app/src/main/java/com/example/devaudioreccordings/pages/Homepage/ExtendableFAB.kt`

The app already requests `RECORD_AUDIO` for `MediaCaptureService`. The AI call also needs this permission. Add a runtime check in the FAB action before navigating:

- [ ] **Step 1: Update the "Talk to AI" FAB action to check permission**

Find the "Talk to AI" `FabOption` action you just added. Replace `actionToExecute`:

```kotlin
actionToExecute = {
    appViewModel.isNewTextCreated.value = false
    expanded = false
    val permissionStatus = androidx.core.content.ContextCompat.checkSelfPermission(
        context,
        android.Manifest.permission.RECORD_AUDIO
    )
    if (permissionStatus == android.content.pm.PackageManager.PERMISSION_GRANTED) {
        navController.navigate(Routes.AICall.name)
    } else {
        // The existing permission request flow in MainActivity handles RECORD_AUDIO.
        // Show a toast guiding the user.
        android.widget.Toast.makeText(
            context,
            "Microphone permission is required for AI calls",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}
```

- [ ] **Step 2: Build the full project**

```bash
./gradlew :app:assembleDebug
```
Expected: BUILD SUCCESSFUL.

---

### Task 9: Manual End-to-End Test

This feature requires a real device and a real Gemini API key. No automated test can fully cover the WebSocket + audio pipeline.

- [ ] **Step 1: Install the debug APK on a physical Android device**

```bash
./gradlew :app:installDebug
```

- [ ] **Step 2: Verify the "Talk to AI" FAB option appears**

Open the app → tap the FAB (+) → confirm "Talk to AI" is the 5th option with a phone icon.

- [ ] **Step 3: Verify the full call flow**

1. Tap "Talk to AI"
2. Confirm permission dialog if prompted — grant microphone access
3. Confirm the call screen appears (full screen, dark background, "Connecting...")
4. After ~1-2 seconds confirm it transitions to "Listening..."
5. Speak a short phrase (e.g., "Hello, can you hear me?")
6. Confirm the AI responds with voice audio and the circle pulses faster
7. Confirm the timer counts up in the top right
8. Tap the mute button — confirm icon changes to MicOff and state shows "Muted"
9. Tap mute again — confirm it returns to Listening
10. Tap the red End Call button
11. Confirm the "Save this conversation?" dialog appears
12. Tap "Save" → confirm you are navigated to EditPage with the transcript text
13. Tap back, start another call, end it, tap "Discard" → confirm return to Homepage

- [ ] **Step 4: Verify error handling**

Turn off WiFi/data before starting a call. Confirm the app handles the connection failure gracefully (no crash, returns to Homepage or shows a reasonable state).

---

## Summary

| Task | Status |
|------|--------|
| Task 1: API key in BuildConfig | `- [ ]` |
| Task 2: GeminiLiveClient + unit tests | `- [ ]` |
| Task 3: CallState + AICallViewModel + unit tests | `- [ ]` |
| Task 4: PulsingCircle animation | `- [ ]` |
| Task 5: AICallScreen UI | `- [ ]` |
| Task 6: Route + NavHost integration | `- [ ]` |
| Task 7: FAB option | `- [ ]` |
| Task 8: Permission check | `- [ ]` |
| Task 9: Manual E2E test | `- [ ]` |
