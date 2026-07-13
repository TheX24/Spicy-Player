package com.tx24.spicyplayer.ui.theme

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTonalElevationEnabled
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.android.material.color.utilities.Scheme
import com.tx24.spicyplayer.ui.albumart.LocalEfficientThumbnailImageLoader
import com.tx24.spicyplayer.ui.albumart.LocalInefficientThumbnailImageLoader
import com.tx24.spicyplayer.ui.albumart.efficientAlbumArtImageLoader
import com.tx24.spicyplayer.ui.albumart.inefficientAlbumArtImageLoader
import com.tx24.spicyplayer.ui.common.AppColorScheme
import com.tx24.spicyplayer.ui.common.LocalAppColorScheme
import com.tx24.spicyplayer.ui.common.LocalUserPreferences
import com.tx24.spicyplayer.ui.model.AppThemeUi
import com.tx24.spicyplayer.ui.model.UserPreferencesUi

@SuppressLint("RestrictedApi")
@Composable
fun SpicyTheme(
    userPreferences: UserPreferencesUi,
    content: @Composable () -> Unit
) {
    val darkTheme = when (userPreferences.uiSettings.theme) {
        AppThemeUi.DARK -> true
        AppThemeUi.LIGHT -> false
        AppThemeUi.SYSTEM -> isSystemInDarkTheme()
    }

    val userAccentColor = userPreferences.uiSettings.accentColor
    val context = LocalContext.current

    val lightColorScheme = when {
        userPreferences.uiSettings.isUsingDynamicColor
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        -> dynamicLightColorScheme(context)
        else -> Scheme.light(userAccentColor).toLightComposeColorScheme()
    }

    val darkColorScheme = when {
        userPreferences.uiSettings.isUsingDynamicColor
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (userPreferences.uiSettings.blackBackgroundForDarkTheme)
                dynamicAmoledTheme(context)
            else
                dynamicDarkColorScheme(context)
        }

        else -> {
            val colorScheme = Scheme.dark(userAccentColor).toDarkComposeColorScheme()
            if (userPreferences.uiSettings.blackBackgroundForDarkTheme)
                colorScheme.copy(surface = Color.Black, background = Color.Black)
            else
                colorScheme
        }
    }

    val appThemeColorScheme = AppColorScheme(lightColorScheme, darkColorScheme)
    val colorScheme = if (darkTheme) darkColorScheme else lightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    SideEffect {
        val window = (view.context as Activity).window

        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        val windowsInsetsController = WindowCompat.getInsetsController(window, view)

        windowsInsetsController.isAppearanceLightStatusBars = !darkTheme
        windowsInsetsController.isAppearanceLightNavigationBars = !darkTheme
    }

    val efficientImageLoader = remember { context.efficientAlbumArtImageLoader() }
    val inefficientImageLoader = remember { context.inefficientAlbumArtImageLoader() }
    CompositionLocalProvider(
        LocalEfficientThumbnailImageLoader provides efficientImageLoader,
        LocalInefficientThumbnailImageLoader provides inefficientImageLoader,
        LocalAppColorScheme provides appThemeColorScheme,
        LocalTonalElevationEnabled provides false
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = ExpressiveShapes,
            content = content
        )
    }
}

@SuppressLint("RestrictedApi")
fun Scheme.toLightComposeColorScheme() =
    lightColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        inversePrimary = Color(inversePrimary),
        surfaceContainerLowest = Color(surface),
        surfaceContainerLow = Color(surface),
        surfaceContainer = Color(surface),
        surfaceContainerHigh = Color(surface),
        surfaceContainerHighest = Color(surface),
    )

@SuppressLint("RestrictedApi")
fun Scheme.toDarkComposeColorScheme() =
    darkColorScheme(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        onTertiaryContainer = Color(onTertiaryContainer),
        tertiaryContainer = Color(tertiaryContainer),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        inversePrimary = Color(inversePrimary),
        surfaceContainerLowest = Color(surface),
        surfaceContainerLow = Color(surface),
        surfaceContainer = Color(surface),
        surfaceContainerHigh = Color(surface),
        surfaceContainerHighest = Color(surface),
    )

@RequiresApi(Build.VERSION_CODES.S)
private fun dynamicAmoledTheme(context: Context): ColorScheme {
    val darkColorScheme = dynamicDarkColorScheme(context)
    return darkColorScheme.copy(background = Color.Black, surface = Color.Black)
}

@Composable
fun isAppInDarkTheme(): Boolean {
    return when(LocalUserPreferences.current.uiSettings.theme) {
        AppThemeUi.DARK -> true
        AppThemeUi.LIGHT -> false
        AppThemeUi.SYSTEM -> isSystemInDarkTheme()
    }
}