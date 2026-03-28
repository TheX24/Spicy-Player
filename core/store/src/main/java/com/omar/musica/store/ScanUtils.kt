package com.omar.musica.store

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.omar.musica.store.model.song.Song
import com.omar.musica.model.song.BasicSongMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer

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
                if (audioFile.exists()) {
                    val basicMetadata = BasicSongMetadata(
                        title = parts[2],
                        artistName = parts[3],
                        albumName = parts[4],
                        durationMillis = parts[5].toLongOrNull() ?: 0L,
                        sizeBytes = audioFile.length(),
                        trackNumber = parts[7].toIntOrNull() ?: 0
                    )
                    results.add(Song(
                        uri = Uri.fromFile(audioFile),
                        metadata = basicMetadata,
                        filePath = audioFile.absolutePath,
                        albumId = 0L // We don't have albumId natively here
                    ))
                }
            }
        }
        Log.d("ScanUtils", "Loaded \${results.size} songs from disk cache")
        results.sortedBy { it.metadata.title.lowercase() }
    } catch (e: Exception) {
        Log.e("ScanUtils", "Failed to load cached scan", e)
        null
    }
}

suspend fun saveScanToCache(context: Context, scanPath: String, results: List<Song>) = withContext(Dispatchers.IO) {
    try {
        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        cacheFile.bufferedWriter().use { writer ->
            writer.write(scanPath)
            writer.newLine()
            results.forEach { song ->
                writer.write(song.filePath)
                writer.write("|")
                writer.write("") // ttml file placeholder if we decide to add it later
                writer.write("|")
                writer.write(song.metadata.title)
                writer.write("|")
                writer.write(song.metadata.artistName)
                writer.write("|")
                writer.write(song.metadata.albumName)
                writer.write("|")
                writer.write(song.metadata.durationMillis.toString())
                writer.write("|")
                writer.write("") // Year (empty for now)
                writer.write("|")
                writer.write(song.metadata.trackNumber.toString())
                writer.newLine()
            }
        }
        Log.d("ScanUtils", "Saved \${results.size} songs to disk cache")
    } catch (e: Exception) {
        Log.e("ScanUtils", "Failed to save scan to cache", e)
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

    onProgress(ScanProgress(phase = "Discovered", currentCount = audioFiles.size, summary = "Discovered \${audioFiles.size} audio files"))
    delay(300)

    val totalAudio = audioFiles.size
    
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
        var trackNumber: Int = 0

        try {
            audioFile.inputStream().use { retriever.setDataSource(it.fd) }
            title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: audioFile.nameWithoutExtension
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: 
                     retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST) ?: "Unknown Artist"
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM) ?: "Unknown Album"
            duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            trackNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)?.toIntOrNull() ?: 0
        } catch (e: Exception) {
            Log.e("ScanUtils", "Error reading metadata for \${audioFile.name}", e)
        }

        val basicMetadata = BasicSongMetadata(
            title = title,
            artistName = artist,
            albumName = album,
            durationMillis = duration,
            sizeBytes = audioFile.length(),
            trackNumber = trackNumber % 1000
        )

        Song(
            uri = Uri.fromFile(audioFile),
            metadata = basicMetadata,
            filePath = audioFile.absolutePath,
            albumId = 0L // We don't have this in manual scan, but media repository will resolve it
        )
    }.sortedBy { it.metadata.title.lowercase() }
    
    retriever.release()

    onProgress(ScanProgress(
        phase = "Matching...", 
        summary = "Processed \${finalResults.size} out of \$totalAudio"
    ))
    delay(800)
    
    saveScanToCache(context, scanPath, finalResults)
    return finalResults
}
