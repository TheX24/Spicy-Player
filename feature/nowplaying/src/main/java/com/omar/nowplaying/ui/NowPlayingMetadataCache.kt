package com.omar.nowplaying.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.collection.LruCache
import com.omar.musica.store.model.song.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class NowPlayingMetadata(
    val title: String,
    val artist: String,
    val art: Bitmap?,
    val bitrate: String,
    val format: String
)

object NowPlayingMetadataCache {
    private const val TAG = "NowPlayingMetadataCache"
    
    val cache = LruCache<String, NowPlayingMetadata>(20) // Cache 20 songs metadata

    suspend fun getMetadata(song: Song): NowPlayingMetadata = withContext(Dispatchers.IO) {
        val cached = cache.get(song.filePath)
        if (cached != null) {
            return@withContext cached
        }

        try {
            val audioFile = File(song.filePath)
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(audioFile.absolutePath)
                val t = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    ?: audioFile.nameWithoutExtension
                val a = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    ?: "Unknown Artist"
                val br = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
                    ?.let { (it.toInt() / 1000).toString() + " kbps" } ?: ""
                val ext = audioFile.extension.uppercase()
                
                val artBytes = retriever.embeddedPicture
                val bmp = artBytes?.let {
                    val raw = BitmapFactory.decodeByteArray(it, 0, it.size)
                    if (raw != null && (raw.width > 1024 || raw.height > 1024)) {
                        Bitmap.createScaledBitmap(raw, 1024, 1024, true)
                    } else {
                        raw
                    }
                }
                
                val metadata = NowPlayingMetadata(t, a, bmp, br, ext)
                cache.put(song.filePath, metadata)
                return@withContext metadata
            } finally {
                retriever.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract metadata for \${song.filePath}", e)
            val format = File(song.filePath).extension.uppercase()
            val fallback = NowPlayingMetadata(
                title = song.metadata.title,
                artist = song.metadata.artistName ?: "Unknown",
                art = null,
                bitrate = "",
                format = format
            )
            cache.put(song.filePath, fallback)
            return@withContext fallback
        }
    }
}
