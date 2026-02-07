package com.example.devaudioreccordings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.SwipeRight
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VideoCall
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.myapp.ridescribe.R
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SplashScreen(onSplashScreenComplete: () -> Unit, viewModel: AppViewModel) {
    val width = 96.dp
    val squareSize = 48.dp
    val isFirstLaunch = viewModel.isFirstLaunch

    var currentScreen by remember { mutableStateOf(0) }
    val totalScreens = 3
    val swipeableState = rememberSwipeableState(0)
    val sizePx = with(LocalDensity.current) { squareSize.toPx() }
    val anchors = mapOf(0f to 0, sizePx to 1) // Maps anchor points (in px) to states
    val configuration = LocalConfiguration.current

    val screenHeight = configuration.screenHeightDp.dp
    val contentHeight = screenHeight * 0.95f
    val navHeight = screenHeight * 0.04f

    // Handle swipe navigation
    LaunchedEffect(key1 = swipeableState.direction) {
        if (swipeableState.direction == -1f) {
            if (currentScreen < 2) currentScreen = currentScreen + 1
            else if (currentScreen == 2) onSplashScreenComplete()
        } else if (swipeableState.direction == 1f) {
            if (currentScreen > 0) currentScreen = currentScreen - 1
        }
    }

    // Skip onboarding if not first launch
    LaunchedEffect(key1 = isFirstLaunch.value) {
        if (isFirstLaunch.value != true) {
            onSplashScreenComplete()
        }
    }

    // Auto-advance first screen only
    LaunchedEffect(currentScreen) {
        if (currentScreen == 0) {
            delay(8000)
            currentScreen = 1
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .swipeable(
                state = swipeableState,
                anchors = anchors,
                thresholds = { _, _ -> FractionalThreshold(0.3f) },
                orientation = Orientation.Horizontal
            )
    ) {
        // Main content area - 85% of screen
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(contentHeight)
                .align(Alignment.TopCenter)
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = tween(500)),
                exit = fadeOut(animationSpec = tween(500))
            ) {
                when (currentScreen) {
                    0 -> LogoScreen()
                    1 -> FeaturesScreen()
                    2 -> ScreenRecordPermissionsScreen()
                    3 -> AddMediaScreen()
                }
            }
        }

        // Navigation controls area - 15% of screen
        val totalIndicators = totalScreens + 1 // For 4 dots if totalScreens = 3

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(navHeight)
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(totalIndicators) { index ->
                    Box(
                        modifier = Modifier
                            .size(
                                width = if (index == currentScreen) 24.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index == currentScreen)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f)
                            )
                            .animateContentSize(
                                animationSpec = tween(300)
                            )
                    )
                    if (index < totalIndicators - 1) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            // Navigation buttons can go here if needed
        }

    }
}



@Composable
fun LogoScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App logo animation
        val scale = remember { androidx.compose.animation.core.Animatable(0.5f) }
        LaunchedEffect(Unit) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        // App logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "App Logo",
            modifier = Modifier
                .size(160.dp)
                .scale(scale.value)
                .padding(bottom = 0.dp),
            contentScale = ContentScale.Fit
        )

        // App name with animated fade-in
        var textVisible by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            delay(500)
            textVisible = true
        }

        AnimatedVisibility(
            visible = textVisible,
            enter = fadeIn(animationSpec = tween(1000))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.app_name_inside_app),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                // App tagline
                Text(
                    text = "Capture, Create, Share",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp)
                )

                // Swipe instruction
                Spacer(modifier = Modifier.height(40.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .alpha(0.7f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SwipeRight,
                        contentDescription = "Swipe",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Swipe to continue",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}


@Composable
fun FeaturesScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Discover Our Features",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Instructions for navigation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.TouchApp,
                    contentDescription = "Navigation Tip",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Swipe left/right to navigate",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        FeatureItem(
            icon = Icons.Filled.VideoCall,
            title = "Screen Recording",
            description = "Record your screen and capture system audio seamlessly. Automatically transcribe your recordings into easy-to-read notes."
        )

        FeatureItem(
            icon = Icons.Filled.PhotoLibrary,
            title = "Import Videos",
            description = "Add videos that can't be screen-recorded, using your phone's built-in screen recorder. Keep everything organized and transcribed."
        )

        FeatureItem(
            icon = Icons.Filled.Edit,
            title = "Write Your Own Notes",
            description = "Create and edit custom text notes anytime. Stay organized by combining written notes with your recordings."
        )

        FeatureItem(
            icon = Icons.Filled.ContentPaste,
            title = "Floating Clipboard",
            description = "Use a floating window to instantly copy and save content from other apps — no need to switch screens."
        )
    }
}


@Composable
fun ScreenRecordPermissionsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Required Permissions",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Illustration for screen recording
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Screen Recording Illustration",
            modifier = Modifier
                .size(200.dp)
                .padding(vertical = 24.dp),
            contentScale = ContentScale.Fit
        )

        // Instructions for navigation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Information",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "You'll need to grant these permissions to use all features",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        PermissionItem(
            icon = Icons.Filled.PhoneAndroid,
            title = "MediaCapture Service",
            description = "To transcribe audio effortlessly."
        )

        PermissionItem(
            icon = Icons.Filled.Cloud,
            title = "Display Over Other Apps",
            description = "For easy recording and categorisation of the internal system audio."
        )

        PermissionItem(
            icon = Icons.Filled.Mic,
            title = "Microphone",
            description = "To record audio."
        )

        PermissionItem(
            icon = Icons.Filled.PermMedia,
            title = "Media Access",
            description = "To upload your own videos/images."
        )
    }
}

@Composable
fun AddMediaScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Add Media",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Illustration for add media
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_monochrome),
            contentDescription = "Add Media Illustration",
            modifier = Modifier
                .size(200.dp)
                .padding(bottom = 24.dp),
            contentScale = ContentScale.Fit
        )

        MediaTypeCard(
            icon = Icons.Filled.Camera,
            title = "Camera",
            description = "Take a new photo or video"
        )

        MediaTypeCard(
            icon = Icons.Filled.PhotoLibrary,
            title = "Gallery",
            description = "Import from your photo library"
        )

        MediaTypeCard(
            icon = Icons.Filled.VideoCall,
            title = "Screen Recording",
            description = "Capture your screen activity"
        )
    }
}

@Composable
fun FeatureItem(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(text = description, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PermissionItem(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun MediaTypeCard(icon: ImageVector, title: String, description: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }

            Column(
                modifier = Modifier.padding(start = 16.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )

                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}