package com.tx24.spicyplayer.util

import android.content.Context
import android.media.MediaMetadataRetriever
import android.util.Log
import java.io.File
import java.text.Normalizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

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

private const val CACHE_FILE_NAME = "library_cache.txt"

suspend fun loadCachedScan(context: Context, scanPath: String): List<Pair<File, File?>>? = withContext(Dispatchers.IO) {
    try {
        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        if (!cacheFile.exists()) return@withContext null

        val lines = cacheFile.readLines()
        if (lines.isEmpty()) return@withContext null

        val cachedScanPath = lines[0]
        if (cachedScanPath != scanPath) return@withContext null

        val results = mutableListOf<Pair<File, File?>>()
        for (i in 1 until lines.size) {
            val line = lines[i]
            val parts = line.split("|")
            if (parts.size >= 2) {
                val audioFile = File(parts[0])
                val ttmlFile = if (parts[1].isNotEmpty()) File(parts[1]) else null
                
                // Verify files still exist
                if (audioFile.exists()) {
                    results.add(audioFile to ttmlFile)
                }
            }
        }
        Log.d("SpicyPlayer", "Loaded ${results.size} songs from disk cache")
        results.sortedBy { it.first.name.lowercase() }
    } catch (e: Exception) {
        Log.e("SpicyPlayer", "Failed to load cached scan", e)
        null
    }
}

private suspend fun saveScanToCache(context: Context, scanPath: String, results: List<Pair<File, File?>>) = withContext(Dispatchers.IO) {
    try {
        val cacheFile = File(context.cacheDir, CACHE_FILE_NAME)
        cacheFile.bufferedWriter().use { writer ->
            writer.write(scanPath)
            writer.newLine()
            results.forEach { (audio, ttml) ->
                writer.write(audio.absolutePath)
                writer.write("|")
                writer.write(ttml?.absolutePath ?: "")
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
): List<Pair<File, File?>> {
    val musicDir = File(scanPath)
    if (!musicDir.exists()) return emptyList()
    
    onProgress(ScanProgress(phase = "Scanning songs...", isUpdating = true))
    
    val allFiles = mutableListOf<File>()
    musicDir.walkTopDown().forEach { 
        if (it.isFile) {
            allFiles.add(it)
            if (allFiles.size % 20 == 0) {
                onProgress(ScanProgress(phase = "Scanning songs...", currentCount = allFiles.size, isUpdating = true))
            }
        }
    }
    
    val audioExtensions = listOf("flac", "mp3", "m4a", "wav", "ogg", "aac")
    val audioFiles = allFiles.filter { it.extension.lowercase() in audioExtensions }
    val ttmlFiles = allFiles.filter { it.name.endsWith(".ttml", ignoreCase = true) }

    onProgress(ScanProgress(phase = "Discovered", currentCount = audioFiles.size, summary = "Discovered ${audioFiles.size} audio files"))
    delay(300)
    onProgress(ScanProgress(phase = "Scanning TTML's...", currentCount = ttmlFiles.size, isUpdating = true))
    delay(300)

    val totalAudio = audioFiles.size
    var matchedCount = 0
    
    val finalResults = audioFiles.mapIndexed { index, audioFile ->
        onProgress(ScanProgress(
            phase = "Matching TTML's with songs...", 
            currentCount = index + 1, 
            totalCount = totalAudio, 
            isUpdating = true
        ))

        val baseName = robustNormalize(audioFile.nameWithoutExtension)
        val fuzzyBase = fuzzyNormalize(audioFile.nameWithoutExtension)

        var ttmlFile = allFiles.find {
            it.name.endsWith(".ttml", ignoreCase = true) &&
                robustNormalize(it.nameWithoutExtension) == baseName
        } ?: allFiles.find {
            it.name.endsWith(".ttml", ignoreCase = true) &&
                fuzzyNormalize(it.nameWithoutExtension) == fuzzyBase
        }

        if (ttmlFile == null && fuzzyBase.length >= 3) {
            ttmlFile = ttmlFiles.find {
                fuzzyNormalize(it.nameWithoutExtension).contains(fuzzyBase) ||
                    fuzzyBase.contains(fuzzyNormalize(it.nameWithoutExtension))
            }
        }

        if (ttmlFile == null) {
            try {
                val retriever = MediaMetadataRetriever()
                audioFile.inputStream().use { retriever.setDataSource(it.fd) }
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                retriever.release()
                if (!title.isNullOrBlank()) {
                    val fuzzyTitle = fuzzyNormalize(title)
                    if (fuzzyTitle.length >= 3) {
                        ttmlFile = ttmlFiles.find {
                            val ft = fuzzyNormalize(it.nameWithoutExtension)
                            ft == fuzzyTitle || ft.contains(fuzzyTitle) || fuzzyTitle.contains(ft)
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        if (ttmlFile == null) {
            val latinOnly = fuzzyBase.replace(Regex("[^a-zA-Z0-9]"), "")
            if (latinOnly.length >= 5) {
                ttmlFile = ttmlFiles.find {
                    val tl = fuzzyNormalize(it.nameWithoutExtension).replace(Regex("[^a-zA-Z0-9]"), "")
                    tl == latinOnly && tl.isNotEmpty()
                }
            }
        }

        if (ttmlFile != null) {
            matchedCount++
            Log.d("SpicyPlayer", "Paired: ${audioFile.name} -> ${ttmlFile.name}")
            audioFile to ttmlFile
        } else {
            Log.d("SpicyPlayer", "No lyrics for: ${audioFile.name}")
            audioFile to null
        }
    }.sortedBy { it.first.name.lowercase() }
    
    onProgress(ScanProgress(
        phase = "Matching...", 
        summary = "Matched $matchedCount out of $totalAudio"
    ))
    delay(800)
    
    saveScanToCache(context, scanPath, finalResults)
    return finalResults
}
