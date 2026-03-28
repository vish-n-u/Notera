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

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%d:%02d".format(m, s)
}

@Composable
fun AICallScreen(
    navController: NavController,
    onSaveTranscript: (String) -> Unit
) {
    val viewModel: AICallViewModel = viewModel()
    val callState by viewModel.callState.collectAsState()
    val durationSeconds by viewModel.callDurationSeconds.collectAsState()
    var showSaveDialog by remember { mutableStateOf(false) }

    // Start the call when the screen is first composed
    LaunchedEffect(Unit) {
        viewModel.startCall()
    }

    // When state becomes Ended, show the save dialog (only if transcript is non-empty)
    LaunchedEffect(callState) {
        if (callState is CallState.Ended) {
            val transcript = (callState as CallState.Ended).transcript
            if (transcript.isNotBlank()) {
                showSaveDialog = true
            } else {
                // Empty transcript — connection failed or call too short, just go back
                navController.popBackStack()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D0D))
    ) {
        // Timer top right
        Text(
            text = formatDuration(durationSeconds),
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
                    is CallState.Thinking -> "Thinking..."
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
                    isThinking = callState is CallState.Thinking,
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
                color = Color.White.copy(alpha = if (callState !is CallState.Ended) 0.15f else 0.05f),
                modifier = Modifier.size(64.dp),
                onClick = { viewModel.toggleMute() },
                enabled = callState !is CallState.Ended
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (callState is CallState.Muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (callState is CallState.Muted) "Unmute" else "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // End call button (red)
            Surface(
                shape = CircleShape,
                color = if (callState !is CallState.Ended) Color(0xFFD32F2F) else Color(0xFF8B0000),
                modifier = Modifier.size(72.dp),
                onClick = { viewModel.endCall() },
                enabled = callState !is CallState.Ended
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
                    onSaveTranscript((callState as CallState.Ended).transcript)
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
