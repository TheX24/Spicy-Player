package com.tx24.spicyplayer.library.store.preferences

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import com.tx24.spicyplayer.library.database.dao.BlacklistedFoldersDao
import com.tx24.spicyplayer.library.database.entities.prefs.BlacklistedFolderEntity
import com.tx24.spicyplayer.model.AlbumsSortOption
import com.tx24.spicyplayer.model.SongSortOption
import com.tx24.spicyplayer.model.prefs.AppTheme
import com.tx24.spicyplayer.model.prefs.DEFAULT_ACCENT_COLOR
import com.tx24.spicyplayer.model.prefs.DEFAULT_JUMP_DURATION_MILLIS
import com.tx24.spicyplayer.model.prefs.LibrarySettings
import com.tx24.spicyplayer.model.prefs.MiniPlayerMode
import com.tx24.spicyplayer.model.prefs.PlayerSettings
import com.tx24.spicyplayer.model.prefs.PlayerTheme
import com.tx24.spicyplayer.model.prefs.UiSettings
import com.tx24.spicyplayer.model.prefs.UserPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val blacklistDao: BlacklistedFoldersDao
) {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Shared so the DataStore read + blacklist Room query run once for all
    // collectors (PlaybackManager, PlaybackService, MainActivity, widgets)
    // instead of re-running the combine per collector.
    val userSettingsFlow: Flow<UserPreferences> =
        combine(
            context.datastore.data.catch { emptyPreferences() },
            blacklistDao.getAllBlacklistedFoldersFlow()
        ) { settings, blacklistFolders ->
            mapPrefsToModel(settings, blacklistFolders)
        }.shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    val librarySettingsFlow = userSettingsFlow
        .map {
            it.librarySettings
        }.distinctUntilChanged()

    val playerSettingsFlow = userSettingsFlow
        .map {
            it.playerSettings
        }.distinctUntilChanged()


    suspend fun saveCurrentPosition(songUriString: String, position: Long) {
        context.datastore.edit {
            it[SONG_URI_KEY] = songUriString
            it[SONG_POSITION_KEY] = position
        }
    }

    suspend fun getSavedPosition(): Pair<String?, Long> {
        val prefs = context.datastore.data.first()
        val songUri = prefs[SONG_URI_KEY]
        val songPosition = prefs[SONG_POSITION_KEY] ?: 0
        return songUri to songPosition
    }

    suspend fun changeLibrarySortOrder(songSortOption: SongSortOption, isAscending: Boolean) {
        context.datastore.edit {
            it[SONGS_SORT_ORDER_KEY] = "${songSortOption}:$isAscending"
        }
    }

    suspend fun changeAlbumsSortOrder(order: AlbumsSortOption, isAscending: Boolean) {
        context.datastore.edit {
            it[ALBUMS_SORT_ORDER_KEY] = "${order}:$isAscending"
        }
    }

    suspend fun changeAlbumsGridSize(size: Int) {
        context.datastore.edit {
            it[ALBUMS_GRID_SIZE_KEY] = size
        }
    }

    suspend fun changeTheme(appTheme: AppTheme) {
        context.datastore.edit {
            it[THEME_KEY] = appTheme.toString()
        }
    }

    suspend fun toggleBlackBackgroundForDarkTheme() {
        toggleBoolean(BLACK_BACKGROUND_FOR_DARK_THEME_KEY)
    }

    suspend fun setBlackBackgroundForDarkTheme(enabled: Boolean) {
        context.datastore.edit { it[BLACK_BACKGROUND_FOR_DARK_THEME_KEY] = enabled }
    }

    suspend fun setAccentColor(color: Int) {
        context.datastore.edit {
            it[ACCENT_COLOR_KEY] = color
        }
    }

    suspend fun changePlayerTheme(playerTheme: PlayerTheme) {
        context.datastore.edit {
            it[PLAYER_THEME_KEY] = playerTheme.toString()
        }
    }

    suspend fun toggleCacheAlbumArt() {
        toggleBoolean(CACHE_ALBUM_COVER_ART_KEY)
    }

    suspend fun toggleDynamicColor() {
        toggleBoolean(DYNAMIC_COLOR_KEY)
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.datastore.edit { it[DYNAMIC_COLOR_KEY] = enabled }
    }

    suspend fun togglePauseVolumeZero() {
        toggleBoolean(PAUSE_IF_VOLUME_ZERO)
    }

    suspend fun setPauseVolumeZero(enabled: Boolean) {
        context.datastore.edit { it[PAUSE_IF_VOLUME_ZERO] = enabled }
    }

    suspend fun toggleMiniPlayerExtraControls() {
        toggleBoolean(MINI_PLAYER_EXTRA_CONTROLS)
    }

    suspend fun setMiniPlayerExtraControls(enabled: Boolean) {
        context.datastore.edit { it[MINI_PLAYER_EXTRA_CONTROLS] = enabled }
    }

    suspend fun toggleResumeVolumeNotZero() {
        toggleBoolean(RESUME_IF_VOLUME_INCREASED)
    }

    suspend fun setResumeVolumeNotZero(enabled: Boolean) {
        context.datastore.edit { it[RESUME_IF_VOLUME_INCREASED] = enabled }
    }

    suspend fun deleteFolderFromBlacklist(folder: String) = withContext(Dispatchers.IO) {
        blacklistDao.deleteFolder(folder)
    }

    suspend fun addBlacklistedFolder(folder: String) = withContext(Dispatchers.IO) {
        blacklistDao.addFolder(BlacklistedFolderEntity(0, folder))
    }

    suspend fun setPreviousSkipThreshold(duration: Int) {
        context.datastore.edit {
            it[PREVIOUS_SKIP_THRESHOLD_KEY] = duration
        }
    }

    suspend fun setShowTranslation(show: Boolean) {
        context.datastore.edit { it[SHOW_TRANSLATION_KEY] = show }
    }

    suspend fun setReplayGain(gain: Boolean) {
        context.datastore.edit { it[REPLAY_GAIN_KEY] = gain }
    }

    suspend fun setVisualizerEnabled(enabled: Boolean) {
        context.datastore.edit { it[VISUALIZER_ENABLED_KEY] = enabled }
    }

    suspend fun setLyricsOffsetMs(offset: Int) {
        context.datastore.edit { it[LYRICS_OFFSET_KEY] = offset }
    }

    suspend fun setLyricsFontSize(size: String) {
        context.datastore.edit { it[LYRICS_FONT_SIZE_KEY] = size }
    }

    suspend fun setBackgroundBlur(blur: Int) {
        context.datastore.edit { it[BACKGROUND_BLUR_KEY] = blur }
    }

    suspend fun setLyricsQualityMode(mode: String) {
        context.datastore.edit { it[LYRICS_QUALITY_MODE_KEY] = mode }
    }

    suspend fun setLyricsBackgroundEngine(engine: String) {
        context.datastore.edit { it[LYRICS_BG_ENGINE_KEY] = engine }
    }

    suspend fun setLyricsRomanize(enabled: Boolean) {
        context.datastore.edit { it[LYRICS_ROMANIZE_KEY] = enabled }
    }


    suspend fun clear() {
        context.datastore.edit { it.clear() }
    }

    suspend fun onCrossfadeDurationChanged(duration: Int) {
        context.datastore.edit { it[CROSSFADE_DURATION_KEY] = duration }
    }

    suspend fun setGaplessPlayback(gapless: Boolean) {
        context.datastore.edit { it[GAPLESS_PLAYBACK_KEY] = gapless }
    }

    suspend fun setKeepScreenOn(keep: Boolean) {
        context.datastore.edit { it[KEEP_SCREEN_ON_KEY] = keep }
    }

    suspend fun setCrossfadeDuration(duration: Int) {
        context.datastore.edit { it[CROSSFADE_DURATION_KEY] = duration }
    }


    suspend fun setAudioFocusBehavior(behavior: String) {
        context.datastore.edit { it[AUDIO_FOCUS_BEHAVIOR_KEY] = behavior }
    }

    private suspend fun toggleBoolean(key: Preferences.Key<Boolean>, default: Boolean = true) {
        context.datastore.edit {
            it[key] = !(it[key] ?: !default)
        }
    }


    private fun Preferences.getPlayerSettings(): PlayerSettings {
        val previousSkipThreshold = this[PREVIOUS_SKIP_THRESHOLD_KEY] ?: 5
        val pauseOnVolumeZero = this[PAUSE_IF_VOLUME_ZERO] ?: false
        val resumeWhenVolumeIncreases = this[RESUME_IF_VOLUME_INCREASED] ?: false
        val crossfadeDuration = this[CROSSFADE_DURATION_KEY] ?: 0
        val gaplessPlayback = this[GAPLESS_PLAYBACK_KEY] ?: true
        val audioFocusBehavior = this[AUDIO_FOCUS_BEHAVIOR_KEY] ?: "PAUSE"
        val showTranslation = this[SHOW_TRANSLATION_KEY] ?: false
        val replayGain = this[REPLAY_GAIN_KEY] ?: false
        val visualizerEnabled = this[VISUALIZER_ENABLED_KEY] ?: false
        return PlayerSettings(
            previousSkipThreshold,
            pauseOnVolumeZero,
            resumeWhenVolumeIncreases,
            crossfadeDuration,
            gaplessPlayback,
            audioFocusBehavior,
            showTranslation,
            replayGain,
            visualizerEnabled
        )
    }

    /**
     * Decodes a stored enum name, falling back to [default] when the stored value
     * no longer matches any constant (renamed across versions, corrupted store).
     * A plain valueOf here would throw inside the settings flow mapping and crash
     * every collector (theme, playback service, widgets) with no recovery.
     */
    private inline fun <reified T : Enum<T>> safeEnumValueOf(value: String?, default: T): T =
        try {
            if (value == null) default else enumValueOf<T>(value)
        } catch (e: IllegalArgumentException) {
            default
        }

    private fun Preferences.getUiSettings(): UiSettings {
        val theme = safeEnumValueOf(this[THEME_KEY], AppTheme.SYSTEM)
        val isUsingDynamicColor = this[DYNAMIC_COLOR_KEY] ?: true
        val playerTheme = safeEnumValueOf(this[PLAYER_THEME_KEY], PlayerTheme.BLUR)
        val blackBackgroundForDarkTheme = this[BLACK_BACKGROUND_FOR_DARK_THEME_KEY] ?: false
        val accentColor = this[ACCENT_COLOR_KEY] ?: DEFAULT_ACCENT_COLOR
        val miniPlayerExtraControls = this[MINI_PLAYER_EXTRA_CONTROLS] ?: false
        
        val lyricsOffsetMs = this[LYRICS_OFFSET_KEY] ?: 0
        val lyricsFontSize = this[LYRICS_FONT_SIZE_KEY] ?: "MEDIUM"
        val backgroundBlur = this[BACKGROUND_BLUR_KEY] ?: 60
        val keepScreenOn = this[KEEP_SCREEN_ON_KEY] ?: false
        val lyricsQualityMode = this[LYRICS_QUALITY_MODE_KEY] ?: "FULL"
        val lyricsBackgroundEngine = this[LYRICS_BG_ENGINE_KEY] ?: "AUTO"
        val lyricsRomanize = this[LYRICS_ROMANIZE_KEY] ?: false

        return UiSettings(
            theme,
            isUsingDynamicColor,
            playerTheme,
            blackBackgroundForDarkTheme,
            MiniPlayerMode.PINNED,
            accentColor,
            miniPlayerExtraControls,
            lyricsOffsetMs,
            lyricsFontSize,
            backgroundBlur,
            keepScreenOn,
            lyricsQualityMode,
            lyricsBackgroundEngine,
            lyricsRomanize
        )
    }

    private fun Preferences.getLibrarySettings(excludedFolders: List<String>): LibrarySettings {
        val songSortOptionsParts = this[SONGS_SORT_ORDER_KEY]?.split(":")
        val albumsSortOptionsParts = this[ALBUMS_SORT_ORDER_KEY]?.split(":")

        val albumsGridSize = this[ALBUMS_GRID_SIZE_KEY] ?: 2

        val songsSortOrder = if (songSortOptionsParts == null)
            SongSortOption.TITLE to true
        else
            safeEnumValueOf(songSortOptionsParts.getOrNull(0), SongSortOption.TITLE) to
                    (songSortOptionsParts.getOrNull(1)?.toBoolean() ?: true)

        val albumsSortOrder = if (albumsSortOptionsParts == null)
            AlbumsSortOption.NAME to true
        else
            safeEnumValueOf(albumsSortOptionsParts.getOrNull(0), AlbumsSortOption.NAME) to
                    (albumsSortOptionsParts.getOrNull(1)?.toBoolean() ?: true)


        val cacheAlbumCoverArt = this[CACHE_ALBUM_COVER_ART_KEY] ?: true

        return LibrarySettings(
            songsSortOrder, albumsSortOrder, albumsGridSize, cacheAlbumCoverArt, excludedFolders
        )
    }

    private fun mapPrefsToModel(
        prefs: Preferences,
        blacklistedFolders: List<BlacklistedFolderEntity>
    ) = UserPreferences(
        prefs.getLibrarySettings(blacklistedFolders.map { it.folderPath }),
        prefs.getUiSettings(),
        prefs.getPlayerSettings()
    )

    companion object {
        val SONGS_SORT_ORDER_KEY = stringPreferencesKey("SONGS_SORT")
        val ALBUMS_SORT_ORDER_KEY = stringPreferencesKey("ALBUMS_SORT")
        val THEME_KEY = stringPreferencesKey("THEME")
        val DYNAMIC_COLOR_KEY = booleanPreferencesKey("DYNAMIC_COLOR")
        val PLAYER_THEME_KEY = stringPreferencesKey("PLAYER_THEME")
        val BLACK_BACKGROUND_FOR_DARK_THEME_KEY =
            booleanPreferencesKey("BLACK_BACKGROUND_FOR_DARK_THEME")
        val CACHE_ALBUM_COVER_ART_KEY = booleanPreferencesKey("CACHE_ALBUM_COVER_ART")
        val PREVIOUS_SKIP_THRESHOLD_KEY = intPreferencesKey("PREVIOUS_SKIP_THRESHOLD_KEY")
        val SONG_URI_KEY = stringPreferencesKey("SONG_URI")
        val SONG_POSITION_KEY = longPreferencesKey("SONG_POSITION")
        val PAUSE_IF_VOLUME_ZERO = booleanPreferencesKey("PAUSE_VOLUME_ZERO")
        val RESUME_IF_VOLUME_INCREASED = booleanPreferencesKey("RESUME_IF_VOLUME_INCREASED")
        val ACCENT_COLOR_KEY = intPreferencesKey("ACCENT_COLOR")
        val MINI_PLAYER_EXTRA_CONTROLS = booleanPreferencesKey("MINI_PLAYER_EXTRA_CONTROLS")
        val ALBUMS_GRID_SIZE_KEY = intPreferencesKey("ALBUMS_GRID_SIZE")

        val LYRICS_OFFSET_KEY = intPreferencesKey("LYRICS_OFFSET")
        val LYRICS_FONT_SIZE_KEY = stringPreferencesKey("LYRICS_FONT_SIZE")
        val BACKGROUND_BLUR_KEY = intPreferencesKey("BACKGROUND_BLUR")
        val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("KEEP_SCREEN_ON")
        val LYRICS_QUALITY_MODE_KEY = stringPreferencesKey("LYRICS_QUALITY_MODE")
        val LYRICS_BG_ENGINE_KEY = stringPreferencesKey("LYRICS_BG_ENGINE")
        val LYRICS_ROMANIZE_KEY = booleanPreferencesKey("LYRICS_ROMANIZE")

        val CROSSFADE_DURATION_KEY = intPreferencesKey("CROSSFADE_DURATION")
        val GAPLESS_PLAYBACK_KEY = booleanPreferencesKey("GAPLESS_PLAYBACK")
        val AUDIO_FOCUS_BEHAVIOR_KEY = stringPreferencesKey("AUDIO_FOCUS_BEHAVIOR")
        val SHOW_TRANSLATION_KEY = booleanPreferencesKey("SHOW_TRANSLATION")
        val REPLAY_GAIN_KEY = booleanPreferencesKey("REPLAY_GAIN")
        val VISUALIZER_ENABLED_KEY = booleanPreferencesKey("VISUALIZER_ENABLED")
    }

}