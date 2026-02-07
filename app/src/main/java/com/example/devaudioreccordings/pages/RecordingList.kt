package com.example.devaudioreccordings.pages

import android.app.Service.RECEIVER_EXPORTED
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.text.Html
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.ripple
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.core.text.HtmlCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.devaudioreccordings.Routes
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.myapp.ridescribe.R
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingList(header: String, appViewModel: AppViewModel, navController: NavController) {
    val fullData = appViewModel.fullData.collectAsState()
    val data = appViewModel.filterRecordingList(fullData, header)
    var currentlyPlayingAudioId = remember {
        mutableStateOf(-1)
    }





    LaunchedEffect(Unit) {
        delay(200)
        if (data.value.size == 0) {
            appViewModel.selectHeader(header)

        }
    }

    LaunchedEffect(Unit) {
        appViewModel.isNewTextCreated.value = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Go back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            if (data.value.isEmpty()) {
                EmptyRecordingsList()
            } else {


                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState(0)),
                    verticalArrangement = Arrangement.Center
                ) {
                    data.value.map { audioText ->
                        ShowData(
                            audioText,
                            context = LocalContext.current,
                            navController,
                            currentlyPlayingAudioId
                        )
                    }

                }
                // Add bottom padding for better scrolling experience
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun EmptyRecordingsList() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "No recordings",
            tint = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No notes found",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Notes you create will appear here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShowData(
    audioText: AudioText,
    context: Context,
    navController: NavController,
    currentlyPlayingAudioId: MutableState<Int>
) {
    var mediaPlayer = remember<MutableState<MediaPlayer?>> { mutableStateOf(null) }
    var isMusicPlaying = remember { mutableStateOf(false) }
    var selectedImagePathInFullScreen = remember { mutableStateOf<String?>(null) }
    var elapsedTime = remember { mutableStateOf(0) }
    var totalDuration = remember { mutableStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current


    if (isMusicPlaying.value) {
        if (currentlyPlayingAudioId.value != audioText.id) {
            isMusicPlaying.value = false
            mediaPlayer.value?.pause()
        }
    }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (event.name == Lifecycle.Event.ON_STOP.name) {
                isMusicPlaying.value = false

                mediaPlayer.value?.apply {
                    stop()
                    release()
                }
                mediaPlayer.value = null
            }
            Log.d("event.name==>", event.name)
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(key1 = Unit) {
        onDispose {
            mediaPlayer.value?.pause()
        }
    }

    DisposableEffect(Unit) {
        val screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (Intent.ACTION_SCREEN_OFF == intent?.action) {
                    isMusicPlaying.value = false
                    mediaPlayer.value?.pause()
                }
            }
        }

        val intentFilter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(screenReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(screenReceiver, intentFilter)
        }

        onDispose {
            context.unregisterReceiver(screenReceiver)
        }
    }


    fun getAudioFile(context: Context, fileName: String?): File? {
        if (fileName == "") return null
        if (fileName == null) return null

        val audioCapturesDirectory = File(context.getExternalFilesDir(null), "AudioCaptures")
        if (!audioCapturesDirectory.exists()) return null

        val file = File(audioCapturesDirectory, fileName)
        return if (file.exists()) file else null
    }

    val audioFile = getAudioFile(context, audioText.audioFileName)
    var selectedImagePath = remember { mutableStateOf<String?>(null) }


    if (isMusicPlaying.value) {
        mediaPlayer.value?.setOnCompletionListener {
            isMusicPlaying.value = false
            elapsedTime.value = 0
        }

        LaunchedEffect(isMusicPlaying.value) {
            while (isMusicPlaying.value) {
                delay(100)
                mediaPlayer.value?.let {
                    if (it.isPlaying) {
                        elapsedTime.value = it.currentPosition
                        totalDuration.value = it.duration
                    }
                }
            }
        }
    }

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
            .clickable(
                indication = ripple(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ),
                interactionSource = remember { MutableInteractionSource() }
            ) {
                navController.navigate(Routes.EditPage.name + "?id=" + audioText.id)
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDate(audioText.updatedAt),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {

                        TooltipBox(
                            positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
                            tooltip = {
                                PlainTooltip {
                                    Text("Open in ChatGPT")
                                }
                            },
                            state = rememberTooltipState()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10A37F))
                                    // ChatGPT's brand color
                                    .clickable { openChatGPT(context, audioText.text) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.chatgpt_logo),
                                    contentDescription = "Open in ChatGPT",
                                    tint = Color.White,
                                    modifier = Modifier.size(60.dp)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit recording",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(30.dp)
                                .padding(5.dp, 0.dp)
                        )
                    }
                }

                if (audioText.subHeader != null) {
                    Text(
                        text = audioText.subHeader,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        overflow = TextOverflow.Ellipsis
                    )
                }

                WebViewUI(
                    html = audioText.text
                        ?: "Lorem Ipsum is simply dummy text of the printing and typesetting industry...",
                    selectedImagePathInFullScreen = selectedImagePathInFullScreen,
                    selectedImagePath = selectedImagePath,
                    handleBodyClick = {
                        navController.navigate(Routes.EditPage.name + "?id=" + audioText.id)
                    }

                )



                if (audioText.imageCollection != null) {

                    HorizontalMultiBrowseCarousel(
                        state = rememberCarouselState { audioText.imageCollection.count() },
                        modifier = Modifier
                            .width(412.dp)
                            .height(221.dp),
                        preferredItemWidth = 186.dp,
                        itemSpacing = 10.dp,
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) { i ->
                        val item = audioText.imageCollection[i]
                        val imageDirectory =
                            File(context.getExternalFilesDir(null), "ImageDirectory")
                        val imageFile = File(imageDirectory, audioText.imageCollection[i])

                        Card(
                            modifier = Modifier
                                .padding(4.dp)
                                .clickable {
                                    selectedImagePath.value = audioText.imageCollection[i]
                                    selectedImagePathInFullScreen.value =
                                        audioText.imageCollection[i]
                                }
                                .aspectRatio(1f),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 2.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()

                            ) {

                                GlideImage(
                                    imageModel = { imageFile.absoluteFile },  // Pass as a lambda for better state handling

                                    modifier = Modifier.fillMaxSize(),
                                    // Loading placeholder
                                    loading = {
                                        Box(modifier = Modifier.matchParentSize()) {
                                            androidx.compose.material3.CircularProgressIndicator(
                                                modifier = Modifier.align(Alignment.Center),
                                                color = MaterialTheme.colorScheme.primary
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
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                )
                            }


//                        ImageShow(context, item) {
//                            selectedImagePath = audioText.imageCollection[i]
//                            selectedImagePathInFullScreen.value = audioText.imageCollection[i]
//                        }
                        }
                    }

                    if (selectedImagePath.value != null) {
                        Dialog(
                            onDismissRequest = { selectedImagePath.value = null },
                            properties = DialogProperties(
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true,
                                usePlatformDefaultWidth = false
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.95f))
                            ) {
                                IconButton(
                                    onClick = { selectedImagePath.value = null },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .zIndex(1f)
                                        .padding(16.dp)
                                        .size(48.dp)
                                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    Arrangement.Start,
                                    Alignment.CenterVertically,
                                ) {
                                    audioText.imageCollection.mapIndexed { index, imagePath ->
                                        if (imagePath == selectedImagePathInFullScreen.value)
                                            FullScreenImage(
                                                LocalContext.current,
                                                imagePath,
                                                selectedImagePathInFullScreen,
                                                index,
                                                audioText.imageCollection
                                            )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (audioFile != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (isMusicPlaying.value) {
                                mediaPlayer.value?.pause()
                                currentlyPlayingAudioId.value = -1
                            } else {
                                if (mediaPlayer.value == null) {
                                    mediaPlayer.value = MediaPlayer().apply {
                                        setDataSource(audioFile.absolutePath)
                                        prepare()
                                        totalDuration.value = duration
                                    }
                                }
                                mediaPlayer.value?.start()
                                currentlyPlayingAudioId.value = audioText.id
                            }
                            isMusicPlaying.value = !isMusicPlaying.value
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (isMusicPlaying.value) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isMusicPlaying.value) "Pause" else "Play",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 4.dp)
                    ) {
                        Slider(
                            value = if (totalDuration.value > 0)
                                elapsedTime.value.toFloat() / totalDuration.value
                            else 0f,
                            onValueChange = { newPosition ->
                                mediaPlayer.value?.let { player ->
                                    val newTimeMs = (newPosition * totalDuration.value).toInt()
                                    player.seekTo(newTimeMs)
                                    elapsedTime.value = newTimeMs

                                    if (!isMusicPlaying.value) {
                                        player.start()
                                        isMusicPlaying.value = true
                                    }
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary,
                                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = formatDuration(elapsedTime.value) + " / " + formatDuration(
                            totalDuration.value
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


@Composable
fun ImageShow(context: Context, imgFilePath: String, onClick: () -> Unit) {
    val imageDirectory = File(context.getExternalFilesDir(null), "ImageDirectory")
    val file = File(imageDirectory, imgFilePath)
    var isLoading by remember {
        mutableStateOf(true)
    }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(imgFilePath) {
        isLoading = true
        withContext(Dispatchers.IO) {
            bitmap = BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }
        isLoading = false
    }

    if (isLoading) {
        // Show loading indicator
        CircularProgressIndicator(
            color = Color.White, modifier = Modifier.size(48.dp)
        )
    } else if (bitmap != null) {
        Row {
            Spacer(modifier = Modifier.width(10.dp))
            Image(bitmap = bitmap!!,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(200.dp)
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() })
        }
    }
}

@Composable
fun FullScreenImage(
    context: Context,
    imgFilePath: String,
    selectedImagePath: MutableState<String?>,
    index: Int,
    imageCollection: MutableList<String>
) {
    val imageDirectory = File(context.getExternalFilesDir(null), "ImageDirectory")
    val file = File(imageDirectory, imgFilePath)

    // Use mutableStateOf to handle loading state
    var isLoading by remember { mutableStateOf(true) }
    var bitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var rotation by remember { mutableStateOf(0f) }

    // Track if current image is zoomed
    val isZoomed = scale > 1f

    val state = rememberTransformableState { zoomChange, offsetChange, rotationChange ->
        scale = (scale * zoomChange).coerceAtLeast(1f)
        rotation += rotationChange

        // Calculate the new offset to zoom at the touch point
        val newOffset = offset + offsetChange
        offset = newOffset
    }

    LaunchedEffect(imgFilePath) {
        isLoading = true
        withContext(Dispatchers.IO) {
            bitmap = BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
        }
        isLoading = false
    }

    Box(
        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            // Show loading indicator
            CircularProgressIndicator(
                color = Color.White, modifier = Modifier.size(48.dp)
            )
        } else if (bitmap != null) {
            // Image zoom and pan capabilities
            // Create a custom pointer input interceptor for handling transformations


            val surfaceOverlayColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            val iconColor = MaterialTheme.colorScheme.onSurface

            IconButton(
                onClick = {
                    if (index != 0) selectedImagePath.value = imageCollection[index - 1]
                },
                enabled = index != 0,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .size(48.dp)
                    .background(
                        color = if (index != 0) surfaceOverlayColor else surfaceOverlayColor.copy(
                            alpha = 0.3f
                        ),
                        shape = CircleShape
                    )
                    .zIndex(1f),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (index != 0) iconColor else iconColor.copy(alpha = 0.3f),
                    disabledContentColor = iconColor.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    Icons.Default.ArrowBackIosNew,
                    contentDescription = "Previous image",
                    modifier = Modifier.alpha(if (index != 0) 1f else 0.3f)
                )
            }

            IconButton(
                onClick = {
                    selectedImagePath.value = imageCollection[index + 1]
                },
                enabled = index != imageCollection.size - 1,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(48.dp)
                    .background(
                        color = if (index != imageCollection.size - 1) surfaceOverlayColor else surfaceOverlayColor.copy(
                            alpha = 0.3f
                        ),
                        shape = CircleShape
                    )
                    .zIndex(1f),
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = if (index != imageCollection.size - 1) iconColor else iconColor.copy(
                        alpha = 0.3f
                    ),
                    disabledContentColor = iconColor.copy(alpha = 0.3f)
                )
            ) {
                Icon(
                    Icons.Default.ArrowForwardIos,
                    contentDescription = "Next image",
                    modifier = Modifier.alpha(if (index != imageCollection.size - 1) 1f else 0.3f)
                )
            }

            GlideImage(imageModel = { file.absolutePath },
                imageOptions = ImageOptions(contentScale = ContentScale.Fit),

                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer(
                        scaleY = scale,
                        scaleX = scale,
                        rotationZ = rotation,
                        translationX = offset.x,
                        translationY = offset.y
                    )
                    .transformable(state)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = {
                            // Toggle zoom on double tap
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                                rotation = 0f
                            } else {
                                scale = 2f
                            }
                        })
                    },
                loading = {
                    Box(modifier = Modifier.matchParentSize()) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.primary
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
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                })
        } else {
            // Error state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.BrokenImage,
                    contentDescription = "Image not found",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Unable to load image",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

fun formatDate(milliseconds: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    return sdf.format(Date(milliseconds))
}

private fun formatDuration(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}

fun openChatGPT(context: Context, text: String) {
    // Copy text to clipboard first
    val spannedText = Html.fromHtml(text, HtmlCompat.FROM_HTML_MODE_COMPACT).toString()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Copied Text", spannedText)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Text copied to clipboard", Toast.LENGTH_SHORT).show()

    val packageManager = context.packageManager
    var intent: Intent? = null

    try {
        // 1. Try creating an ACTION_SEND intent specifically for the ChatGPT app
        intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, spannedText)
            setPackage(CHATGPT_PACKAGE_NAME) // Target the app directly
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        // Check if any activity can handle this specific intent
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent)
            return // Success, app's share handler should open
        }
        // else: App is installed but doesn't handle ACTION_SEND with package set? Fallback below.

    } catch (e: ActivityNotFoundException) {
        Log.e("ChatGPTIntent0", "Error creating SEND intent", e)
        // ACTION_SEND intent failed or app not found for it
        // Proceed to try launching the app directly or opening the browser
    } catch (e: Exception) {
        // Catch other potential exceptions during intent creation/resolution
        Log.e("ChatGPTIntent", "Error creating SEND intent", e)
    }


    try {
        // 2. If ACTION_SEND didn't work or wasn't resolvable, try getting the standard launch intent
        intent = packageManager.getLaunchIntentForPackage(CHATGPT_PACKAGE_NAME)
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            // Note: We already copied the text, user needs to paste it manually.
            return // Success, app launched
        }
    } catch (e: Exception) {
        Log.e("ChatGPTIntent", "Error getting launch intent", e)
        // Fallback to browser
    }


    // 3. If app is not installed or launch intent failed, open the browser
    try {
        intent = Intent(Intent.ACTION_VIEW, Uri.parse(CHATGPT_WEB_URL)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        // Note: Text is already copied for pasting.
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No application found to open web link.", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        // Log.e("ChatGPTIntent", "Error opening browser", e)
        Toast.makeText(context, "Could not open ChatGPT.", Toast.LENGTH_SHORT).show()
    }
}


private const val CHATGPT_PACKAGE_NAME = "com.openai.chatgpt"
private const val CHATGPT_WEB_URL = "https://chat.openai.com/"



// Example with Coil image loading



@Composable
fun WebViewUI(
    html: String, selectedImagePathInFullScreen:
    MutableState<String?>,
    selectedImagePath: MutableState<String?>,
    handleBodyClick: () -> Unit
) {
    Log.d("html==>", html)

    var htmlFixed = html.replace(
        Regex("""<img\s+([^>]*?)src\s*=\s*["'](/storage/[^"']+)["']""")
    ) {
        val attrs = it.groupValues[1]
        val src = it.groupValues[2]
        """<img $attrs src="file://$src""""
    }

    if (htmlFixed.isBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "No content",
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        return
    }

    val backgroundColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    val bgHex = "#${Integer.toHexString(backgroundColor.hashCode()).takeLast(6)}"
    val textHex = "#${Integer.toHexString(textColor.hashCode()).takeLast(6)}"

    var allowPageChange by remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        delay(1000)
        allowPageChange = true
    }

    val htmlWrapped = """
        <html>
          <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <style>
              body {
                background-color: $bgHex;
                color: $textHex;
                font-family: sans-serif;
                padding: 0px;
                margin: 0px;
              }
              img {
                max-width: 100%;
                padding-top: 20px;
                padding-bottom: 20px;
                height: auto;
                max-height: 400px;
                border-radius: 8px;
                margin: 0 auto;
                display: block;
              }
            </style>
          </head>
          <body>
            $htmlFixed
          </body>
        </html>
    """.trimIndent()

    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) } // 👈 track loading

    val webView = remember {
        WebView(context).apply {
            WebView.setWebContentsDebuggingEnabled(true)
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowFileAccessFromFileURLs = true
            settings.allowUniversalAccessFromFileURLs = true
            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

            addJavascriptInterface(
                ImageClickInterface(
                    onImageClick2 = {
                            imageSrc ->
                        Log.d("ImageClickInterface", "FROM JS: $imageSrc")
                        var imgFileName = imageSrc.replace(
                            "file:///storage/emulated/0/Android/data/com.myapp.ridescribe/files/ImageDirectory/",
                            ""
                        )
                        selectedImagePath.value = imgFileName
                        selectedImagePathInFullScreen.value = imgFileName
                    },
                    onClick = {
                        if(!isLoading&&allowPageChange) handleBodyClick() }
                ),"ImageClickInterface"
            )
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false // 👈 stop loader when page finishes

                    view?.evaluateJavascript(
                        """
                        console.log('Injecting JS only once per img...');
                      document.querySelectorAll('img').forEach(img => {
                          if (!img.hasAttribute('data-click-attached')) {
                            img.setAttribute('data-click-attached', 'true');
                            img.onclick = (e) => {
                              console.log('JS click calling interface:', img.src);
                              ImageClickInterface.onImageClick(img.src);
                              e.stopPropagation()
                            };
                          }
                           });
                          document.querySelectorAll('body').forEach( body =>{
                          body.onclick = () => {
                           console.log('JS click calling body:')
                           ImageClickInterface.onBodyClick();
                          }
                          }
                          )
                        """.trimIndent(),
                        null
                    )
                }
            }
        }
    }

    LaunchedEffect(htmlWrapped) {
        isLoading = true // 👈 show loader while new content loads
        webView.loadDataWithBaseURL(null, htmlWrapped, "text/html", "UTF-8", null)
    }

    DisposableEffect(Unit) {
        onDispose {
            webView.destroy()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .heightIn(min = 40.dp)
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface) // same BG as WebView
                    .heightIn(min = 200.dp), // 👈 force minimum height for loader
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


class ImageClickInterface(private val onImageClick2: (String) -> Unit , private val onClick:()->Unit) {
    @JavascriptInterface
    fun onImageClick(imageSrc: String) {
        Handler(Looper.getMainLooper()).post {
//            Log.d("ImageClickInterface", "FROM JS: $imageSrc")
            onImageClick2(imageSrc)
        }
    }

    @JavascriptInterface
    fun onBodyClick() {
        Handler(Looper.getMainLooper()).post {
            Log.d("Called here==>","123")
            onClick()
        }
    }
}


