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

fun isInstrumental(s: String): Boolean {
    val lower = s.lowercase()
    return lower.contains("instrumental") || lower.contains("karaoke")
}

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

        // 3. Metadata match (if available)
        if (ttmlFile == null) {
            try {
                val retriever = MediaMetadataRetriever()
                audioFile.inputStream().use { retriever.setDataSource(it.fd) }
                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: 
                             retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                retriever.release()
                
                if (!title.isNullOrBlank()) {
                    val fuzzyTitle = fuzzyNormalize(title)
                    val fuzzyArtist = artist?.let { fuzzyNormalize(it) } ?: ""
                    
                    if (fuzzyTitle.length >= 3) {
                        ttmlFile = filteredTtmls.find {
                            val ttmlName = it.nameWithoutExtension
                            val ft = fuzzyNormalize(ttmlName)
                            
                            // High confidence: Title matches exactly or (Title matches part and Artist matches part)
                            val exactTitle = ft == fuzzyTitle
                            val titlePartMatches = ft.contains(fuzzyTitle)
                            val artistMatches = fuzzyArtist.isNotEmpty() && ft.contains(fuzzyArtist)
                            
                            if (exactTitle) return@find true
                            if (titlePartMatches && artistMatches) return@find true
                            
                            // If in same directory, allow Title match alone but only if it matches a full segment
                            // and the title is a significant part of the filename (to avoid "Enough" matching long sentences)
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
            } catch (_: Exception) {}
        }

        // 4. Loose match but only in the same directory or with high similarity
        if (ttmlFile == null && fuzzyBase.length >= 4) {
            ttmlFile = filteredTtmls.find { ttml ->
                val ft = fuzzyNormalize(ttml.nameWithoutExtension)
                val sameDir = ttml.parentFile == audioDir
                
                // Same directory: allow "contains" if it matches a logical segment
                if (sameDir) {
                    val ttmlSegments = ttml.nameWithoutExtension.split(Regex("[-_\\s\\.]")).map { fuzzyNormalize(it) }
                    val audioSegments = audioFile.nameWithoutExtension.split(Regex("[-_\\s\\.]")).map { fuzzyNormalize(it) }
                    if (ttmlSegments.any { it == fuzzyBase } || audioSegments.any { it == ft }) return@find true
                }
                
                // Global matches: must be very similar or structured as Artist - Title
                val verySimilar = (ft.length >= 8 && (ft.startsWith(fuzzyBase) || ft.endsWith(fuzzyBase))) ||
                                  (fuzzyBase.length >= 8 && (fuzzyBase.startsWith(ft) || fuzzyBase.endsWith(ft)))
                
                if (verySimilar) {
                    val originalFt = robustNormalize(ttml.nameWithoutExtension)
                    val originalBase = robustNormalize(audioFile.nameWithoutExtension)
                    
                    // High confidence structure match
                    if (originalFt.contains(" - $originalBase") || originalFt.contains("$originalBase - ")) return@find true
                    if (originalBase.contains(" - $originalFt") || originalBase.contains("$originalFt - ")) return@find true
                    
                    // High similarity fallback for Global: Title is a major part and match is stable
                    val diff = Math.abs(ft.length - fuzzyBase.length)
                    if (diff <= 6 && (ft.length <= fuzzyBase.length * 2 || fuzzyBase.length <= ft.length * 2)) {
                        if (ft.startsWith(fuzzyBase) || ft.endsWith(fuzzyBase)) return@find true
                    }
                }
                false
            }
        }

        // 5. Hardcore latin-only match as last resort (must be in same dir or very similar)
        if (ttmlFile == null) {
            val latinOnly = fuzzyBase.replace(Regex("[^a-zA-Z0-9]"), "")
            if (latinOnly.length >= 5) {
                ttmlFile = filteredTtmls.find {
                    val tl = fuzzyNormalize(it.nameWithoutExtension).replace(Regex("[^a-zA-Z0-9]"), "")
                    tl == latinOnly && tl.isNotEmpty() && (it.parentFile == audioDir || tl.length > 8)
                }
            }
        }

        if (ttmlFile != null) {
            matchedCount++
            Log.d("SpicyPlayer", "Matched: ${audioFile.name} -> ${ttmlFile.name} (${if (ttmlFile.parentFile == audioDir) "Local" else "Global"})")
            audioFile to ttmlFile
        } else {
            if (audioFiles.size < 50) Log.d("SpicyPlayer", "No match found for: ${audioFile.name}")
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
