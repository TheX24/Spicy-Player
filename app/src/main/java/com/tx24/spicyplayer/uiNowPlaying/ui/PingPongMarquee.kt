package com.tx24.spicyplayer.uiNowPlaying.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * Scrolls overflowing content to its end and back, pausing at both ends, instead of
 * looping/wrapping. Matches Spicy Lyrics' SongMoreInfo marquee (`Marquee_*_SongMoreInfo`
 * keyframes in spicy-lyrics/src/app.tsx: infinite alternate with 10%/90% holds), eased
 * rather than linear, plus its edge mask (SongMoreInfo.css `mask-image`).
 * No-op (centered, unscrolled) when content already fits the available width.
 */
@Composable
fun PingPongMarquee(
    modifier: Modifier = Modifier,
    durationMillis: Int = 16_000,
    edgeFadeWidth: Dp = 16.dp,
    content: @Composable () -> Unit,
) {
    var overflowPx by remember { mutableIntStateOf(0) }

    val progress by rememberInfiniteTransition(label = "PingPongMarquee").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                this.durationMillis = durationMillis
                0f at 0
                0f at (durationMillis * 0.1f).roundToInt()
                1f at (durationMillis * 0.9f).roundToInt() using FastOutSlowInEasing
                1f at durationMillis
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "PingPongMarqueeProgress"
    )

    SubcomposeLayout(
        modifier = modifier
            .clipToBounds()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                val edgePx = edgeFadeWidth.toPx().coerceAtMost(size.width / 2f)
                if (edgePx > 0f && overflowPx > 0) {
                    val fraction = edgePx / size.width
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            fraction to Color.Black,
                            1f - fraction to Color.Black,
                            1f to Color.Transparent
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    ) { constraints ->
        val placeable = subcompose(Unit, content).first()
            .measure(constraints.copy(minWidth = 0, maxWidth = Constraints.Infinity))
        val containerWidth = if (constraints.hasBoundedWidth) {
            constraints.constrainWidth(minOf(placeable.width, constraints.maxWidth))
        } else {
            placeable.width
        }
        val overflow = (placeable.width - containerWidth).coerceAtLeast(0)
        if (overflow != overflowPx) overflowPx = overflow

        layout(containerWidth, placeable.height) {
            val x = if (overflowPx > 0) {
                -(overflowPx * progress).roundToInt()
            } else {
                (containerWidth - placeable.width) / 2
            }
            placeable.placeRelative(x, 0)
        }
    }
}
