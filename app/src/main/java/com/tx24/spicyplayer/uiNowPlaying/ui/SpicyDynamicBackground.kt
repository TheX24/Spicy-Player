package com.tx24.spicyplayer.uiNowPlaying.ui

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.tx24.spicyplayer.library.store.model.song.Song
import com.tx24.spicyplayer.ui.albumart.LocalInefficientThumbnailImageLoader
import com.tx24.spicyplayer.ui.albumart.toSongAlbumArtModel
import com.tx24.spicyplayer.uiNowPlaying.spicy.canvas.DynamicBackgroundView

import com.tx24.spicyplayer.ui.common.LocalUserPreferences

@Composable
fun SpicyDynamicBackground(
    modifier: Modifier,
    song: Song?,
) {
    val context = LocalContext.current
    val blurIntensity = LocalUserPreferences.current.uiSettings.backgroundBlur
    val songModel = remember(song?.uri) { song?.toSongAlbumArtModel() }
    
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    val imageLoader = LocalInefficientThumbnailImageLoader.current
    
    LaunchedEffect(songModel?.uri) {
        if (songModel == null) {
            bitmap = null
            return@LaunchedEffect
        }
        
        // Load the bitmap. Using a smaller size (256x256) is sufficient for a blurred background 
        // and significantly better for performance.
        val request = ImageRequest.Builder(context)
            .data(songModel)
            .size(Size(256, 256))
            .allowHardware(false) // Essential: Renderer needs to access pixels via Canvas
            .build()
            
        val result = imageLoader.execute(request)
        if (result is SuccessResult) {
            val drawable = result.drawable
            if (drawable is BitmapDrawable) {
                bitmap = drawable.bitmap
            }
        }
    }

    DynamicBackgroundView(
        coverArtBitmap = bitmap,
        modifier = modifier,
        blurIntensity = blurIntensity
    )
}
