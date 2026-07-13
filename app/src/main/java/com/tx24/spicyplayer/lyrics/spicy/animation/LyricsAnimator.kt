package com.tx24.spicyplayer.lyrics.spicy.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Exact port of the reference animator (`spicy-lyrics/.../Animator/Lyrics/LyricsAnimator.ts`).
 *
 * Structure mirrors the original's per-frame `Animate(position)`:
 * - Only **Active** lines have their word/letter/dot springs retargeted and stepped.
 * - **NotSung** lines are never touched — their words freeze at whatever state they last had
 *   (initially the resting state). There is no snap-to-resting reset pass.
 * - A **Sung** line keeps stepping toward its final targets (the `checkNextLine` pass) only
 *   while the following line is itself not yet Sung (or when it's the last line); afterwards
 *   it freezes. This is what makes a just-finished line settle out gracefully instead of
 *   stopping dead the moment its time window ends.
 * - Line-level opacity is the Compose equivalent of the CSS class transition
 *   (200ms cubic-bezier(0.61,1,0.88,1)); distance blur is recomputed only when the active
 *   line index changes.
 *
 * Springs are [SpringSimulation] (exact spr.lua port); dt is in **seconds**, positions in ms.
 */
class LyricsAnimator(
    private val coroutineScope: CoroutineScope,
    config: RenderConfig = RenderConfig.FULL,
) {

    var config: RenderConfig = config
        set(value) {
            if (value != field) {
                field = value
                rebuildModeSplines()
                reset()
            }
        }

    // ── Splines (control points verbatim from the reference) ──────────────────────────
    private val scaleSpline = spline(0f to 0.95f, 0.7f to 1.0505f, 1f to 1f)
    private val glowSpline = spline(0f to 0f, 0.15f to 1f, 0.6f to 1f, 1f to 0f)
    private val dotScaleSpline = spline(0f to 0.75f, 0.7f to 1.05f, 1f to 1f)
    private val dotYOffsetSpline = spline(0f to 0f, 0.9f to -0.12f, 1f to 0f)
    private val dotGlowSpline = spline(0f to 0f, 0.6f to 1f, 1f to 1f)
    private val lineGlowSpline = spline(0f to 0f, 0.5f to 1f, 1f to 0f)

    // Mode-dependent splines (rebuilt when the quality mode changes).
    private lateinit var yOffsetSpline: CubicSplineInterpolator
    private lateinit var letterScaleSpline: CubicSplineInterpolator
    private lateinit var letterYOffsetSpline: CubicSplineInterpolator
    private lateinit var dotOpacitySpline: CubicSplineInterpolator

    init { rebuildModeSplines() }

    private fun rebuildModeSplines() {
        val simple = config.isSimple
        yOffsetSpline = if (simple) spline(0f to 0.01f, 1f to -0.033f)
            else spline(0f to 0.01f, 0.9f to -1f / 60f, 1f to 0f)
        letterScaleSpline = if (simple) spline(0f to 0.95f, 0.7f to 1.07f, 1f to 1f)
            else spline(0f to 0.95f, 0.7f to 1.175f, 1f to 1f)
        letterYOffsetSpline = if (simple) spline(0f to 0.01f, 0.9f to -1f / 62f, 1f to 0f)
            else spline(0f to 0.01f, 0.9f to -1f / 56f, 1f to 0f)
        dotOpacitySpline = spline(0f to (if (simple) 0.27f else 0.35f), 0.6f to 1f, 1f to 1f)
    }

    private companion object {
        // Word/letter spring tuning (reference "active" values).
        const val SCALE_FREQUENCY = 0.88f
        const val SCALE_DAMPING = 0.64f
        const val Y_OFFSET_FREQUENCY = 1.45f
        const val Y_OFFSET_DAMPING = 0.4f
        const val GLOW_FREQUENCY = 1.18f
        const val GLOW_DAMPING = 0.56f

        // Dot spring tuning.
        const val DOT_SCALE_FREQUENCY = 0.7f
        const val DOT_SCALE_DAMPING = 0.6f
        const val DOT_Y_OFFSET_FREQUENCY = 1.25f
        const val DOT_Y_OFFSET_DAMPING = 0.4f
        const val DOT_GLOW_FREQUENCY = 1f
        const val DOT_GLOW_DAMPING = 0.5f
        const val DOT_OPACITY_FREQUENCY = 1f
        const val DOT_OPACITY_DAMPING = 0.5f

        // Line-mode whole-line glow spring.
        const val LINE_GLOW_FREQUENCY = 1f
        const val LINE_GLOW_DAMPING = 0.5f

        /** GlowSpline input for a finished letter while its word still has an active letter path. */
        const val SUNG_LETTER_GLOW = 0.2f

        // Distance blur (Shared.ts).
        const val BLUR_MULTIPLIER = 1.25f
        val MAX_BLUR = BLUR_MULTIPLIER * 5f + BLUR_MULTIPLIER * 0.465f  // 6.83125

        /** Dots collapse this long before their line ends (lyrics.ts preHiddenDotLineMs). */
        const val PRE_HIDDEN_DOT_LINE_MS = 500L
        /** Position shift applied in simple mode (Animate: `- 33.5`). */
        const val SIMPLE_MODE_POSITION_SHIFT_MS = 33.5f

        // Simple-mode letter effect strength (lyrics.ts SimpleLyricsMode_LetterEffectsStrengthConfig).
        const val SIMPLE_LETTER_LONGER_THAN_MS = 1500L
        const val SIMPLE_LONGER_SCALE = 1.103f
        const val SIMPLE_LONGER_Y_OFFSET = 0.45f
        const val SIMPLE_LONGER_GLOW = 0.4f
        const val SIMPLE_SHORTER_SCALE = 1.09f
        const val SIMPLE_SHORTER_Y_OFFSET = 0.1f
        const val SIMPLE_SHORTER_GLOW = 0.285f

        val OPACITY_EASING = CubicBezierEasing(0.61f, 1f, 0.88f, 1f)
        val SCALE_EASING = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
        /** Reference dot-group collapse (Mixed.css .pre-hidden .dotGroup): 0.4s w/ dip-then-overshoot. */
        val DOT_GROUP_COLLAPSE_EASING = CubicBezierEasing(0.68f, -0.6f, 0.32f, 1.6f)
        const val DOT_GROUP_COLLAPSE_MS = 400

        fun spline(vararg points: Pair<Float, Float>) =
            CubicSplineInterpolator(points.map { AnimationPoint(it.first, it.second) })
    }

    /** Word/letter-group container springs, snapped to resting on creation (reference lazy-init). */
    private inner class WordSprings {
        val scale = SpringSimulation(scaleSpline.at(0f), SCALE_FREQUENCY, SCALE_DAMPING)
        val yOffset = SpringSimulation(yOffsetSpline.at(0f), Y_OFFSET_FREQUENCY, Y_OFFSET_DAMPING)
        val glow = SpringSimulation(glowSpline.at(0f), GLOW_FREQUENCY, GLOW_DAMPING)
    }

    private inner class LetterSprings {
        val scale = SpringSimulation(letterScaleSpline.at(0f), SCALE_FREQUENCY, SCALE_DAMPING)
        val yOffset = SpringSimulation(letterYOffsetSpline.at(0f), Y_OFFSET_FREQUENCY, Y_OFFSET_DAMPING)
        val glow = SpringSimulation(glowSpline.at(0f), GLOW_FREQUENCY, GLOW_DAMPING)
    }

    private inner class DotSprings {
        val scale = SpringSimulation(dotScaleSpline.at(0f), DOT_SCALE_FREQUENCY, DOT_SCALE_DAMPING)
        val yOffset = SpringSimulation(dotYOffsetSpline.at(0f), DOT_Y_OFFSET_FREQUENCY, DOT_Y_OFFSET_DAMPING)
        val glow = SpringSimulation(dotGlowSpline.at(0f), DOT_GLOW_FREQUENCY, DOT_GLOW_DAMPING)
        val opacity = SpringSimulation(dotOpacitySpline.at(0f), DOT_OPACITY_FREQUENCY, DOT_OPACITY_DAMPING)
    }

    // ── Per-element animation state ─────────────────────────────────────────────────
    private val wordSpringsMap = mutableMapOf<Long, WordSprings>()
    private val letterSpringsMap = mutableMapOf<Long, LetterSprings>()
    private val dotSpringsMap = mutableMapOf<Long, DotSprings>()
    private val lineGlowSprings = mutableMapOf<Int, SpringSimulation>()

    private val lineOpacityAnims = mutableMapOf<Int, Animatable<Float, AnimationVector1D>>()
    private val lineScaleAnims = mutableMapOf<Int, Animatable<Float, AnimationVector1D>>()

    /** Frozen word states per line — reused verbatim on frames where the line isn't touched. */
    private val cachedWordStates = mutableMapOf<Int, List<WordAnimState>>()
    private val cachedLineGradient = mutableMapOf<Int, Float>()
    private val cachedLineGlow = mutableMapOf<Int, Float>()

    /** Blur is recomputed only when the active line index changes (reference Blurring_LastLine). */
    private var blurringLastLine: Int = -1
    private var blurAmounts = FloatArray(0)

    fun reset() {
        wordSpringsMap.clear()
        letterSpringsMap.clear()
        dotSpringsMap.clear()
        lineGlowSprings.clear()
        lineOpacityAnims.clear()
        lineScaleAnims.clear()
        cachedWordStates.clear()
        cachedLineGradient.clear()
        cachedLineGlow.clear()
        blurringLastLine = -1
        blurAmounts = FloatArray(0)
    }

    /**
     * Computes the animation state for all lines at [currentTimeMs].
     *
     * @param deltaTime seconds since the last frame (unclamped, like the reference).
     * @param suppressBlur true while the user is dragging (reference: HideLineBlur).
     */
    fun animate(
        lines: List<Line>,
        currentTimeMs: Long,
        deltaTime: Float,
        suppressBlur: Boolean = false,
        lyricsType: LyricsType = LyricsType.Syllable,
    ): List<LineAnimState> {
        if (lines.isEmpty()) return emptyList()

        // Static lyrics have no timing: full-white, no animation (reference Static mode).
        if (lyricsType == LyricsType.Static) {
            return lines.map {
                LineAnimState(
                    opacity = 1f, blur = 0f, scale = 1f, isActive = false,
                    wordStates = emptyList(), isBackground = false, isSongwriter = false,
                )
            }
        }

        val processedPosition = if (config.isSimple)
            currentTimeMs - SIMPLE_MODE_POSITION_SHIFT_MS.toLong() else currentTimeMs

        if (blurAmounts.size != lines.size) blurAmounts = FloatArray(lines.size)

        return lines.mapIndexed { lineIdx, line ->
            val lineState = elementState(processedPosition, line.startMs, line.endMs)
            val isActive = !line.isSongwriter && lineState == ElementState.Active

            // Distance blur: recompute only when the active line changes.
            if (isActive && !line.isBackground && blurringLastLine != lineIdx) {
                applyBlur(lines, lineIdx, processedPosition)
                blurringLastLine = lineIdx
            }

            // A line whose own words just finished can still be mid-settle (checkNextLine) while
            // a concurrent bg line — or the array-adjacency check — keeps it from freezing yet;
            // this only governs how long the scale/glow springs keep relaxing smoothly instead of
            // freezing abruptly. It must NOT keep the line's fill brighter than any other inactive
            // line — the renderer treats every non-active line's words identically regardless of
            // Sung/NotSung, so line opacity can go back to tracking lineState alone.
            val stillFinalizing = lineState == ElementState.Sung &&
                (shouldFinalize(lines, lineIdx, processedPosition) || !isSettled(cachedWordStates[lineIdx]))
            val opacity = animateLineOpacity(lineIdx, line, lineState, isActive)
            val lineScale = when {
                line.isInterlude -> animateInterludeScale(lineIdx, line, processedPosition, isActive)
                // Line-mode active line scales to 1.05 (CSS: data-lyrics-type="Line" .line.Active).
                lyricsType == LyricsType.Line && !line.isSongwriter ->
                    animateTweenScale(lineIdx, if (isActive) 1.05f else 1f)
                else -> 1f
            }

            val wordStates: List<WordAnimState>
            var lineGradient = cachedLineGradient[lineIdx] ?: -20f
            var lineGlow = cachedLineGlow[lineIdx] ?: 0f

            if (lyricsType == LyricsType.Line && !line.isInterlude) {
                // ── Line mode: whole-line gradient + glow spring; no word processing. ──
                when (lineState) {
                    ElementState.Active -> {
                        val pct = progress(processedPosition, line.startMs, line.endMs)
                        lineGradient = pct * 100f
                        val spring = lineGlowSprings.getOrPut(lineIdx) {
                            SpringSimulation(lineGlowSpline.at(0f), LINE_GLOW_FREQUENCY, LINE_GLOW_DAMPING)
                        }
                        spring.setGoal(lineGlowSpline.at(pct))
                        lineGlow = spring.step(deltaTime)
                    }
                    ElementState.NotSung -> lineGradient = -20f
                    ElementState.Sung -> lineGradient = 100f
                }
                cachedLineGradient[lineIdx] = lineGradient
                cachedLineGlow[lineIdx] = lineGlow
                wordStates = emptyList()
            } else {
                wordStates = when {
                    line.isSongwriter -> cachedWordStates.getOrPut(lineIdx) {
                        line.words.map { songwriterWordState(it) }
                    }
                    lineState == ElementState.Active -> {
                        val states = if (line.isInterlude) {
                            animateInterludeDots(line, processedPosition, deltaTime, lineIdx, finalize = false)
                        } else {
                            line.words.mapIndexed { wordIdx, word ->
                                animateWord(word, processedPosition, deltaTime, lineIdx, wordIdx)
                            }
                        }
                        cachedWordStates[lineIdx] = states
                        states
                    }
                    stillFinalizing -> {
                        // checkNextLine: keep settling toward final targets until the next line is Sung.
                        val states = if (line.isInterlude) {
                            animateInterludeDots(line, processedPosition, deltaTime, lineIdx, finalize = true)
                        } else {
                            line.words.mapIndexed { wordIdx, word ->
                                finalizeWord(word, deltaTime, lineIdx, wordIdx)
                            }
                        }
                        cachedWordStates[lineIdx] = states
                        states
                    }
                    else -> {
                        // Frozen: NotSung lines and long-finished Sung lines are never touched.
                        cachedWordStates.getOrPut(lineIdx) { line.words.map { restingWordState(it) } }
                    }
                }
            }

            val blur = if (!config.distanceBlurEnabled || suppressBlur || isActive) 0f
                else blurAmounts.getOrElse(lineIdx) { 0f }

            LineAnimState(
                opacity = opacity,
                blur = blur,
                scale = lineScale,
                isActive = isActive,
                wordStates = wordStates,
                isBackground = line.isBackground,
                isSongwriter = line.isSongwriter,
                lineGradientPercent = lineGradient,
                lineGlow = lineGlow,
                suppressShadows = suppressBlur,
            )
        }
    }

    // ── Line-level pieces ───────────────────────────────────────────────────────────

    private fun animateLineOpacity(
        lineIdx: Int,
        line: Line,
        lineState: ElementState,
        isActive: Boolean,
    ): Float {
        val target = when {
            line.isSongwriter -> 0.6f
            line.isInterlude -> 1.0f  // dots manage their own visibility
            // Reference: bg-lines have NO opacity override — they use the standard .line state
            // opacities; only their gradient alphas differ (0.6/0.3, applied in the renderer).
            else -> when (lineState) {
                ElementState.Active -> config.opacityActive
                ElementState.NotSung -> config.opacityNotSung
                ElementState.Sung -> config.opacitySung
            }
        }
        val animatable = lineOpacityAnims.getOrPut(lineIdx) { Animatable(target) }
        if (animatable.targetValue != target) {
            coroutineScope.launch {
                animatable.animateTo(target, tween(config.lineTransitionMs, easing = OPACITY_EASING))
            }
        }
        return animatable.value
    }

    /**
     * Interlude dot-group visibility (the Compose replacement for the reference's musical-line
     * height collapse + `.pre-hidden` class): 1 while active until 500ms before the line ends,
     * 0 otherwise. Non-interlude lines have no line-level scale (reference Syllable mode).
     */
    private fun animateInterludeScale(
        lineIdx: Int,
        line: Line,
        processedPosition: Long,
        isActive: Boolean,
    ): Float {
        if (!line.isInterlude) return 1f
        val preHidden = processedPosition > line.endMs - PRE_HIDDEN_DOT_LINE_MS
        val target = if (isActive && !preHidden) 1f else 0f
        val animatable = lineScaleAnims.getOrPut(lineIdx) { Animatable(target) }
        if (animatable.targetValue != target) {
            // The collapse (1→0, at pre-hidden) uses the reference's slower 0.4s dip-then-overshoot
            // curve; expansion (0→1, on activation) keeps the default line-transition tween.
            val duration = if (target == 0f) DOT_GROUP_COLLAPSE_MS else config.lineTransitionMs
            val easing = if (target == 0f) DOT_GROUP_COLLAPSE_EASING else SCALE_EASING
            coroutineScope.launch {
                animatable.animateTo(target, tween(duration, easing = easing))
            }
        }
        return animatable.value
    }

    /** Generic line-scale tween (Line-mode active-line 1.05, CSS transition semantics). */
    private fun animateTweenScale(lineIdx: Int, target: Float): Float {
        val animatable = lineScaleAnims.getOrPut(lineIdx) { Animatable(target) }
        if (animatable.targetValue != target) {
            coroutineScope.launch {
                animatable.animateTo(target, tween(config.lineTransitionMs, easing = SCALE_EASING))
            }
        }
        return animatable.value
    }

    private fun applyBlur(lines: List<Line>, activeIndex: Int, processedPosition: Long) {
        for (i in lines.indices) {
            val state = elementState(processedPosition, lines[i].startMs, lines[i].endMs)
            val distance = abs(i - activeIndex)
            blurAmounts[i] = if (state == ElementState.Active || distance == 0) 0f
                else min(BLUR_MULTIPLIER * distance, MAX_BLUR)
        }
    }

    /** A Sung line keeps stepping while the next line is NotSung/Active, or when it's the last line. */
    private fun shouldFinalize(lines: List<Line>, lineIdx: Int, processedPosition: Long): Boolean {
        val next = lines.getOrNull(lineIdx + 1) ?: return true
        return elementState(processedPosition, next.startMs, next.endMs) != ElementState.Sung
    }

    /**
     * True once a settled line's springs have actually reached their final (fully-sung) values.
     * The array-adjacency check in [shouldFinalize] can flip false before that happens — e.g. a
     * background line's "next line" in list order may already be Sung from a much earlier point
     * in the song — which would otherwise freeze the line mid-transition, never fully lighting up.
     */
    private fun isSettled(states: List<WordAnimState>?): Boolean {
        if (states == null) return false
        return states.all { w ->
            w.gradientPosition >= 99.5f && w.glow < 0.02f &&
                (!w.isLetterGroup || w.letterStates.all { it.gradientPosition >= 99.5f && it.glow < 0.02f })
        }
    }

    // ── Word processing (Active line) ───────────────────────────────────────────────

    private fun animateWord(
        word: Word,
        processedPosition: Long,
        deltaTime: Float,
        lineIndex: Int,
        wordIndex: Int,
    ): WordAnimState {
        val springs = wordSprings(lineIndex, wordIndex)
        val wordState = elementState(processedPosition, word.startMs, word.endMs)
        val percentage = progress(processedPosition, word.startMs, word.endMs)
        val simple = config.isSimple
        val gradientBase = if (simple) -50f else -20f

        val targetScale: Float
        val targetYOffset: Float
        val targetGlow: Float
        val targetGradientPos: Float
        when (wordState) {
            ElementState.Active -> {
                targetScale = scaleSpline.at(percentage)
                targetYOffset = yOffsetSpline.at(percentage)
                targetGlow = glowSpline.at(percentage)
                targetGradientPos = gradientBase + 120f * percentage
            }
            ElementState.NotSung -> {
                targetScale = scaleSpline.at(0f)
                targetYOffset = yOffsetSpline.at(0f)
                targetGlow = glowSpline.at(0f)
                targetGradientPos = gradientBase
            }
            ElementState.Sung -> {
                targetScale = scaleSpline.at(1f)
                targetYOffset = yOffsetSpline.at(1f)
                targetGlow = glowSpline.at(1f)
                targetGradientPos = 100f
            }
        }

        springs.scale.setGoal(targetScale)
        springs.yOffset.setGoal(targetYOffset)
        springs.glow.setGoal(targetGlow)
        val currentScale = springs.scale.step(deltaTime)
        val currentYOffset = springs.yOffset.step(deltaTime)
        val currentGlow = springs.glow.step(deltaTime)

        val isLetterGroup = word.isLetterGroup && word.letters.isNotEmpty()
        val letterStates = if (isLetterGroup) {
            animateLetters(word, wordState, processedPosition, deltaTime, lineIndex, wordIndex)
        } else emptyList()

        return WordAnimState(
            // Simple mode: word scale/glow springs are no-ops in the reference (CSS handles it).
            scale = if (simple) 1f else currentScale,
            yOffset = currentYOffset,
            glow = if (simple) 0f else currentGlow,
            gradientPosition = targetGradientPos,
            state = wordState,
            isLetterGroup = isLetterGroup,
            letterStates = letterStates,
        )
    }

    private fun animateLetters(
        word: Word,
        wordState: ElementState,
        processedPosition: Long,
        deltaTime: Float,
        lineIndex: Int,
        wordIndex: Int,
    ): List<LetterAnimState> {
        val simple = config.isSimple
        val gradientBase = if (simple) -50f else -20f

        // 4b/4c: NotSung/Sung word inside an Active line — letters chase resting/final targets.
        if (wordState != ElementState.Active) {
            val t = if (wordState == ElementState.NotSung) 0f else 1f
            val gradient = if (wordState == ElementState.NotSung) gradientBase else 100f
            return word.letters.mapIndexed { li, _ ->
                val s = letterSprings(lineIndex, wordIndex, li)
                s.scale.setGoal(letterScaleSpline.at(t))
                s.yOffset.setGoal(letterYOffsetSpline.at(t))
                s.glow.setGoal(glowSpline.at(t))
                LetterAnimState(
                    gradientPosition = gradient,
                    scale = s.scale.step(deltaTime),
                    yOffset = s.yOffset.step(deltaTime),
                    glow = s.glow.step(deltaTime),
                )
            }
        }

        // 4a: Active word — two-phase per-letter targets.
        var activeLetterIndex = -1
        var activeLetterPercentage = 0f
        for (i in word.letters.indices) {
            val l = word.letters[i]
            if (elementState(processedPosition, l.startMs, l.endMs) == ElementState.Active) {
                activeLetterIndex = i
                activeLetterPercentage = progress(processedPosition, l.startMs, l.endMs)
                break
            }
        }

        val wordDuration = word.endMs - word.startMs

        return word.letters.mapIndexed { k, letter ->
            val letterState = elementState(processedPosition, letter.startMs, letter.endMs)

            // Defaults: resting.
            var targetScale = letterScaleSpline.at(0f)
            var targetYOffset = letterYOffsetSpline.at(0f)
            var targetGlow = glowSpline.at(0f)

            if (activeLetterIndex != -1) {
                // Simple mode blends by whole-word progress with strength multipliers; full mode
                // by the active letter's own progress.
                val percentageCount = if (simple)
                    progress(processedPosition, word.startMs, word.endMs) else activeLetterPercentage
                val longer = wordDuration > SIMPLE_LETTER_LONGER_THAN_MS
                val scaleMult = if (simple) (if (longer) SIMPLE_LONGER_SCALE else SIMPLE_SHORTER_SCALE) else 1f
                val yOffMult = if (simple) (if (longer) SIMPLE_LONGER_Y_OFFSET else SIMPLE_SHORTER_Y_OFFSET) else 1f
                val glowMult = if (simple) (if (longer) SIMPLE_LONGER_GLOW else SIMPLE_SHORTER_GLOW) else 1f

                val baseScale = letterScaleSpline.at(percentageCount) * scaleMult
                val baseYOffset = letterYOffsetSpline.at(percentageCount) * yOffMult
                val baseGlow = glowSpline.at(percentageCount) * glowMult

                val restingScale = letterScaleSpline.at(0f)
                val restingYOffset = letterYOffsetSpline.at(0f)
                val restingGlow = glowSpline.at(0f)

                val distance = abs(k - activeLetterIndex).toFloat()
                val falloff = (1f / (1f + distance.pow(2.8f))).coerceAtLeast(0f)
                val glowFalloff = (1f / (1f + distance * 0.9f)).coerceAtLeast(0f)

                targetScale = restingScale + (baseScale - restingScale) * falloff
                targetYOffset = restingYOffset + (baseYOffset - restingYOffset) * falloff
                targetGlow = restingGlow + (baseGlow - restingGlow) * glowFalloff
            }

            // Overrides (reference order): NotSung letters reset to resting (full mode only);
            // Sung letters in a word with no active letter hold GlowSpline.at(SungLetterGlow).
            if (letterState == ElementState.NotSung && !simple) {
                targetScale = letterScaleSpline.at(0f)
                targetYOffset = letterYOffsetSpline.at(0f)
                targetGlow = glowSpline.at(0f)
            } else if (letterState == ElementState.Sung && activeLetterIndex == -1) {
                targetGlow = glowSpline.at(SUNG_LETTER_GLOW)
            }

            // Gradient: only the actual active letter sweeps (eased); rest hold base/final.
            val targetGradient = when (letterState) {
                ElementState.NotSung -> gradientBase
                ElementState.Sung -> 100f
                ElementState.Active ->
                    if (k == activeLetterIndex) gradientBase + 120f * easeSinOut(activeLetterPercentage)
                    else gradientBase
            }

            val s = letterSprings(lineIndex, wordIndex, k)
            s.scale.setGoal(targetScale)
            s.yOffset.setGoal(targetYOffset)
            s.glow.setGoal(targetGlow)

            LetterAnimState(
                gradientPosition = targetGradient,
                scale = s.scale.step(deltaTime),
                yOffset = s.yOffset.step(deltaTime),
                glow = s.glow.step(deltaTime),
            )
        }
    }

    /** The Sung-line settle pass (`checkNextLine`): step everything toward its final state. */
    private fun finalizeWord(
        word: Word,
        deltaTime: Float,
        lineIndex: Int,
        wordIndex: Int,
    ): WordAnimState {
        val springs = wordSprings(lineIndex, wordIndex)
        springs.scale.setGoal(scaleSpline.at(1f))
        springs.yOffset.setGoal(yOffsetSpline.at(1f))
        springs.glow.setGoal(glowSpline.at(1f))
        val currentScale = springs.scale.step(deltaTime)
        val currentYOffset = springs.yOffset.step(deltaTime)
        val currentGlow = springs.glow.step(deltaTime)

        val isLetterGroup = word.isLetterGroup && word.letters.isNotEmpty()
        val letterStates = if (isLetterGroup) {
            word.letters.mapIndexed { li, _ ->
                val s = letterSprings(lineIndex, wordIndex, li)
                s.scale.setGoal(letterScaleSpline.at(1f))
                s.yOffset.setGoal(letterYOffsetSpline.at(1f))
                s.glow.setGoal(glowSpline.at(1f))
                LetterAnimState(
                    gradientPosition = 100f,
                    scale = s.scale.step(deltaTime),
                    yOffset = s.yOffset.step(deltaTime),
                    glow = s.glow.step(deltaTime),
                )
            }
        } else emptyList()

        val simple = config.isSimple
        return WordAnimState(
            scale = if (simple) 1f else currentScale,
            yOffset = currentYOffset,
            glow = if (simple) 0f else currentGlow,
            gradientPosition = 100f,
            state = ElementState.Sung,
            isLetterGroup = isLetterGroup,
            letterStates = letterStates,
        )
    }

    // ── Interlude dots ──────────────────────────────────────────────────────────────

    private fun animateInterludeDots(
        line: Line,
        processedPosition: Long,
        deltaTime: Float,
        lineIndex: Int,
        finalize: Boolean,
    ): List<WordAnimState> {
        // Dot timing: each dot ends slightly early (applier: dotPadding = -550/3 per dot).
        val span = ((line.duration - 550L).coerceAtLeast(90L)).toFloat() / 3f
        return (0 until 3).map { dotIdx ->
            val dotStart = line.startMs + (dotIdx * span).toLong()
            val dotEnd = line.startMs + ((dotIdx + 1) * span).toLong()
            val state = if (finalize) ElementState.Sung
                else elementState(processedPosition, dotStart, dotEnd)
            val pct = progress(processedPosition, dotStart, dotEnd)

            fun sample(sp: CubicSplineInterpolator) = when (state) {
                ElementState.Active -> sp.at(pct)
                ElementState.NotSung -> sp.at(0f)
                ElementState.Sung -> sp.at(1f)
            }

            val key = lineIndex.toLong() * 3L + dotIdx.toLong()
            val springs = dotSpringsMap.getOrPut(key) { DotSprings() }
            springs.scale.setGoal(sample(dotScaleSpline))
            springs.yOffset.setGoal(sample(dotYOffsetSpline))
            springs.glow.setGoal(sample(dotGlowSpline))
            springs.opacity.setGoal(sample(dotOpacitySpline))

            WordAnimState(
                scale = springs.scale.step(deltaTime),
                yOffset = springs.yOffset.step(deltaTime),
                glow = springs.opacity.step(deltaTime),   // 'glow' carries dot opacity for the base draw
                dotGlow = springs.glow.step(deltaTime),    // 'dotGlow' carries the halo intensity
                gradientPosition = if (state == ElementState.Sung) 100f else 0f,
                state = state,
            )
        }
    }

    // ── Frozen-state synthesis ──────────────────────────────────────────────────────

    /** Initial resting state for a word that has never been animated (reference build-time styles). */
    private fun restingWordState(word: Word): WordAnimState {
        val simple = config.isSimple
        val gradientBase = if (simple) -50f else -20f
        val isLetterGroup = word.isLetterGroup && word.letters.isNotEmpty()
        return WordAnimState(
            scale = if (simple) 1f else scaleSpline.at(0f),
            yOffset = yOffsetSpline.at(0f),
            glow = 0f,
            gradientPosition = gradientBase,
            state = ElementState.NotSung,
            isLetterGroup = isLetterGroup,
            letterStates = if (isLetterGroup) word.letters.map {
                LetterAnimState(
                    gradientPosition = gradientBase,
                    scale = letterScaleSpline.at(0f),
                    yOffset = letterYOffsetSpline.at(0f),
                    glow = 0f,
                )
            } else emptyList(),
        )
    }

    /** Songwriter credits render as plain, fully-swept text (a non-lyric element in the reference). */
    private fun songwriterWordState(word: Word): WordAnimState = WordAnimState(
        scale = 1f,
        yOffset = 0f,
        glow = 0f,
        gradientPosition = 100f,
        state = ElementState.Sung,
        isLetterGroup = false,
        letterStates = emptyList(),
    )

    // ── Spring lookup ───────────────────────────────────────────────────────────────

    private fun wordSprings(lineIndex: Int, wordIndex: Int): WordSprings =
        wordSpringsMap.getOrPut(lineIndex.toLong() * 100000L + wordIndex.toLong()) { WordSprings() }

    private fun letterSprings(lineIndex: Int, wordIndex: Int, letterIndex: Int): LetterSprings =
        letterSpringsMap.getOrPut(
            lineIndex.toLong() * 1000000L + wordIndex.toLong() * 100L + letterIndex.toLong()
        ) { LetterSprings() }

    // ── Helpers ─────────────────────────────────────────────────────────────────────

    private fun elementState(t: Long, startMs: Long, endMs: Long): ElementState = when {
        t < startMs -> ElementState.NotSung
        t >= endMs -> ElementState.Sung
        else -> ElementState.Active
    }

    private fun progress(t: Long, startMs: Long, endMs: Long): Float {
        if (t <= startMs) return 0f
        if (t >= endMs) return 1f
        return (t - startMs).toFloat() / (endMs - startMs).toFloat()
    }

    private fun easeSinOut(t: Float): Float = sin(t * (PI.toFloat() / 2f))
}
