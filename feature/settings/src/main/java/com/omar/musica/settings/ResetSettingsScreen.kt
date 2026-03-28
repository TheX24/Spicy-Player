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
            Icon(Icons.Rounded.Restore, contentDescription = "Reset \$title")
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
                    ResetOptionItem("Global Sync Offset", "\${userPreferences.uiSettings.lyricsOffsetMs}ms (Default: 0ms)") { settingsCallbacks.setLyricsOffsetMs(0) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Font Size", "\${userPreferences.uiSettings.lyricsFontSize} (Default: MEDIUM)") { settingsCallbacks.setLyricsFontSize("MEDIUM") }
                }
            }

            // ━━ Audio & Playback ━━━━
            item { SettingsSectionHeader("Audio & Playback") }
            item {
                SettingsSection {
                    ResetOptionItem("Crossfade Duration", "${userPreferences.playerSettings.crossfadeDuration}s (Default: 0s)") { settingsCallbacks.setCrossfadeDuration(0) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Gapless Playback", "${userPreferences.playerSettings.gaplessPlayback} (Default: true)") { settingsCallbacks.setGaplessPlayback(true) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Previous Skip Threshold", "${userPreferences.playerSettings.previousSkipThreshold}s (Default: 5s)") { settingsCallbacks.setPreviousSkipThreshold(5) }
                }
            }

            // ━━ Appearance & Display ━━━━
            item { SettingsSectionHeader("Appearance & Display") }
            item {
                SettingsSection {
                    ResetOptionItem("App Theme", "${userPreferences.uiSettings.theme} (Default: SYSTEM)") { settingsCallbacks.onThemeSelected(com.omar.musica.ui.model.AppThemeUi.SYSTEM) }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Dynamic Color", "${userPreferences.uiSettings.isUsingDynamicColor} (Default: false)") { if (userPreferences.uiSettings.isUsingDynamicColor) settingsCallbacks.toggleDynamicColorScheme() }
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Contrast Level", "${userPreferences.uiSettings.contrastLevel} (Default: 0.0)") { settingsCallbacks.setContrastLevel(0f) }
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
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
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
