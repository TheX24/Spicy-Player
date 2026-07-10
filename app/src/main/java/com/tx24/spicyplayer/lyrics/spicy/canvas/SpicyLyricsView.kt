package com.tx24.spicyplayer.lyrics.spicy.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.rememberTextMeasurer
import com.tx24.spicyplayer.lyrics.fadingEdge
import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.animation.LineAnimState
import com.tx24.spicyplayer.lyrics.spicy.animation.LyricsAnimator
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.parser.LetterSynthesizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The main lyrics display component representing the split architecture.
 * Delegates text measurement to LyricsLayoutCalculator,
 * scroll physics to ScrollManager,
 * and drawing to LyricsRenderer extensions.
 *
 * @param lines The list of lyric lines to display.
 * @param currentTimeMs The current playback time in milliseconds.
 * @param onSeekWord Callback triggered when a user taps a line to seek to its start time.
 */
@Composable
fun SpicyLyricsView(
    lines: List<Line>,
    currentTimeMs: Long,
    onSeekWord: (Long) -> Unit,
    modifier: Modifier = Modifier,
    fontSizeScale: Float = 1.0f,
    config: RenderConfig = RenderConfig.FULL,
    lyricsType: LyricsType = LyricsType.Syllable,
    romanize: Boolean = false,
) {
    val textMeasurer = rememberTextMeasurer()
    var lineLayouts by remember { mutableStateOf<List<LineLayout>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // Synthesize per-letter emphasis for held words using the active config (mode-dependent
    // thresholds, romanized display). Syllable mode only; Line/Static never letter-split.
    val displayLines = remember(lines, config, romanize, lyricsType) {
        if (lyricsType == LyricsType.Syllable) LetterSynthesizer.apply(lines, config, romanize) else lines
    }

    val animator = remember { LyricsAnimator(coroutineScope, config) }
    LaunchedEffect(config) { animator.config = config }
    LaunchedEffect(lines) { animator.reset() }
    val isStatic = lyricsType == LyricsType.Static

    val currentTimeMsUpdated by rememberUpdatedState(currentTimeMs)
    val linesUpdated by rememberUpdatedState(displayLines)
    val lineLayoutsUpdated by rememberUpdatedState(lineLayouts)

    val scrollManager = remember { ScrollManager() }

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        // Anchor the active line ~25% from the top (reference: margin-top 25cqh).
        val centerY = canvasHeight * 0.25f
        val horizontalPadding = 40f
        val hasDuet = remember(displayLines) { displayLines.any { it.oppositeAligned } }

        // Recalculate layouts whenever the lyrics, dimensions, or font size change.
        LaunchedEffect(displayLines, canvasWidth, fontSizeScale, romanize) {
            withContext(Dispatchers.Default) {
                lineLayouts = LyricsLayoutCalculator.calculateLineLayouts(displayLines, canvasWidth, textMeasurer, fontSizeScale, romanize)
            }
        }

        if (lineLayouts.isEmpty()) return@BoxWithConstraints

        var animStates by remember { mutableStateOf<List<LineAnimState>>(emptyList()) }
        var dynamicYOffsets by remember { mutableStateOf(FloatArray(0)) }
        var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }
        
        // The high-frequency animation loop.
        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    val currentLayouts = lineLayoutsUpdated
                    val currentLines = linesUpdated
                    val currentTime = currentTimeMsUpdated

                    // Unclamped like the reference: springs integrate analytically over any dt.
                    val deltaTime = if (lastFrameTimeNanos == 0L) {
                        0.016f
                    } else {
                        ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceAtLeast(0f)
                    }
                    lastFrameTimeNanos = frameTimeNanos

                    if (currentLayouts.size == currentLines.size && currentLines.isNotEmpty()) {
                        // 1. Step the animator for visual properties (scale, opacity, glow).
                        animStates = animator.animate(currentLines, currentTime, deltaTime, scrollManager.isUserScrolling, lyricsType)

                        // 1.5 Calculate dynamic Y offsets based on interlude scales.
                        var accumulatedY = 0f
                        val newDynamicYOffsets = FloatArray(currentLayouts.size)
                        
                        for (i in currentLayouts.indices) {
                            val layout = currentLayouts[i]
                            val state = animStates.getOrNull(i)
                            
                            if (layout.isInterlude) {
                                val scale = state?.scale?.coerceIn(0f, 1f) ?: 0f
                                val padding = 64f * scale
                                val expansion = padding * 2f
                                
                                newDynamicYOffsets[i] = layout.yOffset + accumulatedY + padding
                                accumulatedY += expansion
                            } else {
                                newDynamicYOffsets[i] = layout.yOffset + accumulatedY
                            }
                        }
                        dynamicYOffsets = newDynamicYOffsets

                        // 2. Identify all active lines and update the scroll target to center on
                        // them. Done with plain index loops to avoid allocating intermediate
                        // lists on every frame.
                        var minY = Float.MAX_VALUE
                        var maxY = -Float.MAX_VALUE
                        var hasActive = false
                        for (i in currentLayouts.indices) {
                            val layout = currentLayouts[i]
                            if (layout.isBackground || layout.isSongwriter) continue
                            val line = currentLines[i]
                            if (line.startMs <= currentTime && currentTime <= line.endMs) {
                                hasActive = true
                                val top = newDynamicYOffsets[i]
                                val bottom = top + layout.height
                                if (top < minY) minY = top
                                if (bottom > maxY) maxY = bottom
                            }
                        }

                        var targetY: Float? = null
                        if (hasActive) {
                            // Center on the combined Y-range of all active lines.
                            val clusterCenterY = (minY + maxY) / 2f
                            targetY = -clusterCenterY
                        } else {
                            // Fallback: center on the latest line that has already started.
                            var lastStartedIdx = -1
                            for (i in currentLayouts.indices) {
                                val layout = currentLayouts[i]
                                if (layout.isBackground || layout.isSongwriter) continue
                                if (currentLines[i].startMs <= currentTime) lastStartedIdx = i
                            }
                            if (lastStartedIdx < 0) lastStartedIdx = 0

                            if (lastStartedIdx < currentLayouts.size) {
                                val fallbackLayout = currentLayouts[lastStartedIdx]
                                val fallbackDynamicY = newDynamicYOffsets[lastStartedIdx]
                                targetY = -(fallbackDynamicY + fallbackLayout.height / 2f)
                            }
                        }

                        // 3. Step the scroll spring and handle user overrides.
                        val lastLayout = currentLayouts.lastOrNull()
                        val totalContentHeight = (lastLayout?.yOffset ?: 0f) + (lastLayout?.height ?: 0f) + accumulatedY

                        // Static lyrics have no timing to follow: leave scrolling entirely to the user.
                        if (isStatic) targetY = null
                        scrollManager.updateScroll(currentTime, deltaTime, totalContentHeight, targetY)
                    }
                }
            }
        }

        // Top/bottom fade mask (reference: 64px --ImageMask fade on the lyrics content).
        val fadeFraction = if (canvasHeight > 0f) (64f / canvasHeight).coerceIn(0f, 0.45f) else 0f
        val fadeBrush = remember(fadeFraction) {
            Brush.verticalGradient(
                0f to Color.Transparent,
                fadeFraction to Color.Black,
                1f - fadeFraction to Color.Black,
                1f to Color.Transparent,
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .fadingEdge(fadeBrush)
                .pointerInput(Unit) {
                    // Interaction: Dragging.
                    detectDragGestures(
                        onDragStart = { scrollManager.onDragStart() },
                        onDragEnd = { scrollManager.onDragEnd() },
                        onDragCancel = { scrollManager.onDragEnd() },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            scrollManager.onDrag(dragAmount.y)
                        }
                    )
                }
                .pointerInput(isStatic) {
                    // Interaction: Tapping to seek. Static lyrics are not seekable.
                    if (isStatic) return@pointerInput
                    detectTapGestures { tapOffset ->
                        val currentScrollY = scrollManager.animScrollY
                        val adjustedTapY = tapOffset.y - (centerY + currentScrollY)

                        for (i in lineLayouts.indices) {
                            val layout = lineLayouts[i]
                            val layoutDynamicY = dynamicYOffsets.getOrElse(i) { layout.yOffset }
                            if (adjustedTapY >= layoutDynamicY && adjustedTapY <= layoutDynamicY + layout.height) {
                                if (layout.isInterlude || layout.isSongwriter) continue
                                if (layout.line.words.isNotEmpty()) {
                                    onSeekWord(layout.line.startMs)
                                    scrollManager.onSeek()
                                }
                                return@detectTapGestures
                            }
                        }
                    }
                }
        ) {
            val scrollOffset = centerY + scrollManager.animScrollY

            lineLayouts.forEachIndexed { lineIdx, layout ->
                val lineAnim = animStates.getOrNull(lineIdx) ?: return@forEachIndexed
                val dynamicY = dynamicYOffsets.getOrElse(lineIdx) { layout.yOffset }

                // Optimization: Don't draw invisible lines.
                if (lineAnim.opacity <= 0.01f) return@forEachIndexed

                // Optimization: Don't draw lines off-screen.
                val lineScreenY = scrollOffset + dynamicY
                if (lineScreenY < -layout.height * 3 || lineScreenY > canvasHeight + layout.height * 3) {
                    return@forEachIndexed
                }

                val lineStartX = getLineStartX(layout, size.width, horizontalPadding, hasDuet)

                when {
                    layout.isInterlude -> drawInterludeGroup(layout, lineAnim, lineStartX, scrollOffset, dynamicY)
                    lyricsType == LyricsType.Static -> drawStaticLine(layout, lineAnim, lineStartX, scrollOffset, dynamicY)
                    lyricsType == LyricsType.Line -> drawLineModeLine(layout, lineAnim, lineStartX, scrollOffset, dynamicY, config)
                    else -> drawStandardLine(layout, lineAnim, lineStartX, scrollOffset, dynamicY, config)
                }
            }
        }
    }
}

/**
 * Calculates the horizontal starting position of a line based on its alignment and the presence of duets.
 */
private fun getLineStartX(
    layout: LineLayout,
    canvasWidth: Float,
    horizontalPadding: Float,
    hasDuet: Boolean,
): Float {
    if (layout.isSongwriter) {
        return horizontalPadding
    }
    return if (layout.oppositeAligned || layout.isRtl) {
        canvasWidth - horizontalPadding - layout.totalWidth
    } else {
        horizontalPadding
    }
}
