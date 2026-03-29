package com.omar.musica.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omar.musica.ui.model.UserPreferencesUi

@Composable
fun ResetOptionItem(
    title: String,
    value: String,
    onReset: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = onReset,
            colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        ) {
            Icon(Icons.Rounded.Restore, contentDescription = "Reset $title")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetSettingsScreen(
    userPreferences: UserPreferencesUi,
    settingsCallbacks: ISettingsViewModel,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(end = 24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBack) {
                                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back", modifier = Modifier.size(20.dp))
                            }
                            Text(
                                text = "Reset Defaults",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = innerPadding.calculateBottomPadding() + 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { settingsCallbacks.resetAll() },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                ) {
                    Icon(Icons.Rounded.Warning, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Reset All Settings")
                }
                Spacer(Modifier.height(8.dp))
            }

            // ━━ Lyrics ━━━━
            item { SettingsSectionHeader("Lyrics") }
            item {
                SettingsSection {
                    com.omar.musica.settings.common.ButtonSettingItem(
                        icon = Icons.Rounded.Restore, 
                        title = "Reset Lyrics Section", 
                        buttonLabel = "Reset", 
                        onClick = { settingsCallbacks.resetLyrics() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Global Sync Offset", "${userPreferences.uiSettings.lyricsOffsetMs}ms (Default: 0ms)") { settingsCallbacks.setLyricsOffsetMs(0) }
                    ResetOptionItem("Font Size", "${userPreferences.uiSettings.lyricsFontSize} (Default: MEDIUM)") { settingsCallbacks.setLyricsFontSize("MEDIUM") }
                }
            }

            // ━━ Audio & Playback ━━━━
            item { SettingsSectionHeader("Audio & Playback") }
            item {
                SettingsSection {
                    com.omar.musica.settings.common.ButtonSettingItem(
                        icon = Icons.Rounded.Restore, 
                        title = "Reset Audio & Playback", 
                        buttonLabel = "Reset", 
                        onClick = { settingsCallbacks.resetAudio() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Crossfade Duration", "${userPreferences.playerSettings.crossfadeDuration}s (Default: 0s)") { settingsCallbacks.setCrossfadeDuration(0) }
                    ResetOptionItem("Gapless Playback", "${userPreferences.playerSettings.gaplessPlayback} (Default: true)") { settingsCallbacks.setGaplessPlayback(true) }
                    ResetOptionItem("Previous Skip Threshold", "${userPreferences.playerSettings.previousSkipThreshold}s (Default: 5s)") { settingsCallbacks.setPreviousSkipThreshold(5) }
                    ResetOptionItem("Audio Focus Behavior", "${userPreferences.playerSettings.audioFocusBehavior} (Default: PAUSE)") { settingsCallbacks.setAudioFocusBehavior("PAUSE") }
                    ResetOptionItem("Replay Gain", "${userPreferences.playerSettings.replayGain} (Default: false)") { settingsCallbacks.setReplayGain(false) }
                    ResetOptionItem("Pause on Vol 0", "${userPreferences.playerSettings.pauseOnVolumeZero} (Default: false)") { if (userPreferences.playerSettings.pauseOnVolumeZero) settingsCallbacks.togglePauseVolumeZero() }
                    ResetOptionItem("Resume on Vol > 0", "${userPreferences.playerSettings.resumeWhenVolumeIncreases} (Default: false)") { if (userPreferences.playerSettings.resumeWhenVolumeIncreases) settingsCallbacks.toggleResumeVolumeNotZero() }
                }
            }

            // ━━ Appearance & Display ━━━━
            item { SettingsSectionHeader("Appearance & Display") }
            item {
                SettingsSection {
                    com.omar.musica.settings.common.ButtonSettingItem(
                        icon = Icons.Rounded.Restore, 
                        title = "Reset Appearance", 
                        buttonLabel = "Reset", 
                        onClick = { settingsCallbacks.resetAppearance() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("App Theme", "${userPreferences.uiSettings.theme} (Default: SYSTEM)") { settingsCallbacks.onThemeSelected(com.omar.musica.ui.model.AppThemeUi.SYSTEM) }
                    ResetOptionItem("Dynamic Color", "${userPreferences.uiSettings.isUsingDynamicColor} (Default: false)") { if (userPreferences.uiSettings.isUsingDynamicColor) settingsCallbacks.toggleDynamicColorScheme() }
                    ResetOptionItem("Black Background (Dark)", "${userPreferences.uiSettings.blackBackgroundForDarkTheme} (Default: false)") { if (userPreferences.uiSettings.blackBackgroundForDarkTheme) settingsCallbacks.toggleBlackBackgroundForDarkTheme() }
                    ResetOptionItem("Background Blur", "${userPreferences.uiSettings.backgroundBlur}% (Default: 60%)") { settingsCallbacks.setBackgroundBlur(60) }
                    ResetOptionItem("Extra Controls", "${userPreferences.uiSettings.showMiniPlayerExtraControls} (Default: false)") { if (userPreferences.uiSettings.showMiniPlayerExtraControls) settingsCallbacks.toggleShowExtraControls() }
                    ResetOptionItem("Visualizer Enabled", "${userPreferences.playerSettings.visualizerEnabled} (Default: false)") { if (userPreferences.playerSettings.visualizerEnabled) settingsCallbacks.setVisualizerEnabled(false) }
                }
            }

            // ━━ Player & Library ━━━━
            item { SettingsSectionHeader("Player & Library") }
            item {
                SettingsSection {
                    com.omar.musica.settings.common.ButtonSettingItem(
                        icon = Icons.Rounded.Restore, 
                        title = "Reset Player & Library", 
                        buttonLabel = "Reset", 
                        onClick = { settingsCallbacks.resetPlayer() }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Keep Screen On", "${userPreferences.uiSettings.keepScreenOn} (Default: false)") { settingsCallbacks.setKeepScreenOn(false) }
                    ResetOptionItem("Scan Directory", "${userPreferences.librarySettings.scanDirectory} (Default: /sdcard/Music/)") { settingsCallbacks.setScanDirectory("/sdcard/Music/") }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 0.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
fun SettingsSection(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
        )
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            content()
        }
    }
}
