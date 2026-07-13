package com.tx24.spicyplayer

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.tx24.spicyplayer.actions.RealGoToAlbumAction
import com.tx24.spicyplayer.actions.RealOpenTagEditorAction
import com.tx24.spicyplayer.playback.PlaybackManager
import com.tx24.spicyplayer.library.store.AlbumsRepository
import com.tx24.spicyplayer.library.store.MediaRepository
import com.tx24.spicyplayer.library.store.preferences.UserPreferencesRepository
import com.tx24.spicyplayer.ui.AskPermissionScreen
import com.tx24.spicyplayer.ui.SpicyApp2
import com.tx24.spicyplayer.ui.common.LocalCommonSongsAction
import com.tx24.spicyplayer.ui.common.LocalUserPreferences
import com.tx24.spicyplayer.ui.common.rememberCommonSongsActions
import com.tx24.spicyplayer.ui.model.toUiModel
import com.tx24.spicyplayer.ui.theme.SpicyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject


@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    @Inject
    lateinit var playbackManager: PlaybackManager

    @Inject
    lateinit var mediaRepository: MediaRepository

    @Inject
    lateinit var albumsRepository: AlbumsRepository

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val userPreferencesFlow = userPreferencesRepository.userSettingsFlow.map { it.toUiModel() }

        setContent {

            // Preferences load asynchronously from DataStore instead of blocking the
            // main thread during onCreate. While they load we draw a neutral surface
            // keyed to the system dark-mode, so the common case (theme follows system)
            // shows no flash and we never risk an ANR on slow storage.
            val userPreferences by userPreferencesFlow
                .collectAsState(initial = null)

            val prefs = userPreferences
            if (prefs == null) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isSystemInDarkTheme()) Color.Black else Color.White
                ) {}
                return@setContent
            }

            LaunchedEffect(prefs.uiSettings.keepScreenOn) {
                if (prefs.uiSettings.keepScreenOn) {
                    window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            val navController = rememberNavController()

            SpicyTheme(
                userPreferences = prefs,
            ) {
                val commonSongsActions =
                    rememberCommonSongsActions(
                        playbackManager,
                        mediaRepository,
                        remember { RealOpenTagEditorAction(navController) },
                        remember { RealGoToAlbumAction(albumsRepository, navController) },
                    )


                val permissionName = getReadingMediaPermissionName()
                val storagePermissionState =
                    rememberPermissionState(permission = permissionName)

                LaunchedEffect(key1 = storagePermissionState.status.isGranted) {
                    if (storagePermissionState.status.isGranted)
                        mediaRepository.onPermissionAccepted()
                }

                CompositionLocalProvider(
                    LocalUserPreferences provides prefs,
                    LocalCommonSongsAction provides commonSongsActions
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        AnimatedContent(
                            targetState = storagePermissionState.status is PermissionStatus.Granted,
                            label = ""
                        ) {
                            if (it)
                                SpicyApp2(modifier = Modifier.fillMaxSize(), navController)
                            else
                                AskPermissionScreen(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    storagePermissionState.status.shouldShowRationale,
                                    onRequestPermission = { storagePermissionState.launchPermissionRequest() },
                                    onOpenSettings = { openAppSettingsScreen() }
                                )
                        }

                    }
                }
            }
        }
    }

    private fun getReadingMediaPermissionName() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            Manifest.permission.READ_MEDIA_AUDIO
        else Manifest.permission.READ_EXTERNAL_STORAGE

    private fun openAppSettingsScreen() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

}