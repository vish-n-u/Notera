package com.example.devaudioreccordings.pages.Homepage

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavController
import com.canopas.lib.showcase.IntroShowcase
import com.canopas.lib.showcase.component.ShowcaseStyle
import com.example.devaudioreccordings.DataStoreKeys
import com.example.devaudioreccordings.Flows
import com.example.devaudioreccordings.Routes
import com.example.devaudioreccordings.services.MediaCaptureService
import com.example.devaudioreccordings.user
import com.example.devaudioreccordings.viewModals.AppViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ExpandableFAB(
    runMediaCaptureService: () -> Unit,
    stopMediaCapture: () -> Unit,
    requestMediaAccess: () -> Unit,
    startFloatingTextWindowService: () -> Unit,
    appViewModel: AppViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var isServiceRunning by remember { mutableStateOf(0) }
    val materialTheme = MaterialTheme.colorScheme
    var showAppIntro by remember {
        mutableStateOf(true)
    }
    LaunchedEffect(Unit) {
        val prefs = context.user.data.first()
        val hasShownAppIntro = prefs[DataStoreKeys.Has_Shown_App_Intro]?:false

        showAppIntro = !hasShownAppIntro
//        showAppIntro = true
    }


    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 135f else 0f,
        animationSpec = tween(durationMillis = 300)
    )

    fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in activityManager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }


    // Options to be displayed when FAB is expanded
    val items = listOf(
        FabOption(
            icon = Icons.Filled.PhoneAndroid, // Represents device audio
            label = "Transcribe System Audio",
            color = Color(0xFF4C662B), // Using tertiary color for first option
            actionToExecute = {
                appViewModel.isNewTextCreated.value = false
                // throw Error("Run Time Crash Analytics Test") // Keep or remove based on need
                if (!isServiceRunning(context, MediaCaptureService::class.java)) {
                    runMediaCaptureService()
                } else {
                    stopMediaCapture()
                }
                expanded = false
            },
            content = {
                RecordSystemAudioContentCompact()

            }
        ),
        FabOption(
            icon = Icons.Filled.PermMedia,
            label = "Add Media",
            color = Color(0xFF586249), // Using secondary color for second option
            actionToExecute = {
                appViewModel.isNewTextCreated.value = false
                requestMediaAccess()
                expanded = false
            },
            content = {
                AddMediaContent()


            }
        ),
        FabOption(
            icon = Icons.Default.Create,
            label = "Create Text",
            color = Color(0xFF386663), // Using primary color for third option
            actionToExecute = {
                CoroutineScope(Dispatchers.IO).launch {
                    appViewModel.isNewTextCreated.value = true
                    val latestId = appViewModel.addInitialTextData()
                    withContext(Dispatchers.Main) {
                        navController.navigate(Routes.EditPage.name + "?id=" + latestId.toString() + "&flow=${Flows.AddText.name}")
                    }
                }
            },
            content = {
                CreateTextContent()

            }
        ),
        FabOption(
            icon = Icons.Default.ContentPaste,
            label = "Floating Clipboard",
            color = Color(0xFF4C662B), // Using tertiary color (example)
            actionToExecute = {
                startFloatingTextWindowService()
                appViewModel.isNewTextCreated.value = false
                expanded = false
            },
            content = {
                FloatingClipboardContent()

            }
        )
    )


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Overlay for closing expanded FAB with animation
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(animationSpec = tween(durationMillis = 200)),
            exit = fadeOut(animationSpec = tween(durationMillis = 200))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(materialTheme.scrim.copy(alpha = 0.5f)) // Using scrim with alpha
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = false }
                    .zIndex(1f)
            )
        }
        IntroShowcase(
            showIntroShowCase = showAppIntro,
            dismissOnClickOutside = false,
            onShowCaseCompleted = {
                //App Intro finished!!
                showAppIntro = false
                CoroutineScope(Dispatchers.IO).launch {
                    val prefs = context.user
                    prefs.edit {
                       it[DataStoreKeys.Has_Shown_App_Intro]= true
                    }
                }
            },
        ) {
            // Expandable options with animation
            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
                modifier = Modifier.zIndex(2f)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier
                        .padding(bottom = 88.dp, end = 16.dp)
                ) {
                    items.forEachIndexed { index, item ->
                        // Stagger the appearance of options
                        val animDelay = 50 * index
                        AnimatedVisibility(
                            visible = expanded,
                            modifier = Modifier.introShowCaseTarget(
                                index = index,
                                style = ShowcaseStyle.Default.copy(
                                    backgroundColor = Color(0xAA1C0A00), // specify color of background
                                    backgroundAlpha = 0.98f, // specify transparency of background
                                    targetCircleColor = Color.White // specify color of target circle,

                                ),
                                // specify the content to show to introduce app feature
                                content = {
                                    item.content()
                                }
                            ),
                            enter = fadeIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = animDelay
                                )
                            ) + scaleIn(
                                animationSpec = tween(
                                    durationMillis = 300,
                                    delayMillis = animDelay
                                )
                            ),
                            exit = fadeOut() + scaleOut()
                        ) {
                            FabOptionItem(
                                item = item,
                                onItemClick = { item.actionToExecute() }
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // Main FAB with elevation and ripple effect
            FloatingActionButton(
                onClick = { expanded = !expanded },
                containerColor = materialTheme.primaryContainer, // Using primaryContainer color
                contentColor = materialTheme.onPrimaryContainer, // Using onPrimaryContainer for content
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 6.dp,
                    pressedElevation = 12.dp
                ),
                modifier = Modifier
                    .padding(16.dp)
                    .shadow(8.dp, CircleShape)
                    .zIndex(3f),
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Expandable Menu",
                    modifier = Modifier
                        .size(28.dp)
                        .rotate(rotationState)
                    // Content color is already set by FloatingActionButton's contentColor parameter
                )
            }
        }
    }
}

@Composable
fun FabOptionItem(
    item: FabOption,
    onItemClick: () -> Unit
) {
    val materialTheme = MaterialTheme.colorScheme

    Surface(
        onClick = onItemClick,
        shape = RoundedCornerShape(16.dp),
        color = materialTheme.surfaceVariant, // Using surfaceVariant for background
        shadowElevation = 4.dp,
        tonalElevation = 4.dp,
        modifier = Modifier
            .padding(end = 8.dp)
            .animateContentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.labelLarge,
                color = materialTheme.onSurfaceVariant // Using onSurfaceVariant for text
            )

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(item.color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = Color.White, // Keep white for contrast on colored backgrounds
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

data class FabOption(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val actionToExecute: () -> Unit,
    val content: @Composable () -> Unit
)


@Composable
fun lesser() {
    Text("Data")
}

@Composable
fun RecordSystemAudioContentCompact() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp).verticalScroll(rememberScrollState(0))
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
            // Header with icon and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(Color(0xFF6200EE), Color(0xFF9C27B0))
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.GraphicEq,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = "System Audio Recording",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Features in a compact grid
            LazyVerticalGrid (
                columns = GridCells.Fixed(2),
                modifier = Modifier.heightIn(max = 100.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        "📱 Transcribe Audio" to "Convert app audio to text",
                        "📹 Screenshots + Audio" to "Record with visual context",
                        "🏷️ Categorize" to "Smart organization using Headers and Sub-headers",
                        "🔴 Live Indicator" to "Recording status"
                    )
                ) { (title, desc) ->
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddMediaContent() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 200.dp).verticalScroll(rememberScrollState(0))
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Alternative Media Sources",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            // Compact problem statement
            Text(
                text = "When apps block audio recording, try these alternatives:",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )

            // Two main options in rows
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Screen recorder option
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MovieCreation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Screen Recorder",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Text(
                        text = "Use built-in recorder, then upload the file",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }

                // Screenshots option
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Screenshots",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                    Text(
                        text = "Attach relevant images during editing",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        lineHeight = 12.sp
                    )
                }
            }

            // Compact tip
            Text(
                text = "💡 Perfect when audio recording is blocked by app restrictions",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun CreateTextContent() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 240.dp).verticalScroll(rememberScrollState(0))
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
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Create,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Create New Text",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = "Blank note for manual content creation",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            // Key features in horizontal layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "✏️" to "Rich Text",
                    "📷" to "Add Images",
                    "💡" to "Flexible Use"
                ).forEach { (emoji, title) ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Description
            Text(
                text = "Ideal for writing notes, summaries, transcripts, and more with formatting & image support.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center
            )

            // Tip
            Text(
                text = "💡 Link with other images during editing",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontStyle = FontStyle.Italic,
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
        }
    }
}


    @Composable
    fun FloatingClipboardContent() {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp).verticalScroll(rememberScrollState(0))
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
                // Header with floating indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        // Small floating indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Green, CircleShape)
                                .offset(12.dp, (-4).dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Floating Clipboard",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "Overlay window for multitasking",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Key features in horizontal layout
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "📝" to "Quick Notes",
                        "📋" to "Paste Content",
                        "👁️" to "Always Visible"
                    ).forEach { (emoji, title) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                textAlign = TextAlign.Center,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Description
                Text(
                    text = "Perfect for copying & pasting content from websites, PDFs, or reference apps while working in other applications.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "💡 Stays on top of all apps for seamless workflow",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontStyle = FontStyle.Italic,
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
