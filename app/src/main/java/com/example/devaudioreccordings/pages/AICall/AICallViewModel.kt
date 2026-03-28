package com.example.devaudioreccordings.pages.AICall

import android.app.Application
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myapp.ridescribe.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed class CallState {
    object Connecting : CallState()
    object Listening : CallState()
    object Thinking : CallState()
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

    // Fix 5: @Volatile on all shared mutable fields accessed from multiple threads
    @Volatile private var audioRecord: AudioRecord? = null
    @Volatile private var audioTrack: AudioTrack? = null
    @Volatile private var geminiClient: GeminiLiveClient? = null
    @Volatile private var isMuted = false

    private var micCaptureJob: Job? = null
    private var timerJob: Job? = null
    private var silenceWatcherJob: Job? = null

    // Timestamp of last mic chunk with speech-level amplitude; 0 = user hasn't spoken yet
    @Volatile private var lastSpeechTimeMs = 0L
    private val SPEECH_RMS_THRESHOLD = 600   // below this = silence (PCM 16-bit, ~2% of max)
    private val THINKING_SILENCE_MS   = 800L // silence duration before showing Thinking

    private val transcriptLines = mutableListOf<String>()

    // Fix 1: AtomicBoolean guard to prevent double-execution of endCall()
    private val hasEnded = java.util.concurrent.atomic.AtomicBoolean(false)

    // Fix 2: Store the pre-mute state so unmuting restores the correct state
    private var stateBeforeMute: CallState = CallState.Listening

    // Fix 3: Single channel for serialized AudioTrack writes
    private val audioChannel = kotlinx.coroutines.channels.Channel<ByteArray>(
        capacity = kotlinx.coroutines.channels.Channel.UNLIMITED
    )

    fun startCall() {
        _callState.value = CallState.Connecting
        transcriptLines.clear()
        _callDurationSeconds.value = 0
        lastSpeechTimeMs = 0

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
        // Fix 3: Start the single audio-playback consumer before connecting
        startAudioPlayback()
        geminiClient?.connect()
    }

    private fun handleServerMessage(msg: ServerMessage) {
        Log.d("AICallViewModel", "handleServerMessage: audio=${msg.audioBase64 != null}, turnComplete=${msg.turnComplete}, interrupted=${msg.interrupted}")
        when {
            msg.interrupted -> {
                audioTrack?.pause()
                audioTrack?.flush()
                _callState.value = CallState.Listening
            }
            msg.audioBase64 != null -> {
                _callState.value = CallState.AISpeaking
                playAudioChunk(msg.audioBase64)
            }
            msg.turnComplete -> {
                lastSpeechTimeMs = 0 // reset so watcher waits for user to speak again
                _callState.value = CallState.Listening
            }
            msg.isThinking -> {
                // Only enter Thinking from Listening — not mid-speech between audio batches
                if (_callState.value is CallState.Listening) {
                    _callState.value = CallState.Thinking
                }
            }
        }
        msg.transcript?.let { transcriptLines.add("AI: $it") }
    }

    // Fix 2: Restore pre-mute state on unmute
    fun toggleMute() {
        if (_callState.value is CallState.Ended) return
        if (!isMuted) {
            stateBeforeMute = _callState.value
            isMuted = true
            _callState.value = CallState.Muted
        } else {
            isMuted = false
            _callState.value = stateBeforeMute
        }
    }

    // Fix 1: Guard against double-execution with AtomicBoolean
    fun endCall() {
        if (!hasEnded.compareAndSet(false, true)) return
        micCaptureJob?.cancel()
        timerJob?.cancel()
        silenceWatcherJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        stopPlayback()
        audioChannel.close()
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
        // Fix 4: Guard AudioRecord initialization before starting recording
        if (audioRecord?.state == AudioRecord.STATE_INITIALIZED) {
            // Suppress acoustic echo: mic otherwise picks up the loudspeaker output and
            // sends it back to Gemini, causing false voice-activity triggers / interruptions
            if (android.media.audiofx.AcousticEchoCanceler.isAvailable()) {
                android.media.audiofx.AcousticEchoCanceler
                    .create(audioRecord!!.audioSessionId)
                    ?.enabled = true
                Log.d("AICallViewModel", "AcousticEchoCanceler enabled")
            } else {
                Log.w("AICallViewModel", "AcousticEchoCanceler not available on this device")
            }
            Log.d("recording","recording started from mic")
            audioRecord?.startRecording()
        } else {
            android.util.Log.e("AICallViewModel", "AudioRecord failed to initialize")
            audioRecord?.release()
            audioRecord = null
            return
        }

        Log.d("geminiClient",geminiClient.toString())

        micCaptureJob = viewModelScope.launch(Dispatchers.IO) {
            val buffer = ByteArray(CHUNK_SIZE_BYTES)
            while (isActive) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: break
                if (read > 0 && !isMuted) {
                    if (rms(buffer, read) > SPEECH_RMS_THRESHOLD) {
                        lastSpeechTimeMs = System.currentTimeMillis()
                    }
                    geminiClient?.sendAudioChunk(buffer.copyOf(read))
                }
            }
        }

        // Transition Listening → Thinking after user goes quiet
        silenceWatcherJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(200)
                val spokenBefore = lastSpeechTimeMs > 0
                val silentLongEnough = System.currentTimeMillis() - lastSpeechTimeMs > THINKING_SILENCE_MS
                if (spokenBefore && silentLongEnough && _callState.value is CallState.Listening) {
                    _callState.value = CallState.Thinking
                }
            }
        }
    }

    private fun rms(buffer: ByteArray, size: Int): Int {
        var sum = 0L
        var i = 0
        while (i < size - 1) {
            val sample = (buffer[i + 1].toInt() shl 8) or (buffer[i].toInt() and 0xFF)
            sum += sample.toLong() * sample
            i += 2
        }
        return Math.sqrt((sum / (size / 2)).toDouble()).toInt()
    }

    private fun startTimer() {
        timerJob = viewModelScope.launch {
            while (isActive) {
                kotlinx.coroutines.delay(1000)
                _callDurationSeconds.value += 1
            }
        }
    }

    // Fix 3: Send PCM bytes into the channel instead of launching a new coroutine per chunk
    private fun playAudioChunk(base64Audio: String) {
        val pcmBytes = java.util.Base64.getDecoder().decode(base64Audio)
        Log.d("AICallViewModel", "Queuing audio chunk: ${pcmBytes.size} bytes")
        audioChannel.trySend(pcmBytes)
    }

    // Fix 3: Single coroutine consumes the channel and writes to AudioTrack serially
    private fun startAudioPlayback() {
        viewModelScope.launch(Dispatchers.IO) {
            initAudioTrack()
            for (pcmBytes in audioChannel) {
                if (audioTrack == null) {
                    initAudioTrack()
                }
                // Resume if paused from a prior interruption — otherwise writes are silent
                if (audioTrack?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                    audioTrack?.play()
                }
                audioTrack?.write(pcmBytes, 0, pcmBytes.size)
            }
            // Channel closed (call ended) — clean up AudioTrack
            stopPlayback()
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
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
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

    // Fix 3: stopPlayback no longer cancels the channel (channel lives with the ViewModel)
    private fun stopPlayback() {
        audioTrack?.pause()
        audioTrack?.flush()
        audioTrack?.release()
        audioTrack = null
    }

    // Fix 1: onCleared simply calls endCall(); the AtomicBoolean guard handles idempotency
    override fun onCleared() {
        super.onCleared()
        endCall()
    }

}
