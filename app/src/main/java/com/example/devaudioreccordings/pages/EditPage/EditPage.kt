package com.example.devaudioreccordings.pages.EditPage

import AudioPlayerCard
import android.annotation.SuppressLint
import android.app.Service.RECEIVER_EXPORTED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.devaudioreccordings.AztecView
import com.example.devaudioreccordings.DataStoreKeys
import com.example.devaudioreccordings.Flows
import com.example.devaudioreccordings.Routes
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.network.RetrofitInstance
import com.example.devaudioreccordings.user
import com.example.devaudioreccordings.viewModals.AddMediaViewModel
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.myapp.ridescribe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.InputStream
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@SuppressLint("UnrememberedMutableState")
@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun EditPage(
    id: String,
    flow: String,
    viewModel: AppViewModel,
    addMediaViewModel: AddMediaViewModel,
    navController: NavController
) {
    var isTextGettingGenerated = addMediaViewModel.isTextGettingGenerated.value
    val addMediaText = addMediaViewModel.content.value
    val context = LocalContext.current
    val token = viewModel.token
    val fullData = viewModel.fullData.collectAsState()
    var audioData: AudioText? by remember { mutableStateOf(null) }
    var needsRefresh by remember { mutableStateOf(false) }
    var currFlowType by remember { mutableStateOf(flow) }
    var moveToEditPage by remember { mutableStateOf(false) }
    Log.d("currFlowType==>", currFlowType)
    val lifecycleOwnerActivity = LocalLifecycleOwner.current

//    val flow = audioData?.flowType?.name?:Flows.MediaCaptureService.name
//
    val navBackStackEntry = navController.currentBackStackEntryAsState()

    LaunchedEffect(moveToEditPage) {
        if (moveToEditPage) {
            val intent = Intent(context, AztecView::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }

    LaunchedEffect(Unit) {
        if (currFlowType == Flows.AddText.name) {
            val intent = Intent(context, AztecView::class.java)
            intent.putExtra("id", id)
            context.startActivity(intent)
        }
    }

    LaunchedEffect(Unit) {
        audioData = viewModel.returnDataBasedOnId2(fullData.value, id.toInt())
        if (currFlowType == "") {
            currFlowType = audioData?.flowType?.name ?: Flows.MediaCaptureService.name
        }
    }
//    DisposableEffect(lifecycleOwnerActivity) {
//        val observer = LifecycleEventObserver { _, event ->
//            if (event == Lifecycle.Event.ON_RESUME) {
//                Log.d("appViewModel.\n", "Returned to Compose")
//                needsRefresh = true
//            }
//        }
//        lifecycleOwnerActivity.lifecycle.addObserver(observer)
//        onDispose {
//            lifecycleOwnerActivity.lifecycle.removeObserver(observer)
//        }
//    }


    if (audioData == null) return Text(text = "Loading..")
    var mediaPlayer = remember { mutableStateOf<MediaPlayer?>(null) }
    var isPlaying = remember { mutableStateOf(false) }
    var elapsedTime = remember { mutableStateOf(0) }
    var totalDuration = remember { mutableStateOf(0) }
    var showDeleteDialog = remember { mutableStateOf(false) }
    var showExitDialogBox = remember { mutableStateOf(false) }
    var showGenerationDialogBox = remember { mutableStateOf(false) }
    var imageFilePathList = remember { mutableStateListOf<String>() }
    var isEnhanceTextLoading = remember { mutableStateOf(false) }
    var isTranscriptTextLoading = remember { mutableStateOf(false) }
    var expandedSection = remember { mutableStateOf("text") } // Controls which section is expanded
    // Get data once using derived state to avoid recomposition issues
    var headerText by remember { mutableStateOf(audioData?.header ?: "") }
    var subHeadingText by remember { mutableStateOf(audioData?.subHeader ?: "") }
    var transcriptionText = remember { mutableStateOf(audioData?.text ?: "") }
    var audioFile by remember {
        mutableStateOf(getAudioFile(context, audioData?.audioFileName ?: ""))
    }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event.name == Lifecycle.Event.ON_STOP.name) {
                mediaPlayer.value?.apply {
                    stop()
                    release()
                }
                mediaPlayer.value = null
                isPlaying.value = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var isNewTextCreated = viewModel.isNewTextCreated

    Log.d("addMediaText==>", addMediaText)

    LaunchedEffect(addMediaText) {
        transcriptionText.value = addMediaText
        if (addMediaText != "") {
            delay(500)
            moveToEditPage = true
        }
    }

    LaunchedEffect(needsRefresh) {
        Log.d("called", "in here")
        if (needsRefresh) {
            audioData = viewModel.returnDataBasedOnId2(fullData.value, id.toInt())
            transcriptionText.value = audioData?.text ?: ""
            if (audioData != null && audioData?.imageCollection != null) {
                audioData?.imageCollection!!.forEach {
                    if (!imageFilePathList.contains(it)) imageFilePathList.add(it)
                }
            }
            Log.d("appViewModel.123\n", "Returned to Compose")
            Log.d("text123==>", audioData?.text ?: "123")
            needsRefresh = false
        }
    }


    fun saveChanges() {
        CoroutineScope(Dispatchers.IO).launch {
            audioData = viewModel.returnDataBasedOnId2(fullData.value, id.toInt())
            if (audioFile == null && audioData!!.audioFileName !== null) {
                audioData!!.audioFileName?.let { viewModel.deleteAudioFile(it) }
                audioData!!.imageCollection?.forEach { imgPath ->
                    if (!imageFilePathList.contains(imgPath)) viewModel.deleteImageFile(imgPath)

                }
            }
            val newAudioText = audioData!!.copy(
                header = headerText,
                imageCollection = imageFilePathList,
                subHeader = subHeadingText,
                updatedAt = System.currentTimeMillis(),
                audioFileName = audioFile?.name
            )
            isNewTextCreated.value = false
            viewModel.updateData(newAudioText)
            withContext(Dispatchers.Main) {
                addMediaViewModel.content.value = ""
                navController.popBackStack(
                    route = Routes.Homepage.name,
                    inclusive = false
                )
                val encodedHeader = URLEncoder.encode(headerText, StandardCharsets.UTF_8.toString())
                navController.navigate(Routes.ListRecordings.name + "?header=${encodedHeader}")
                viewModel.selectHeader(headerText)
            }
        }
    }
    fun saveNewTranscriptionText(newTranscript:String) {
        CoroutineScope(Dispatchers.IO).launch {
            audioData = viewModel.returnDataBasedOnId2(fullData.value, id.toInt())

            val newAudioText = audioData!!.copy(
                text =  newTranscript
            )
            isNewTextCreated.value = false
            viewModel.updateData(newAudioText)
        }
    }

    val hasUnsavedChanges = checkifDataHasChangedOrTextFlow(
        isNewTextCreated.value,
        audioData!!,
        headerText,
        subHeadingText,
        audioData!!.flowType.name,
        audioFile,
    )

    var isTranscriptionOrSummaryGenerationHappening =
        isTranscriptTextLoading.value || isTextGettingGenerated || isEnhanceTextLoading.value
    // Back handler logic :- Also added a logic to save data inside edit text each time user leaves
    // edit page , this is done so that even if save cant detect any Rich Edit Text changes those changes are saved
//    if (hasUnsavedChanges) {
//        BackHandler(
//            enabled = hasUnsavedChanges
//        ) {
//            showExitDialogBox.value = true
//
//        }
//    }

    if (true) {
        BackHandler(enabled = isTranscriptionOrSummaryGenerationHappening || hasUnsavedChanges) {
            if (isTranscriptionOrSummaryGenerationHappening) showGenerationDialogBox.value = true
            else showExitDialogBox.value = true

        }
    }


    // Media player cleanup
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer.value?.apply {
                if (isPlaying.value) stop()
                release()
            }
            mediaPlayer.value = null
        }
    }

    // Image collection setup
    LaunchedEffect(key1 = audioData) {
        if (audioData != null && audioData?.imageCollection != null) {
            audioData?.imageCollection!!.forEach {
                if (!imageFilePathList.contains(it)) imageFilePathList.add(it)
            }
        }
    }

    // Timer effect for updating progress while playing
    LaunchedEffect(isPlaying.value) {
        while (isPlaying.value) {
            delay(100)
            mediaPlayer.value?.let {
                if (it.isPlaying) {
                    elapsedTime.value = it.currentPosition
                    totalDuration.value = it.duration
                }
            }
        }
    }

    // Refresh data when id changes or generation completes
    LaunchedEffect(id, isTextGettingGenerated) {
        if (!isTextGettingGenerated) {
            delay(500)
            audioData = viewModel.returnDataBasedOnId2(fullData.value, id.toInt())
            transcriptionText.value = audioData?.text ?: "No Text Created"
        }
    }

    // Handle playback completion
    DisposableEffect(mediaPlayer.value) {
        mediaPlayer.value?.setOnCompletionListener {
            isPlaying.value = false
            elapsedTime.value = 0
        }
        onDispose {}
    }
    DisposableEffect(Unit) {
        val broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, p1: Intent?) {
                if (p1?.action == Intent.ACTION_SCREEN_OFF) {
                    mediaPlayer?.value?.pause()
                    isPlaying.value = false

                }
            }


        }
        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(broadcastReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(broadcastReceiver, intentFilter)
        }
        onDispose {
            context.unregisterReceiver(broadcastReceiver)
        }

    }

    // Theme colors
    val backgroundColor = MaterialTheme.colorScheme.background
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surface
    val textPrimaryColor = MaterialTheme.colorScheme.onSurface
    val textSecondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    // Dialogs
    if (showDeleteDialog.value) {
        mediaPlayer.value?.stop()
        ShowDeleteDialog(
            showDeleteDialog, viewModel, audioData!!, navController, errorColor, {
                imageFilePathList = mutableStateListOf<String>()
            }
        )
    }

    if (showExitDialogBox.value) {
        ShowSaveChangesDialogBox(
            showDialog = showExitDialogBox,
            onSave = {
                saveChanges()
            },
            onDiscard = {
                if (isTranscriptionOrSummaryGenerationHappening) {
                    showGenerationDialogBox.value = true
                }
                if (isNewTextCreated.value) {
                    CoroutineScope(Dispatchers.IO).launch {
                        viewModel.deleteData(audioData!!)
                        withContext(Dispatchers.Main) {
                            addMediaViewModel.content.value = ""
                            navController.popBackStack()

                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.Main).launch {
                        addMediaViewModel.content.value = ""
                        navController.popBackStack()

                    }
                }
            }
        )
    }

    if (showGenerationDialogBox.value) {
        val process = if (isTextGettingGenerated || isTranscriptTextLoading.value) {
            "Transcription"
        } else {
            "Summary"
        }
        ShowGenerationHappeningDialogBox(
            showDialog = showGenerationDialogBox,
            process = process,
            onStay = {
                showGenerationDialogBox.value = false
            },
            onLeave = {
                addMediaViewModel.isTextGettingGenerated.value = false
                isTranscriptTextLoading.value = false
                isEnhanceTextLoading.value = false
                saveChanges()
            }
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBarsIgnoringVisibility,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currFlowType == Flows.AddText.name || currFlowType == Flows.FloatingText.name) {
                            "Edit Text"
                        } else "Edit Recording",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = textPrimaryColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (checkifDataHasChangedOrTextFlow(
                                isNewTextCreated.value,
                                audioData!!,
                                headerText,
                                subHeadingText,
                                currFlowType,
                                audioFile,
                            )
                        ) {
                            showExitDialogBox.value = true
                        } else {
                            addMediaViewModel.content.value = ""
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.ArrowBack,
                            contentDescription = "Go back",
                            tint = primaryColor
                        )
                    }
                },
                actions = {
                    // More compact actions menu
                    var showMenu by remember { mutableStateOf(false) }

                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options",
                            tint = primaryColor
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Share") },
                            onClick = {
                                val plainText = HtmlCompat.fromHtml(
                                    audioData!!.text,
                                    HtmlCompat.FROM_HTML_MODE_COMPACT
                                ).toString()
                                val intent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, plainText)
                                    type = "text/plain"
                                }
                                val shareIntent = Intent.createChooser(intent, "Share")
                                ContextCompat.startActivity(context, shareIntent, null)
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Share,
                                    contentDescription = null
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Share on LinkedIn") },
                            onClick = {
                                navController.navigate(Routes.AIGeneratedText.name + "?id=${audioData?.id}")
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(id =R.drawable.vecteezy_linkedin_logo_png_linkedin_logo_transparent_png_linkedin_23986970),
                                    contentDescription = "Share on LinkedIn",
                                    tint = Color(0xFF0A66C2),
                                    modifier = Modifier.size(24.dp)
                                    // LinkedIn blue color
                                )
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("Delete") },
                            onClick = {
                                showDeleteDialog.value = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = errorColor
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = surfaceColor,
                    titleContentColor = textPrimaryColor
                )
            )
        },
        containerColor = backgroundColor,
        // Floating action button for quick save
        floatingActionButton = {
            val hasChanges = checkifDataHasChangedOrTextFlow(
                isNewTextCreated.value,
                audioData!!,
                headerText,
                subHeadingText,
                currFlowType,
                audioFile,
            )

            // Apply different colors based on whether changes have been made


            FloatingActionButton(
                onClick = {
                    if (hasChanges) {
                        saveChanges()
                    }
                },
                containerColor = primaryColor,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .alpha(if (hasChanges) 1f else 0.4f)
                    .clickable { hasChanges }  // Extra visual cue
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = if (hasChanges) "Save Changes" else "No Changes to Save"
                )
            }
        }
    ) { paddingValues ->
        audioData?.let { data ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .padding(vertical = 0.dp)
            ) {
                val context = LocalContext.current


                // Last updated timestamp - subtle at the top
                Text(
                    "Last edited: ${formatTimestamp(data.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textSecondaryColor,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )

                // Main content area - scrollable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState(0))
                ) {
                    // Title & Subtitle input - sleek, borderless design
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        // Title
                        OutlinedTextField(
                            value = headerText,
                            onValueChange = {
                                headerText = if (it.length > 30) it.take(30) else it
                            },
                            placeholder = {
                                Text("Add Title")
                            },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = textPrimaryColor
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor.copy(alpha = 0.5f),
                                unfocusedBorderColor = textSecondaryColor.copy(alpha = 0.2f),
                                focusedContainerColor = surfaceColor,
                                unfocusedContainerColor = surfaceColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "${headerText.length}/30",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondaryColor,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Subheading
                        OutlinedTextField(
                            value = subHeadingText,
                            onValueChange = {
                                subHeadingText = if (it.length > 50) it.take(50) else it
                            },
                            placeholder = {
                                Text("Add Sub-Heading")
                            },
                            textStyle = MaterialTheme.typography.titleMedium.copy(
                                color = textPrimaryColor
                            ),
                            maxLines = 2,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = primaryColor.copy(alpha = 0.5f),
                                unfocusedBorderColor = textSecondaryColor.copy(alpha = 0.2f),
                                focusedContainerColor = surfaceColor,
                                unfocusedContainerColor = surfaceColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "${subHeadingText.length}/50",
                            style = MaterialTheme.typography.labelSmall,
                            color = textSecondaryColor,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 4.dp)
                        )
                    }


                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Section chooser - tabs to switch between audio, images and text


                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SectionTab(
                            title = if (currFlowType == Flows.AddText.name || currFlowType == Flows.FloatingText.name) "Edit Text" else "Edit Text",
                            icon = Icons.Default.TextFields,
                            isSelected = expandedSection.value == "text",
                            onClick = { expandedSection.value = "text" },
                            primaryColor = primaryColor,
                            textSecondaryColor = textSecondaryColor
                        )

                        if (audioFile != null) {
                            SectionTab(
                                title = "Audio",
                                icon = Icons.Default.Audiotrack,
                                isSelected = expandedSection.value == "audio",
                                onClick = { expandedSection.value = "audio" },
                                primaryColor = primaryColor,
                                textSecondaryColor = textSecondaryColor
                            )
                        }


                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current section content
                    AnimatedContent(
                        targetState = expandedSection.value,
                        transitionSpec = {
                            (fadeIn() + slideInHorizontally()).togetherWith(fadeOut() + slideOutHorizontally())
                        }, label = ""
                    ) { section ->
                        when (section) {
                            "text" -> {
                                // Transcript section - simplified
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            returnDataBasedOnFlowType(
                                                "Text",
                                                "Transcript",
                                                currFlowType
                                            ),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = textPrimaryColor,
                                            fontWeight = FontWeight.SemiBold
                                        )

                                        // AI Actions as a dropdown to reduce clutter
                                        var showAiMenu by remember { mutableStateOf(false) }
                                        if (showAiMenu) {
                                            DropdownMenu(
                                                expanded = showAiMenu,
                                                onDismissRequest = { showAiMenu = false },
                                                offset = DpOffset(x = (-20.dp), 0.dp)
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("Generate transcript") },
                                                    onClick = {
                                                        CoroutineScope(Dispatchers.IO).launch {

                                                            try {
                                                                isTranscriptTextLoading.value = true
                                                                var transcriptionAudioFile =
                                                                    audioFile
                                                                var imageTimestampList = audioData!!.imageTimestampList

                                                                Log.d(
                                                                    "audioFileForTranscriptName2==>",
                                                                    transcriptionAudioFile!!.name
                                                                )

                                                                val prefs =
                                                                    context.user.data.first()
                                                                val uidData =
                                                                    prefs[DataStoreKeys.USER_UID]
                                                                        ?: ""
                                                                val requestFile =
                                                                    transcriptionAudioFile!!.asRequestBody(
                                                                        "audio/pcm".toMediaTypeOrNull()
                                                                    )
                                                                val filePart =
                                                                    MultipartBody.Part.createFormData(
                                                                        "file",
                                                                        transcriptionAudioFile.name,
                                                                        requestFile
                                                                    )

                                                                val uid =
                                                                    MultipartBody.Part.createFormData(
                                                                        "uid",
                                                                        uidData
                                                                    )
                                                                val timestamps =
                                                                    MultipartBody.Part.createFormData(
                                                                        "timestamps",
                                                                        imageTimestampList ?: "null"
                                                                    )
                                                                val saveContentAsPart =
                                                                    MultipartBody.Part.createFormData(
                                                                        "saveContentAs",
                                                                        "transcript"
                                                                    )
                                                                val responseObj =
                                                                    RetrofitInstance.ApiServices.uploadFile(
                                                                        token,
                                                                        filePart,
                                                                        uid,
                                                                        saveContentAs = saveContentAsPart,
                                                                        timestamps
                                                                    )
                                                                val response = responseObj.response
                                                                Log.d("recieved response==>",response)
                                                                val Used_Transcription_Duration =
                                                                    responseObj.Used_Transcription_Duration.toDouble()
                                                                        .toLong()
                                                                val total_Transcription_Duration =
                                                                    responseObj.Total_Transcription_Duration.toDouble()
                                                                        .toLong()
                                                                if (response != "") {
                                                                    var responseText = response
                                                                    for (x in imageFilePathList) {
                                                                        val imageDirectory = File(
                                                                            context.getExternalFilesDir(
                                                                                null
                                                                            ), "ImageDirectory"
                                                                        )
                                                                        val file =
                                                                            File(imageDirectory, x)
                                                                        Log.d(
                                                                            "file.absolutePath==>",
                                                                            file.absolutePath
                                                                        )
                                                                        responseText =
                                                                            responseText.replaceFirst(
                                                                                """<img src=""/>""",
                                                                                """<img src="${file.absolutePath}"  />"""
                                                                            )
                                                                    }

                                                                    context.user.edit {
                                                                        it[DataStoreKeys.Used_Transcription_Duration] =
                                                                            Used_Transcription_Duration
                                                                        it[DataStoreKeys.Total_Transcription_Duration] =
                                                                            total_Transcription_Duration
                                                                    }
                                                                    transcriptionText.value =
                                                                        responseText

                                                                    Log.d("transcriptionText.value==>",transcriptionText.value)


                                                                }
                                                            } catch (e: Throwable) {
                                                                Log.d("e==>",e.stackTraceToString())
                                                            }
//
                                                            finally {
                                                                saveNewTranscriptionText(transcriptionText.value)
                                                                delay(800)

                                                                showAiMenu = false
                                                                isTranscriptTextLoading.value =
                                                                    false
                                                                moveToEditPage = true


                                                            }
                                                        }
                                                        // Logic for generating transcript

                                                    },
                                                    enabled = audioFile != null && !isTranscriptTextLoading.value && audioData!!.isApiCallRequired
                                                )

                                                DropdownMenuItem(
                                                    text = { Text("Summarize Text") },
                                                    onClick = {
                                                        // Logic for enhancing text
                                                        CoroutineScope(Dispatchers.IO).launch {
                                                            try {
                                                                isEnhanceTextLoading.value = true
                                                                val response =
                                                                    viewModel.enhanceTextUsingAI(
                                                                        transcriptionText.value
                                                                    )
                                                                if (response != "") {
                                                                    transcriptionText.value =
                                                                        response
                                                                }
                                                                isEnhanceTextLoading.value = false
                                                            } catch (e: Throwable) {
                                                            } finally {
                                                                showAiMenu = false
                                                                delay(500)
                                                                moveToEditPage = true

                                                            }


                                                        }
                                                    },
                                                    enabled = !transcriptionText.value.isNullOrEmpty() && !isEnhanceTextLoading.value
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clickable { showAiMenu = true }
                                                .background(
                                                    color = primaryColor.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = primaryColor.copy(alpha = 0f),
                                                    shape = RoundedCornerShape(6.dp)
                                                )
                                                .padding(
                                                    horizontal = 12.dp,
                                                    vertical = 8.dp
                                                ) // Padding goes inside border/background
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AutoAwesome,
                                                contentDescription = "AI options",
                                                tint = primaryColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }


                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (isTranscriptTextLoading.value || isEnhanceTextLoading.value || isTextGettingGenerated) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            CircularProgressIndicator(
                                                color = primaryColor,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(16.dp))

                                            // Animated text
                                            val infiniteTransition = rememberInfiniteTransition()
                                            val alpha = infiniteTransition.animateFloat(
                                                initialValue = 0.3f,
                                                targetValue = 1f,
                                                animationSpec = infiniteRepeatable(
                                                    animation = tween(1000),
                                                    repeatMode = RepeatMode.Reverse
                                                ), label = ""
                                            )

                                            if (isTextGettingGenerated) Text(
                                                text = "Text is getting Generated please wait...",
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha.value),
                                                fontSize = 16.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            if (isTranscriptTextLoading.value) Text(
                                                text = "Transcript Text is Loading please wait...",
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha.value),
                                                fontSize = 16.sp,
                                                textAlign = TextAlign.Center
                                            )
                                            if (isEnhanceTextLoading.value) Text(
                                                text = "Text is getting Enhanced please wait...",
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha.value),
                                                fontSize = 16.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    } else {
                                        // Clean, borderless text field
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                        ) {
                                            Spacer(modifier = Modifier.height(16.dp))
                                            Button(
                                                onClick = {
                                                    val intent =
                                                        Intent(context, AztecView::class.java)
                                                    intent.putExtra("id", id)
                                                    context.startActivity(intent)
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(56.dp)
                                                    .padding(horizontal = 16.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = primaryColor,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                elevation = ButtonDefaults.buttonElevation(
                                                    defaultElevation = 4.dp,
                                                    pressedElevation = 8.dp
                                                )
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "Open Text Editor",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(16.dp))

//                                        OutlinedTextField(
//                                            value = transcriptionText.value,
//                                            onValueChange = { transcriptionText.value = it },
//                                            modifier = Modifier
//                                                .fillMaxWidth()
//                                                .heightIn(min = 200.dp),
//                                            textStyle = MaterialTheme.typography.bodyLarge,
//                                            placeholder = {
//                                                Text(
//                                                    returnDataBasedOnFlowType(
//                                                        "Enter Your Text here",
//                                                        "Transcript will appear here",
//                                                        currFlowType
//                                                    )
//                                                )
//                                            },
//                                            colors = OutlinedTextFieldDefaults.colors(
//                                                focusedBorderColor = primaryColor.copy(alpha = 0.5f),
//                                                unfocusedBorderColor = textSecondaryColor.copy(alpha = 0.2f),
//                                                focusedContainerColor = surfaceColor,
//                                                unfocusedContainerColor = surfaceColor
//                                            )
//                                        )
                                        }
                                    }
                                }
                            }

                            "audio" -> {
                                // Simplified audio player
                                if (audioFile != null) {
                                    AudioPlayerCard(
                                        surfaceColor,
                                        primaryColor,
                                        isPlaying,
                                        mediaPlayer,
                                        totalDuration,
                                        audioFile!!,
                                        audioData!!,
                                        elapsedTime,
                                        textSecondaryColor,
                                        { audioFile = null }
                                    )
                                }
                            }

                            "images" -> {
                                ImageGallerySection(
                                    imageFilePathList = imageFilePathList,
                                    onRemoveImage = { imagePath ->
                                        imageFilePathList.remove(imagePath)
                                    },
                                    onAddImage = {
                                        // Implement your logic to add images
                                        // This might involve launching an image picker
                                    },
                                    textPrimaryColor = textPrimaryColor,
                                    textSecondaryColor = textSecondaryColor,
                                    primaryColor = primaryColor
                                )
                            }
                        }
                    }
                }
            }
        } ?: run {
            // Error state - simplified
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Warning,
                        contentDescription = null,
                        tint = textSecondaryColor,
                        modifier = Modifier.size(48.dp)
                    )

                    Text(
                        "Recording not found",
                        style = MaterialTheme.typography.headlineSmall,
                        color = textSecondaryColor
                    )

                    Button(
                        onClick = {
                            addMediaViewModel.content.value = ""
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Text("Go Back")
                    }
                }
            }
        }

    }


    // Current section content

}


fun returnDataBasedOnFlowType(
    textBasedData: String,
    transcriptBasedData: String,
    flow: String
): String {
    if (flow == Flows.AddText.name || flow == Flows.FloatingText.name) return textBasedData
    return transcriptBasedData

}


private fun encodeImageToBase64(context: android.content.Context, uri: Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes()
        inputStream?.close()
        if (bytes != null) {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        } else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
