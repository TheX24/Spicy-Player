package com.tx24.spicyplayer.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SpringSpec
import com.tx24.spicyplayer.models.Letter
import com.tx24.spicyplayer.models.Line
import com.tx24.spicyplayer.models.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.abs

/**
 * Core animation state machine ported from spicy-lyrics LyricsAnimator.ts.
 *
 * Manages per-word spring-driven animations for Scale, YOffset, and Glow
 * using cubic spline target curves, along with per-line Opacity springs.
 *
 * @param coroutineScope The scope used to launch animation coroutines.
 */
class LyricsAnimator(private val coroutineScope: CoroutineScope) {

    // ── Spline curves ──────────
    // These curves define the target values for different animation properties
    // based on the progress (0.0 to 1.0) of a word or dot being "sung".

    /** Target scale curve for words. */
    private val scaleSpline = CubicSplineInterpolator(listOf(
        AnimationPoint(0f, 0.95f),
        AnimationPoint(0.7f, 1.025f),
        AnimationPoint(1f, 1.0f),
    ))

    /** Target vertical offset curve for words. */
    private val yOffsetSpline = CubicSplineInterpolator(listOf(
        AnimationPoint(0f, 0.01f),
        AnimationPoint(0.9f, -0.0167f),
        AnimationPoint(1f, 0f),
    ))

    /** Target glow (brightness/alpha) curve for words. */
    private val glowSpline = CubicSplineInterpolator(listOf(
        AnimationPoint(0f, 0f),
        AnimationPoint(0.15f, 1f),
        AnimationPoint(0.6f, 1f),
        AnimationPoint(1f, 0f),
    ))

    /** Target scale curve for interlude dots. */
    private val dotScaleSpline = CubicSplineInterpolator(listOf(
        AnimationPoint(0f, 0.75f),
        AnimationPoint(0.7f, 1.05f),
        AnimationPoint(1f, 1.0f),
    ))

    /** Target opacity curve for interlude dots. */
    private val dotOpacitySpline = CubicSplineInterpolator(listOf(
        AnimationPoint(0f, 0.35f),
        AnimationPoint(0.6f, 1f),
        AnimationPoint(1f, 1f),
    ))

    /** Target vertical offset curve for interlude dots (bouncing effect). */
    private val dotYOffsetSpline = CubicSplineInterpolator(listOf(
        AnimationPoint(0f, 0f),
        AnimationPoint(0.9f, -0.12f),
        AnimationPoint(1f, 0f),
    ))

    companion object {
        // Spring frequency and damping coefficients for different properties.
        const val SCALE_FREQUENCY = 0.7f
        const val SCALE_DAMPING = 0.6f
        const val Y_OFFSET_FREQUENCY = 1.25f
        const val Y_OFFSET_DAMPING = 0.4f
        const val GLOW_FREQUENCY = 1.0f
        const val GLOW_DAMPING = 0.5f

        // Opacity levels for different line states.
        const val OPACITY_NOT_SUNG = 0.51f
        const val OPACITY_ACTIVE = 1.0f
        const val OPACITY_SUNG = 0.497f

        // Blur settings for inactive lines.
        const val BLUR_MULTIPLIER = 1.25f
        const val MAX_BLUR = BLUR_MULTIPLIER * 5f + BLUR_MULTIPLIER * 0.465f

        // Initial scale for inactive lines.
        const val IDLE_LINE_SCALE = 0.95f

        // Gradient alpha levels.
        const val GRADIENT_ALPHA_BRIGHT = 0.85f
        const val GRADIENT_ALPHA_DIM = 0.35f

        /** Multiplier for letter glow opacity, ported from spicy-lyrics (185%). */
        const val LETTER_GLOW_MULTIPLIER_OPACITY = 1.85f
        /** Default glow level for already sung letters. */
        const val SUNG_LETTER_GLOW = 0.2f
    }



    // Internal maps to track the state of ongoing animations.
    private val wordSpringsMap = mutableMapOf<Long, WordSprings>()
    private val lineOpacityAnims = mutableMapOf<Int, Animatable<Float, AnimationVector1D>>()
    private val opacitySpringSpec = SpringSpec<Float>(dampingRatio = 1f, stiffness = 100f)
    
    private val lineScaleAnims = mutableMapOf<Int, Animatable<Float, AnimationVector1D>>()
    private val scaleSpringSpec = SpringSpec<Float>(dampingRatio = 0.7f, stiffness = 200f)


    private val dotSpringsMap = mutableMapOf<Long, DotSprings>()
    private val letterSpringsMap = mutableMapOf<Long, LetterSprings>()

    private var lastFrameTimeNanos = 0L

    /**
     * Resets the animator state, clearing all cached spring values.
     */
    fun reset() {
        wordSpringsMap.clear()
        lineOpacityAnims.clear()
        lineScaleAnims.clear()
        dotSpringsMap.clear()
        letterSpringsMap.clear()
        lastFrameTimeNanos = 0L
    }

    /**
     * Computes the animation state for all lines based on the current time.
     *
     * @param lines The list of lyric lines to animate.
     * @param currentTimeMs The current playback time in milliseconds.
     * @param frameTimeNanos The current frame timestamp in nanoseconds.
     * @return A list of [LineAnimState] objects representing the calculated visual state.
     */
    fun animate(
        lines: List<Line>,
        currentTimeMs: Long,
        frameTimeNanos: Long,
    ): List<LineAnimState> {
        // Calculate the time elapsed since the last frame.
        val deltaTime = if (lastFrameTimeNanos == 0L) {
            0.016f // Assume 60fps for the first frame.
        } else {
            ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceIn(0f, 0.1f)
        }
        lastFrameTimeNanos = frameTimeNanos

        // Determine which line is currently "active" (being sung).
        // Background and songwriter lines are excluded from being the primary active line.
        val baseActiveLineIndex = lines.indices
            .filter { !lines[it].isBackground && !lines[it].isSongwriter }
            .lastOrNull { lines[it].startMs <= currentTimeMs && currentTimeMs <= lines[it].endMs }
            ?: lines.indices
                .filter { !lines[it].isBackground && !lines[it].isSongwriter }
                .lastOrNull { lines[it].startMs <= currentTimeMs }
            ?: 0

        // If the active line has already ended more than 1.5s ago, treat it as no active line.
        val activeLineIndex = if (baseActiveLineIndex >= 0 && lines.isNotEmpty() && lines[baseActiveLineIndex].endMs < currentTimeMs - 1500L) {
            -1
        } else {
            baseActiveLineIndex
        }

        // Map each line to its current animated state.
        return lines.mapIndexed { lineIdx, line ->
            val distance = kotlin.math.abs(lineIdx - activeLineIndex)
            val lineState = getElementState(currentTimeMs, line.startMs, line.endMs)

            // A line is considered active if it's the current lineIdx or if it's a background line currently being sung.
            val isActive = if (line.isBackground) {
                lineState == ElementState.Active
            } else {
                lineIdx == activeLineIndex && lineState == ElementState.Active
            }

            // Determine the target opacity based on the line type and state.
            val targetOpacity = when {
                line.isSongwriter -> 0.5f
                line.isInterlude -> 1.0f  // Dots control their own visibility via per-dot opacity.
                line.isBackground -> when {
                    isActive -> 0.7f
                    lineState == ElementState.NotSung -> 0.3f
                    else -> 0.3f
                }
                else -> when {
                    isActive -> OPACITY_ACTIVE
                    lineState == ElementState.NotSung -> OPACITY_NOT_SUNG
                    else -> OPACITY_SUNG
                }
            }

            // Smoothly animate the line opacity using a Compose Animatable.
            val animatable = lineOpacityAnims.getOrPut(lineIdx) {
                Animatable(targetOpacity)
            }
            if (animatable.targetValue != targetOpacity) {
                coroutineScope.launch {
                    animatable.animateTo(targetOpacity, animationSpec = opacitySpringSpec)
                }
            }
            val opacity = animatable.value

            // Compute blur level based on distance from the active line.
            val blur = if (isActive || distance == 0) {
                0f
            } else {
                (BLUR_MULTIPLIER * distance).coerceAtMost(MAX_BLUR)
            }

            // Determine and animate the line scale factor.
            val isInterlude = line.isInterlude
            val targetLineScale = if (isInterlude) {
                if (isActive) 1f else 0f
            } else {
                if (isActive) 1f else IDLE_LINE_SCALE
            }

            val scaleAnimatable = lineScaleAnims.getOrPut(lineIdx) {
                Animatable(targetLineScale)
            }
            if (scaleAnimatable.targetValue != targetLineScale) {
                coroutineScope.launch {
                    scaleAnimatable.animateTo(targetLineScale, animationSpec = scaleSpringSpec)
                }
            }
            val lineScale = scaleAnimatable.value

            // Compute states for each word (or interlude dot) within the line.
            val wordStates = if (isInterlude) {
                animateInterludeDots(line, currentTimeMs, deltaTime, lineIdx)
            } else {
                line.words.mapIndexed { wordIdx, word ->
                    animateWord(word, currentTimeMs, deltaTime, lineIdx, wordIdx, isActive)
                }
            }

            LineAnimState(
                opacity = opacity,
                blur = blur,
                scale = lineScale,
                isActive = isActive,
                wordStates = wordStates,
                isBackground = line.isBackground,
                isSongwriter = line.isSongwriter,
            )
        }
    }

    /**
     * Animates a single word using spring simulations.
     *
     * @param word The word to animate.
     * @param currentTimeMs Current playback time.
     * @param deltaTime Time elapsed since last frame.
     * @param lineIndex Index of the parent line.
     * @param wordIndex Index of the word within the line.
     * @param activeLine Whether the parent line is the active one.
     */
    private fun animateWord(
        word: Word,
        currentTimeMs: Long,
        deltaTime: Float,
        lineIndex: Int,
        wordIndex: Int,
        activeLine: Boolean,
    ): WordAnimState {
        val key = lineIndex.toLong() * 100000L + wordIndex.toLong()
        val springs = wordSpringsMap.getOrPut(key) {
            WordSprings().also {
                // Initialize springs to the "NotSung" state values.
                it.scale.setGoal(scaleSpline.at(0f), immediate = true)
                it.yOffset.setGoal(yOffsetSpline.at(0f), immediate = true)
                it.glow.setGoal(glowSpline.at(0f), immediate = true)
                it.activeFactor.setGoal(0f, immediate = true)
            }
        }

        val state = getElementState(currentTimeMs, word.startMs, word.endMs)
        val progress = getProgress(currentTimeMs, word.startMs, word.endMs)

        var targetScale = scaleSpline.at(0f)
        var targetYOffset = yOffsetSpline.at(0f)
        var targetGlow = glowSpline.at(0f)
        var targetActiveFactor = 0f
        var gradientPosition = -20f

        // Set target values for the springs based on the word's current state.
        if (activeLine) {
            when (state) {
                ElementState.Active -> {
                    targetScale = scaleSpline.at(progress)
                    targetYOffset = yOffsetSpline.at(progress)
                    targetGlow = glowSpline.at(progress)
                    targetActiveFactor = 1f
                    gradientPosition = -20f + 120f * progress
                }
                ElementState.NotSung -> {
                    targetScale = scaleSpline.at(0f)
                    targetYOffset = yOffsetSpline.at(0f)
                    targetGlow = glowSpline.at(0f)
                    targetActiveFactor = 0f
                    gradientPosition = -20f
                }
                ElementState.Sung -> {
                    targetScale = scaleSpline.at(1f)
                    targetYOffset = yOffsetSpline.at(1f)
                    targetGlow = glowSpline.at(1f)
                    targetActiveFactor = 1f
                    gradientPosition = 100f
                }
            }
        } else {
            // If the line is not active, simplify the targets.
            targetScale = scaleSpline.at(if (state == ElementState.NotSung) 0f else 1f)
            targetYOffset = yOffsetSpline.at(if (state == ElementState.NotSung) 0f else 1f)
            targetGlow = 0f
            targetActiveFactor = 0f
            gradientPosition = if (state == ElementState.NotSung) -20f else 100f
        }

        // Logic for LetterGroup words (syllabic highlight).
        val letterStates = if (activeLine && word.isLetterGroup && word.letters.isNotEmpty()) {
            // Identify the currently active letter/syllable.
            val activeLetterIdx = word.letters.indexOfFirst { letter ->
                getElementState(currentTimeMs, letter.startMs, letter.endMs) == ElementState.Active
            }
            val activeLetterProgress = if (activeLetterIdx >= 0) {
                getProgress(currentTimeMs, word.letters[activeLetterIdx].startMs, word.letters[activeLetterIdx].endMs)
            } else 0f

            word.letters.mapIndexed { li, letter ->
                val letterState = getElementState(currentTimeMs, letter.startMs, letter.endMs)
                // Proximity-based falloff: Neighbors of the active letter also move slightly.
                val distance = if (activeLetterIdx >= 0) abs(li - activeLetterIdx).toFloat() else Float.MAX_VALUE
                val falloff = if (activeLetterIdx >= 0 && letterState != ElementState.NotSung)
                    (1f / (1f + distance * 0.9f)).coerceIn(0f, 1f)
                else 0f

                val restScale  = scaleSpline.at(0f)
                val restYOff   = yOffsetSpline.at(0f)
                val restGlow   = glowSpline.at(0f)

                var targetScale: Float
                var targetYOff: Float
                var targetGlow: Float

                when (letterState) {
                    ElementState.NotSung -> {
                        targetScale = restScale
                        targetYOff  = restYOff
                        targetGlow  = restGlow
                    }
                    ElementState.Sung -> {
                        if (activeLetterIdx == -1) {
                            targetScale = scaleSpline.at(1f)
                            targetYOff  = yOffsetSpline.at(1f)
                            targetGlow  = glowSpline.at(SUNG_LETTER_GLOW)
                        } else {
                            val activeValScale = scaleSpline.at(activeLetterProgress)
                            val activeValYOff  = yOffsetSpline.at(activeLetterProgress)
                            val activeValGlow  = glowSpline.at(activeLetterProgress)
                            targetScale = restScale + (activeValScale - restScale) * falloff
                            targetYOff  = restYOff  + (activeValYOff  - restYOff)  * falloff
                            targetGlow  = restGlow  + (activeValGlow  - restGlow)  * falloff
                        }
                    }
                    ElementState.Active -> {
                        val activeValScale = scaleSpline.at(activeLetterProgress)
                        val activeValYOff  = yOffsetSpline.at(activeLetterProgress)
                        val activeValGlow  = glowSpline.at(activeLetterProgress)
                        targetScale = restScale + (activeValScale - restScale) * falloff
                        targetYOff  = restYOff  + (activeValYOff  - restYOff)  * falloff
                        targetGlow  = restGlow  + (activeValGlow  - restGlow)  * falloff
                    }
                }

                // Smooth it with a spring
                val letterKey = lineIndex.toLong() * 1000000L + wordIndex.toLong() * 100L + li.toLong()
                val lSprings = letterSpringsMap.getOrPut(letterKey) {
                    LetterSprings().also {
                        it.scale.setGoal(targetScale, immediate = true)
                        it.yOffset.setGoal(targetYOff, immediate = true)
                        it.glow.setGoal(targetGlow, immediate = true)
                    }
                }
                lSprings.scale.setGoal(targetScale)
                lSprings.yOffset.setGoal(targetYOff)
                lSprings.glow.setGoal(targetGlow)

                val lGradPos = when (letterState) {
                    ElementState.NotSung -> -20f
                    ElementState.Sung    -> 100f
                    ElementState.Active  -> if (li == activeLetterIdx) -20f + 120f * easeSinOut(activeLetterProgress) else -20f
                }

                LetterAnimState(
                    gradientPosition = lGradPos,
                    scale = lSprings.scale.step(deltaTime),
                    yOffset = lSprings.yOffset.step(deltaTime),
                    glow = lSprings.glow.step(deltaTime)
                )
            }
        } else emptyList()

        // Update spring goals and step the simulations.
        springs.scale.setGoal(targetScale)
        springs.yOffset.setGoal(targetYOffset)
        springs.glow.setGoal(targetGlow)
        springs.activeFactor.setGoal(targetActiveFactor)

        return WordAnimState(
            scale = springs.scale.step(deltaTime),
            yOffset = springs.yOffset.step(deltaTime),
            glow = springs.glow.step(deltaTime),
            gradientPosition = gradientPosition,
            state = state,
            activeAnimFactor = springs.activeFactor.step(deltaTime),
            isLetterGroup = word.isLetterGroup,
            letterStates = letterStates,
        )
    }

    /**
     * Animates the three dots typically used for instrumental interludes.
     */
    private fun animateInterludeDots(
        line: Line,
        currentTimeMs: Long,
        deltaTime: Float,
        lineIndex: Int,
    ): List<WordAnimState> {
        val effectiveDuration = maxOf(30L, line.duration - 250L) // 250ms breather matching layout
        val dotDuration = effectiveDuration.toFloat() / 3f
        return (0 until 3).map { dotIdx ->
            val dotStart = line.startMs + (dotIdx * dotDuration).toLong()
            val dotEnd   = dotStart + dotDuration.toLong()
            val progress = getProgress(currentTimeMs, dotStart, dotEnd)
            val state    = getElementState(currentTimeMs, dotStart, dotEnd)

            // Determine target values from the dot-specific splines.
            val targetScale   = when (state) {
                ElementState.Active  -> dotScaleSpline.at(progress)
                ElementState.Sung    -> dotScaleSpline.at(1f)
                ElementState.NotSung -> dotScaleSpline.at(0f)
            }
            val targetYOffset = when (state) {
                ElementState.Active  -> dotYOffsetSpline.at(progress)
                ElementState.Sung    -> dotYOffsetSpline.at(1f)
                ElementState.NotSung -> dotYOffsetSpline.at(0f)
            }
            val targetOpacity = when (state) {
                ElementState.Active  -> dotOpacitySpline.at(progress)
                ElementState.Sung    -> dotOpacitySpline.at(1f)
                ElementState.NotSung -> dotOpacitySpline.at(0f)
            }

            val key = lineIndex.toLong() * 3L + dotIdx.toLong()
            val springs = dotSpringsMap.getOrPut(key) {
                DotSprings().also {
                    it.scale.setGoal(targetScale,   immediate = true)
                    it.yOffset.setGoal(targetYOffset, immediate = true)
                    it.opacity.setGoal(targetOpacity, immediate = true)
                }
            }

            springs.scale.setGoal(targetScale)
            springs.yOffset.setGoal(targetYOffset)
            springs.opacity.setGoal(targetOpacity)

            WordAnimState(
                scale   = springs.scale.step(deltaTime),
                yOffset = springs.yOffset.step(deltaTime),
                glow    = springs.opacity.step(deltaTime), // The 'glow' field repurposed for dot opacity.
                gradientPosition = if (state == ElementState.Sung) 100f else 0f,
                state = state,
                activeAnimFactor = if (state == ElementState.Sung) 1f else 0f,
            )
        }
    }

    /** Helper to determine the singing state of an element. */
    private fun getElementState(currentTimeMs: Long, startMs: Long, endMs: Long): ElementState {
        return when {
            currentTimeMs < startMs -> ElementState.NotSung
            currentTimeMs > endMs -> ElementState.Sung
            else -> ElementState.Active
        }
    }

    /** Helper to calculate progress (0.0 to 1.0) within a time range. */
    private fun getProgress(currentTimeMs: Long, startMs: Long, endMs: Long): Float {
        if (currentTimeMs <= startMs) return 0f
        if (currentTimeMs >= endMs) return 1f
        return (currentTimeMs - startMs).toFloat() / (endMs - startMs).toFloat()
    }

    private fun easeSinOut(t: Float): Float {
        return sin(t * (PI.toFloat() / 2f))
    }
}