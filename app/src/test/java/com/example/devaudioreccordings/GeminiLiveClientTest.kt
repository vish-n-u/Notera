package com.example.devaudioreccordings

import com.example.devaudioreccordings.pages.AICall.GeminiLiveClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiLiveClientTest {

    @Test
    fun `buildSetupMessage returns valid JSON with model and system instruction`() {
        val json = GeminiLiveClient.buildSetupMessage("Be helpful.")
        assertTrue(json.contains("\"setup\""))
        assertTrue(json.contains("models/gemini-2.0-flash-live-001"))
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
