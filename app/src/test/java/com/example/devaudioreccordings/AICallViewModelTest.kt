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
