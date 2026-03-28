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
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PulsingCircle(
    isSpeaking: Boolean,
    isThinking: Boolean = false,
    size: Dp = 160.dp,
    baseColor: Color = MaterialTheme.colorScheme.primary
) {
    val duration = when {
        isSpeaking -> 600
        isThinking -> 900
        else -> 1400
    }
    val color = if (isThinking) Color(0xFFD4A017) else baseColor

    key(isSpeaking, isThinking) {
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
            val maxRadius = this.size.minDimension / 2
            // Cap base radius so outer ring (×1.3 ×1.15) never clips canvas
            val baseRadius = maxRadius / (1.3f * 1.15f)
            val radius = baseRadius * scale
            drawCircle(color = color.copy(alpha = alpha * 0.3f), radius = radius * 1.3f)
            drawCircle(color = color.copy(alpha = alpha * 0.6f), radius = radius * 1.1f)
            drawCircle(color = color.copy(alpha = alpha), radius = radius * 0.8f)
        }
    }
}
