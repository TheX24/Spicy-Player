package com.tx24.spicyplayer.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.tx24.spicyplayer.models.Song

data class ScanProgress(
    val phase: String = "",
    val currentCount: Int = 0,
    val totalCount: Int = 0,
    val isUpdating: Boolean = false,
    val summary: String = ""
)

fun robustNormalize(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFC)
    .lowercase().trim().replace(Regex("\\s+"), " ")

fun fuzzyNormalize(s: String): String =
    robustNormalize(s)
        .replace(Regex("\\s*[\\[({].*?[\\])}]\\s*"), " ")
        .replace(Regex("[^\\p{L}\\p{N}]"), "")
        .trim()

fun isInstrumental(s: String): Boolean {
    val lower = s.lowercase()
    return lower.contains("instrumental") || lower.contains("karaoke")
}

private const val CACHE_FILE_NAME = "library_cache.txt"

suspend fun loadCachedScan(context: Context, scanPath: String): List<Song>? = withContext(Dispatchers.IO) {
    try {
        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        if (!cacheFile.exists()) return@withContext null

        val lines = cacheFile.readLines()
        if (lines.isEmpty()) return@withContext null

        val cachedScanPath = lines[0]
        if (cachedScanPath != scanPath) return@withContext null

        val results = mutableListOf<Song>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            val parts = line.split("|")
            if (parts.size >= 8) {
                val audioFile = File(parts[0])
                val ttmlFile = if (parts[1].isNotEmpty()) File(parts[1]) else null
                
                // Verify files still exist
                if (audioFile.exists()) {
                    results.add(Song(
                        file = audioFile,
                        ttmlFile = ttmlFile,
                        title = parts[2],
                        artist = parts[3],
                        album = parts[4],
                        duration = parts[5].toLongOrNull() ?: 0L,
                        year = parts[6],
                        trackNumber = parts[7].toIntOrNull() ?: 0
                    ))
                }
            }
        }
        Log.d("SpicyPlayer", "Loaded ${results.size} songs from disk cache")
        results.sortedBy { it.title.lowercase() }
    } catch (e: Exception) {
        Log.e("SpicyPlayer", "Failed to load cached scan", e)
        null
    }
}

private suspend fun saveScanToCache(context: Context, scanPath: String, results: List<Song>) = withContext(Dispatchers.IO) {
    try {
        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        cacheFile.bufferedWriter().use { writer ->
            writer.write(scanPath)
            writer.newLine()
            results.forEach { song ->
                writer.write(song.file.absolutePath)
                writer.write("|")
                writer.write(song.ttmlFile?.absolutePath ?: "")
                writer.write("|")
                writer.write(song.title)
                writer.write("|")
                writer.write(song.artist)
                writer.write("|")
                writer.write(song.album)
                writer.write("|")
                writer.write(song.duration.toString())
                writer.write("|")
                writer.write(song.year)
                writer.write("|")
                writer.write(song.trackNumber.toString())
                writer.newLine()
            }
        }
        Log.d("SpicyPlayer", "Saved ${results.size} songs to disk cache")
    } catch (e: Exception) {
        Log.e("SpicyPlayer", "Failed to save scan to cache", e)
    }
}

suspend fun performScan(
    context: Context, 
    scanPath: String,
    onProgress: (ScanProgress) -> Unit = {}
): List<Song> {
    val musicDir = File(scanPath)
    if (!musicDir.exists()) return emptyList()
    
    onProgress(ScanProgress(phase = "Scanning files...", isUpdating = true))
    
    val allFiles = mutableListOf<File>()
    musicDir.walkTopDown().forEach { 
        if (it.isFile) {
            allFiles.add(it)
            if (allFiles.size % 50 == 0) {
                onProgress(ScanProgress(phase = "Scanning files...", currentCount = allFiles.size, isUpdating = true))
            }
        }
    }
    
    val audioExtensions = listOf("flac", "mp3", "m4a", "wav", "ogg", "aac")
    val audioFiles = allFiles.filter { it.extension.lowercase() in audioExtensions }
    val ttmlFiles = allFiles.filter { it.name.endsWith(".ttml", ignoreCase = true) }

    onProgress(ScanProgress(phase = "Discovered", currentCount = audioFiles.size, summary = "Discovered ${audioFiles.size} audio files"))
    delay(300)

    val totalAudio = audioFiles.size
    var matchedCount = 0
    
    val retriever = MediaMetadataRetriever()
    val finalResults = audioFiles.mapIndexed { index, audioFile ->
        onProgress(ScanProgress(
            phase = "Processing metadata...", 
            currentCount = index + 1, 
            totalCount = totalAudio, 
            isUpdating = true
        ))

        var title: String = audioFile.nameWithoutExtension
        var artist: String = "Unknown Artist"
        var album: String = "Unknown Album"
        var duration: Long = 0L
        var year: String = ""
        var trackNumber: Int = 0

        try {
            audioFile.inputStream().use { retriever.setDataSource(it.fd) }
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: audioFile.nameWithoutExtension
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: 
                     retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) ?: "Unknown Artist"
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            year = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR) ?: ""
            trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            Log.e("SpicyPlayer", "Error reading metadata for ${audioFile.name}", e)
        }

        val baseName = robustNormalize(audioFile.nameWithoutExtension)
        val fuzzyBase = fuzzyNormalize(audioFile.nameWithoutExtension)
        val audioIsInstrumental = isInstrumental(audioFile.nameWithoutExtension)
        val audioDir = audioFile.parentFile

        // Candidates filtering by instrumental status
        val filteredTtmls = ttmlFiles.filter { 
            isInstrumental(it.nameWithoutExtension) == audioIsInstrumental 
        }

        var ttmlFile: File? = null

        // 1. Exact or fuzzy match in the same directory
        ttmlFile = filteredTtmls.find { 
            it.parentFile == audioDir && (robustNormalize(it.nameWithoutExtension) == baseName || fuzzyNormalize(it.nameWithoutExtension) == fuzzyBase)
        }

        // 2. Exact or fuzzy match anywhere
        if (ttmlFile == null) {
            ttmlFile = filteredTtmls.find { 
                robustNormalize(it.nameWithoutExtension) == baseName || fuzzyNormalize(it.nameWithoutExtension) == fuzzyBase
            }
        }

        // 3. Metadata match
        if (ttmlFile == null && title.isNotBlank()) {
            val fuzzyTitle = fuzzyNormalize(title)
            val fuzzyArtist = fuzzyNormalize(artist)
            
            if (fuzzyTitle.length >= 3) {
                ttmlFile = filteredTtmls.find {
                    val ttmlName = it.nameWithoutExtension
                    val ft = fuzzyNormalize(ttmlName)
                    
                    val exactTitle = ft == fuzzyTitle
                    val titlePartMatches = ft.contains(fuzzyTitle)
                    val artistMatches = fuzzyArtist != "unknown artist" && ft.contains(fuzzyArtist)
                    
                    if (exactTitle) return@find true
                    if (titlePartMatches && artistMatches) return@find true
                    
                    if (it.parentFile == audioDir && titlePartMatches) {
                        val ttmlSegments = it.nameWithoutExtension.split(Regex("[-_\\s\\.\\(\\)\\[\\]]")).map { fuzzyNormalize(it) }
                        if (ttmlSegments.contains(fuzzyTitle)) {
                            if (ft.length <= fuzzyTitle.length * 3) return@find true
                        }
                    }
                    false
                }
            }
        }

        // 4. Loose match but only in the same directory or with high similarity
        if (ttmlFile == null && fuzzyBase.length >= 4) {
            ttmlFile = filteredTtmls.find { ttml ->
                val ft = fuzzyNormalize(ttml.nameWithoutExtension)
                val sameDir = ttml.parentFile == audioDir
                
                if (sameDir) {
                    val ttmlSegments = ttml.nameWithoutExtension.split(Regex("[-_\\s\\.]")).map { fuzzyNormalize(it) }
                    val audioSegments = audioFile.nameWithoutExtension.split(Regex("[-_\\s\\.]")).map { fuzzyNormalize(it) }
                    if (ttmlSegments.any { it == fuzzyBase } || audioSegments.any { it == ft }) return@find true
                }
                
                val verySimilar = (ft.length >= 8 && (ft.startsWith(fuzzyBase) || ft.endsWith(fuzzyBase))) ||
                                  (fuzzyBase.length >= 8 && (fuzzyBase.startsWith(ft) || fuzzyBase.endsWith(ft)))
                
                if (verySimilar) {
                    val originalFt = robustNormalize(ttml.nameWithoutExtension)
                    val originalBase = robustNormalize(audioFile.nameWithoutExtension)
                    
                    if (originalFt.contains(" - $originalBase") || originalFt.contains("$originalBase - ")) return@find true
                    if (originalBase.contains(" - $originalFt") || originalBase.contains("$originalFt - ")) return@find true
                    
                    val diff = Math.abs(ft.length - fuzzyBase.length)
                    if (diff <= 6 && (ft.length <= fuzzyBase.length * 2 || fuzzyBase.length <= ft.length * 2)) {
                        if (ft.startsWith(fuzzyBase) || ft.endsWith(fuzzyBase)) return@find true
                    }
                }
                false
            }
        }

        if (ttmlFile != null) matchedCount++

        Song(
            file = audioFile,
            ttmlFile = ttmlFile,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            year = year,
            trackNumber = trackNumber
        )
    }.sortedBy { it.title.lowercase() }
    
    retriever.release()

    onProgress(ScanProgress(
        phase = "Matching...", 
        summary = "Matched $matchedCount out of $totalAudio"
    ))
    delay(800)
    
    saveScanToCache(context, scanPath, finalResults)
    return finalResults
}
