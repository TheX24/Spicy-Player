package com.tx24.spicyplayer.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

import com.tx24.spicyplayer.ui.settings.components.*
import com.tx24.spicyplayer.viewmodel.SettingsViewModel

import com.tx24.spicyplayer.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRescan: () -> Unit = {},
    onNavigateToEqualizer: () -> Unit = {},
    onDirectoryClick: () -> Unit = {},
    isScanning: Boolean = false,
    vm: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current

    // ── State collection ──────────────────────────────────────────────────────
    val lyricsOffsetMs     by vm.lyricsOffsetMs.collectAsStateWithLifecycle()
    val lyricsFontSize     by vm.lyricsFontSize.collectAsStateWithLifecycle()

    val eqPreset           by vm.eqPreset.collectAsStateWithLifecycle()
    val bassBoost          by vm.bassBoostEnabled.collectAsStateWithLifecycle()
    val bassBoostStrength  by vm.bassBoostStrength.collectAsStateWithLifecycle()
    val crossfade          by vm.crossfadeDuration.collectAsStateWithLifecycle()
    val gapless            by vm.gaplessPlayback.collectAsStateWithLifecycle()
    val backSkipThreshold  by vm.backSkipThreshold.collectAsStateWithLifecycle()

    val appTheme           by vm.appTheme.collectAsStateWithLifecycle()
    val materialYou        by vm.materialYou.collectAsStateWithLifecycle()
    val controlsStyle      by vm.controlsStyle.collectAsStateWithLifecycle()
    val blur               by vm.backgroundBlur.collectAsStateWithLifecycle()
    val pureBlack          by vm.pureBlack.collectAsStateWithLifecycle()
    val contrastLevel      by vm.contrastLevel.collectAsStateWithLifecycle()

    val keepScreenOn       by vm.keepScreenOn.collectAsStateWithLifecycle()
    val audioFocus         by vm.audioFocus.collectAsStateWithLifecycle()
    val scanDir            by vm.scanDirectory.collectAsStateWithLifecycle()

    // ── Local UI state ────────────────────────────────────────────────────────
    var showResetSettings  by remember { mutableStateOf(false) }

    // ── Sub-screens ───────────────────────────────────────────────────────────
    if (showResetSettings) {
        ResetSettingsScreen(
            onBack = { showResetSettings = false },
            vm = vm
        )
        return
    }


    // ── Content ───────────────────────────────────────────────────────────────
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
                                Icon(
                                    Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = "Back",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Settings",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                },
                title = {},
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
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

            // ━━ Lyrics ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            item { SettingsSectionHeader("Lyrics") }
            item {
                SettingsSection {
                    NumberSettingItem(
                        icon = Icons.Rounded.Tune,
                        title = "Global Sync Offset",
                        value = lyricsOffsetMs,
                        onValueChange = { vm.setLyricsOffsetMs(it) },
                        valueRange = -5000..5000,
                        step = 50,
                        suffix = "ms"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SegmentedSettingItem(
                        icon = Icons.Rounded.FormatSize,
                        title = "Font Size",
                        options = listOf("Small", "Medium", "Large"),
                        selectedIndex = when (lyricsFontSize) { "SMALL" -> 0; "LARGE" -> 2; else -> 1 },
                        onSelect = { vm.setLyricsFontSize(when (it) { 0 -> "SMALL"; 2 -> "LARGE"; else -> "MEDIUM" }) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SwitchSettingItem(
                        icon = Icons.Rounded.Translate,
                        title = "Show Translation",
                        subtitle = "Coming soon",
                        checked = false,
                        onCheckedChange = {},
                        enabled = false
                    )
                }
            }

            // ━━ Audio & Playback ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            item { SettingsSectionHeader("Audio & Playback") }
            item {
                SettingsSection {
                    NavigationSettingItem(
                        icon = Icons.Rounded.GraphicEq,
                        title = "Equalizer",
                        subtitle = eqPreset.lowercase().replaceFirstChar { it.uppercase() },
                        onClick = onNavigateToEqualizer
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SwitchSettingItem(
                        icon = Icons.Rounded.MusicNote,
                        title = "Gapless Playback",
                        checked = gapless,
                        onCheckedChange = { vm.setGaplessPlayback(it) }
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
                    SliderSettingItem(
                        icon = Icons.Rounded.BlurOn,
                        title = "Crossfade Duration",
                        valueLabel = if (crossfade == 0) "Off" else "${crossfade}s",
                        value = crossfade.toFloat(),
                        onValueChange = { vm.setCrossfadeDuration(it.toInt()) },
                        onValueChangeFinished = {},
                        valueRange = 0f..10f,
                        steps = 9
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SliderSettingItem(
                        icon = Icons.Rounded.Replay,
                        title = "Previous Skip Threshold",
                        valueLabel = "${backSkipThreshold}s",
                        value = backSkipThreshold.toFloat(),
                        onValueChange = { vm.setBackSkipThreshold(it.toInt()) },
                        valueRange = 0f..10f,
                        steps = 9
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SegmentedSettingItem(
                        icon = Icons.AutoMirrored.Rounded.VolumeDown,
                        title = "Audio Focus Behavior",
                        options = listOf("Pause", "Continue"),
                        selectedIndex = if (audioFocus == "PAUSE") 0 else 1,
                        onSelect = { vm.setAudioFocus(if (it == 0) "PAUSE" else "DUCK") }
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
                        selectedIndex = when (appTheme) { "LIGHT" -> 0; "DARK" -> 1; else -> 2 },
                        onSelect = { vm.setAppTheme(when (it) { 0 -> "LIGHT"; 1 -> "DARK"; else -> "SYSTEM" }) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SwitchSettingItem(
                        icon = Icons.Rounded.AutoAwesome,
                        title = "Material You",
                        subtitle = "Dynamic colors based on your wallpaper",
                        checked = materialYou,
                        onCheckedChange = { vm.setMaterialYou(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SwitchSettingItem(
                        icon = Icons.Rounded.Brightness1,
                        title = "AMOLED Pure Black",
                        subtitle = "Pure black background for OLED screens",
                        checked = pureBlack,
                        onCheckedChange = { vm.setPureBlack(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SegmentedSettingItem(
                        icon = Icons.Rounded.Contrast,
                        title = "Contrast Level",
                        options = listOf("Standard", "Medium", "High"),
                        selectedIndex = when {
                            contrastLevel <= 0.1f -> 0
                            contrastLevel <= 0.75f -> 1
                            else -> 2
                        },
                        onSelect = { vm.setContrastLevel(when (it) { 1 -> 0.5f; 2 -> 1.0f; else -> 0.0f }) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SegmentedSettingItem(
                        icon = Icons.Rounded.Style,
                        title = "Player Controls Style",
                        options = listOf("Classic", "Expressive"),
                        selectedIndex = if (controlsStyle == "EXPRESSIVE") 1 else 0,
                        onSelect = { vm.setControlsStyle(if (it == 1) "EXPRESSIVE" else "CLASSIC") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    NumberSettingItem(
                        icon = Icons.Rounded.BlurCircular,
                        title = "Background Blur Intensity",
                        value = blur,
                        onValueChange = { vm.setBackgroundBlur(it) },
                        valueRange = 0..100,
                        step = 5,
                        suffix = "%"
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    NavigationSettingItem(
                        icon = Icons.Rounded.Equalizer,
                        title = "Visualizer",
                        subtitle = "Coming soon",
                        onClick = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    SwitchSettingItem(
                        icon = Icons.Rounded.ScreenLockPortrait,
                        title = "Keep Screen On",
                        checked = keepScreenOn,
                        onCheckedChange = { vm.setKeepScreenOn(it) }
                    )
                }
            }

            // ━━ Library & Storage ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

            item { SettingsSectionHeader("Library & Storage") }
            item {
                SettingsSection {
                    NavigationSettingItem(
                        icon = Icons.Rounded.Folder,
                        title = "Scan Directory",
                        subtitle = scanDir,
                        onClick = onDirectoryClick
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    ButtonSettingItem(
                        icon = Icons.Rounded.Refresh,
                        title = "Rescan Library",
                        buttonLabel = if (isScanning) "Scanning…" else "Rescan",
                        isLoading = isScanning,
                        onClick = {
                            if (!isScanning) {
                                onRescan()
                            }
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
                        title = "Version",
                        subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        onClick = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    NavigationSettingItem(
                        icon = Icons.Rounded.Policy,
                        title = "Open Source Licenses",
                        onClick = { /* LicensesActivity */ }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    NavigationSettingItem(
                        icon = Icons.Rounded.Code,
                        title = "View on GitHub",
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/tx24/spicy-player"))
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
                    NavigationSettingItem(
                        icon = Icons.Rounded.Restore,
                        title = "Reset to Defaults",
                        subtitle = "Reset specific settings or all",
                        onClick = { showResetSettings = true }
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}
