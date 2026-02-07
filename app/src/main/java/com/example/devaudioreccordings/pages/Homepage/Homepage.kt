package com.example.devaudioreccordings.pages.Homepage


import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation.NavHostController
import com.example.devaudioreccordings.AppColorTheme
import com.example.devaudioreccordings.AppTheme
import com.example.devaudioreccordings.DataStoreKeys
import com.example.devaudioreccordings.RecordTranscription
import com.example.devaudioreccordings.Routes
import com.example.devaudioreccordings.TranscriptionState
import com.example.devaudioreccordings.UploadDetails
import com.example.devaudioreccordings.database.HeaderAndCreatedAt
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.myapp.ridescribe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@Composable
fun Homepage(
    runMediaCaptureService: () -> Unit,
    stopMediaCapture: () -> Unit,
    appViewModel: AppViewModel,
    requestMediaAccess: () -> Unit,
    startFloatingTextWindowService: () -> Unit,
    navigationController: NavHostController,
) {
    val searchData = remember { mutableStateOf("") }
    val fullData = appViewModel.fullHeaderData.collectAsState()
    val headerAndCreatedAtList = appViewModel.filterHeaders(fullData, searchData.value)
    val dataStoreData = appViewModel.dataStoreDataFlow.collectAsState(initial = emptyPreferences())
    val openUsageDialogBox = remember { mutableStateOf(false) }

    val transcriptionStatus = RecordTranscription.uploadDetails.collectAsState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(ScrollState(0))
                .padding(top = if (transcriptionStatus.value.count > 0) 36.dp else 8.dp) // Push content down if banner is shown
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            if (fullData.value.isNotEmpty()) {
                AnimatedSearchBar(
                    searchData,
                    isVisible = fullData.value.isNotEmpty(),
                    openUsageDialogBox,
                    navigationController
                )
            } else {
                SettingsGearUI(navigationController)
            }

            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(24.dp))

            if(fullData.value.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Header List",
                        fontSize = 22.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    HeaderCountBadge(count = headerAndCreatedAtList.value.size)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (headerAndCreatedAtList.value.isEmpty()) {
                EnhancedEmptyStateMessage(fullData.value.size)
            } else {
                if(transcriptionStatus.value.count>0) {
                    arrayOfNulls<String>(transcriptionStatus.value.count).map {
                        ShimmerHeaderCard()
                    }
                }
                headerAndCreatedAtList.value.forEachIndexed { index, headerAndCreatedAt ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(
                            tween(durationMillis = 300, delayMillis = index * 50)
                        ) + expandVertically(
                            tween(durationMillis = 300, delayMillis = index * 50)
                        )
                    ) {
                        EnhancedHeaderCard(headerAndCreatedAt, navigationController, appViewModel)
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }

        if (openUsageDialogBox.value) {
            Dialog(onDismissRequest = { openUsageDialogBox.value = false }) {
                EnhancedTranscriptionTimeUsage(dataStoreData)
            }
        }

        ExpandableFAB(
            runMediaCaptureService,
            stopMediaCapture,
            requestMediaAccess,
            startFloatingTextWindowService,
            appViewModel,
            navigationController
        )

        // 👉 NEW: Top-bar banner
        if (transcriptionStatus.value.count > 0) {
            TopBanner(transcriptionStatus.value.transcriptionState, transcriptionStatus.value , appViewModel)
        }
    }
}

@Composable
fun TopBanner(state: TranscriptionState, transcriptionStatus: UploadDetails, appViewModel: AppViewModel) {
    val isLightMode = appViewModel.appTheme.value == AppTheme.LIGHT.name
    val isDynamicColor = appViewModel.appColorTheme.value.toString() == AppColorTheme.SYSTEM.toString()

    val backgroundColor = when (state) {
        TranscriptionState.LOADING -> {
            if (isDynamicColor) {
                // Use Material Theme colors for dynamic theming
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                // Use fixed colors based on light/dark mode
                if (isLightMode) Color(0xFFFFF3C4) else Color(0xFF3E2723) // Darker pale yellow / Dark brown
            }
        }
        TranscriptionState.FAILURE -> {
            if (isDynamicColor) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                if (isLightMode) Color(0xFFFFCDD2) else Color(0xFF3E1723) // Darker pale red / Dark red
            }
        }
        TranscriptionState.PARTIALSUCCESS -> {
            if (isDynamicColor) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
            } else {
                if (isLightMode) Color(0xFFFFCDD2) else Color(0xFF3E1723) // Darker pale red / Dark red
            }
        }
        TranscriptionState.SUCCESS -> {
            if (isDynamicColor) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                if (isLightMode) Color(0xFFC8E6C9) else Color(0xFF1B3E1F) // Darker pale green / Dark green
            }
        }
    }

    val textColor = when (state) {
        TranscriptionState.LOADING -> {
            if (isDynamicColor) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                if (isLightMode) Color(0xFFFF8F00) else Color(0xFFFFCC02) // Darker amber / Light amber
            }
        }
        TranscriptionState.FAILURE -> {
            if (isDynamicColor) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                if (isLightMode) Color(0xFFC62828) else Color(0xFFFF6B6B) // Darker red / Light red
            }
        }
        TranscriptionState.PARTIALSUCCESS -> {
            if (isDynamicColor) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                if (isLightMode) Color(0xFFC62828) else Color(0xFFFF6B6B) // Darker red / Light red
            }
        }
        TranscriptionState.SUCCESS -> {
            if (isDynamicColor) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                if (isLightMode) Color(0xFF2E7D32) else Color(0xFF66BB6A) // Darker green / Light green
            }
        }
    }

    val message = when (state) {
        TranscriptionState.LOADING -> "${transcriptionStatus.count} Note generation in progress..."
        TranscriptionState.FAILURE -> "1 Note generation failed."
        TranscriptionState.PARTIALSUCCESS -> "1 Note generation partially succeeded. Your Free Transcription time might've exhausted"
        TranscriptionState.SUCCESS -> "1 Note generation successful."
    }


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            color = textColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}




@Composable
fun HeaderCountBadge(count: Int) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Text(
            text = "$count items",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun EnhancedEmptyStateMessage(fullDataLength: Int) {
    val isDataEmpty = fullDataLength == 0

    if (!isDataEmpty) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = "No search results",
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No headers found",
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Try a different search term or create a new note",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    } else {
        Spacer(modifier = Modifier.height(46.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.audiobook_pana),
                contentDescription = "Empty state illustration",
                modifier = Modifier.size(240.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Create Your First Note",
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimatedSearchBar(
    searchData: MutableState<String>,
    isVisible: Boolean,
    openUsageDialogBox: MutableState<Boolean>,
    navigationController: NavHostController
) {
    var isExpanded by remember { mutableStateOf(false) }
    var settingsClicked by remember { mutableStateOf(false) }


    // Animate visibility and expansion
    val animatedAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "alpha"
    )

    val animatedWidth by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0.15f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "width"
    )
    val rotationState by animateFloatAsState(
        targetValue = if (settingsClicked) 135f else 0f,
        animationSpec = tween(300)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(animatedAlpha)
            .padding(vertical = 8.dp),

        ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(animatedWidth).shadow(
                    if(isExpanded)2.dp else 0.dp,
                    RoundedCornerShape(28.dp),
                    ambientColor = Color.White,
                    spotColor = MaterialTheme.colorScheme.onSurface
                )
                .height(56.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(28.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { isExpanded = true }, modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search Icon",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                if (animatedWidth > 0.3f) {
                    BasicTextField(value = searchData.value,
                        onValueChange = { searchData.value = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                        textStyle = TextStyle(
                            fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box {
                                if (searchData.value.isEmpty()) {
                                    Text(
                                        text = "Search headers...",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                            alpha = 0.7f
                                        ),
                                        fontSize = 16.sp
                                    )
                                }
                                innerTextField()
                            }
                        })

                    if (searchData.value.isNotEmpty()) {
                        IconButton(onClick = { searchData.value = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (isExpanded) {
                        IconButton(onClick = { isExpanded = false }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close search",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }


                }

            }

        }


        if (!isExpanded) {
            SettingsGearUI(navigationController)
        }

    }

}

@Composable
fun EnhancedTranscriptionTimeUsage(dataStoreData: State<androidx.datastore.preferences.core.Preferences>) {
    var showInfoDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val usedTextEnhanceCount = dataStoreData.value[DataStoreKeys.Used_Enhance_Text_Count] ?: 0
    val totalTextEnhanceCount = dataStoreData.value[DataStoreKeys.Total_Enhance_Text_Count] ?: 10
    val uid = dataStoreData.value[DataStoreKeys.USER_UID] ?: ""

    val usedLinkedinTextEnhance =
        dataStoreData.value[DataStoreKeys.Used_Linkedin_Text_Conversion_Count] ?: 0
    val totalLinkedinTextEnhance =
        dataStoreData.value[DataStoreKeys.Total_Linkedin_Text_Conversion_Count] ?: 0

    val usedTranscriptionTimeInSec =
        dataStoreData.value[DataStoreKeys.Used_Transcription_Duration]?.let {
            milliSecToSec(it)
        } ?: 0
    val totalTranscriptionTimeInSec =
        dataStoreData.value[DataStoreKeys.Total_Transcription_Duration]?.let {
            milliSecToSec(it)
        } ?: 0

    val usedTranscriptionTimeInMin =
        dataStoreData.value[DataStoreKeys.Used_Transcription_Duration]?.let {
            milliSecToMin(it)
        } ?: 0
    val totalTranscriptionTimeInMin =
        dataStoreData.value[DataStoreKeys.Total_Transcription_Duration]?.let {
            milliSecToMin(it)
        } ?: 0

    val usedTranscriptionTime = if (totalTranscriptionTimeInSec > 0) {
        (usedTranscriptionTimeInSec.toFloat() / totalTranscriptionTimeInSec)
    } else {
        0f
    }
    val usedTextEnhanceCountForProgress = if (totalTextEnhanceCount > 0) {
        (usedTextEnhanceCount.toFloat() / totalTextEnhanceCount.toFloat())
    } else {
        0f
    }
    val usedLinkedinTextEnhanceCountForProgress = if (totalLinkedinTextEnhance > 0) {
        (usedLinkedinTextEnhance.toFloat() / totalLinkedinTextEnhance.toFloat())
    } else {
        0f
    }

    val usedTransitionTimeInPercentage = Math.round(usedTranscriptionTime.toDouble() * 100)

    val progressAnimationValueForTranscription by animateFloatAsState(
        targetValue = usedTranscriptionTime,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    // Info Dialog
    if (showInfoDialog) {
        AlertDialog(onDismissRequest = { showInfoDialog = false }, title = {
            Text(
                text = "Transcription Usage Details",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }, text = {
            Column {
                Text(
                    text = "Transcription Time Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                UsageDetailRow(
                    label = "Used Transcription Time", value = "$usedTranscriptionTimeInMin minutes"
                )
                UsageDetailRow(
                    label = "Total Transcription Time",
                    value = "$totalTranscriptionTimeInMin minutes"
                )
                UsageDetailRow(
                    label = "Percentage Used", value = "$usedTransitionTimeInPercentage%"
                )
            }
        }, confirmButton = {
            TextButton(onClick = { showInfoDialog = false }) {
                Text("Close")
            }
        })
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            // Transcription Usage Section
            UsageSection(
                title = "Transcription Usage",
                usedValue = usedTranscriptionTimeInMin,
                totalValue = totalTranscriptionTimeInMin,
                progressValue = progressAnimationValueForTranscription,
                percentageUsed = usedTransitionTimeInPercentage,
                unitLabel = "minutes"
            )

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // Text Enhance Usage Section
            UsageSection(
                title = "Text Enhance Usage",
                usedValue = usedTextEnhanceCount,
                totalValue = totalTextEnhanceCount,
                progressValue = usedTextEnhanceCountForProgress,
                percentageUsed = Math.round(usedTextEnhanceCountForProgress * 100),
                unitLabel = "times"
            )

            Divider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            )

            // LinkedIn Text Enhance Usage Section
            UsageSection(
                title = "LinkedIn Text Enhance Usage",
                usedValue = usedLinkedinTextEnhance,
                totalValue = totalLinkedinTextEnhance,
                progressValue = usedLinkedinTextEnhanceCountForProgress,
                percentageUsed = Math.round(usedLinkedinTextEnhanceCountForProgress * 100),
                unitLabel = "times"
            )

            // Request Message Button
            Button(
                onClick = {
                    val profileUrl =
                        "https://www.linkedin.com/in/vishnu-nair-%F0%9F%9B%A9%EF%B8%8F-439472204/"
                    val intent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "Hey Vishnu I would Like to increase my Usage Limit , this is my UID:- ${uid}",

                            )
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(intent, "Share on LinkedIn")
                    ContextCompat.startActivity(context, shareIntent, null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "Request Limit Increase", style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Composable
private fun UsageSection(
    title: String,
    usedValue: Number,
    totalValue: Number,
    progressValue: Float,
    percentageUsed: Number,
    unitLabel: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${usedValue}/${totalValue} $unitLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${percentageUsed}% used",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressValue)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
    }
}

@Composable
private fun UsageDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@SuppressLint("RememberReturnType")
@Composable
fun EnhancedHeaderCard(
    it: HeaderAndCreatedAt,
    navigationController: NavHostController,
    appViewModel: AppViewModel
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val cardElevation by animateDpAsState(
        targetValue = if (isPressed) 0.dp else 4.dp,
        animationSpec = tween(durationMillis = 200),
        label = "elevation"
    )

    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .scale(cardScale)
            .shadow(
                2.dp,
                RoundedCornerShape(16.dp),
                ambientColor = Color.White,
                spotColor = MaterialTheme.colorScheme.onSurface
            )
            .clickable(
                interactionSource = interactionSource, indication = null
            ) {
                val encodedHeader = URLEncoder.encode(it.header, StandardCharsets.UTF_8.toString())
                navigationController.navigate(Routes.ListRecordings.name + "?header=${encodedHeader}")
                appViewModel.selectHeader(it.header)
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent with gradient
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(50.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        ), shape = RoundedCornerShape(3.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = it.header,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DateRange,
                        contentDescription = "Date",
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = formatDate(it.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Button with subtle background
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        shape = CircleShape
                    ), contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


// Note: This is a placeholder. You'll need to implement the actual ExpandableFAB logic.

fun formatDate(milliseconds: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    return sdf.format(Date(milliseconds))
}

fun milliSecToSec(milliseconds: Long): Long {
    return Math.round((milliseconds / 1000).toDouble())
}

fun milliSecToMin(milliseconds: Long): Long {
    return Math.round((milliseconds / 60000).toDouble())
}


@Composable
fun SettingsGearUI(navigationController: NavHostController) {
    var settingsClicked by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(
        targetValue = if (settingsClicked) 135f else 0f,
        animationSpec = tween(300)
    )
    return Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        IconButton(
            onClick = {
                settingsClicked = true
                CoroutineScope(Dispatchers.Main).launch {
                    delay(300)
                    navigationController.navigate(Routes.Settings.name)
                }
            },
            Modifier
                .rotate(rotationState)
        ) {
            Icon(
                imageVector = Icons.Outlined.Settings,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                contentDescription = "Show Usage Time",
                modifier = Modifier.rotate(rotationState)
            )

        }
    }
}

@Composable
fun ShimmerHeaderCard(
    context: Context= LocalContext.current
) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(
                2.dp,
                RoundedCornerShape(16.dp),
                ambientColor = Color.White,
                spotColor = MaterialTheme.colorScheme.onSurface
            )
            .clickable {
                Toast.makeText(context, "Note generation is in progress", Toast.LENGTH_SHORT).show()
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent shimmer
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(50.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.3f),
                        shape = RoundedCornerShape(3.dp)
                    )
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Title shimmer - two lines
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Date row shimmer
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon shimmer
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.2f),
                                shape = CircleShape
                            )
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    // Date text shimmer
                    Box(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                            .background(
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Arrow button shimmer
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = alpha * 0.2f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha * 0.3f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}