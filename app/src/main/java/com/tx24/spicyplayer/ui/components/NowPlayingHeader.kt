package com.tx24.spicyplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.R
import com.tx24.spicyplayer.models.NowPlayingData

/**
 * Animated header displaying track title, artist, and album art.
 */
@Composable
fun NowPlayingHeader(
    nowPlayingData: NowPlayingData,
    hasLyrics: Boolean,
    headerProgress: Float,
    currentImageSize: Dp,
    currentSpacerWidth: Dp,
    currentHeaderBias: Float,
    metadataAlpha: Float
) {
    val targetProgress = if (hasLyrics) 0f else 1f
    val expansionProgressState = animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(800, easing = LinearOutSlowInEasing),
        label = "ExpansionProgress"
    )
    val expansionProgress = expansionProgressState.value

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
    ) {
        val maxImageSize = maxWidth
        val imageSize = androidx.compose.ui.unit.lerp(currentImageSize, maxImageSize, expansionProgress)
        val horizontalBias = -1f + expansionProgress // Lerp from -1f to 0f
        
        // Vertical alignment is centered (0f) in Small mode to align text to image middle,
        // and Top-aligned (-1f) in Large mode to keep the "below" offset math simple.
        val verticalBias by animateFloatAsState(
            targetValue = if (hasLyrics) 0f else -1f,
            animationSpec = tween(800, easing = LinearOutSlowInEasing)
        )

        val data = nowPlayingData

        // 1. Track Info Column (Orbiting) - Drawn FIRST (Under)
        val offsetX = androidx.compose.ui.unit.lerp(currentImageSize + 16.dp, 0.dp, expansionProgress)
        val offsetY = androidx.compose.ui.unit.lerp(0.dp, imageSize + 12.dp, expansionProgress)

        Column(
            modifier = Modifier
                .align(BiasAlignment(horizontalBias, verticalBias))
                .offset(x = offsetX, y = offsetY)
                .graphicsLayer {
                    // Alpha still follows metadata visibility
                    alpha = if (headerProgress > 0.01f) metadataAlpha else 1f
                },
            horizontalAlignment = if (expansionProgress > 0.5f) Alignment.CenterHorizontally else Alignment.Start
        ) {
            if (headerProgress > 0.01f || !hasLyrics) {
                @OptIn(ExperimentalFoundationApi::class)
                Text(
                    text = data.trackName.ifBlank { "Spicy Player" },
                    color = Color.White,
                    style = if (expansionProgress > 0.5f) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 2000
                    )
                )
                @OptIn(ExperimentalFoundationApi::class)
                Text(
                    text = if (data.index == -1) "Minimalist Music Player" else data.artistName,
                    color = Color.White.copy(alpha = 0.7f),
                    style = if (expansionProgress > 0.5f) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 2000
                    )
                )
            } else {
                Text(
                    text = "Spicy Player",
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall
                )
            }
        }

        // 2. Cover Art - Drawn SECOND (Over)
        Box(
            modifier = Modifier
                .size(imageSize)
                .align(BiasAlignment(horizontalBias, verticalBias))
                .clip(RoundedCornerShape(12.dp))
        ) {
            if (data.index == -1) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "App Logo",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                if (data.coverArt != null) {
                    Image(
                        bitmap = data.coverArt.asImageBitmap(),
                        contentDescription = "Album Art",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Gray)
                    )
                }
            }
        }
    }
}
