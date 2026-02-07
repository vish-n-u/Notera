package com.example.devaudioreccordings.pages.Settings

// Add this import statement at the top of your file

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.navigation.NavHostController
import com.example.compose.BlueLightScheme
import com.example.devaudioreccordings.AppColorTheme
import com.example.devaudioreccordings.AppTheme
import com.example.devaudioreccordings.DataStoreKeys
import com.example.devaudioreccordings.Routes
import com.example.devaudioreccordings.pages.Homepage.milliSecToMin
import com.example.devaudioreccordings.ui.theme.blackScheme.BlackLightScheme
import com.example.devaudioreccordings.ui.theme.deepGreenScheme.DeepGreenLightScheme
import com.example.devaudioreccordings.ui.theme.greenScheme.GreenLightScheme
import com.example.devaudioreccordings.ui.theme.greyScheme.GreyLightScheme
import com.example.devaudioreccordings.ui.theme.purpleScheme.PurpleLightScheme
import com.example.devaudioreccordings.ui.theme.redScheme.RedLightScheme
import com.example.devaudioreccordings.ui.theme.tealScheme.TealLightScheme
import com.example.devaudioreccordings.ui.theme.whiteScheme.WhiteLightScheme
import com.example.devaudioreccordings.ui.theme.yellowScheme.YellowLightScheme
import com.example.devaudioreccordings.user
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.myapp.ridescribe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appViewModel: AppViewModel,
    navHostController: NavHostController
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dataStoreData = appViewModel.dataStoreDataFlow.collectAsState(initial = emptyPreferences())
    val searchData = remember {
        mutableStateOf("")

    }


    // Theme options
    val themeOptions =
        listOf(AppTheme.LIGHT.toString(), AppTheme.DARK.toString(), AppTheme.SYSTEM.toString())
//    val colorSchemeOptions = listOf("System", "Blue", "Green", "Purple", "Orange", "Red", "Teal")
    val colorSchemeColors = listOf(
//        Color(0xFFFF9999), // Light Red/Pink
//        Color(0xFFFFB3BA), // Pink
//        Color(0xFFE6B3FF), // Light Purple
//        Color(0xFFD1C4E9), // Lavender
//        Color(0xFFB3D9FF), // Light Blue
//        Color(0xFF87CEEB), // Sky Blue
//        Color(0xFF40E0D0), // Turquoise
//        Color(0xFF00CED1), // Dark Turquoise
//        Color(0xFF90EE90), // Light Green
//        Color(0xFF98FB98), // Pale Green
//        Color(0xFFADFF2F), // Green Yellow
//        Color(0xFFFFFF99), // Light Yellow
//        Color(0xFFFFD700), // Gold
//        Color(0xFFFFA500), // Orange
        Color(0xFFDEB887), // Burlywood/Beige
        Color(0xFFFFFFFF), // White
        Color(0xFF2F4F4F)  // Dark Slate Gray
    )
    val colorSchemeOptions = mapOf<String, Color>(
//        "Burlywood/Beige" to Color(0xFFDEB887),
//        "Dark Slate Gray" to  Color(0xFF2F4F4F),
        AppColorTheme.GREEN.toString() to GreenLightScheme.primaryContainer,
        AppColorTheme.BLUE.toString() to BlueLightScheme.primaryContainer,
        AppColorTheme.RED.toString() to RedLightScheme.primaryContainer,
        AppColorTheme.YELLOW.toString() to YellowLightScheme.primaryContainer,
        AppColorTheme.TEAL.toString() to TealLightScheme.primaryContainer,
        AppColorTheme.PURPLE.toString() to PurpleLightScheme.primaryContainer,
        AppColorTheme.GREY.toString() to GreyLightScheme.primaryContainer,
        AppColorTheme.BLACK.toString() to BlackLightScheme.primaryContainer,
        AppColorTheme.WHITE.toString() to WhiteLightScheme.primaryContainer,
        AppColorTheme.DEEP_GREEN.toString() to DeepGreenLightScheme.primaryContainer,
        AppColorTheme.SYSTEM.toString() to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) dynamicDarkColorScheme(
            context
        ).primaryContainer else GreenLightScheme.primaryContainer

    )
    val openLimitIncreaseDialog = remember { mutableStateOf(false) }
    val fullData = appViewModel.fullData.collectAsState()

    val audioTextList = fullData
    val headerList: MutableList<String> = audioTextList.value.map { it ->
        it.header
    }.toMutableList()
    headerList.add(0, "None")

    // Settings state

    var showBuyMeCoffeeDialog by remember { mutableStateOf(false) }
    var showUsageStatsDialog by remember { mutableStateOf(false) }
    // New state for theme and color scheme dialogs
    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorSchemeDialog by remember { mutableStateOf(false) }
    var selectedTheme by remember { mutableStateOf(appViewModel.appTheme.value) } // 0: Light, 1: Dark, 2: System
    var selectedColorScheme by remember { mutableStateOf(appViewModel.appColorTheme.value.toString()) } // 0: System, 1: Blue, 2: Green, etc.


    val calendar = Calendar.getInstance()


    fun onNavigateBack() {
        navHostController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onNavigateBack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Transparent)
                .verticalScroll(rememberScrollState())
        ) {
            // Usage Stats Button
            SettingsItem(
                icon = Icons.Outlined.DataUsage,
                title = "View Usage Statistics",
                subtitle = "Check your transcription and enhancement usage",
                onClick = { showUsageStatsDialog = true }
            )

            // Premium Upgrade
            SettingsItem(
                icon = Icons.Outlined.Workspaces,
                title = "Upgrade to Premium",
                subtitle = "Get unlimited transcription time and features",
                onClick = {
                    Toast.makeText(
                        context,
                        "Upgrade feature will be added in update",
                        Toast.LENGTH_LONG
                    ).show()
                },
                badgeText = "PRO",
                badgeColor = MaterialTheme.colorScheme.tertiary
            )

            // Notification Settings Section
            SettingsHeader(title = "Preferences")


//            RadioSettingsItem(
//                icon = Icons.Outlined.DarkMode,
//                title = "App Theme",
//                selectedOptionIndex = if(useAppTheme) 0 else 1,
//                onOptionSelected = {
//                    var selecttBoolean = if(it==0) true else false
//                    useAppTheme = selecttBoolean
//                    appViewModel.useAppTheme.value = selecttBoolean
//
//                    CoroutineScope(Dispatchers.IO).launch {
//                        context.user.edit { value ->
//                            value[DataStoreKeys.Use_App_Theme] = selecttBoolean
//
//                        }
//                    }
//                }
//            )
            SettingsItem2(
                icon = Icons.Outlined.DarkMode,
                title = "App Theme",
                subtitle = selectedTheme,
                onClick = { showThemeDialog = true }
            )

            // Color Scheme Setting
            SettingsItem2(
                icon = Icons.Outlined.Palette,
                title = "Color Scheme",
                subtitle = selectedColorScheme,
                onClick = { showColorSchemeDialog = true },
                trailingContent = {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(
                                color = colorSchemeOptions[selectedColorScheme]!!,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                    )
                }
            )

            SettingsItem(
                icon = Icons.Outlined.PlayArrow,
                title = "Onboarding",
                subtitle = "View the onboarding screens again",
                onClick = {
                    appViewModel.showSplashPreview.value = true
                }
            )

            // Support Section
            SettingsHeader(title = "Support")

            SettingsItem(
                icon = Icons.Outlined.LocalCafe,
                title = "Buy Me a Coffee ❤\uFE0F",
                subtitle = "Support the development of this app",
                onClick = { showBuyMeCoffeeDialog = true }
            )

            SettingsItem(
                icon = Icons.Outlined.Email,
                title = "Contact Support",
                subtitle = "Get help with the app",
                onClick = {
                    val manufacturer = Build.MANUFACTURER
                    val model = Build.MODEL
                    val device = Build.DEVICE
                    val brand = Build.BRAND
                    val product = Build.PRODUCT
                    val hardware = Build.HARDWARE
                    val versionRelease = Build.VERSION.RELEASE
                    val sdkInt = Build.VERSION.SDK_INT

                    val info = """
        Device Info:
        • Manufacturer: $manufacturer
        • Model: $model
        • Device: $device
        • Brand: $brand
        • Product: $product
        • Hardware: $hardware
        • Android Version: $versionRelease
        • SDK: $sdkInt
    """.trimIndent()

                    val subject = "Support Request"
                    val body = info

                    val uri = Uri.parse(
                        "mailto:ridescribenotes@gmail.com" +
                                "?subject=" + Uri.encode(subject) +
                                "&body=" + Uri.encode(body)
                    )

                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = uri
                    }


                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
                    }
                }
            )


            // About Section
            SettingsHeader(title = "About")

            val uid = dataStoreData.value[DataStoreKeys.USER_UID] ?: ""
            SettingsItem(
                icon = Icons.Outlined.Person,
                title = "User ID",
                subtitle = if (uid.length > 12) "${uid.take(12)}..." else uid,
                onClick = {
                    val clipboardManager =
                        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("User ID", uid)
                    clipboardManager.setPrimaryClip(clip)
                    Toast.makeText(context, "User ID copied to clipboard", Toast.LENGTH_SHORT)
                        .show()
                }
            )
//            SettingsItem(
//                icon = Icons.Outlined.Code,
//                title = "Vishnu Nair",
//                subtitle = "Software Engineer",
//                onClick = {
//                    val intent = Intent(
//                        Intent.ACTION_VIEW,
//                        Uri.parse("https://www.linkedin.com/in/vishnu-nair-%F0%9F%9B%A9%EF%B8%8F-439472204/")
//                    ).apply {
//                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                    }
//                    context.startActivity(intent)
//                }
//            )
            SettingsItem(
                icon = Icons.Outlined.Lock,
                title = "Privacy Policy",
                subtitle = "Learn how your data is stored",
                onClick = {
                    navHostController.navigate(Routes.PrivacyPolicy.name)
                }
            )


            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "App Version",
                subtitle = "1.9.8 (Build 101)",
                onClick = { /* Show version details */ }
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Choose app theme") },
            text = {
                Column(
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    // Light Theme Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedTheme = AppTheme.LIGHT.toString()
                                appViewModel.appTheme.value = AppTheme.LIGHT.toString()
                                // Save to DataStore
                                CoroutineScope(Dispatchers.IO).launch {
                                    context.user.edit { value ->
                                        value[DataStoreKeys.App_Theme] = AppTheme.LIGHT.toString()
                                    }
                                }
                            }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Light",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedTheme == AppTheme.LIGHT.toString()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Dark Theme Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedTheme = AppTheme.DARK.toString()
                                appViewModel.appTheme.value = AppTheme.DARK.toString()
                                // Save to DataStore
                                CoroutineScope(Dispatchers.IO).launch {
                                    context.user.edit { value ->
                                        value[DataStoreKeys.App_Theme] = AppTheme.DARK.toString()
                                    }
                                }
                            }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DarkMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Dark",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (selectedTheme == AppTheme.DARK.toString()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Follow System Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                selectedTheme = AppTheme.SYSTEM.toString()
                                // Save to DataStore
                                appViewModel.appTheme.value = AppTheme.SYSTEM.toString()
                                CoroutineScope(Dispatchers.IO).launch {
                                    context.user.edit { value ->
                                        value[DataStoreKeys.App_Theme] = AppTheme.SYSTEM.toString()
                                    }
                                }
                            }
                            .padding(vertical = 16.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhoneAndroid,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "Follow System",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Matches system theme",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedTheme == AppTheme.SYSTEM.toString()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Cancel Button

                }
            },
            confirmButton = { },
            dismissButton = { }
        )
    }

    // Color Scheme Selection Dialog
    if (showColorSchemeDialog) {
        AlertDialog(
            onDismissRequest = { showColorSchemeDialog = false },
            title = { Text("Color scheme") },
            text = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.height(280.dp)
                ) {
                    itemsIndexed(colorSchemeOptions.keys.toList()) { index, colorName ->
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(60.dp)
                                    .background(
                                        color = colorSchemeOptions[colorName]!!,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        selectedColorScheme = colorName
                                        // Save to colorName
                                        appViewModel.appColorTheme.value = colorName
                                        CoroutineScope(Dispatchers.IO).launch {
                                            context.user.edit { value ->
                                                value[DataStoreKeys.Color_Scheme] = colorName
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (colorName == AppColorTheme.SYSTEM.toString()) {
                                    Icon(
                                        imageVector = Icons.Default.Android,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )

                                }
                                if (selectedColorScheme == colorName) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (colorName == "SYSTEM") Color.Black else if (colorSchemeOptions[colorName] == Color.White || colorSchemeOptions[colorName]!!.luminance() > 0.5f) {
                                            Color.Black
                                        } else {
                                            Color.White
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                            }
                            if (colorName == AppColorTheme.SYSTEM.toString()) {
                                Text(
                                    "System Theme",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showColorSchemeDialog = false }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {}

        )
    }


    // Usage Stats Dialog
    if (showUsageStatsDialog) {
        AlertDialog(
            onDismissRequest = { showUsageStatsDialog = false },
            title = { Text("Usage Statistics") },
            icon = { Icon(Icons.Default.DataUsage, contentDescription = null) },
            text = {
                ImprovedUsageStats(dataStoreData, openLimitIncreaseDialog, appViewModel)
            },
            confirmButton = {
                Button(
                    onClick = { showUsageStatsDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Close")
                }
            }
        )
    }

    if (openLimitIncreaseDialog.value) {
        LimitIncreaseDialog({
            openLimitIncreaseDialog.value = false
        }, {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://buymeacoffee.com/ridescriben")
            )
            context.startActivity(intent)
        }, context)

    }


    // Buy Me Coffee Dialog
    if (showBuyMeCoffeeDialog) {
        AlertDialog(
            onDismissRequest = { showBuyMeCoffeeDialog = false },
            icon = { Icon(Icons.Default.LocalCafe, contentDescription = null) },
            title = { Text("Powered by Coffee ☕") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "This app runs on code, caffeine, and kindness.\n\nIf it made your day a little easier, maybe fuel the next update? 😄",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://buymeacoffee.com/ridescriben")
                        )
                        context.startActivity(intent)
                        showBuyMeCoffeeDialog = false
                    }
                ) {
                    Text("Send Coffee ❤\uFE0F")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBuyMeCoffeeDialog = false }) {
                    Text("Nah, I'm good")
                }
            }
        )
    }


}

@Composable
fun ImprovedUsageStats(
    dataStoreData: State<androidx.datastore.preferences.core.Preferences>,
    openLimitIncreaseDialog: MutableState<Boolean>,
    appViewModel: AppViewModel
) {
    val context = LocalContext.current
    val uid = dataStoreData.value.get(DataStoreKeys.USER_UID)
    // Extract usage data
    val usedTranscriptionTimeInMin =
        dataStoreData.value[DataStoreKeys.Used_Transcription_Duration]?.let {
            milliSecToMin(it)
        } ?: 0
    val totalTranscriptionTimeInMin =
        dataStoreData.value[DataStoreKeys.Total_Transcription_Duration]?.let {
            milliSecToMin(it)
        } ?: 0

    val usedTextEnhanceCount = dataStoreData.value[DataStoreKeys.Used_Enhance_Text_Count] ?: 0
    val totalTextEnhanceCount = dataStoreData.value[DataStoreKeys.Total_Enhance_Text_Count] ?: 10

    val usedLinkedinTextEnhance =
        dataStoreData.value[DataStoreKeys.Used_Linkedin_Text_Conversion_Count] ?: 0
    val totalLinkedinTextEnhance =
        dataStoreData.value[DataStoreKeys.Total_Linkedin_Text_Conversion_Count] ?: 0

    // Calculate progress values
    val transcriptionProgress = if (totalTranscriptionTimeInMin > 0) {
        (usedTranscriptionTimeInMin.toFloat() / totalTranscriptionTimeInMin)
    } else {
        0f
    }

    val enhanceProgress = if (totalTextEnhanceCount > 0) {
        (usedTextEnhanceCount.toFloat() / totalTextEnhanceCount)
    } else {
        0f
    }

    val linkedinProgress = if (totalLinkedinTextEnhance > 0) {
        (usedLinkedinTextEnhance.toFloat() / totalLinkedinTextEnhance)
    } else {
        0f
    }

    // Animate progress values
    val animatedTranscriptionProgress by animateFloatAsState(
        targetValue = transcriptionProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "transcription_progress"
    )

    val animatedEnhanceProgress by animateFloatAsState(
        targetValue = enhanceProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "enhance_progress"
    )

    val animatedLinkedinProgress by animateFloatAsState(
        targetValue = linkedinProgress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "linkedin_progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Transcription Usage
        UsageProgressItem(
            title = "Transcription Time",
            usedValue = usedTranscriptionTimeInMin.toInt(),
            totalValue = totalTranscriptionTimeInMin.toInt(),
            unit = "min",
            progress = animatedTranscriptionProgress,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Text Enhance
        UsageProgressItem(
            title = "Text Enhancement",
            usedValue = usedTextEnhanceCount,
            totalValue = totalTextEnhanceCount,
            unit = "uses",
            progress = animatedEnhanceProgress,
            color = MaterialTheme.colorScheme.secondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        // LinkedIn Enhance
        if (totalLinkedinTextEnhance > 0) {
            UsageProgressItem(
                title = "LinkedIn Enhancement",
                usedValue = usedLinkedinTextEnhance,
                totalValue = totalLinkedinTextEnhance,
                unit = "uses",
                progress = animatedLinkedinProgress,
                color = MaterialTheme.colorScheme.tertiary
            )

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Request increase button
        OutlinedButton(
            onClick = {
                openLimitIncreaseDialog.value = true
//                val mailtoUri = Uri.parse(
//                    "mailto:ridescribenotes@gmail.com?subject=Request to Increase Usage Limit&body=${
//                        "Hey team,\n" +
//                                "\n" +
//                                "I would like to increase my usage limit.\n" +
//                                "Here is my UID: $uid\n" +
//                                "\n" +
//                                "Thanks!"
//                    }"
//                )
//                val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
//                    data = mailtoUri
//                    putExtra(Intent.EXTRA_SUBJECT, "Request to Increase Usage Limit")
//                    putExtra(
//                        Intent.EXTRA_TEXT,
//                        "Hey team,\n\nI would like to increase my usage limit.\nHere is my UID: $uid\n\nThanks!"
//                    )
//                }
//
                try {
                    CoroutineScope(Dispatchers.IO).launch {
                        appViewModel.increaseLimit("Hey team,\\n\\nI would like to increase my usage limit.\\nHere is my UID: $uid\\n\\nThanks!")

                    }
                    Toast.makeText(context, "Request Successfully Sent!", Toast.LENGTH_SHORT).show()

                } catch (e: Throwable) {
                    Toast.makeText(context, "Some error occurred Please Retry", Toast.LENGTH_SHORT)
                        .show()
                }

            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Upgrade,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Request Limit Increase")
        }
    }
}


@Composable
fun UsageProgressItem(
    title: String,
    usedValue: Int,
    totalValue: Int,
    unit: String,
    progress: Float,
    color: Color
) {
    Column(modifier = Modifier.background(Color.Transparent)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Transparent),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = "$usedValue/$totalValue $unit",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(24.dp)
                    .padding(start = 8.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
            }

            if (badgeText != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = badgeColor,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = badgeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }

    Divider(
        modifier = Modifier.padding(start = 72.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}


// Add this button to a visible location in your main UI


// Helper function to schedule the notification


@Composable
fun LimitIncreaseDialog(
    onDismiss: () -> Unit,
    onDonateClick: () -> Unit,
    context: Context = LocalContext.current
) {
    val isConnected by remember { mutableStateOf(isInternetAvailable(context)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Default.Favorite else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isConnected) MaterialTheme.colorScheme.primary else Color(0xFFFF5722)
                )
                Text(
                    text = if (isConnected) "Support Our App" else "No Internet Connection",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isConnected) {
                    Text(
                        text = "Thank you for using ${stringResource( R.string.app_name_inside_app)}! 🎉",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )

                    Text(
                        text = "To maintain the same great user experience and keep our app completely ad-free, we have ongoing running costs for:",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("• Server maintenance", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "• App updates & improvements",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("• Bug fixes & support", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Would you like to support us with a small donation? Every contribution helps us keep the app free and ad-free for everyone! ☕",
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = "Internet connection is not available. Please check your connection and try again.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Make sure you have:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Column(
                        modifier = Modifier.padding(start = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "• Wi-Fi or mobile data enabled",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("• Strong signal strength", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "• No network restrictions",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isConnected) {
                Button(
                    onClick = {
                        onDonateClick()
//                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Buy Me a Coffee")
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Retry")
                }
            }
        },
        dismissButton = {
            if (isConnected) {
                TextButton(onClick = onDismiss) {
                    Text("Maybe Later")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    )
}


fun isInternetAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    } else {
        @Suppress("DEPRECATION")
        val networkInfo = connectivityManager.activeNetworkInfo
        networkInfo?.isConnected == true
    }
}


@Composable
fun SettingsItem2(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    badgeText: String? = null,
    badgeColor: Color = MaterialTheme.colorScheme.primary,
    trailingContent: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (badgeText != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = badgeColor,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(start = 4.dp)
                        ) {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            trailingContent?.invoke()
        }
    }
}

