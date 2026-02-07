package com.example.devaudioreccordings.pages

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.devaudioreccordings.database.AudioText
import com.example.devaudioreccordings.viewModals.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AITextGenerated(
    id: String,
    appViewModel: AppViewModel,
    navController: NavController
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showContentIsGeneratingDialog = remember { mutableStateOf(false) }
    val fullData by appViewModel.fullData.collectAsState()

    var audioData by remember { mutableStateOf<AudioText?>(null) }
    var audioText  = appViewModel.audioText
    var fetchData by remember { mutableStateOf(true) }

    var uiState by remember { mutableStateOf<UIState>(UIState.Loading) }

    // Load audio data
    LaunchedEffect(Unit) {
        uiState = UIState.Loading
        val data = appViewModel.returnDataBasedOnId2(fullData, id.toInt())
        audioData = data
    }

    BackHandler(enabled = uiState == UIState.Generating) {
        showContentIsGeneratingDialog.value = true
    }



    if (showContentIsGeneratingDialog.value) {
        ShowGenerationHappeningDialogBox(
            showDialog = showContentIsGeneratingDialog,
            onLeave = {
                showContentIsGeneratingDialog.value = false
                appViewModel.audioText.value = ""
                navController.popBackStack()
            },
            onStay = {

            }
        )
    }

    // Generate AI text when audioData is available
    LaunchedEffect(audioData, fetchData) {
        if (fetchData && audioData != null) {
            val isInternetAvailable = isNetworkAvailable(context)
            if (!isInternetAvailable) {
                Toast.makeText(context, "Internet is Required For This Feature", Toast.LENGTH_LONG)
                    .show()
                uiState = UIState.Error("Internet connection is required to generate content.")
            } else {

                if(audioText.value!=""){
                    uiState = UIState.Ready
                }

               else if (audioData != null) {
                    uiState = UIState.Generating
                    val result = withContext(Dispatchers.IO) {
                        appViewModel.getLinkedinShareableAiGeneratedText(audioData!!.text)
                    }

                    audioText.value = result
                    uiState = UIState.Ready
                } else {
                    uiState = UIState.Error("Could not find required data")
                }
            }
            fetchData = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Generated Text") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (uiState == UIState.Generating) {
                            showContentIsGeneratingDialog.value = true
                        } else {
                            appViewModel.audioText.value = ""
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            when (uiState) {
                UIState.Loading -> {
                    LoadingState(message = "Loading your audio data...")
                }

                UIState.Generating -> {
                    LoadingState(message = "Generating LinkedIn-ready content...")
                }

                is UIState.Error -> {
                    ErrorState(message = (uiState as UIState.Error).message) {
                        // Retry logic
                        fetchData = true

                    }
                }

                UIState.Ready -> {
                    ReadyState(
                        textVal = audioText,
                        onTextChange = { audioText.value = it },
                        onShare = {
                            val intent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, audioText.value)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(intent, "Share on LinkedIn")
                            ContextCompat.startActivity(context, shareIntent, null)

                            scope.launch {
                                snackbarHostState.showSnackbar("Content ready to share")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(56.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadyState(
    textVal: MutableState<String>,
    onTextChange: (String) -> Unit,
    onShare: () -> Unit
) {
    val text = textVal.value
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Your LinkedIn Content",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 250.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    placeholder = { Text("AI generated text will appear here") },
                    shape = RoundedCornerShape(8.dp)
                )
                Text(
                    text = "Feel free to edit the text before sharing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        Text(
            text = "${text.length} characters",
            style = MaterialTheme.typography.bodySmall,
            color = if (text.length > 1200) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.outline
        )

        Button(
            onClick = onShare,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                    text = "Share on LinkedIn",
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Tips for LinkedIn",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Keep your post concise and professional\n" +
                            "• Add relevant hashtags to increase visibility\n" +
                            "• Engage with comments on your post",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

sealed class UIState {
    object Loading : UIState()
    object Generating : UIState()
    object Ready : UIState()
    data class Error(val message: String) : UIState()
}

fun isNetworkAvailable(context: Context): Boolean {
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
    return activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            || activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}


@Composable
fun ShowGenerationHappeningDialogBox(
    showDialog: MutableState<Boolean>,
    onLeave: () -> Unit,
    onStay: () -> Unit
) {
//    val process = "summary" // or "transcription"
    if (showDialog.value) {
        AlertDialog(
            onDismissRequest = {
                showDialog.value = false
            },
            title = {
                Text(
                    text = "Content generation is ongoing",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Normal
                )
            },
            text = {
                Text(
                    text = "Your Linkedin Ready Content is being generated. Do you want to leave midway?",
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