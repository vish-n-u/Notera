package com.example.devaudioreccordings.pages.EditPage

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.viewModals.AppViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Helper component for section tabs
@Composable
 fun SectionTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color,
    textSecondaryColor: Color
) {
    val backgroundColor = if (isSelected) {
        primaryColor.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }

    val contentColor = if (isSelected) {
        primaryColor
    } else {
        textSecondaryColor
    }

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

// Utility function for formatting milliseconds
 fun formatMillisToTimeString(millis: Int): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return "%d:%02d".format(minutes, remainingSeconds)
}

// Helper function to get audio file
 fun getAudioFile(context: Context, fileName: String): File? {
    if (fileName.isEmpty()) return null
    val audioCapturesDirectory = File(context.getExternalFilesDir(null), "AudioCaptures")
    val file = File(audioCapturesDirectory, fileName)
    return if (file.exists()) file else null
}

// Helper function to format duration
fun formatDuration(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

// Helper function to format timestamp
 fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val formatter = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
    return formatter.format(date)
}

@Composable
fun AnimatedDots(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dots by infiniteTransition.animateValue(
        initialValue = 1,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )

    Text(
        text = ".".repeat(dots % 4),
        style = MaterialTheme.typography.bodySmall,
        color = color,
        fontWeight = FontWeight.Bold
    )
}

@Composable
fun ThemedShimmerEffect(modifier: Modifier = Modifier, primaryColor: Color) {
    // Create shimmer colors using the theme primary color
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        primaryColor.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(10f, 10f),
        end = Offset(translateAnim.value, translateAnim.value)
    )

    Column(modifier = modifier) {
        // Title line shimmer
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )

        // Content lines shimmer
        repeat(5) {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(brush)
            )
        }

        // Last line shimmer (shorter)
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )

        // Add a card with AI status at the bottom
        Spacer(modifier = Modifier.height(24.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = primaryColor.copy(alpha = 0.1f)
            ),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "AI is generating your text",
                    style = MaterialTheme.typography.bodySmall,
                    color = primaryColor,
                    fontWeight = FontWeight.Medium
                )
                AnimatedDots(primaryColor)
            }
        }
    }
}

@Composable
fun ShowSaveChangesDialogBox(
    showDialog: MutableState<Boolean>,
    onSave: () -> Unit,
    onDiscard: () -> Unit
) {
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            title = {
                Text(
                    text = "Unsaved Changes",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "You have unsaved changes. Do you want to save before leaving?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        onSave()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "Save")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDialog.value = false
                        onDiscard()
                    }
                ) {
                    Text(text = "Discard")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp
        )
    }
}


@Composable
fun ShowGenerationHappeningDialogBox(
    showDialog: MutableState<Boolean>,
    process:String,
    onStay: () -> Unit,
    onLeave: () -> Unit
) {
//    val process = "summary" // or "transcription"
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            title = {
                Text(
                    text = "$process Generation is ongoing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Normal
                )
            },
            text = {
                Text(
                    text = "Leaving now will stop the generation. Are you sure you want to exit?",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog.value = false
                        onStay()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(text = "No, stay")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showDialog.value = false
                        onLeave()
                    }
                ) {
                    Text(text = "Yes, leave")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 4.dp
        )
    }
}


fun checkifDataHasChangedOrTextFlow(
    isNewTextCreated:Boolean,
    audioData: AudioText,
    headerText: String,
    subHeaderText:String,
    flow: String,
    audioFile: File?,
): Boolean {
    var subHeaderCheck  =audioData.subHeader
    if(audioData.subHeader==null){
        subHeaderCheck = ""
    }

    Log.d("actualText==>",audioData.text)

    if(isNewTextCreated) return true
    if (audioData.header != headerText) return true
    if(subHeaderCheck!= subHeaderText) return true
    if(audioFile==null&&audioData.audioFileName!=null) return true
    return false
}



@Composable
fun ShowDeleteDialog(
    showDeleteDialog: MutableState<Boolean>,
    viewModel: AppViewModel,
    audioData: AudioText,
    navController: NavController,
    errorColor: Color,
    removeImages :()->Unit // remove the images held in memory
) {
    return AlertDialog(
        onDismissRequest = { showDeleteDialog.value = false },
        title = { Text("Delete Recording", fontWeight = FontWeight.Bold) },
        text = { Text("Are you sure you want to delete this recording? This action cannot be undone.") },
        confirmButton = {
            Button(
                onClick = {
                    removeImages()
                    viewModel.deleteData(audioData!!)
                    navController.popBackStack()
                    showDeleteDialog.value = false
                    // Here would go the actual delete functionality

                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = errorColor
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { showDeleteDialog.value = false }
            ) {
                Text("Cancel")
            }
        }
    )
}
