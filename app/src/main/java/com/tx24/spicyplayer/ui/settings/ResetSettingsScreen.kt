package com.tx24.spicyplayer.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tx24.spicyplayer.ui.settings.components.ButtonSettingItem
import com.tx24.spicyplayer.ui.settings.components.SettingsSection
import com.tx24.spicyplayer.ui.settings.components.SettingsSectionHeader
import com.tx24.spicyplayer.viewmodel.SettingsViewModel

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
    onBack: () -> Unit,
    vm: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current

    // -- State collection --
    val lyricsOffsetMs     by vm.lyricsOffsetMs.collectAsStateWithLifecycle()
    val lyricsFontSize     by vm.lyricsFontSize.collectAsStateWithLifecycle()

    val eqPreset           by vm.eqPreset.collectAsStateWithLifecycle()
    val bassBoost          by vm.bassBoostEnabled.collectAsStateWithLifecycle()
    val bassBoostStrength  by vm.bassBoostStrength.collectAsStateWithLifecycle()
    val crossfade          by vm.crossfadeDuration.collectAsStateWithLifecycle()
    val gapless            by vm.gaplessPlayback.collectAsStateWithLifecycle()
    val backSkipThreshold  by vm.backSkipThreshold.collectAsStateWithLifecycle()
    val audioFocus         by vm.audioFocus.collectAsStateWithLifecycle()

    val appTheme           by vm.appTheme.collectAsStateWithLifecycle()
    val materialYou        by vm.materialYou.collectAsStateWithLifecycle()
    val controlsStyle      by vm.controlsStyle.collectAsStateWithLifecycle()
    val blur               by vm.backgroundBlur.collectAsStateWithLifecycle()
    val pureBlack          by vm.pureBlack.collectAsStateWithLifecycle()
    val contrastLevel      by vm.contrastLevel.collectAsStateWithLifecycle()

    val keepScreenOn       by vm.keepScreenOn.collectAsStateWithLifecycle()
    val scanDir            by vm.scanDirectory.collectAsStateWithLifecycle()

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
                    onClick = { vm.resetAll() },
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
                    ButtonSettingItem(icon = Icons.Rounded.Restore, title = "Reset Lyrics Section", buttonLabel = "Reset", onClick = { vm.resetLyrics() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Global Sync Offset", "${lyricsOffsetMs}ms (Default: 0ms)") { vm.setLyricsOffsetMs(0) }
                    ResetOptionItem("Font Size", "$lyricsFontSize (Default: MEDIUM)") { vm.setLyricsFontSize("MEDIUM") }
                }
            }

            // ━━ Audio & Playback ━━━━
            item { SettingsSectionHeader("Audio & Playback") }
            item {
                SettingsSection {
                    ButtonSettingItem(icon = Icons.Rounded.Restore, title = "Reset Audio & Playback", buttonLabel = "Reset", onClick = { vm.resetAudio() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Equalizer", "$eqPreset (Default: FLAT)") { vm.setEqPreset("FLAT") }
                    ResetOptionItem("Bass Boost", if (bassBoost) "On (Strength: $bassBoostStrength) -> Default: Off" else "Off (Default: Off)") { vm.setBassBoost(false); vm.setBassBoostStrength(800) }
                    ResetOptionItem("Crossfade Duration", "${crossfade}s (Default: 0s)") { vm.setCrossfadeDuration(0) }
                    ResetOptionItem("Gapless Playback", "$gapless (Default: true)") { vm.setGaplessPlayback(true) }
                    ResetOptionItem("Previous Skip Threshold", "${backSkipThreshold}s (Default: 5s)") { vm.setBackSkipThreshold(3) }
                    ResetOptionItem("Audio Focus Behavior", "$audioFocus (Default: PAUSE)") { vm.setAudioFocus("PAUSE") }
                }
            }

            // ━━ Appearance & Display ━━━━
            item { SettingsSectionHeader("Appearance & Display") }
            item {
                SettingsSection {
                    ButtonSettingItem(icon = Icons.Rounded.Restore, title = "Reset Appearance", buttonLabel = "Reset", onClick = { vm.resetAppearance() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("App Theme", "$appTheme (Default: SYSTEM)") { vm.setAppTheme("SYSTEM") }
                    ResetOptionItem("Material You", "$materialYou (Default: false)") { vm.setMaterialYou(false) }
                    ResetOptionItem("Controls Style", "$controlsStyle (Default: EXPRESSIVE)") { vm.setControlsStyle("EXPRESSIVE") }
                    ResetOptionItem("Background Blur", "$blur% (Default: 60%)") { vm.setBackgroundBlur(60) }
                    ResetOptionItem("AMOLED Pure Black", "$pureBlack (Default: false)") { vm.setPureBlack(false) }
                    ResetOptionItem("Contrast Level", "$contrastLevel (Default: 0.0)") { vm.setContrastLevel(0f) }
                }
            }

            // ━━ Library & Storage ━━━━
            item { SettingsSectionHeader("Library & Storage") }
            item {
                SettingsSection {
                    ButtonSettingItem(icon = Icons.Rounded.Restore, title = "Reset Library & Storage", buttonLabel = "Reset", onClick = { vm.resetLibrary() })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ResetOptionItem("Keep Screen On", "$keepScreenOn (Default: false)") { vm.setKeepScreenOn(false) }
                    ResetOptionItem("Scan Directory", "$scanDir (Default: /sdcard/Music/)") { vm.setScanDirectory("/sdcard/Music/") }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
