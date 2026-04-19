package com.tx24.spicyplayer.ui.canvas

import android.graphics.Bitmap
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A full-screen composable that renders the spicy-lyrics style dynamic background.
 * Place this behind all other content using a Box/Stack layout.
 *
 * @param coverArtBitmap The album cover art to derive the background from.
 * @param blurIntensity Background blur intensity 0–100 (default 60).
 */
@Composable
fun DynamicBackgroundView(
    coverArtBitmap: Bitmap?,
    modifier: Modifier = Modifier,
    blurIntensity: Int = 60,
) {
    val bgRenderer = remember { DynamicBackgroundRenderer() }
    
    // Use a state to trigger a redraw once setImage is done
    var textureUpdateTrigger by remember { mutableIntStateOf(0) }

    var rotationAngle by remember { mutableFloatStateOf(0f) }

    val transitionProgress = remember { Animatable(1.0f) }
    
    // Prepare the blurred texture when the cover art changes
    LaunchedEffect(coverArtBitmap, blurIntensity) {
        coverArtBitmap?.let { bmp ->
            withContext(Dispatchers.Default) {
                bgRenderer.setImage(bmp, bmp.generationId.toString() + "_blur$blurIntensity", blurIntensity)
                // Reset and animate transition
                transitionProgress.snapTo(0.0f)
                transitionProgress.animateTo(
                    targetValue = 1.0f,
                    animationSpec = tween(1500, easing = LinearEasing)
                )
                textureUpdateTrigger++
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { bgRenderer.release() }
    }

    // Continuous animation loop for smooth, non-periodic rotation
    LaunchedEffect(Unit) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                // Use a constant speed of 0.25 rad/s (approx 16ms per frame = 0.004 rad)
                rotationAngle += 0.016f * DynamicBackgroundRenderer.ROTATION_SPEED
            }
        }
    }

    // The redraw is triggered by both rotationAngle and textureUpdateTrigger
    Canvas(modifier = modifier.fillMaxSize()) {
        val trigger = textureUpdateTrigger // Observe trigger
        val progress = transitionProgress.value // Observe progress
        if (bgRenderer.hasTexture()) {
            drawIntoCanvas { canvas ->
                bgRenderer.draw(canvas.nativeCanvas, size.width, size.height, rotationAngle, progress)
            }
        }
    }
}

