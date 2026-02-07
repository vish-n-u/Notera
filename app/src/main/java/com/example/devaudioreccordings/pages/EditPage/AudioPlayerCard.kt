
import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.pages.EditPage.formatMillisToTimeString
import java.io.File


@Composable
fun AudioPlayerCard(
    surfaceColor: Color,
    primaryColor: Color,
    isPlaying: MutableState<Boolean>,
    mediaPlayer: MutableState<MediaPlayer?>,
    totalDuration: MutableState<Int>,
    audioFile: File,
    audioData: AudioText,
    elapsedTime: MutableState<Int>,
    textSecondaryColor: Color,
    onDeleteAudio: () -> Unit // New parameter for delete action
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        // Audio visualization
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(surfaceColor)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            // Audio waveform visualization placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(30) { index ->
                    val height = (20 + (index % 5) * 10).dp
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(height)
                            .background(
                                primaryColor.copy(
                                    alpha = if (isPlaying.value) 0.8f else 0.4f
                                ),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Audio controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/pause button
            IconButton(
                onClick = {
                    if (isPlaying.value) {
                        mediaPlayer.value?.pause()
                        isPlaying.value = false
                    } else {
                        if (mediaPlayer.value == null) {
                            mediaPlayer.value =
                                MediaPlayer().apply {
                                    setDataSource(audioFile!!.absolutePath)
                                    prepare()
                                    totalDuration.value = duration
                                }
                        }
                        mediaPlayer.value?.start()
                        isPlaying.value = true
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(primaryColor, CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying.value) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying.value) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Progress slider
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Slider(
                    value = elapsedTime.value.toFloat(),
                    onValueChange = { newValue ->
                        elapsedTime.value = newValue.toInt()
                        mediaPlayer.value?.seekTo(newValue.toInt())
                    },
                    valueRange = 0f..totalDuration.value.toFloat()
                        .coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = primaryColor,
                        activeTrackColor = primaryColor,
                        inactiveTrackColor = textSecondaryColor.copy(
                            alpha = 0.3f
                        )
                    )
                )

                // Time indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        formatMillisToTimeString(elapsedTime.value),
                        style = MaterialTheme.typography.labelMedium,
                        color = textSecondaryColor
                    )

                    Text(
                        formatMillisToTimeString(totalDuration.value),
                        style = MaterialTheme.typography.labelMedium,
                        color = textSecondaryColor
                    )
                }
            }

            // Delete button
            IconButton(
                onClick = {
                    // Stop and release media player before deleting
                    mediaPlayer.value?.apply {
                        if (isPlaying.value) {
                            stop()
                            isPlaying.value = false
                        }
                        release()
                        mediaPlayer.value = null
                    }
                    onDeleteAudio()
                },
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete audio",
                    tint = textSecondaryColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
