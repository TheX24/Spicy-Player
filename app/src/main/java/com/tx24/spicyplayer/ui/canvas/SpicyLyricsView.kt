package com.tx24.spicyplayer.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.rememberTextMeasurer
import com.tx24.spicyplayer.animation.LineAnimState
import com.tx24.spicyplayer.animation.LyricsAnimator
import com.tx24.spicyplayer.models.Line
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

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
) {
    val textMeasurer = rememberTextMeasurer()
    var lineLayouts by remember { mutableStateOf<List<LineLayout>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    val animator = remember { LyricsAnimator(coroutineScope) }
    LaunchedEffect(lines) { animator.reset() }

    val currentTimeMsUpdated by rememberUpdatedState(currentTimeMs)
    val linesUpdated by rememberUpdatedState(lines)
    val lineLayoutsUpdated by rememberUpdatedState(lineLayouts)

    val scrollManager = remember { ScrollManager() }

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        val centerY = canvasHeight / 2f
        val horizontalPadding = 40f
        val hasDuet = remember(lines) { lines.any { it.oppositeAligned } }

        // Recalculate layouts whenever the lyrics or dimensions change.
        LaunchedEffect(lines, canvasWidth) {
            withContext(Dispatchers.Default) {
                lineLayouts = LyricsLayoutCalculator.calculateLineLayouts(lines, canvasWidth, textMeasurer)
            }
        }

        if (lineLayouts.isEmpty()) return@BoxWithConstraints

        var animStates by remember { mutableStateOf<List<LineAnimState>>(emptyList()) }
        var dynamicYOffsets by remember { mutableStateOf<List<Float>>(emptyList()) }

        // The high-frequency animation loop.
        LaunchedEffect(Unit) {
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    val currentLayouts = lineLayoutsUpdated
                    val currentLines = linesUpdated
                    val currentTime = currentTimeMsUpdated

                    if (currentLayouts.isNotEmpty() && currentLines.isNotEmpty()) {
                        // 1. Step the animator for visual properties (scale, opacity, glow).
                        animStates = animator.animate(currentLines, currentTime, frameTimeNanos)

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
                        dynamicYOffsets = newDynamicYOffsets.toList()

                        // 2. Identify the active line and update the scroll target.
                        val activeIdx = currentLayouts.indices
                            .filter { !currentLayouts[it].isBackground }
                            .lastOrNull { currentLayouts[it].line.startMs <= currentTime }
                            ?.coerceAtLeast(0) ?: 0

                        var targetY: Float? = null
                        if (activeIdx < currentLayouts.size) {
                            val activeLayout = currentLayouts[activeIdx]
                            val activeDynamicY = newDynamicYOffsets[activeIdx]
                            targetY = -(activeDynamicY + activeLayout.height / 2f)
                        }

                        // 3. Step the scroll spring and handle user overrides.
                        val dt = 0.016f
                        val totalContentHeight = (currentLayouts.lastOrNull()?.yOffset ?: 0f) + accumulatedY
                        
                        scrollManager.updateScroll(currentTime, dt, totalContentHeight, targetY)
                    }
                }
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
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
                .pointerInput(Unit) {
                    // Interaction: Tapping to seek.
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

                if (layout.isInterlude) {
                    drawInterludeGroup(layout, lineAnim, lineStartX, scrollOffset, dynamicY)
                } else {
                    drawStandardLine(layout, lineAnim, lineStartX, scrollOffset, dynamicY)
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
    return if (!hasDuet || layout.oppositeAligned) {
        horizontalPadding
    } else {
        canvasWidth - horizontalPadding - layout.totalWidth
    }
}
