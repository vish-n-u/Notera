package com.example.devaudioreccordings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.devaudioreccordings.pages.AITextGenerated
import com.example.devaudioreccordings.pages.EditPage.EditPage
import com.example.devaudioreccordings.pages.Homepage.Homepage
import com.example.devaudioreccordings.pages.RecordingList
import com.example.devaudioreccordings.pages.Settings.PrivacyPolicyScreen
import com.example.devaudioreccordings.pages.Settings.SettingsScreen
import com.example.devaudioreccordings.viewModals.AddMediaViewModel
import com.example.devaudioreccordings.viewModals.AppViewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import java.net.URLDecoder

enum class Routes {
    Homepage, ListRecordings, EditPage, AddMediaPage, AIGeneratedText, Settings, PrivacyPolicy, FloatingUI
}

enum class Flows {
    MediaCaptureService, AddMedia, AddText, FloatingText
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Navigation(
    runMediaCaptureService: () -> Unit,
    stopMediaCapture: () -> Unit,
    requestMediaAccess: () -> Unit,
    startFloatingTextWindowService: () -> Unit,
    appViewModel: AppViewModel,
    addMediaViewModel: AddMediaViewModel,
    isTextGettingGenerated: Boolean
) {
    val navigationController: NavHostController = rememberNavController()
    val systemUiController = rememberSystemUiController()
    val view = LocalView.current
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    var previousPage: String = ""
    val trial: MutableState<String> = remember { mutableStateOf("") }


    LaunchedEffect(key1 = isTextGettingGenerated) {
        if (isTextGettingGenerated) {
            val latestId = appViewModel.getLatestCreatedId()

            navigationController.navigate(Routes.EditPage.name + "?id=" + latestId)
            previousPage = Routes.EditPage.name
        }
    }

    // Set the status bar color
    SideEffect {
        systemUiController.setStatusBarColor(
            color = primaryContainerColor, // Change this to your desired color
            darkIcons = false // Set to true for dark icons (light status bar), false for light icons (dark status bar)
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),

    ) {

        NavHost(
            navController = navigationController,
            modifier = Modifier.padding(it),
            startDestination = Routes.Homepage.name
        ) {
            composable(route = Routes.Homepage.name) {
                Homepage(
                    runMediaCaptureService,
                    stopMediaCapture,
                    appViewModel,
                    requestMediaAccess,
                    startFloatingTextWindowService,
                    navigationController
                )
                previousPage = Routes.Homepage.name
            }
            composable(
                route = Routes.EditPage.name + "?id={id}&flow={flow}",
                arguments = listOf(navArgument("header") {
                    type = NavType.StringType
                    nullable = true
                }
                )
            ) {
                val id = it.arguments?.getString("id")
                val flow = it.arguments?.getString("flow")
                EditPage(
                    id = id!!,
                    flow = flow ?: "",
                    viewModel = appViewModel,
                    addMediaViewModel = addMediaViewModel,
                    navController = navigationController
                )
                previousPage = Routes.EditPage.name
            }

            composable(route = Routes.AIGeneratedText.name + "?id={id}") {
                val id = it.arguments?.getString("id")
                AITextGenerated(
                    id = id ?: "1",
                    appViewModel = appViewModel,
                    navController = navigationController
                )
                previousPage = Routes.AIGeneratedText.name

            }

            composable(route = Routes.PrivacyPolicy.name) {
                PrivacyPolicyScreen({ navigationController.popBackStack() })
            }

//            composable(route = Routes.FloatingUI.name){
////                FloatingNoteWindow({},fun(title,subHeading){},{},{},{},"2:54",30,50,false)
////                FloatingControlPanel(Modifier,true,false,{},{},{},{},{},true,"00:00",true,false,{},true)
//                MainScreenxx("",{},trial)
//            }

            composable(route = Routes.ListRecordings.name + "?header={header}", arguments = listOf(
                navArgument("header") {
                    type = NavType.StringType
                    nullable = true
                }
            )
            ) {
                val encodedHeader = it.arguments?.getString("header")
                val decodedHeader = URLDecoder.decode(encodedHeader, "UTF-8")
                if (encodedHeader != null) {
                }
                RecordingList(decodedHeader ?: "hello", appViewModel, navigationController)
                previousPage = Routes.ListRecordings.name
            }

            composable(
                route = Routes.Settings.name
            ) {
                SettingsScreen(appViewModel, navigationController)
            }
        }
    }
}

