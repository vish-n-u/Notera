package com.example.devaudioreccordings.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.compose.BlueDarkScheme
import com.example.compose.BlueLightScheme
import com.example.devaudioreccordings.AppColorTheme
import com.example.devaudioreccordings.AppTheme
import com.example.devaudioreccordings.ui.theme.blackScheme.BlackDarkScheme
import com.example.devaudioreccordings.ui.theme.blackScheme.BlackLightScheme
import com.example.devaudioreccordings.ui.theme.deepGreenScheme.DeepGreenDarkScheme
import com.example.devaudioreccordings.ui.theme.deepGreenScheme.DeepGreenLightScheme
import com.example.devaudioreccordings.ui.theme.greenScheme.GreenDarkScheme
import com.example.devaudioreccordings.ui.theme.greenScheme.GreenLightScheme
import com.example.devaudioreccordings.ui.theme.greyScheme.GreyDarkScheme
import com.example.devaudioreccordings.ui.theme.greyScheme.GreyLightScheme
import com.example.devaudioreccordings.ui.theme.purpleScheme.PurpleDarkScheme
import com.example.devaudioreccordings.ui.theme.purpleScheme.PurpleLightScheme
import com.example.devaudioreccordings.ui.theme.redScheme.RedDarkScheme
import com.example.devaudioreccordings.ui.theme.redScheme.RedLightScheme
import com.example.devaudioreccordings.ui.theme.tealScheme.TealDarkScheme
import com.example.devaudioreccordings.ui.theme.tealScheme.TealLightScheme
import com.example.devaudioreccordings.ui.theme.whiteScheme.WhiteDarkScheme
import com.example.devaudioreccordings.ui.theme.whiteScheme.WhiteLightScheme
import com.example.devaudioreccordings.ui.theme.yellowScheme.YellowDarkScheme
import com.example.devaudioreccordings.ui.theme.yellowScheme.YellowLightScheme

@Composable
fun AppThemexx(
    selectedTheme: String = AppColorTheme.GREEN.toString(), // User-selected theme
    theme: String = AppTheme.SYSTEM.toString(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
   var darkTheme: Boolean = isSystemInDarkTheme()


    if(theme==AppTheme.LIGHT.toString()) darkTheme = false
    if(theme==AppTheme.DARK.toString()) darkTheme = true


    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }

        else -> when (selectedTheme) {
            AppColorTheme.GREEN.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.GREEN.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.GREEN.toString()]!!["lightColors"]!!
            }

            AppColorTheme.BLUE.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.BLUE.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.BLUE.toString()]!!["lightColors"]!!
            }
            AppColorTheme.RED.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.RED.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.RED.toString()]!!["lightColors"]!!
            }
            AppColorTheme.YELLOW.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.YELLOW.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.YELLOW.toString()]!!["lightColors"]!!
            }
            AppColorTheme.PURPLE.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.PURPLE.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.PURPLE.toString()]!!["lightColors"]!!
            }
            AppColorTheme.TEAL.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.TEAL.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.TEAL.toString()]!!["lightColors"]!!
            }
            AppColorTheme.GREY.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.GREY.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.GREY.toString()]!!["lightColors"]!!
            }
            AppColorTheme.BLACK.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.BLACK.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.BLACK.toString()]!!["lightColors"]!!
            }
            AppColorTheme.WHITE.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.WHITE.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.WHITE.toString()]!!["lightColors"]!!
            }
            AppColorTheme.DEEP_GREEN.toString() -> {
                if (darkTheme) colorSchemes[AppColorTheme.DEEP_GREEN.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.DEEP_GREEN.toString()]!!["lightColors"]!!
            }

            else -> {
                if (darkTheme) colorSchemes[AppColorTheme.GREEN.toString()]!!["darkColors"]!!
                else colorSchemes[AppColorTheme.GREEN.toString()]!!["lightColors"]!!
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}





val colorSchemes = mapOf(
    AppColorTheme.GREEN.toString() to mapOf(
        "darkColors" to GreenDarkScheme,
        "lightColors" to GreenLightScheme
    ),
    AppColorTheme.BLUE.toString() to mapOf("darkColors" to BlueDarkScheme, "lightColors" to BlueLightScheme),
    AppColorTheme.RED.toString() to mapOf("darkColors" to RedDarkScheme, "lightColors" to RedLightScheme),
    AppColorTheme.YELLOW.toString() to mapOf("darkColors" to YellowDarkScheme, "lightColors" to YellowLightScheme),
    AppColorTheme.PURPLE.toString() to mapOf("darkColors" to PurpleDarkScheme, "lightColors" to PurpleLightScheme),
    AppColorTheme.GREY.toString() to mapOf("darkColors" to GreyDarkScheme, "lightColors" to GreyLightScheme),
    AppColorTheme.TEAL.toString() to mapOf("darkColors" to TealDarkScheme, "lightColors" to TealLightScheme),
    AppColorTheme.BLACK.toString() to mapOf("darkColors" to BlackDarkScheme, "lightColors" to BlackLightScheme),
    AppColorTheme.WHITE.toString() to mapOf("darkColors" to WhiteDarkScheme, "lightColors" to WhiteLightScheme),
    AppColorTheme.DEEP_GREEN.toString() to mapOf("darkColors" to DeepGreenDarkScheme, "lightColors" to DeepGreenLightScheme)


)