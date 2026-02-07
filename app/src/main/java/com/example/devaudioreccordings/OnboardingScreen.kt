package com.example.devaudioreccordings

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import com.example.compose.BlueLightScheme
import com.example.devaudioreccordings.ui.theme.blackScheme.BlackLightScheme
import com.example.devaudioreccordings.ui.theme.deepGreenScheme.DeepGreenLightScheme
import com.example.devaudioreccordings.ui.theme.greenScheme.GreenLightScheme
import com.example.devaudioreccordings.ui.theme.greyScheme.GreyLightScheme
import com.example.devaudioreccordings.ui.theme.purpleScheme.PurpleLightScheme
import com.example.devaudioreccordings.ui.theme.redScheme.RedLightScheme
import com.example.devaudioreccordings.ui.theme.tealScheme.TealLightScheme
import com.example.devaudioreccordings.ui.theme.whiteScheme.WhiteLightScheme
import com.example.devaudioreccordings.ui.theme.yellowScheme.YellowLightScheme
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.myapp.ridescribe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Onboarding Palette

private val OnboardingBg = Color.White
private val AccentOrange = Color(0xFFE8734A)
private val AccentOrangeLight = Color(0xFFFFF3EE)
private val AccentBlue = Color(0xFF4A8FD9)
private val AccentBlueLight = Color(0xFFEBF3FC)
private val TextPrimary = Color(0xFF1E293B)
private val TextSecondary = Color(0xFF64748B)
private val TextTertiary = Color(0xFF94A3B8)
private val LineColor = Color(0xFFE2E8F0)

private const val PAGE_COUNT = 4

// Main OnboardingScreen

@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit, viewModel: AppViewModel) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBg)
    ) {
        // Skip button (hidden on last page)
        if (pagerState.currentPage < PAGE_COUNT - 1) {
            TextButton(
                onClick = onOnboardingComplete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 12.dp, end = 8.dp)
            ) {
                Text("Skip", color = TextTertiary, fontSize = 14.sp)
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> WelcomePage()
                    1 -> FeaturesPage()
                    2 -> PersonalizePage(viewModel)
                    3 -> GetStartedPage(onOnboardingComplete)
                }
            }

            // Dot indicators + Next/Get Started button
            BottomBar(
                currentPage = pagerState.currentPage,
                onNext = {
                    if (pagerState.currentPage < PAGE_COUNT - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onOnboardingComplete()
                    }
                }
            )
        }
    }
}

// Page 1: Welcome

@Composable
private fun WelcomePage() {
    var visible by remember { mutableStateOf(false) }
    val bounceScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        visible = true
        bounceScale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(OnboardingBg)
    ) {
        // Floating clouds in background
        FloatingClouds()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(800)) + slideInVertically(
                    initialOffsetY = { -it / 4 },
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = 100f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.scale(bounceScale.value)) {
                        LogoMascot(size = 160.dp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Meet",
                        fontSize = 18.sp,
                        color = TextTertiary
                    )
                    Text(
                        text = stringResource(id = R.string.app_name_inside_app),
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Capture audio, transcribe with AI,\nand organize your notes\u2009\u2014\u2009all in one place.",
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

// Page 2: Features

private data class Feature(
    val icon: ImageVector,
    val title: String,
    val desc: String,
    val useOrange: Boolean = false
)

private val features = listOf(
    Feature(
        Icons.Filled.Mic,
        "Record & Transcribe",
        "Capture audio from any app \u2014 auto-transcribe and summarize with AI"
    ),
    Feature(
        Icons.Filled.PictureInPicture,
        "Floating Window",
        "Record or take notes while using YouTube, browsers, or any other app",
        useOrange = true
    ),
    Feature(
        Icons.Filled.ContentPaste,
        "Quick Capture",
        "Pop-up clipboard to grab content from any app, or write rich-text notes",
        useOrange = true
    ),
    Feature(
        Icons.Filled.AutoAwesome,
        "AI Powered",
        "Get formatted summaries, LinkedIn posts, and enhanced text in one tap"
    )
)

@Composable
private fun FeaturesPage() {
    var headerVisible by remember { mutableStateOf(false) }
    var trainVisible by remember { mutableStateOf(false) }
    val itemVisible = remember { List(features.size) { mutableStateOf(false) } }

    LaunchedEffect(Unit) {
        headerVisible = true
        delay(300)
        trainVisible = true
        delay(400)
        itemVisible.forEach { state ->
            state.value = true
            delay(150)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 48.dp, bottom = 16.dp)
    ) {
        // Header
        AnimatedVisibility(
            visible = headerVisible,
            enter = fadeIn(tween(500))
        ) {
            Column(modifier = Modifier.padding(start = 36.dp, end = 28.dp)) {
                Text(
                    text = "What I can do",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Everything you need, hands free",
                    fontSize = 14.sp,
                    color = TextTertiary
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Animated train scene
        AnimatedVisibility(
            visible = trainVisible,
            enter = fadeIn(tween(600)) + slideInHorizontally(
                initialOffsetX = { -it },
                animationSpec = tween(900, easing = FastOutSlowInEasing)
            )
        ) {
            TrainScene(modifier = Modifier.padding(horizontal = 20.dp))
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Feature items
        Column(modifier = Modifier.padding(start = 36.dp, end = 28.dp)) {
            features.forEachIndexed { i, feature ->
                FeatureTimelineItem(
                    visible = itemVisible[i].value,
                    icon = feature.icon,
                    title = feature.title,
                    desc = feature.desc,
                    isLast = i == features.lastIndex,
                    useOrangeAccent = feature.useOrange
                )
            }
        }
    }
}

// Page 3: Personalize

@Composable
private fun PersonalizePage(viewModel: AppViewModel) {
    val context = LocalContext.current
    var visible by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(viewModel.appTheme.value) }
    var selectedColor by remember { mutableStateOf(viewModel.appColorTheme.value.toString()) }

    val colorSchemeOptions = remember {
        linkedMapOf(
            AppColorTheme.GREEN.toString() to GreenLightScheme.primaryContainer,
            AppColorTheme.BLUE.toString() to BlueLightScheme.primaryContainer,
            AppColorTheme.RED.toString() to RedLightScheme.primaryContainer,
            AppColorTheme.YELLOW.toString() to YellowLightScheme.primaryContainer,
            AppColorTheme.TEAL.toString() to TealLightScheme.primaryContainer,
            AppColorTheme.PURPLE.toString() to PurpleLightScheme.primaryContainer,
            AppColorTheme.GREY.toString() to GreyLightScheme.primaryContainer,
            AppColorTheme.BLACK.toString() to BlackLightScheme.primaryContainer,
            AppColorTheme.WHITE.toString() to WhiteLightScheme.primaryContainer,
            AppColorTheme.DEEP_GREEN.toString() to DeepGreenLightScheme.primaryContainer
        )
    }

    // Add SYSTEM option dynamically (needs context)
    val systemColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        dynamicDarkColorScheme(context).primaryContainer
    else GreenLightScheme.primaryContainer

    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 48.dp)
        ) {
            Text(
                text = "Make it yours",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pick a theme and color scheme",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Theme Selection
            Text(
                text = "Theme",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ThemeOptionChip(
                    label = "Light",
                    icon = Icons.Outlined.LightMode,
                    isSelected = selectedTheme == AppTheme.LIGHT.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTheme = AppTheme.LIGHT.toString()
                        viewModel.appTheme.value = AppTheme.LIGHT.toString()
                        CoroutineScope(Dispatchers.IO).launch {
                            context.user.edit { it[DataStoreKeys.App_Theme] = AppTheme.LIGHT.toString() }
                        }
                    }
                )
                ThemeOptionChip(
                    label = "Dark",
                    icon = Icons.Outlined.DarkMode,
                    isSelected = selectedTheme == AppTheme.DARK.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTheme = AppTheme.DARK.toString()
                        viewModel.appTheme.value = AppTheme.DARK.toString()
                        CoroutineScope(Dispatchers.IO).launch {
                            context.user.edit { it[DataStoreKeys.App_Theme] = AppTheme.DARK.toString() }
                        }
                    }
                )
                ThemeOptionChip(
                    label = "System",
                    icon = Icons.Outlined.PhoneAndroid,
                    isSelected = selectedTheme == AppTheme.SYSTEM.toString(),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        selectedTheme = AppTheme.SYSTEM.toString()
                        viewModel.appTheme.value = AppTheme.SYSTEM.toString()
                        CoroutineScope(Dispatchers.IO).launch {
                            context.user.edit { it[DataStoreKeys.App_Theme] = AppTheme.SYSTEM.toString() }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Color Scheme Selection
            Text(
                text = "Color Scheme",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.height(280.dp)
            ) {
                // Regular color options
                itemsIndexed(colorSchemeOptions.keys.toList()) { _, colorName ->
                    ColorCircle(
                        color = colorSchemeOptions[colorName]!!,
                        isSelected = selectedColor == colorName,
                        onClick = {
                            selectedColor = colorName
                            viewModel.appColorTheme.value = colorName
                            CoroutineScope(Dispatchers.IO).launch {
                                context.user.edit { it[DataStoreKeys.Color_Scheme] = colorName }
                            }
                        }
                    )
                }
                // System color option
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(systemColor)
                                .then(
                                    if (selectedColor == AppColorTheme.SYSTEM.toString())
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                )
                                .clickable {
                                    selectedColor = AppColorTheme.SYSTEM.toString()
                                    viewModel.appColorTheme.value = AppColorTheme.SYSTEM.toString()
                                    CoroutineScope(Dispatchers.IO).launch {
                                        context.user.edit {
                                            it[DataStoreKeys.Color_Scheme] = AppColorTheme.SYSTEM.toString()
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = "System",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            if (selectedColor == AppColorTheme.SYSTEM.toString()) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp).offset(x = 10.dp, y = 10.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("System", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val borderColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.outlineVariant
    val bgColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val contentColor = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            )
            .background(bgColor)
            .clickable { onClick() }
            .padding(vertical = 16.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor
        )
        if (isSelected) {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            )
        }
    }
}

@Composable
private fun ColorCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                else Modifier.border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Page 4: Get Started

@Composable
private fun GetStartedPage(onOnboardingComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    val bgColor = MaterialTheme.colorScheme.background
    val cloud1 = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    val cloud2 = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Clouds in background (themed)
        FloatingClouds(cloudColor1 = cloud1, cloudColor2 = cloud2)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(600)) + slideInVertically(
                    initialOffsetY = { it / 3 },
                    animationSpec = spring(dampingRatio = 0.7f, stiffness = 120f)
                )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LogoMascot(size = 120.dp)

                    Spacer(modifier = Modifier.height(28.dp))

                    Text(
                        text = "You\u2019re all set!",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Start recording, importing, or writing\u2009\u2014\nyour notes are waiting.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(36.dp))

                    Button(
                        onClick = onOnboardingComplete,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Get Started",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

// Bottom Bar (Dots + Button)

@Composable
private fun BottomBar(currentPage: Int, onNext: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Dot indicators
        val dotColor = MaterialTheme.colorScheme.primary
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(PAGE_COUNT) { index ->
                Box(
                    modifier = Modifier
                        .size(if (index == currentPage) 24.dp else 8.dp, 8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (index == currentPage) dotColor
                            else dotColor.copy(alpha = 0.25f)
                        )
                )
            }
        }

        // Next / Get Started button
        Button(
            onClick = onNext,
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = if (currentPage == PAGE_COUNT - 1) "Get Started" else "Next",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

// Feature Timeline Item

@Composable
private fun FeatureTimelineItem(
    visible: Boolean,
    icon: ImageVector,
    title: String,
    desc: String,
    isLast: Boolean = false,
    useOrangeAccent: Boolean = false
) {
    val accent = if (useOrangeAccent) AccentOrange else AccentBlue
    val iconBg = if (useOrangeAccent) AccentOrangeLight else AccentBlueLight

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it / 5 },
            animationSpec = tween(400, easing = FastOutSlowInEasing)
        ) + fadeIn(tween(400))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            // Vertical line + dot
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .fillMaxHeight(if (isLast) 0.5f else 1f)
                        .background(LineColor)
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.5f))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 22.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon, title,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 2.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = desc,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// Floating Clouds

@Composable
private fun FloatingClouds(
    cloudColor1: Color = Color(0xFFDCEBFA),
    cloudColor2: Color = Color(0xFFFDE8DF)
) {
    val inf = rememberInfiniteTransition(label = "clouds")

    val drift1 by inf.animateFloat(
        initialValue = -18f, targetValue = 18f,
        animationSpec = infiniteRepeatable(
            tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "d1"
    )
    val drift2 by inf.animateFloat(
        initialValue = 14f, targetValue = -14f,
        animationSpec = infiniteRepeatable(
            tween(20000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "d2"
    )
    val vert by inf.animateFloat(
        initialValue = -6f, targetValue = 6f,
        animationSpec = infiniteRepeatable(
            tween(12000, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "vd"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawCloud(w * 0.82f + drift1, h * 0.05f + vert, 2.0f, cloudColor1)
        drawCloud(w * 0.10f + drift2, h * 0.10f - vert * 0.5f, 1.3f, cloudColor2)
        drawCloud(w * 0.90f + drift2 * 0.6f, h * 0.45f + vert * 0.7f, 1.4f, cloudColor1)
        drawCloud(w * 0.05f + drift1 * 0.4f, h * 0.75f - vert * 0.4f, 1.2f, cloudColor2)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(
    cx: Float, cy: Float, scale: Float, color: Color
) {
    val r = 24f * scale
    drawCircle(color, r * 1.0f, Offset(cx - r * 1.4f, cy + r * 0.4f))
    drawCircle(color, r * 1.1f, Offset(cx, cy + r * 0.4f))
    drawCircle(color, r * 1.0f, Offset(cx + r * 1.4f, cy + r * 0.4f))
    drawCircle(color, r * 1.15f, Offset(cx - r * 0.6f, cy - r * 0.3f))
    drawCircle(color, r * 1.3f, Offset(cx + r * 0.5f, cy - r * 0.5f))
}

// Logo Mascot

@Composable
fun LogoMascot(
    size: Dp = 140.dp,
    showEyes: Boolean = true,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascotFloat")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f, targetValue = 5f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "float"
    )

    Box(
        modifier = modifier
            .size(size)
            .offset(y = floatOffset.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = stringResource(id = R.string.app_name_inside_app),
            modifier = Modifier.size(size),
            contentScale = ContentScale.Fit
        )
        if (showEyes) {
            Canvas(modifier = Modifier.size(size)) {
                val w = this.size.width
                val h = this.size.height
                val eyeY = h * 0.22f
                val eyeSpacing = w * 0.12f
                val centerX = w * 0.48f
                val eyeOuterR = w * 0.055f
                val pupilR = w * 0.03f

                drawCircle(Color.White, eyeOuterR, Offset(centerX - eyeSpacing, eyeY))
                drawCircle(Color(0xFF2D2D2D), pupilR, Offset(centerX - eyeSpacing + 1f, eyeY + 1f))
                drawCircle(Color.White, pupilR * 0.4f, Offset(centerX - eyeSpacing - 1f, eyeY - 2f))
                drawCircle(Color.White, eyeOuterR, Offset(centerX + eyeSpacing, eyeY))
                drawCircle(Color(0xFF2D2D2D), pupilR, Offset(centerX + eyeSpacing + 1f, eyeY + 1f))
                drawCircle(Color.White, pupilR * 0.4f, Offset(centerX + eyeSpacing - 1f, eyeY - 2f))
            }
        }
    }
}

// Train Scene

@Composable
private fun TrainScene(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "train")
    val trainOffset by infiniteTransition.animateFloat(
        initialValue = -12f, targetValue = 12f,
        animationSpec = infiniteRepeatable(
            tween(3500, easing = FastOutSlowInEasing), RepeatMode.Reverse
        ), label = "trainMove"
    )

    val trainBody = AccentBlueLight
    val trainAccent = AccentBlue
    val windowColor = AccentBlue.copy(alpha = 0.25f)
    val trackColor = TextTertiary.copy(alpha = 0.25f)
    val wheelColor = TextSecondary.copy(alpha = 0.4f)

    Canvas(modifier = modifier.fillMaxWidth().height(80.dp)) {
        val w = size.width
        val h = size.height

        // Track
        drawLine(trackColor, Offset(0f, h * 0.84f), Offset(w, h * 0.84f), strokeWidth = 2.5f)
        val tieCount = 22
        for (i in 0..tieCount) {
            val x = (w / tieCount) * i
            drawLine(trackColor, Offset(x, h * 0.80f), Offset(x, h * 0.88f), strokeWidth = 1.5f)
        }

        val tx = w * 0.12f + trainOffset

        drawRoundRect(trainBody, Offset(tx, h * 0.28f), Size(w * 0.72f, h * 0.48f), CornerRadius(14f))
        drawRect(trainAccent, Offset(tx, h * 0.56f), Size(w * 0.72f, h * 0.06f))
        drawRoundRect(trainBody, Offset(tx + w * 0.67f, h * 0.33f), Size(w * 0.1f, h * 0.40f), CornerRadius(22f))

        // Orange accent stripe at top
        drawRect(AccentOrange.copy(alpha = 0.6f), Offset(tx, h * 0.28f), Size(w * 0.72f, h * 0.04f))

        val winW = w * 0.07f
        val winH = h * 0.14f
        val winY = h * 0.36f
        for (i in 0 until 6) {
            drawRoundRect(windowColor, Offset(tx + w * 0.04f + i * (winW + w * 0.035f), winY), Size(winW, winH), CornerRadius(3f))
        }
        drawRoundRect(windowColor, Offset(tx + w * 0.68f, winY), Size(w * 0.055f, winH), CornerRadius(3f))

        drawCircle(AccentOrange, 3.5f, Offset(tx + w * 0.77f, h * 0.62f))

        val wheelR = h * 0.05f
        for (pos in listOf(0.1f, 0.22f, 0.48f, 0.6f)) {
            drawCircle(wheelColor, wheelR, Offset(tx + w * pos, h * 0.80f))
        }

        // Person silhouette with headphones in window 2
        val pX = tx + w * 0.04f + 1 * (winW + w * 0.035f) + winW * 0.5f
        val pY = winY + winH * 0.3f
        drawCircle(trainAccent.copy(alpha = 0.5f), winW * 0.16f, Offset(pX, pY))
        drawLine(trainAccent.copy(alpha = 0.5f), Offset(pX, pY + winW * 0.16f), Offset(pX, pY + winH * 0.45f), winW * 0.1f)
        drawArc(
            AccentOrange.copy(alpha = 0.7f), 200f, 140f, false,
            Offset(pX - winW * 0.22f, pY - winW * 0.26f),
            Size(winW * 0.44f, winW * 0.28f),
            style = Stroke(1.8f)
        )
    }
}
