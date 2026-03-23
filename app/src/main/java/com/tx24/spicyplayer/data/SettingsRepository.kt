package com.tx24.spicyplayer.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "spicy_settings")

class SettingsRepository(private val context: Context) {

    companion object {
        // Lyrics
        val LYRICS_OFFSET_MS    = intPreferencesKey("lyrics_offset_ms")
        val LYRICS_FONT_SIZE    = stringPreferencesKey("lyrics_font_size")   // SMALL, MEDIUM, LARGE

        // Audio
        val EQ_PRESET           = stringPreferencesKey("eq_preset")           // FLAT, BASS_BOOST, TREBLE, VOCAL, CUSTOM
        val BASS_BOOST_ENABLED  = booleanPreferencesKey("bass_boost_enabled")
        val BASS_BOOST_STRENGTH = intPreferencesKey("bass_boost_strength")    // 0–1000
        val LOUDNESS_ENABLED    = booleanPreferencesKey("loudness_enabled")
        val LOUDNESS_STRENGTH   = intPreferencesKey("loudness_strength")      // 0–1000
        val CROSSFADE_DURATION  = intPreferencesKey("crossfade_duration_s")   // 0–10
        val GAPLESS_PLAYBACK    = booleanPreferencesKey("gapless_playback")
        val BACK_SKIP_THRESHOLD = intPreferencesKey("back_skip_threshold")
        val CUSTOM_EQ_BANDS     = stringPreferencesKey("custom_eq_bands")

        // Appearance
        val APP_THEME           = stringPreferencesKey("app_theme")            // LIGHT, DARK, SYSTEM
        val MATERIAL_YOU        = booleanPreferencesKey("material_you")
        val CONTROLS_STYLE      = stringPreferencesKey("controls_style")       // CLASSIC, EXPRESSIVE
        val BACKGROUND_BLUR     = intPreferencesKey("background_blur_pct")     // 0–100
        val PURE_BLACK          = booleanPreferencesKey("pure_black")
        val CONTRAST_LEVEL      = floatPreferencesKey("contrast_level")        // 0.0, 0.5, 1.0

        // General
        val KEEP_SCREEN_ON      = booleanPreferencesKey("keep_screen_on")
        val AUDIO_FOCUS         = stringPreferencesKey("audio_focus")          // PAUSE, DUCK, IGNORE
        val SCAN_DIRECTORY      = stringPreferencesKey("scan_directory")
    }

    private val dataFlow: Flow<Preferences> = context.dataStore.data
        .catch { e ->
            if (e is IOException) emit(emptyPreferences()) else throw e
        }

    // ── Lyrics ────────────────────────────────────────────────────────────
    val lyricsOffsetMs: Flow<Int>    = dataFlow.map { it[LYRICS_OFFSET_MS] ?: 0 }
    val lyricsFontSize: Flow<String> = dataFlow.map { it[LYRICS_FONT_SIZE] ?: "MEDIUM" }

    // ── Audio ─────────────────────────────────────────────────────────────
    val eqPreset: Flow<String>         = dataFlow.map { it[EQ_PRESET] ?: "FLAT" }
    val bassBoostEnabled: Flow<Boolean> = dataFlow.map { it[BASS_BOOST_ENABLED] ?: false }
    val bassBoostStrength: Flow<Int>   = dataFlow.map { it[BASS_BOOST_STRENGTH] ?: 800 }
    val loudnessEnabled: Flow<Boolean> = dataFlow.map { it[LOUDNESS_ENABLED] ?: false }
    val loudnessStrength: Flow<Int>    = dataFlow.map { it[LOUDNESS_STRENGTH] ?: 0 }
    val crossfadeDuration: Flow<Int>   = dataFlow.map { it[CROSSFADE_DURATION] ?: 0 }
    val gaplessPlayback: Flow<Boolean> = dataFlow.map { it[GAPLESS_PLAYBACK] ?: true }
    val backSkipThreshold: Flow<Int>   = dataFlow.map { it[BACK_SKIP_THRESHOLD] ?: 5 }
    val customEqBands: Flow<List<Float>> = dataFlow.map { prefs ->
        prefs[CUSTOM_EQ_BANDS]?.split(",")?.mapNotNull { it.toFloatOrNull() }?.takeIf { it.size == 5 } ?: listOf(0f, 0f, 0f, 0f, 0f)
    }

    // ── Appearance ────────────────────────────────────────────────────────
    val appTheme: Flow<String>       = dataFlow.map { it[APP_THEME] ?: "SYSTEM" }
    val materialYou: Flow<Boolean>   = dataFlow.map { it[MATERIAL_YOU] ?: false }
    val controlsStyle: Flow<String>  = dataFlow.map { it[CONTROLS_STYLE] ?: "EXPRESSIVE" }
    val backgroundBlur: Flow<Int>    = dataFlow.map { it[BACKGROUND_BLUR] ?: 60 }
    val pureBlack: Flow<Boolean>     = dataFlow.map { it[PURE_BLACK] ?: false }
    val contrastLevel: Flow<Float>   = dataFlow.map { it[CONTRAST_LEVEL] ?: 0f }

    // ── General ───────────────────────────────────────────────────────────
    val keepScreenOn: Flow<Boolean>  = dataFlow.map { it[KEEP_SCREEN_ON] ?: false }
    val audioFocus: Flow<String>     = dataFlow.map { it[AUDIO_FOCUS] ?: "PAUSE" }
    val scanDirectory: Flow<String>  = dataFlow.map { it[SCAN_DIRECTORY] ?: "/sdcard/Music/" }

    // ── Update helpers ────────────────────────────────────────────────────
    suspend fun setLyricsOffsetMs(value: Int)       = context.dataStore.edit { it[LYRICS_OFFSET_MS] = value }
    suspend fun setLyricsFontSize(value: String)    = context.dataStore.edit { it[LYRICS_FONT_SIZE] = value }

    suspend fun setEqPreset(value: String)          = context.dataStore.edit { it[EQ_PRESET] = value }
    suspend fun setBassBoost(value: Boolean)        = context.dataStore.edit { it[BASS_BOOST_ENABLED] = value }
    suspend fun setBassBoostStrength(value: Int)    = context.dataStore.edit { it[BASS_BOOST_STRENGTH] = value }
    suspend fun setLoudnessEnabled(value: Boolean)  = context.dataStore.edit { it[LOUDNESS_ENABLED] = value }
    suspend fun setLoudnessStrength(value: Int)     = context.dataStore.edit { it[LOUDNESS_STRENGTH] = value }
    suspend fun setCrossfadeDuration(value: Int)    = context.dataStore.edit { it[CROSSFADE_DURATION] = value }
    suspend fun setGaplessPlayback(value: Boolean)  = context.dataStore.edit { it[GAPLESS_PLAYBACK] = value }
    suspend fun setBackSkipThreshold(value: Int)    = context.dataStore.edit { it[BACK_SKIP_THRESHOLD] = value }
    suspend fun setCustomEqBands(value: List<Float>) = context.dataStore.edit { it[CUSTOM_EQ_BANDS] = value.joinToString(",") }

    suspend fun setAppTheme(value: String)          = context.dataStore.edit { it[APP_THEME] = value }
    suspend fun setMaterialYou(value: Boolean)      = context.dataStore.edit { it[MATERIAL_YOU] = value }
    suspend fun setControlsStyle(value: String)     = context.dataStore.edit { it[CONTROLS_STYLE] = value }
    suspend fun setBackgroundBlur(value: Int)       = context.dataStore.edit { it[BACKGROUND_BLUR] = value }
    suspend fun setPureBlack(value: Boolean)        = context.dataStore.edit { it[PURE_BLACK] = value }
    suspend fun setContrastLevel(value: Float)      = context.dataStore.edit { it[CONTRAST_LEVEL] = value }

    suspend fun setKeepScreenOn(value: Boolean)     = context.dataStore.edit { it[KEEP_SCREEN_ON] = value }
    suspend fun setAudioFocus(value: String)        = context.dataStore.edit { it[AUDIO_FOCUS] = value }
    suspend fun setScanDirectory(value: String)     = context.dataStore.edit { it[SCAN_DIRECTORY] = value }
}
