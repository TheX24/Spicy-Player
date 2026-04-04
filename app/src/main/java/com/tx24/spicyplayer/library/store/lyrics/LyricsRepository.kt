package com.tx24.spicyplayer.library.store.lyrics

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.tx24.spicyplayer.library.database.dao.LyricsDao
import com.tx24.spicyplayer.library.database.entities.lyrics.LyricsEntity
import com.tx24.spicyplayer.model.lyrics.LyricsFetchSource
import com.tx24.spicyplayer.model.lyrics.PlainLyrics
import com.tx24.spicyplayer.model.lyrics.SynchronizedLyrics
import com.tx24.spicyplayer.network.data.LyricsSource
import com.tx24.spicyplayer.network.model.NotFoundException
import com.tx24.spicyplayer.library.store.MediaRepository
import com.shabinder.jaudiotagger.audio.AudioFileIO
import com.shabinder.jaudiotagger.tag.FieldKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class LyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val lyricsDataSource: LyricsSource,
    private val lyricsDao: LyricsDao,
    private val mediaRepository: MediaRepository
) {
    
    private fun robustNormalize(s: String): String = 
        Normalizer.normalize(s, Normalizer.Form.NFC)
            .lowercase().trim().replace(Regex("\\s+"), " ")

    private fun fuzzyNormalize(s: String): String =
        robustNormalize(s)
            .replace(Regex("\\s*[\\[({].*?[\\])}]\\s*"), " ")
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "") // Keep spaces
            .replace(Regex("\\s+"), " ")
            .trim()
            
    private fun isArtistMatch(songArtist: String, fileName: String): Boolean {
        val fArtist = fuzzyNormalize(songArtist)
        val fFileName = fuzzyNormalize(fileName)
        
        if (fFileName.contains(fArtist)) return true
        
        // Split by common separators and check for individual artists
        val individualArtists = songArtist.split(Regex("[,&;/]|\\b(?:feat|ft|with)\\b", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.length > 2 }
            
        return individualArtists.any { fuzzyNormalize(it).let { fa -> fa.isNotBlank() && fFileName.contains(fa) } }
    }

    /**
     * Gets lyrics of some song with specific URI.
     * The song file itself is checked for any embedded lyrics,
     * if they are not found in the file, then we check the database for any cached
     * lyrics, and if they are not found in the database, we use the API service.
     */
    suspend fun getLyrics(
        uri: Uri,
        title: String,
        album: String,
        artist: String,
        durationSeconds: Int
    ): LyricsResult = withContext(Dispatchers.IO) {

        val songPath = mediaRepository.getSongPath(uri)
        val audioFile = File(songPath)

        // 1. Check for local TTML file (rich spicy lyrics)
        val ttmlFile = File(audioFile.parent, audioFile.nameWithoutExtension + ".ttml")
        if (ttmlFile.exists()) {
            return@withContext LyricsResult.FoundTtmlLyrics(
                ttmlFile.readText(),
                LyricsFetchSource.FROM_LOCAL_FILE
            )
        }

        // 2. Check for local LRC file
        val lrcFile = File(audioFile.parent, audioFile.nameWithoutExtension + ".lrc")
        if (lrcFile.exists()) {
            val syncedLyrics = SynchronizedLyrics.fromString(lrcFile.readText())
            if (syncedLyrics != null) {
                return@withContext LyricsResult.FoundSyncedLyrics(
                    syncedLyrics,
                    LyricsFetchSource.FROM_LOCAL_FILE
                )
            }
        }

        // 3. Fuzzy directory match for TTML/LRC
        run {
            val parentDir = audioFile.parentFile ?: return@run
            val files = parentDir.listFiles() ?: return@run
            
            val fuzzyAudioName = fuzzyNormalize(audioFile.nameWithoutExtension)
            val fuzzyTitle = fuzzyNormalize(title)
            
            val candidate = files.find { file ->
                val ext = file.extension.lowercase()
                if (ext != "ttml" && ext != "lrc") return@find false
                
                val name = file.nameWithoutExtension
                val fuzzyName = fuzzyNormalize(name)
                
                // Compare with filename or metadata title + artist
                fuzzyName == fuzzyAudioName || 
                (fuzzyName.contains(fuzzyTitle) && isArtistMatch(artist, name)) ||
                (fuzzyTitle.length >= 4 && fuzzyName == fuzzyTitle) // Just the title if we're in the same folder
            }
            
            if (candidate != null) {
                val content = candidate.readText()
                if (candidate.extension.lowercase() == "ttml") {
                    return@withContext LyricsResult.FoundTtmlLyrics(content, LyricsFetchSource.FROM_LOCAL_FILE)
                } else {
                    val syncedLyrics = SynchronizedLyrics.fromString(content)
                    if (syncedLyrics != null) {
                        return@withContext LyricsResult.FoundSyncedLyrics(syncedLyrics, LyricsFetchSource.FROM_LOCAL_FILE)
                    }
                }
            }
        }

        val audioFileIO = AudioFileIO().readFile(audioFile)
        val tags = audioFileIO.tagOrCreateAndSetDefault

        // check for embedded lyrics first
        kotlin.run {
            val lyrics = tags.getFirst(FieldKey.LYRICS) ?: return@run
            val syncedLyrics = SynchronizedLyrics.fromString(lyrics)
            if (syncedLyrics != null)
                return@withContext LyricsResult.FoundSyncedLyrics(
                    syncedLyrics,
                    LyricsFetchSource.FROM_SONG_METADATA
                )
            else if (lyrics.isNotBlank())
                return@withContext LyricsResult.FoundPlainLyrics(
                    PlainLyrics.fromString(lyrics),
                    LyricsFetchSource.FROM_SONG_METADATA
                )
        }

        return@withContext downloadLyricsFromInternet(title, album, artist, durationSeconds)
    }

    /**
     * First checks the database if the query is cached
     * and then falls back to the API
     */
    suspend fun downloadLyricsFromInternet(
        title: String,
        album: String,
        artist: String,
        durationSeconds: Int
    ): LyricsResult = withContext(Dispatchers.IO) {
        // check in the DB
        Log.d("lyrics", "Starting lyrics fetch")
        kotlin.run {
            Log.d("lyrics", "Checking DB")
            val lyricsEntity = lyricsDao.getSongLyrics(title, album, artist)

            Log.d("lyrics", "DB result: $lyricsEntity")
            if (lyricsEntity != null && lyricsEntity.syncedLyrics.isNotBlank()) {
                val synced = SynchronizedLyrics.fromString(lyricsEntity.syncedLyrics)
                if (synced != null) {
                    return@withContext LyricsResult.FoundSyncedLyrics(
                        synced,
                        LyricsFetchSource.FROM_INTERNET
                    )
                } else if (lyricsEntity.plainLyrics.isNotBlank()) {
                    return@withContext LyricsResult.FoundPlainLyrics(
                        PlainLyrics.fromString(lyricsEntity.plainLyrics),
                        LyricsFetchSource.FROM_INTERNET
                    )
                }
            }
        }

        Log.d("lyrics", "Downloading Lyrics")
        // finally check from the API
        return@withContext try {
            val lyricsNetwork =
                lyricsDataSource.getSongLyrics(artist, title, album, durationSeconds)
            Log.d("lyrics", "Downloaded: $lyricsNetwork")
            val syncedLyrics = SynchronizedLyrics.fromString(lyricsNetwork.syncedLyrics)
            lyricsDao.saveSongLyrics(
                LyricsEntity(
                    0,
                    title,
                    album,
                    artist,
                    lyricsNetwork.plainLyrics,
                    lyricsNetwork.syncedLyrics
                )
            )
            if (syncedLyrics != null)
                LyricsResult.FoundSyncedLyrics(
                    syncedLyrics,
                    LyricsFetchSource.FROM_INTERNET
                )
            else LyricsResult.FoundPlainLyrics(
                PlainLyrics.fromString(
                    lyricsNetwork.plainLyrics,
                ),
                LyricsFetchSource.FROM_INTERNET
            )
        } catch (e: NotFoundException) {
            Log.d("lyrics", "Downloaded: Not found")
            LyricsResult.NotFound
        } catch (e: Exception) {
            Log.d("lyrics", "Downloaded: ${e.stackTraceToString()}")
            LyricsResult.NetworkError
        }
    }



}
