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
import androidx.compose.ui.platform.LocalDensity
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
    documentId: String,
    currentTimeMs: () -> Long,
    onSeekWord: (Long) -> Unit,
    modifier: Modifier = Modifier,
    fontSizeScale: Float = 1.0f,
    config: RenderConfig = RenderConfig.FULL,
    lyricsType: LyricsType = LyricsType.Syllable,
    romanize: Boolean = false,
    focusAnchorFraction: Float = 0.25f,
    // Invoked with the raw frame-nanos at the top of this view's own animation frame, before
    // currentTimeMs() is read. Lets a caller (e.g. the playback clock smoothing in
    // SpicyLyricsPlayer) piggyback on this view's single withFrameNanos loop instead of running
    // a second, independent one — halving the Choreographer callbacks registered per lyrics
    // screen. Optional so other/future callers aren't forced to supply one.
    onFrameTick: ((Long) -> Unit)? = null,
) {
    val textMeasurer = rememberTextMeasurer()
    var lineLayouts by remember(documentId) { mutableStateOf<List<LineLayout>>(emptyList()) }
    val layoutGeneration = remember(documentId) { LayoutGenerationGate() }
    val coroutineScope = rememberCoroutineScope()

    // Synthesize per-letter emphasis for held words using the active config (mode-dependent
    // thresholds, romanized display). Syllable mode only; Line/Static never letter-split.
    val displayLines = remember(lines, config, romanize, lyricsType) {
        if (lyricsType == LyricsType.Syllable) LetterSynthesizer.apply(lines, config, romanize) else lines
    }

    val animator = remember(documentId) { LyricsAnimator(coroutineScope, config) }
    LaunchedEffect(config) { animator.config = config }
    LaunchedEffect(documentId) { animator.reset() }
    val isStatic = lyricsType == LyricsType.Static

    // Keep the latest time provider without recomposing on every position tick: the frame loop
    // invokes it off-composition (inside withFrameNanos), so the changing clock never re-runs this
    // composable's body — only the Canvas redraws when the derived anim state actually changes.
    val currentTimeProvider by rememberUpdatedState(currentTimeMs)
    val linesUpdated by rememberUpdatedState(displayLines)
    val lineLayoutsUpdated by rememberUpdatedState(lineLayouts)
    val onFrameTickUpdated by rememberUpdatedState(onFrameTick)

    val scrollManager = remember(documentId) { ScrollManager().also { it.reset() } }
    val scrollPolicy = remember(documentId) { ScrollPolicyController() }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()
        // Reference anchor: viewport center minus 30 logical dp.
        val centerY = ScrollPolicyController.anchorY(canvasHeight, density.density)
        // Recalculate layouts whenever the lyrics, dimensions, or font size change.
        LaunchedEffect(displayLines, canvasWidth, fontSizeScale, romanize, documentId) {
            val generation = layoutGeneration.next()
            val measured = withContext(Dispatchers.Default) {
                LyricsLayoutCalculator.calculateLineLayouts(
                    displayLines, canvasWidth, textMeasurer, density.density, lyricsType, fontSizeScale, romanize,
                )
            }
            if (layoutGeneration.isCurrent(generation)) {
                lineLayouts = measured
            }
        }

        if (lineLayouts.isEmpty()) return@BoxWithConstraints

        var animStates by remember { mutableStateOf<List<LineAnimState>>(emptyList()) }
        var dynamicYOffsets by remember { mutableStateOf(FloatArray(0)) }
        var lastFrameTimeNanos by remember { mutableLongStateOf(0L) }
        
        // The high-frequency animation loop.
        LaunchedEffect(Unit) {
            // Reused per-frame scratch for the dynamic Y offsets: filled every frame but only
            // published to state when its contents actually change, so a paused/idle screen stops
            // invalidating the Canvas (a fresh FloatArray each frame was forcing a redraw via array
            // identity-equality even when nothing moved).
            var dynamicYScratch = FloatArray(0)
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    onFrameTickUpdated?.invoke(frameTimeNanos)

                    val currentLayouts = lineLayoutsUpdated
                    val currentLines = linesUpdated
                    val currentTime = currentTimeProvider()

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
                        if (dynamicYScratch.size != currentLayouts.size) {
                            dynamicYScratch = FloatArray(currentLayouts.size)
                        }
                        val newDynamicYOffsets = dynamicYScratch

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
                        // Publish only when the offsets actually changed (i.e. an interlude is
                        // expanding/collapsing); otherwise the Canvas keeps the last array and
                        // isn't invalidated. copyOf() so the published snapshot is immutable while
                        // the scratch keeps mutating next frame.
                        if (!newDynamicYOffsets.contentEquals(dynamicYOffsets)) {
                            dynamicYOffsets = newDynamicYOffsets.copyOf()
                        }

                        // 2. Resolve the reference lead/background overlap policy, then anchor
                        // that one line at viewport center minus 30dp.
                        val decision = scrollPolicy.decide(currentLines, currentTime)
                        val targetIndex = decision.targetIndex
                        var targetY: Float? = targetIndex?.let { index ->
                            -(newDynamicYOffsets[index] + currentLayouts[index].height / 2f)
                        }
                        val targetVisiblePx = targetIndex?.let { index ->
                            val top = centerY + scrollManager.animScrollY + newDynamicYOffsets[index]
                            val bottom = top + currentLayouts[index].height
                            (minOf(bottom, canvasHeight) - maxOf(top, 0f)).coerceAtLeast(0f)
                        } ?: Float.POSITIVE_INFINITY

                        // 3. Step the scroll spring and handle user overrides.
                        val lastLayout = currentLayouts.lastOrNull()
                        val totalContentHeight = (lastLayout?.yOffset ?: 0f) + (lastLayout?.height ?: 0f) + accumulatedY

                        // Static lyrics have no timing to follow: leave scrolling entirely to the user.
                        if (isStatic) targetY = null
                        scrollManager.updateScroll(
                            currentTime, deltaTime, totalContentHeight, targetY,
                            snap = decision.motion == ScrollMotion.SNAP,
                            targetVisiblePx = targetVisiblePx,
                        )
                    }
                }
            }
        }

        // The sole renderer mask: transparent through 16dp, ramping to opaque at 64dp,
        // with a symmetric bottom edge.
        val maskStops = lyricsMaskStops(canvasHeight, density.density)
        val fadeBrush = remember(maskStops) {
            Brush.verticalGradient(
                0f to Color.Transparent,
                maskStops.outerTop to Color.Transparent,
                maskStops.innerTop to Color.Black,
                maskStops.innerBottom to Color.Black,
                maskStops.outerBottom to Color.Transparent,
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

                val lineStartX = getLineStartX(layout)

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
): Float {
    return if (layout.isRightAligned) {
        layout.contentStartX + layout.contentWidth - layout.totalWidth
    } else {
        layout.contentStartX
    }
}
