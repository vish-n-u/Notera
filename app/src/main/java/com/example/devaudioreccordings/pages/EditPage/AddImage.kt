package com.example.devaudioreccordings.pages.EditPage

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.skydoves.landscapist.glide.GlideImage
import java.io.File


@Composable
fun ImageGallerySection(
    imageFilePathList: MutableList<String>,
    onRemoveImage: (String) -> Unit,
    onAddImage: () -> Unit,
    textPrimaryColor: Color,
    textSecondaryColor: Color,
    primaryColor: Color
) {
    val context = LocalContext.current

    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(5)
    ) { uriList ->
        if (uriList.isNotEmpty()) {
            uriList.forEach { uri ->
                val filename = System.currentTimeMillis().toString()
                val imageDirectory = File(context.getExternalFilesDir(null), "ImageDirectory")
                if (!imageDirectory.exists()) {
                    imageDirectory.mkdirs()
                }
                val file = convertImageToSave(context, uri, filename)
                file?.let {
                    imageFilePathList.add(it.name)
                }
            }
        } else {
        }
    }
    return Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Images",
                style = MaterialTheme.typography.titleMedium,
                color = textPrimaryColor,
                fontWeight = FontWeight.SemiBold
            )

            Button(
                onClick = {
                    pickMedia.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add image"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Image")
            }
        }

        // Image grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 300.dp)
        ) {
            items(imageFilePathList.size) { imagePath ->
                val context = LocalContext.current
                val imageDirectory = File(context.getExternalFilesDir(null), "ImageDirectory")
                val file = File(imageDirectory, imageFilePathList[imagePath])
                Card(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 2.dp
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Using GlideImage instead of AsyncImage
                        GlideImage(
                            imageModel = { file.absolutePath },  // Pass as a lambda for better state handling

                            modifier = Modifier.fillMaxSize(),
                            // Loading placeholder
                            loading = {
                                Box(modifier = Modifier.matchParentSize()) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.align(Alignment.Center),
                                        color = primaryColor
                                    )
                                }
                            },
                            // Error fallback
                            failure = {
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .background(Color.Gray.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = "Failed to load image",
                                        tint = textSecondaryColor
                                    )
                                }
                            }
                        )

                        // Delete button overlay
                        IconButton(
                            onClick = {
                                imageFilePathList.removeAt(imagePath)
                                 },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(28.dp)
                                .background(
                                    Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove image",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Empty state
            if (imageFilePathList.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = textSecondaryColor.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "No images added yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textSecondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}


fun convertImageToSave(context: Context, uri: Uri, filename: String): File? {
    try {
        val contentResolver = context.contentResolver
        val tempFile = File(File(context.getExternalFilesDir(null), "ImageDirectory"), filename)
        contentResolver.openInputStream(uri).use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream?.copyTo(outputStream)
            }
        }
        return tempFile
    } catch (e: Throwable) {
        Log.d("e", e.message.toString())
        return null
    }
}