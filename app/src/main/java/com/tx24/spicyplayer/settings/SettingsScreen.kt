package com.tx24.spicyplayer.settings

import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.foundation.layout.PaddingValues
import com.tx24.spicyplayer.settings.common.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tx24.spicyplayer.settings.common.ColorPickerDialog
import com.tx24.spicyplayer.settings.common.GeneralSettingsItem
import com.tx24.spicyplayer.settings.common.SettingInfo
import com.tx24.spicyplayer.settings.common.SwitchSettingsItem
import com.tx24.spicyplayer.ui.common.fromIntToAccentColor
import com.tx24.spicyplayer.ui.common.toInt
import com.tx24.spicyplayer.ui.model.AppThemeUi
import com.tx24.spicyplayer.ui.model.PlayerThemeUi
import com.tx24.spicyplayer.ui.model.UserPreferencesUi
import com.tx24.spicyplayer.BuildConfig
import getPath


@Composable
fun SettingsRoute(
    modifier: Modifier = Modifier,
    onBackPressed: () -> Unit,
    onNavigateToReset: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val state by settingsViewModel.state.collectAsState()
    val scanDirectory by settingsViewModel.scanDirectory.collectAsState()
    val scanProgress by settingsViewModel.scanProgress.collectAsState()
    val scanHistory by settingsViewModel.scanHistory.collectAsState()
    val updateStatus by settingsViewModel.updateStatus.collectAsState()

    SettingsScreen(
        modifier = modifier,
        state = state,
        scanDirectory = scanDirectory,
        scanProgress = scanProgress,
        scanHistory = scanHistory,
        updateStatus = updateStatus,
        onBackPressed = onBackPressed,
        onNavigateToReset = onNavigateToReset,
        settingsCallbacks = settingsViewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier,
    state: SettingsState,
    scanDirectory: String,
    scanProgress: com.tx24.spicyplayer.library.store.ScanProgress?,
    scanHistory: List<String>,
    updateStatus: com.tx24.spicyplayer.settings.components.UpdateStatus,
    onBackPressed: () -> Unit,
    onNavigateToReset: () -> Unit,
    settingsCallbacks: ISettingsViewModel
) {

    val topBarScrollBehaviour = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = modifier,
        topBar = { SettingsTopAppBar(topBarScrollBehaviour, onBackPressed = onBackPressed) }
    )
    { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding()),
            contentAlignment = Alignment.Center
        ) {

            if (scanProgress != null) {
                com.tx24.spicyplayer.ui.dialogs.ScanProgressDialog(
                    isScanning = true,
                    scanProgress = scanProgress!!,
                    scanHistory = scanHistory
                )
            }
            
            com.tx24.spicyplayer.settings.components.UpdateDialog(
                status = updateStatus,
                onClearStatus = { settingsCallbacks.clearUpdateStatus() },
                context = LocalContext.current
            )

            var showLicensesDialog by remember { mutableStateOf(false) }

            if (showLicensesDialog) {
                AlertDialog(
                    onDismissRequest = { showLicensesDialog = false },
                    title = { Text("Open Source Licenses", style = MaterialTheme.typography.headlineSmall) },
                    text = {
                        Column(
                            modifier = Modifier
                                .height(400.dp)
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                            androidx.compose.foundation.lazy.LazyColumn(state = listState) {
                                val libraries = listOf(
                                    "AndroidX (Core, Lifecycle, Activity, Compose, Palette, DataStore)" to "Apache License 2.0",
                                    "Kotlin & Coroutines" to "Apache License 2.0",
                                    "Media3 / ExoPlayer" to "Apache License 2.0",
                                    "Material Design 3" to "Apache License 2.0",
                                    "Material Color Utilities" to "Apache License 2.0",
                                    "Hilt / Dagger" to "Apache License 2.0",
                                    "Timber" to "Apache License 2.0"
                                )
                                items(libraries) { (name, license) ->
                                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                        Text(name, style = MaterialTheme.typography.titleSmall)
                                        Text(license, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showLicensesDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                )
            }

            var showAboutAppDialog by remember { mutableStateOf(false) }

            if (showAboutAppDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutAppDialog = false },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Spicy Player", style = MaterialTheme.typography.headlineMedium)
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "An offline Android music player with a port of Spicy Lyrics based on Material 3 Music Player.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Developed by TX24", style = MaterialTheme.typography.labelLarge)
                            val versionText = remember { "Version ${BuildConfig.VERSION_NAME}" }
                            Text(versionText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showAboutAppDialog = false }) {
                            Text("Dismiss")
                        }
                    }
                )
            }

            if (state is SettingsState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            } else if (state is SettingsState.Loaded) {
                SettingsList(
                    modifier = Modifier.fillMaxSize(),
                    userPreferences = state.userPreferences,
                    scanDirectory = scanDirectory,
                    settingsCallbacks = settingsCallbacks,
                    onNavigateToReset = onNavigateToReset,
                    nestedScrollConnection = topBarScrollBehaviour.nestedScrollConnection,
                    showLicenses = { showLicensesDialog = true },
                    showAbout = { showAboutAppDialog = true }
                )
            }

        }
    }

}


@Composable
fun SettingsList(
    modifier: Modifier,
    userPreferences: UserPreferencesUi,
    scanDirectory: String,
    settingsCallbacks: ISettingsViewModel,
    onNavigateToReset: () -> Unit,
    nestedScrollConnection: NestedScrollConnection,
    showLicenses: () -> Unit,
    showAbout: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.nestedScroll(nestedScrollConnection),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // ━━ Lyrics ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        item { SettingsSectionHeader("Lyrics") }
        item {
            SettingsSection {
                NumberSettingItem(
                    icon = Icons.Rounded.Tune,
                    title = "Global Sync Offset",
                    value = userPreferences.uiSettings.lyricsOffsetMs,
                    onValueChange = { settingsCallbacks.setLyricsOffsetMs(it) },
                    valueRange = -5000..5000,
                    step = 50,
                    suffix = "ms"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SegmentedSettingItem(
                    icon = Icons.Rounded.FormatSize,
                    title = "Font Size",
                    options = listOf("Small", "Medium", "Large"),
                    selectedIndex = when (userPreferences.uiSettings.lyricsFontSize) { "SMALL" -> 0; "LARGE" -> 2; else -> 1 },
                    onSelect = { settingsCallbacks.setLyricsFontSize(when (it) { 0 -> "SMALL"; 2 -> "LARGE"; else -> "MEDIUM" }) }
                )
            }
        }

        // ━━ Audio & Playback ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        item { SettingsSectionHeader("Audio & Playback") }
        item {
            SettingsSection {
                SwitchSettingItem(
                    icon = Icons.Rounded.MusicNote,
                    title = "Gapless Playback",
                    checked = userPreferences.playerSettings.gaplessPlayback,
                    onCheckedChange = { settingsCallbacks.setGaplessPlayback(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SwitchSettingItem(
                    icon = Icons.Rounded.Science,
                    title = "Replay Gain",
                    subtitle = "Coming Soon",
                    checked = false,
                    onCheckedChange = {},
                    enabled = false
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
//                SliderSettingItem(
//                    icon = Icons.Rounded.BlurOn,
//                    title = "Crossfade Duration",
//                    valueLabel = if (userPreferences.playerSettings.crossfadeDuration == 0) "Off" else "${userPreferences.playerSettings.crossfadeDuration}s",
//                    value = userPreferences.playerSettings.crossfadeDuration.toFloat(),
//                    onValueChange = { settingsCallbacks.setCrossfadeDuration(it.toInt()) },
//                    onValueChangeFinished = {},
//                    valueRange = 0f..10f,
//                    steps = 9
//                )
//                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SliderSettingItem(
                    icon = Icons.Rounded.Replay,
                    title = "Previous Skip Threshold",
                    valueLabel = "${userPreferences.playerSettings.previousSkipThreshold}s",
                    value = userPreferences.playerSettings.previousSkipThreshold.toFloat(),
                    onValueChange = { settingsCallbacks.setPreviousSkipThreshold(it.toInt()) },
                    valueRange = 0f..10f,
                    steps = 9
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SegmentedSettingItem(
                    icon = Icons.AutoMirrored.Rounded.VolumeDown,
                    title = "Audio Focus Behavior",
                    options = listOf("Pause", "Ignore"),
                    selectedIndex = if (userPreferences.playerSettings.audioFocusBehavior == "PAUSE") 0 else 1,
                    onSelect = { settingsCallbacks.setAudioFocusBehavior(if (it == 0) "PAUSE" else "IGNORE") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SwitchSettingItem(
                    icon = Icons.AutoMirrored.Rounded.VolumeOff,
                    title = "Pause on Volume Zero",
                    checked = userPreferences.playerSettings.pauseOnVolumeZero,
                    onCheckedChange = { settingsCallbacks.togglePauseVolumeZero() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SwitchSettingItem(
                    icon = Icons.AutoMirrored.Rounded.VolumeUp,
                    title = "Resume when gaining volume",
                    checked = userPreferences.playerSettings.resumeWhenVolumeIncreases,
                    onCheckedChange = { settingsCallbacks.toggleResumeVolumeNotZero() }
                )
            }
        }

        // ━━ Appearance & Display ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        item { SettingsSectionHeader("Appearance & Display") }
        item {
            SettingsSection {
                SegmentedSettingItem(
                    icon = Icons.Rounded.DarkMode,
                    title = "App Theme",
                    options = listOf("Light", "Dark", "System"),
                    selectedIndex = when (userPreferences.uiSettings.theme) { AppThemeUi.LIGHT -> 0; AppThemeUi.DARK -> 1; else -> 2 },
                    onSelect = { settingsCallbacks.onThemeSelected(when (it) { 0 -> AppThemeUi.LIGHT; 1 -> AppThemeUi.DARK; else -> AppThemeUi.SYSTEM }) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SwitchSettingItem(
                    icon = Icons.Rounded.DarkMode,
                    title = "Pure Black Dark Mode",
                    checked = userPreferences.uiSettings.blackBackgroundForDarkTheme,
                    onCheckedChange = { settingsCallbacks.toggleBlackBackgroundForDarkTheme() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SwitchSettingItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Dynamic Color Scheme",
                        checked = userPreferences.uiSettings.isUsingDynamicColor,
                        onCheckedChange = { settingsCallbacks.toggleDynamicColorScheme() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                }
                var accentColorDialogVisible by remember { mutableStateOf(false) }
                if (accentColorDialogVisible) {
                    ColorPickerDialog(
                        initialColor = userPreferences.uiSettings.accentColor.fromIntToAccentColor(),
                        onColorChanged = { color -> settingsCallbacks.setAccentColor(color.toInt()) },
                        onDismissRequest = { accentColorDialogVisible = false }
                    )
                }
                NavigationSettingItem(
                    icon = Icons.Rounded.ColorLens,
                    title = "Accent Color",
                    subtitle = "Tap to pick primary color",
                    onClick = { accentColorDialogVisible = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                NavigationSettingItem(
                    icon = Icons.Rounded.Equalizer,
                    title = "Visualizer",
                    subtitle = "Coming soon",
                    onClick = {}
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SegmentedSettingItem(
                    icon = Icons.Rounded.Palette,
                    title = "Now Playing Background",
                    options = listOf("Solid", "Dynamic"),
                    selectedIndex = when (userPreferences.uiSettings.playerThemeUi) { PlayerThemeUi.SOLID -> 0; else -> 1 },
                    onSelect = { settingsCallbacks.onPlayerThemeChanged(when (it) { 0 -> PlayerThemeUi.SOLID; else -> PlayerThemeUi.BLUR }) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                NumberSettingItem(
                    icon = Icons.Rounded.BlurCircular,
                    title = "Background Blur Intensity",
                    value = userPreferences.uiSettings.backgroundBlur,
                    onValueChange = { settingsCallbacks.setBackgroundBlur(it) },
                    valueRange = 0..100,
                    step = 5,
                    suffix = "%"
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SwitchSettingItem(
                    icon = Icons.Rounded.ScreenLockPortrait,
                    title = "Keep Screen On",
                    checked = userPreferences.uiSettings.keepScreenOn,
                    onCheckedChange = { settingsCallbacks.setKeepScreenOn(it) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                SwitchSettingItem(
                    icon = Icons.Rounded.SmartDisplay,
                    title = "MiniPlayer Extra Controls",
                    checked = userPreferences.uiSettings.showMiniPlayerExtraControls,
                    onCheckedChange = { settingsCallbacks.toggleShowExtraControls() }
                )
            }
        }

        // ━━ Library & Storage ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        item { SettingsSectionHeader("Library & Storage") }
        item {
            SettingsSection {
                var blacklistDialogVisible by remember { mutableStateOf(false) }
                BlacklistedFoldersDialog(
                    isVisible = blacklistDialogVisible,
                    folders = userPreferences.librarySettings.excludedFolders,
                    onFolderAdded = { settingsCallbacks.onFolderAdded(it) },
                    onFolderDeleted = settingsCallbacks::onFolderDeleted,
                    onDismissRequest = { blacklistDialogVisible = false }
                )
                SwitchSettingItem(
                    icon = Icons.Rounded.Cached,
                    title = "Cache Album Art",
                    subtitle = "Reuses album art to improve loading",
                    checked = userPreferences.librarySettings.cacheAlbumCoverArt,
                    onCheckedChange = { settingsCallbacks.onToggleCacheAlbumArt() }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                NavigationSettingItem(
                    icon = Icons.Rounded.Block,
                    title = "Blacklisted Folders",
                    subtitle = "Music in these folders will not appear in the app",
                    onClick = { blacklistDialogVisible = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                val context = LocalContext.current
                
                val directoryPicker = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.OpenDocumentTree(),
                    onResult = { uri ->
                        if (uri == null) return@rememberLauncherForActivityResult
                        val documentTree = DocumentsContract.buildDocumentUriUsingTree(
                            uri,
                            DocumentsContract.getTreeDocumentId(uri)
                        )
                        val path = getPath(context, documentTree) ?: return@rememberLauncherForActivityResult
                        settingsCallbacks.setScanDirectory(path)
                    }
                )

                NavigationSettingItem(
                    icon = Icons.Rounded.FolderOpen,
                    title = "Scan Directory",
                    subtitle = scanDirectory,
                    onClick = { directoryPicker.launch(null) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                ButtonSettingItem(
                    icon = Icons.Rounded.Sync,
                    title = "Rescan Library",
                    subtitle = "Trigger background library deep scan",
                    buttonLabel = "Scan",
                    onClick = {
                        val intent = android.content.Intent(context, com.tx24.spicyplayer.library.store.ScanService::class.java).apply {
                            putExtra("scan_path", scanDirectory)
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        android.widget.Toast.makeText(context, "Scanning Started", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )

                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

                ButtonSettingItem(
                    icon = Icons.Rounded.Photo,
                    title = "Clear Image Cache",
                    subtitle = "Album art bitmaps",
                    buttonLabel = "Clear",
                    onClick = {
                        context.cacheDir.listFiles()?.filter {
                            it.name.endsWith(".png") || it.name.endsWith(".jpg") || it.name.endsWith(".webp")
                        }?.forEach { it.delete() }
                        android.widget.Toast.makeText(context, "Image cache cleared", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                ButtonSettingItem(
                    icon = Icons.Rounded.Lyrics,
                    title = "Clear Lyrics Cache",
                    subtitle = "Cached TTML parse results",
                    buttonLabel = "Clear",
                    onClick = {
                        context.cacheDir.listFiles()?.filter { it.name.endsWith(".ttml") }?.forEach { it.delete() }
                        android.widget.Toast.makeText(context, "Lyrics cache cleared", android.widget.Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
        
        // ━━ About ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
        item { SettingsSectionHeader("About") }
        item {
            SettingsSection {
                NavigationSettingItem(
                    icon = Icons.Rounded.Info,
                    title = "About Spicy Player",
                    onClick = showAbout
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                NavigationSettingItem(
                    icon = Icons.Rounded.Update,
                    title = "Check for Updates",
                    subtitle = "Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                    onClick = { settingsCallbacks.checkForUpdates(true) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                NavigationSettingItem(
                    icon = Icons.Rounded.Policy,
                    title = "Open Source Licenses",
                    onClick = showLicenses
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                NavigationSettingItem(
                    icon = Icons.Rounded.Code,
                    title = "View on GitHub",
                    subtitle = "TheX24/Spicy-Player",
                    onClick = {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/thex24/Spicy-Player"))
                        context.startActivity(intent)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                NavigationSettingItem(
                    icon = Icons.Rounded.Restore,
                    title = "Reset Defaults",
                    subtitle = "Restore all settings to original values",
                    onClick = onNavigateToReset
                )
            }
        }
    }
}


@Composable
fun BlacklistedFoldersDialog(
    isVisible: Boolean,
    folders: List<String>,
    onFolderAdded: (String) -> Unit,
    onFolderDeleted: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {

    if (!isVisible) return

    val context = LocalContext.current
    val directoryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri == null) return@rememberLauncherForActivityResult
            val documentTree = DocumentsContract.buildDocumentUriUsingTree(
                uri,
                DocumentsContract.getTreeDocumentId(uri)
            )
            val path = getPath(context, documentTree) ?: return@rememberLauncherForActivityResult
            onFolderAdded(path)
        }
    )


    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(text = "Close") } },
        confirmButton = { },
        icon = { Icon(Icons.Rounded.Block, contentDescription = null) },
        title = { Text(text = "Blacklisted Folders") },
        text = {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                LazyColumn(modifier = Modifier) {
                    items(folders) {
                        Row(
                            modifier = Modifier,
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = it, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(4.dp))
                            IconButton(onClick = { onFolderDeleted(it) }) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Remove Folder from Blacklist"
                                )
                            }
                        }
                        if (it != folders.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(Modifier.fillMaxWidth())
                Row(
                    verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(4.dp)
                        )
                        .clickable { directoryPicker.launch(null) }
                        .padding(8.dp)) {
                    Icon(imageVector = Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Add Path")
                }
            }
        }
    )

}


@Composable
fun AppThemeDialog(
    visible: Boolean,
    currentSelected: AppThemeUi,
    onDismissRequest: () -> Unit,
    onThemeSelected: (AppThemeUi) -> Unit,
) {
    if (!visible) return
    val optionsStrings = listOf("Follow System Settings", "Light", "Dark")
    val options = listOf(AppThemeUi.SYSTEM, AppThemeUi.LIGHT, AppThemeUi.DARK)
    val selectedOptionIndex by remember {
        mutableStateOf(
            options.indexOf(currentSelected).coerceAtLeast(0)
        )
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(text = "Cancel") } },
        confirmButton = { },
        icon = { Icon(Icons.Rounded.LightMode, contentDescription = null) },
        title = { Text(text = "App Theme") },
        text = {
            Column {
                optionsStrings.forEachIndexed { index, option ->
                    val onSelected = {
                        if (index == selectedOptionIndex) {
                            Unit
                        } else {
                            onThemeSelected(options[index])
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelected() }) {
                        RadioButton(
                            selected = selectedOptionIndex == index,
                            onClick = { onSelected() }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = option)
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    )
}

@Composable
fun PlayerThemeDialog(
    visible: Boolean,
    currentSelected: PlayerThemeUi,
    onDismissRequest: () -> Unit,
    onThemeSelected: (PlayerThemeUi) -> Unit,
) {
    if (!visible) return
    val optionsStrings = listOf("Solid", "Blur")
    val options = listOf(PlayerThemeUi.SOLID, PlayerThemeUi.BLUR)
    val selectedOptionIndex by remember {
        mutableStateOf(
            options.indexOf(currentSelected).coerceAtLeast(0)
        )
    }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = { TextButton(onClick = onDismissRequest) { Text(text = "Cancel") } },
        confirmButton = { },
        icon = { Icon(Icons.Rounded.BlurCircular, contentDescription = null) },
        title = { Text(text = "Player Theme") },
        text = {
            Column {
                optionsStrings.forEachIndexed { index, option ->
                    val onSelected = {
                        if (index == selectedOptionIndex) {
                            Unit
                        } else {
                            onThemeSelected(options[index])
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelected() }
                    ) {
                        RadioButton(
                            selected = selectedOptionIndex == index,
                            onClick = { }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = option)
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    )
}


@Composable
fun SectionTitle(
    modifier: Modifier,
    title: String
) {
    Text(
        modifier = modifier,
        text = title,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.tertiary
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopAppBar(topAppBarScrollBehavior: TopAppBarScrollBehavior, onBackPressed: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Settings", fontWeight = FontWeight.SemiBold) },
        scrollBehavior = topAppBarScrollBehavior,
        navigationIcon = {
            IconButton(onClick = onBackPressed) {
                Icon(
                    imageVector = Icons.AutoMirrored.Sharp.ArrowBack,
                    contentDescription = "Go Back"
                )
            }
        }
    )
}