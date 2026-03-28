package com.example.devaudioreccordings.pages.AICall

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.util.Base64
import java.util.concurrent.TimeUnit

data class ServerMessage(
    val audioBase64: String? = null,
    val transcript: String? = null,
    val turnComplete: Boolean = false,
    val interrupted: Boolean = false,
    val isThinking: Boolean = false
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
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun connect() {
         val url = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta.GenerativeService.BidiGenerateContent?key=$apiKey"
        val request = Request.Builder().url(url).build()
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                android.util.Log.d("GeminiLiveClient", "WebSocket opened — sending setup")
                webSocket.send(buildSetupMessage("You are a helpful AI assistant. Keep your responses conversational, warm, and concise as if talking on a phone call."))
                // Do NOT call onOpen() here — wait for setupComplete from Gemini
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                android.util.Log.d("GeminiLiveClient", "TEXT message: ${text.take(300)}")
                if (text.contains("setupComplete")) {
                    android.util.Log.d("GeminiLiveClient", "Setup complete (text) — starting mic")
                    onOpen()
                    return
                }
                val parsed = parseServerMessage(text)
                android.util.Log.d("GeminiLiveClient", "Parsed: audio=${parsed.audioBase64 != null}, turnComplete=${parsed.turnComplete}, interrupted=${parsed.interrupted}")
                onMessage(parsed)
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                // Gemini may send responses as binary frames — convert to UTF-8 and process
                val text = bytes.utf8()
                android.util.Log.d("GeminiLiveClient", "BINARY message (${bytes.size} bytes): ${text.take(300)}")
                if (text.contains("setupComplete")) {
                    android.util.Log.d("GeminiLiveClient", "Setup complete (binary) — starting mic")
                    onOpen()
                    return
                }
                val parsed = parseServerMessage(text)
                android.util.Log.d("GeminiLiveClient", "Parsed: audio=${parsed.audioBase64 != null}, turnComplete=${parsed.turnComplete}, interrupted=${parsed.interrupted}")
                onMessage(parsed)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                android.util.Log.e("GeminiLiveClient", "WebSocket failure: ${t.message}, response code: ${response?.code}")
                onError(t)
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("GeminiLiveClient", "WebSocket closing: code=$code reason=$reason")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                android.util.Log.d("GeminiLiveClient", "WebSocket closed: code=$code reason=$reason")
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
            val gson = com.google.gson.Gson()
            val root = com.google.gson.JsonObject()
            val setup = com.google.gson.JsonObject()
            setup.addProperty("model", "models/gemini-2.5-flash-native-audio-preview-12-2025")

            val generationConfig = com.google.gson.JsonObject()
            val modalities = com.google.gson.JsonArray()
            modalities.add("AUDIO")
            generationConfig.add("responseModalities", modalities)
            setup.add("generationConfig", generationConfig)

            val systemInstruction = com.google.gson.JsonObject()
            val parts = com.google.gson.JsonArray()
            val part = com.google.gson.JsonObject()
            part.addProperty("text", systemPrompt)
            parts.add(part)
            systemInstruction.add("parts", parts)
            setup.add("systemInstruction", systemInstruction)

            root.add("setup", setup)
            return gson.toJson(root)
        }

        fun buildAudioChunkMessage(pcmBytes: ByteArray): String {
            val base64 = Base64.getEncoder().encodeToString(pcmBytes)
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
                    ?: return ServerMessage(turnComplete = turnComplete, isThinking = !turnComplete)

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
                android.util.Log.e("GeminiLiveClient", "Failed to parse server message: ${e.message}", e)
                ServerMessage()
            }
        }
    }
}
