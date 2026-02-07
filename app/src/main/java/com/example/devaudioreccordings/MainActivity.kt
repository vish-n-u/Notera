package com.example.devaudioreccordings

import android.app.Activity
import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.database.AudioTextDatabase
import com.example.devaudioreccordings.database.FlowType
import com.example.devaudioreccordings.services.FloatingTextWindowService
import com.example.devaudioreccordings.services.FloatingWindow
import com.example.devaudioreccordings.services.MediaCaptureService
import com.example.devaudioreccordings.ui.theme.AppThemexx
import com.example.devaudioreccordings.viewModals.AddMediaViewModel
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.example.devaudioreccordings.viewModals.convertAudioTextToAudioTextDbData
import com.google.firebase.BuildConfig
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.initialize
import com.myapp.ridescribe.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {
    private lateinit var mediaProjectionManager: MediaProjectionManager
    private lateinit var mediaProjectionLauncher: ActivityResultLauncher<Intent>
    private val MEDIA_PROJECTION_REQUEST_CODE = 13
    private lateinit var auth: FirebaseAuth
    private lateinit var database: AudioTextDatabase
    private lateinit var pickVideoLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var broadCastReciever: BroadcastReceiver
    var mediaCaptureIntent: Intent? = null
    val appViewModel: AppViewModel by viewModels()
    var showDialogBox by mutableStateOf(false)
    var showDialogBoxOf by mutableStateOf<dialogBoxPermissionCategory?>(null)
    var showErrorDialogBox = mutableStateOf(false)
    var showErrorMessage = mutableStateOf("")
    var showSplashScreen = mutableStateOf(true)


    val addMediaViewModel = AddMediaViewModel()

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults) // ✅ Call first


        if (grantResults.isNotEmpty() && permissions.isNotEmpty()) { // ✅ Prevent crashes


            if (requestCode == 10) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) { // ✅ Correct check
                    val captureScreen = mediaProjectionManager.createScreenCaptureIntent()

                    mediaProjectionLauncher.launch(captureScreen)
                } else {
//                    showMicrophoneAccessDialogBox =
//                        true // ✅ Update state to show the Compose dialog
                    showDialogBox = true
                    showDialogBoxOf = dialogBoxPermissionCategory.microphone
                }
            }

        }
        if (requestCode == 12) {
            if (grantResults[0] == 0) { // ✅ Correct check
                val intent = Intent(Intent.ACTION_PICK)
                intent.type = "video/*"
                ActivityCompat.startActivityForResult(this, intent, 1, null)
            } else {
                showDialogBox = true
                showDialogBoxOf = dialogBoxPermissionCategory.videoMedia
//                showMicrophoneAccessDialogBox =
//                    true // ✅ Update state to show the Compose dialog
            }

        } else {
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val selectedVideoUri: Uri? = data?.data
            selectedVideoUri?.let {
                // Do something with the selected video, like showing it in a video player or saving the URI

                val inputfile = getFileFromUri(this, uri = it)
                val inputPath = inputfile?.absolutePath
                if (inputPath != null) {
                    val fileName = "${System.currentTimeMillis()}.wav"
                    extractAudioFromVideo(
                        inputfile,
                        fileName,
                        applicationContext,
                        addMediaViewModel.audioFileName,
                        addMediaViewModel,
                        database,
                        showErrorDialogBox,
                        showErrorMessage,

                        )

                } else {
                    Log.e("Error", "Could not get real path from URI")
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        database = AudioTextDatabase.getDatabase(context = this)
        Firebase.initialize(context = this)
        val appName = getString(R.string.app_name_inside_app)
        val providerFactory = if (BuildConfig.DEBUG) {

            DebugAppCheckProviderFactory.getInstance()
        } else {
            PlayIntegrityAppCheckProviderFactory.getInstance()
        }

        Firebase.appCheck.installAppCheckProviderFactory(providerFactory)

        Firebase.appCheck.getAppCheckToken(/* forceRefresh = */ false)
            .addOnSuccessListener { result ->
                val token = result.token
                appViewModel.token = result.token
                addMediaViewModel.token = result.token

                // send 'token' in a custom header to your backend
            }
            .addOnFailureListener { e->
                Log.d("failed==>",e.stackTraceToString())
            }
        auth = FirebaseAuth.getInstance()
        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user

                } else {
                    Log.e("Auth", "Anonymous sign-in failed", task.exception)
                }
            }
        // 1
        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        // 2
        mediaProjectionManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjectionLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                    mediaCaptureIntent = Intent(this, MediaCaptureService::class.java).apply {
                        putExtra("data", result.data!!)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        // Use ContextCompat for starting foreground services
                        startForegroundService(
                            mediaCaptureIntent
                        )
                    } else {
                        startService(mediaCaptureIntent)
                    }

                    startService(Intent(this, FloatingWindow::class.java))

                    finish()

                } else {
                    showDialogBox = true
                    showDialogBoxOf = dialogBoxPermissionCategory.mediaCapture

                }


            }
        var pickVideoLauncher = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
//                handlePickedVideo(uri) // 🔁 your existing logic reused here
                val inputFile = getFileFromUri(this, uri)
                val inputPath = inputFile?.absolutePath
                if (inputPath != null) {
                    val fileName = "${System.currentTimeMillis()}.wav"
                    extractAudioFromVideo(
                        inputFile,
                        fileName,
                        applicationContext,
                        addMediaViewModel.audioFileName,
                        addMediaViewModel,
                        database,
                        showErrorDialogBox,
                        showErrorMessage
                    )
                } else {
                    Log.e("Error", "Could not get real path from URI")
                }
            } else {
                Log.d("VideoPicker", "No video selected")
            }
        }
        fun broadCastServiceFun() {
            broadCastReciever = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val header = intent?.getStringExtra("header") ?: "Anonymous"
                    val subHeaderTitle = intent?.getStringExtra("subHeader") ?: ""
                    val bodyText = intent?.getStringExtra("bodyText") ?: "Could not Create"

                    CoroutineScope(Dispatchers.IO).launch {
                        database.dao.instertAudioText(
                            convertAudioTextToAudioTextDbData(
                                AudioText(
                                    text = bodyText,
                                    header = header,
                                    subHeader = subHeaderTitle,
                                    audioFileName = null,
                                    flowType = FlowType.AddText,
                                    imageCollection = null,
                                    isApiCallRequired = false,


                                    ), true
                            )
                        )
                    }
                }

            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(
                    broadCastReciever,
                    IntentFilter(ACTION_SAVE_FLOATING_TEXT),
                    RECEIVER_EXPORTED
                )
            } else {
                registerReceiver(
                    broadCastReciever,
                    IntentFilter(ACTION_SAVE_FLOATING_TEXT),
                )
            }
        }
        broadCastServiceFun()
//        fun requestMediaAccess() {
//            var permission = ""
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                permission = android.Manifest.permission.READ_MEDIA_VIDEO
//            } else {
//                permission = android.Manifest.permission.READ_EXTERNAL_STORAGE
//            }
//            val permissions = arrayOf(
//                permission,
//            )
//
//            //3
//            ActivityCompat.requestPermissions(this, permissions, 12)
//        }


        fun isFloatingWindowServiceRunning(): Boolean {
            val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            for (service in manager.getRunningServices(Int.MAX_VALUE)) {
                if (service.service.className == FloatingWindow::class.java.name) return true
            }
            return false
        }

        fun checkOverlayPermission(): Boolean {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                true
            } else {
                Settings.canDrawOverlays(this)
            }

        }

        fun requestPermissionForMicAudioCapture() {
            if (ContextCompat.checkSelfPermission(
                    this, android.Manifest.permission.RECORD_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                // 2
                val permissions = arrayOf(
                    android.Manifest.permission.RECORD_AUDIO,
                )

                //3
                ActivityCompat.requestPermissions(this, permissions, 10)

            } else {
                val captureScreen = mediaProjectionManager.createScreenCaptureIntent()
                mediaProjectionLauncher.launch(captureScreen)
            }
        }

        fun requestFloatingWindowPermission() {
            if (isFloatingWindowServiceRunning()) return
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            showDialogBox = true
            showDialogBoxOf = dialogBoxPermissionCategory.floatingWindow
        }

        fun startFloatingTextWindowService() {
            if (!checkOverlayPermission()) {
                requestFloatingWindowPermission()
                return
            } else {
                startForegroundService(Intent(this, FloatingTextWindowService::class.java))
                finish()
            }

        }

        fun runMediaCaptureService() {
            if (!checkOverlayPermission()) {
                requestFloatingWindowPermission()
                return
            }

            requestPermissionForMicAudioCapture()
        }

        fun stopMediaCapture() {
            mediaCaptureIntent = Intent(this, MediaCaptureService::class.java).apply {
                action = ACTION_STOP_MEDIACAPTURE
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Use ContextCompat for starting foreground services
                startForegroundService(
                    mediaCaptureIntent
                )
            } else {
                startService(mediaCaptureIntent)
            }
        }

        val PermissionDialogBoxContent =
            mapOf(dialogBoxPermissionCategory.microphone to PermissionDialogBox("Microphone Access Required",
                "${appName} needs microphone access to record internal audio. " +
                        "You can grant this permission in your device settings.",
                "Grant Permission",
                "Cancel",
                {
                    try {
                        val intent =
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts(
                                    "package", "com.myapp.ridescribe", null
                                )
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        this.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("MicrophoneDialog", "Failed to open settings: ${e.message}")
                        // Fallback to general settings if package-specific fails
                        val generalIntent = Intent(Settings.ACTION_SETTINGS)
                        this.startActivity(generalIntent)
                    } finally {
                        showDialogBox = false
                        showDialogBoxOf = null
                    }
                },
                {
                    showDialogBox = false
                    showDialogBoxOf = null
                }
            ),
                dialogBoxPermissionCategory.floatingWindow to PermissionDialogBox("Overlay Permission Required",
                    "${appName} requires permission to display a floating window over other apps. " +
                            "Please grant this permission in your device settings.",
                    "Grant Permission",
                    "Cancel",

                    {
                        try {
                            val overlayIntent =
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            this.startActivity(overlayIntent)

                        } catch (e: Exception) {
                            val overlayIntent =
                                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                            this.startActivity(overlayIntent)
                        } finally {
                            showDialogBox = false
                            showDialogBoxOf = null
                        }
                    },
                    {
                        showDialogBox = false
                        showDialogBoxOf = null
                    }),
                dialogBoxPermissionCategory.mediaCapture to PermissionDialogBox("Media Capture Permission Required",
                    "${appName} needs permission to capture media from your device. " +
                            "This is necessary for recording audio and accessing media content.",
                    "Grant Permission",
                    "Cancel",
                    {
                        try {
                            requestPermissionForMicAudioCapture()
                        } finally {
                            showDialogBox = false
                            showDialogBoxOf = null
                        }
                    },
                    {
                        showDialogBox = false
                        showDialogBoxOf = null
                    }
                ),
                dialogBoxPermissionCategory.videoMedia to PermissionDialogBox(" Media Access Permission Required",
                    "${appName} needs access to your media to record audio and manage media content. "
                            + " This permission ensures seamless functionality for capturing and accessing your recordings.",
                    "Grant Permission",
                    "Cancel",
                    {
                        try {
                            val intent =
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts(
                                        "package", "com.myapp.ridescribe", null
                                    )
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                            this.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e("MicrophoneDialog", "Failed to open settings: ${e.message}")
                            // Fallback to general settings if package-specific fails
                            val generalIntent = Intent(Settings.ACTION_SETTINGS)
                            this.startActivity(generalIntent)
                        } finally {
                            showDialogBox = false
                            showDialogBoxOf = null
                        }


                    },
                    {
                        showDialogBox = false
                        showDialogBoxOf = null
                    }
                )


            )






        super.onCreate(savedInstanceState)
        setContent {
            AppThemexx(
                selectedTheme = appViewModel.appColorTheme.value.toString(),
                theme = appViewModel.appTheme.value.toString(),
                dynamicColor = appViewModel.appColorTheme.value.toString() == AppColorTheme.SYSTEM.toString()
            ) {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.primary),
                ) {
                    if (!showSplashScreen.value) {
                        val context = LocalContext.current
                        PermissionDialogBoxContent[showDialogBoxOf]?.let {
                            DialogBoxForPermissionDenial(
                                permissionDialogBox = it,
                                showDialogBox

                            )
                        }
                        ErrorInMediaToAudioDialogBox(
                            showDialog = showErrorDialogBox.value,
                            onDismissRequest = {
                                showErrorDialogBox.value = false
                                showErrorMessage.value = ""
                            },
                            error = showErrorMessage.value
                        )

                        Navigation(
                            { runMediaCaptureService() },
                            { stopMediaCapture() },
                            {  pickVideoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
                            { startFloatingTextWindowService() },
                            appViewModel,
                            addMediaViewModel,
                            addMediaViewModel.isTextGettingGenerated.value
                        )
                    } else {
                        SplashScreen(viewModel = appViewModel, onSplashScreenComplete = {
                            showSplashScreen.value = false
                            CoroutineScope(Dispatchers.IO).launch {
                                appViewModel.firstLaunch()
                            }

                        })
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(broadCastReciever)
    }

    companion object BroadcastMessages {
        var ACTION_START_AUDIOCAPTURE = "startCapture"
        var ACTION_STOP_AUDIOCAPTURE = "stopCapture"
        var ACTION_STOP_MEDIACAPTURE = "stopMediaCapture"
        var ACTION_PAUSE_AUDIOCAPTURE = "pauseCapture"
        var ACTION_RESUME_AUDIOCAPTURE = "resumeCapture"
        var ACTION_AUDIO_NOT_AVAILABLE = "audioNotAvailable"
        var ACTION_STOP_APP = "stopApp"
        var ACTION_DATA_SAVING_FAILED = "dataSavingFailed"
        var ACTION_DATA_SAVING_PARTIAL = "dataSavingPartial"
        var ACTION_DATA_SAVING_SUCCESS = "dataSavingSuccess"
        var ACTION_SAVE_FLOATING_TEXT = "saveFloationgText"
        var ACTION_TAKE_SCREENSHOT = "takeScreenshot"
        var ACTION_ORIENTATION_CHANGE_TO_LANDSCAPE = "orientationChangeToLandscape"
        var ACTION_ORIENTATION_CHANGE_TO_POTRAIT = "orientationChangeToPotrait"
        var ACTION_AUDIO_NOT_AVAILABLE_INDICATOR = "audioNotAvailableIndicator"
        var ACTION_AUDIO_AVAILABLE_INDICATOR = "audioAvailableIndicator"

        var ACTION_STOP_MEDIACAPTURE_SERVICE = "stopMediaCaptureService"
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.End
    ) {}

}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AppThemexx {
        Greeting("Android")
    }
}

@Composable
fun AllowMicrophoneAccessDialog(
    showDialog: Boolean, mainActivityContext: Context, onDismiss: () -> Unit
) {
    val appName = stringResource(R.string.app_name_inside_app)

    if (showDialog) {
        AlertDialog(onDismissRequest = { onDismiss() }, title = {
            Text(
                "Microphone Access Required", fontWeight = FontWeight.Bold
            )
        }, text = {
            Text(
                "${appName}  needs microphone access to record internal audio. " + "You can grant this permission in your device settings."
            )
        }, confirmButton = {
            Button(onClick = {
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts(
                            "package", "com.myapp.ridescribe", null
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    mainActivityContext.startActivity(intent)
                } catch (e: Exception) {
                    Log.e("MicrophoneDialog", "Failed to open settings: ${e.message}")
                    // Fallback to general settings if package-specific fails
                    val generalIntent = Intent(Settings.ACTION_SETTINGS)
                    mainActivityContext.startActivity(generalIntent)
                } finally {
                    onDismiss()
                }
            }) {
                Text("Go To Settings")
            }
        }, dismissButton = {
            OutlinedButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        })
    }
}


@Composable
fun FloatingWindowAccessDialogBox(
    showDialog: Boolean, mainActivityContext: Context, onDismiss: () -> Unit
) {
    val appName = stringResource(R.string.app_name_inside_app)
    if (showDialog) {
        AlertDialog(onDismissRequest = { onDismiss() }, title = {
            Text(
                "Overlay Permission Required", fontWeight = FontWeight.Bold
            )
        }, text = {
            Text(
                "${appName}  requires permission to display a floating window over other apps. " + "Please grant this permission in your device settings."
            )
        }, confirmButton = {
            Button(onClick = {
                try {
                    val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    mainActivityContext.startActivity(overlayIntent)

                } catch (e: Exception) {
                    val overlayIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                    mainActivityContext.startActivity(overlayIntent)
                } finally {
                    onDismiss()
                }
            }) {
                Text("Grant Permission")
            }
        }, dismissButton = {
            OutlinedButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        })
    }
}

@Composable
fun MediaCaptureAccessDialogBox(
    showDialog: Boolean,
    mainActivityContext: Context,
    requestPermissionForMicAudioCapture: () -> Unit,
    onDismiss: () -> Unit
) {
    val appName = stringResource(R.string.app_name_inside_app)
    if (showDialog) {
        AlertDialog(onDismissRequest = { onDismiss() }, title = {
            Text(
                "Media Capture Permission Required", fontWeight = FontWeight.Bold
            )
        }, text = {
            Text(
                "${appName} needs permission to capture media from your device. " + "This is necessary for recording audio and accessing media content."
            )
        }, confirmButton = {
            Button(onClick = {
                try {
                    requestPermissionForMicAudioCapture()
                } finally {
                    onDismiss()
                }
            }) {
                Text("Grant Permission")
            }
        }, dismissButton = {
            OutlinedButton(onClick = { onDismiss() }) {
                Text("Cancel")
            }
        })
    }
}

@Composable
fun DialogBoxForPermissionDenial(
    permissionDialogBox: PermissionDialogBox,
    showDialog: Boolean
) {
    if (showDialog) {
        AlertDialog(onDismissRequest = { permissionDialogBox.onNegativeBtnClick() }, title = {
            Text(
//                    "Media Capture Permission Required",
                permissionDialogBox.title, fontWeight = FontWeight.Bold
            )
        }, text = {
            Text(
                permissionDialogBox.description
//                    "${R.string.app_name_inside_app} needs permission to capture media from your device. " +
//                            "This is necessary for recording audio and accessing media content."
            )
        }, confirmButton = {
            Button(onClick = {
                try {
                    permissionDialogBox.onPositiveBtnClick()
                } finally {
                    permissionDialogBox.onNegativeBtnClick
                }
            }) {
                Text(
                    permissionDialogBox.positiveBtnContent
//                        "Grant Permission"
                )
            }
        }, dismissButton = {
            OutlinedButton(onClick = { permissionDialogBox.onNegativeBtnClick() }) {
                Text(
                    permissionDialogBox.negativeButtonContent
//                        "Cancel"
                )
            }
        })


    }
}


@Composable
fun ErrorInMediaToAudioDialogBox(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    error: String
) {
    val appName = stringResource(R.string.app_name_inside_app)
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            icon = {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = "Error Icon",
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = {
                Text(
                    text = "Unable to Convert to Audio",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${appName} couldn't create audio from this media.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "This may be due to unsupported format. WhatsApp videos and some other formats aren't convertible.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )

                    if (error.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                text = error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onDismissRequest,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK, Got It")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismissRequest
                ) {
                    Text("Dismiss")
                }
            }
        )
    }
}


fun extractAudioFromVideo(
    inputFile: File,
    outputAudioFileName: String,
    context: Context,
    newFileName: MutableState<String>,
    addMediaViewModel: AddMediaViewModel,
    database: AudioTextDatabase,
    showErrorDialogBox: MutableState<Boolean>,
    showErrorMessage: MutableState<String>
) {
    try {
        val inputVideoPath = inputFile.absolutePath


        // Ensure the input file exists
        if (!File(inputVideoPath).exists()) {
            Log.e("FFmpeg", "Input file doesn't exist: $inputVideoPath")
            return
        }
        val audioCapturesDirectory = File(context.getExternalFilesDir(null), "/AudioCaptures")
        if (!audioCapturesDirectory.exists()) {
            audioCapturesDirectory.mkdirs()
        }
        val timeStamp = SimpleDateFormat("dd-mm-yyyy-hh-mm-ss", Locale.US).format(Date())


        val outputAudioPath =
            "${context.getExternalFilesDir(null)?.absolutePath.toString()}/AudioCaptures/${outputAudioFileName}"


        // FFmpeg command to extract audio
        val command = arrayOf(
            "-i", inputVideoPath,
            "-vn",
            "-acodec", "libmp3lame",
            "-ar", "44100",
            "-ac", "1",
            "-b:a", "192k",
            outputAudioPath
        ).joinToString(" ")

        FFmpegKit.executeAsync(command) { session ->
            val returnCode = session.returnCode
            if (ReturnCode.isSuccess(returnCode)) {
                newFileName.value = outputAudioFileName
                CoroutineScope(Dispatchers.IO).launch {
                    addMediaViewModel.uploadFile(File(outputAudioPath), database, context)
                    inputFile.delete()
                }

            } else {
                val failStackTrace = session.failStackTrace
                showErrorDialogBox.value = true

                inputFile.delete()
            }
        }
    } catch (e: Exception) {
        Log.e("FFmpeg", "Error during audio extraction: ${e.message}", e)
        showErrorDialogBox.value = true
        showErrorMessage.value = e.message.toString()
        inputFile.delete()
    }
}


fun getFileFromUri(context: Context, uri: Uri): File? {
    val contentResolver = context.contentResolver
    val fileName = "${System.currentTimeMillis()}.mp4" // Unique filename
    val tempFile = File(context.cacheDir, fileName)

    return try {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        tempFile
    } catch (e: Exception) {
        Log.e("FileError", "Error copying file: ${e.message}")
        null
    }
}

data class PermissionDialogBox(
    val title: String,
    val description: String,
    val positiveBtnContent: String,
    val negativeButtonContent: String,
    val onPositiveBtnClick: () -> Unit,
    val onNegativeBtnClick: () -> Unit,
)

enum class dialogBoxPermissionCategory {
    microphone, floatingWindow, mediaCapture, videoMedia,
}


enum class AppColorTheme {
    GREEN, BLUE, YELLOW, PURPLE, TEAL, GREY, RED, BLACK, WHITE, DEEP_GREEN, SYSTEM
}

enum class AppTheme {
    LIGHT, DARK, SYSTEM
}
