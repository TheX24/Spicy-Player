package com.tx24.spicyplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    headerProgress: Float,
    currentImageSize: Dp,
    currentSpacerWidth: Dp,
    currentHeaderBias: Float,
    metadataAlpha: Float
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        AnimatedContent(
            targetState = nowPlayingData,
            modifier = Modifier.align(BiasAlignment(currentHeaderBias, 0f)),
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) togetherWith
                    fadeOut(animationSpec = tween(500)) using
                    SizeTransform { _, _ -> tween(500, easing = LinearOutSlowInEasing) }
            },
            label = "HeaderTransition"
        ) { data ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(modifier = Modifier.size(currentImageSize)) {
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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray, RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }

                if (headerProgress > 0.01f) {
                    Spacer(modifier = Modifier.width(currentSpacerWidth))
                    Column(
                        modifier = Modifier.graphicsLayer {
                            alpha = metadataAlpha
                            translationX = (20f * (1f - metadataAlpha))
                        }
                    ) {
                        @OptIn(ExperimentalFoundationApi::class)
                        Text(
                            text = data.trackName,
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
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
                            text = data.artistName,
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Visible,
                            softWrap = false,
                            modifier = Modifier.basicMarquee(
                                iterations = Int.MAX_VALUE,
                                repeatDelayMillis = 2000
                            )
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Spicy Player",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        }
    }
}
