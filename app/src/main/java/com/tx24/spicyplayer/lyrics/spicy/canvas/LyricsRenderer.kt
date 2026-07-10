package com.tx24.spicyplayer.lyrics.spicy.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.animation.LineAnimState
import com.tx24.spicyplayer.lyrics.spicy.animation.WordAnimState

/**
 * Draws a text fragment with a moving left-to-right gradient "wipe" (the karaoke fill), in a
 * single pass. This mirrors the original's exact CSS model:
 * `linear-gradient(bright stop1%, dim stop2%)` where `stop1 = gradientPositionPercent` and
 * `stop2 = stop1 + 20`, both expressed as percentages of [fullWidth] — NOT renormalized to a
 * plain 0..1 range. That distinction matters: the reference's position sweeps from -20% to
 * 100% (a 120-point range), so for roughly the first ~17% and last ~17% of a syllable's own
 * timing window the transition band sits entirely off the visible text (uniformly dim, then
 * uniformly bright) before/after actually crossing it — matching the reference's real feel
 * instead of stretching the wipe evenly across the whole syllable.
 *
 * @param gradientPositionPercent the raw position value (e.g. -20 at NotSung, 100 at Sung),
 *   in percent of [fullWidth].
 * @param fullWidth total width the gradient's percentages are relative to (whole word across
 *   rows, whole line, or — for a standalone letter — that letter's own width).
 * @param startXOffset this fragment's offset within [fullWidth].
 */
private fun DrawScope.drawWipeText(
    layoutResult: TextLayoutResult,
    xPos: Float,
    yPos: Float,
    fragmentWidth: Float,
    fullWidth: Float,
    startXOffset: Float,
    gradientPositionPercent: Float,
    brightAlpha: Float,
    dimAlpha: Float,
    shadow: Shadow?,
    rtl: Boolean = false,
) {
    val bright = Color.White.copy(alpha = brightAlpha.coerceIn(0f, 1f))
    val dim = Color.White.copy(alpha = dimAlpha.coerceIn(0f, 1f))
    val topLeft = Offset(xPos, yPos)

    val fw = fullWidth.coerceAtLeast(1f)
    val w = fragmentWidth.coerceAtLeast(1f)
    // Reference: stop2 = stop1 + 20% (+ --gradient-offset, 0 outside Simple mode).
    val stop1Frac = gradientPositionPercent / 100f
    val stop2Frac = (gradientPositionPercent + 20f) / 100f
    // RTL mirrors the whole sweep: bright grows from the right instead of the left, so both the
    // band's position and its colour order flip.
    val loFrac = if (rtl) 1f - stop2Frac else stop1Frac
    val hiFrac = if (rtl) 1f - stop1Frac else stop2Frac
    val leftColor = if (rtl) dim else bright
    val rightColor = if (rtl) bright else dim

    val fragStartFrac = startXOffset / fw
    val fragEndFrac = (startXOffset + w) / fw

    // Fast paths: the whole fragment lies entirely before or after the transition band.
    if (fragEndFrac <= loFrac) {
        drawText(layoutResult, color = leftColor, shadow = shadow, topLeft = topLeft); return
    }
    if (fragStartFrac >= hiFrac) {
        drawText(layoutResult, color = rightColor, shadow = shadow, topLeft = topLeft); return
    }

    // The band intersects this fragment: map the (possibly off-fragment) stop positions into
    // the fragment's own 0..1 local space.
    val localLo = ((loFrac * fw) - startXOffset) / w
    val localHi = ((hiFrac * fw) - startXOffset) / w

    // The colour at any local x is the band's linear interpolation, clamped at the ends —
    // exactly what the CSS gradient shows when part of the band lies outside the box. When a
    // word has just started (band mostly off the left edge) its left edge must show the
    // mid-fade value and build up gradually, NOT snap to full bright: keeping the endpoint
    // colour at a clamped stop makes the word's tip flash and the rest look darkened.
    val bandW = (localHi - localLo).coerceAtLeast(1e-4f)
    fun colorAt(x: Float): Color =
        androidx.compose.ui.graphics.lerp(leftColor, rightColor, ((x - localLo) / bandW).coerceIn(0f, 1f))

    val s0 = localLo.coerceIn(0.0001f, 0.9997f)
    val s1 = localHi.coerceIn(s0 + 0.0001f, 0.9998f)
    // IMPORTANT: drawText(textLayoutResult, brush) evaluates the brush in TEXT-LOCAL
    // coordinates (the canvas is translated to topLeft before the shader is applied), so the
    // gradient must span [0, w] — NOT absolute canvas x. Using absolute coords shifts the
    // band off the glyphs entirely and the clamped shader floods the word with one color.
    val brush = Brush.horizontalGradient(
        0f to colorAt(0f),
        s0 to colorAt(s0),
        s1 to colorAt(s1),
        1f to colorAt(1f),
        startX = 0f,
        endX = w,
    )
    drawText(layoutResult, brush = brush, shadow = shadow, topLeft = topLeft)
}

/**
 * Vertical (top→bottom) variant of [drawWipeText] for Line-mode lines: the `.line` element
 * takes the `--gradient-degrees: 180deg !important` rule (unlike `.word`/`.letter`, whose own
 * 90deg declarations win), so line-synced lyrics fill downward across the LINE's full height.
 * The position percent maps over [fullHeight]; each wrapped row fragment maps the band into its
 * own local vertical space via [startYOffset].
 */
private fun DrawScope.drawVerticalWipeText(
    layoutResult: TextLayoutResult,
    xPos: Float,
    yPos: Float,
    fragmentHeight: Float,
    fullHeight: Float,
    startYOffset: Float,
    gradientPositionPercent: Float,
    brightAlpha: Float,
    dimAlpha: Float,
    shadow: Shadow?,
) {
    val bright = Color.White.copy(alpha = brightAlpha.coerceIn(0f, 1f))
    val dim = Color.White.copy(alpha = dimAlpha.coerceIn(0f, 1f))
    val topLeft = Offset(xPos, yPos)

    val fh = fullHeight.coerceAtLeast(1f)
    val h = fragmentHeight.coerceAtLeast(1f)
    val loFrac = gradientPositionPercent / 100f
    val hiFrac = (gradientPositionPercent + 20f) / 100f

    val fragStartFrac = startYOffset / fh
    val fragEndFrac = (startYOffset + h) / fh

    if (fragEndFrac <= loFrac) {
        drawText(layoutResult, color = bright, shadow = shadow, topLeft = topLeft); return
    }
    if (fragStartFrac >= hiFrac) {
        drawText(layoutResult, color = dim, shadow = shadow, topLeft = topLeft); return
    }

    val localLo = ((loFrac * fh) - startYOffset) / h
    val localHi = ((hiFrac * fh) - startYOffset) / h
    val bandW = (localHi - localLo).coerceAtLeast(1e-4f)
    fun colorAt(y: Float): Color =
        androidx.compose.ui.graphics.lerp(bright, dim, ((y - localLo) / bandW).coerceIn(0f, 1f))

    val s0 = localLo.coerceIn(0.0001f, 0.9997f)
    val s1 = localHi.coerceIn(s0 + 0.0001f, 0.9998f)
    val brush = Brush.verticalGradient(
        0f to colorAt(0f),
        s0 to colorAt(s0),
        s1 to colorAt(s1),
        1f to colorAt(1f),
        startY = 0f,
        endY = h,
    )
    drawText(layoutResult, brush = brush, shadow = shadow, topLeft = topLeft)
}

/**
 * Shadow used for an inactive line's distance blur, or null if not blurred. The reference
 * paints inactive text as its own text-shadow (NotSung at the dim alpha, Sung at the bright
 * alpha) whose blur radius is the distance-based --BlurAmount.
 */
private fun blurShadow(lineAnim: LineAnimState, stateAlpha: Float): Shadow? =
    if (lineAnim.blur > 0.1f)
        Shadow(
            color = Color.White.copy(alpha = (stateAlpha * lineAnim.opacity).coerceIn(0f, 1f)),
            blurRadius = lineAnim.blur,
        )
    else null

internal fun DrawScope.drawInterludeGroup(
    layout: LineLayout,
    lineAnim: LineAnimState,
    lineStartX: Float,
    scrollOffset: Float,
    dynamicY: Float,
) {
    val groupScale = lineAnim.scale.coerceIn(0f, 1f)
    if (groupScale < 0.01f) return

    val firstDot = layout.words.firstOrNull() ?: return
    val lastDot = layout.words.lastOrNull() ?: return
    val firstTextW = firstDot.textLayoutResult.size.width.toFloat()
    val lastTextW = lastDot.textLayoutResult.size.width.toFloat()

    val dotGroupCentreX = lineStartX + firstDot.relativeOffset.x + firstTextW / 2f +
        (lastDot.relativeOffset.x + lastTextW / 2f - firstDot.relativeOffset.x - firstTextW / 2f) / 2f
    val dotGroupCentreY = dynamicY + scrollOffset

    layout.words.forEachIndexed { dotIdx, wLayout ->
        val dotAnim = lineAnim.wordStates.getOrNull(dotIdx) ?: return@forEachIndexed
        val dotOpacity = dotAnim.glow.coerceIn(0f, 1f)

        val xPos = lineStartX + wLayout.relativeOffset.x
        val textW = wLayout.textLayoutResult.size.width.toFloat()
        val textH = wLayout.textLayoutResult.size.height.toFloat()
        val baseYPos = dotGroupCentreY - textH / 2f

        val dotPivotX = xPos + textW / 2f
        val dotPivotY = baseYPos + textH / 2f
        val dotYShift = dotAnim.yOffset * textH

        // Dot halo driven by its own glow spring: blur 4 + 6·glow, opacity glow·0.9.
        val dotGlow = dotAnim.dotGlow.coerceIn(0f, 1f)
        val dotGlowAlpha = (dotGlow * 0.9f).coerceIn(0f, 1f)
        val dotShadow = if (dotGlowAlpha > 0.02f) {
            Shadow(color = Color.White.copy(alpha = dotGlowAlpha * lineAnim.opacity), blurRadius = 4f + 6f * dotGlow)
        } else null

        withTransform({
            scale(groupScale, groupScale, Offset(dotGroupCentreX, dotGroupCentreY))
            scale(dotAnim.scale.coerceIn(0f, 1.5f), dotAnim.scale.coerceIn(0f, 1.5f), Offset(dotPivotX, dotPivotY))
            translate(top = dotYShift)
        }) {
            drawText(
                textLayoutResult = wLayout.textLayoutResult,
                color = Color.White,
                alpha = dotOpacity * lineAnim.opacity,
                shadow = dotShadow,
                topLeft = Offset(xPos, baseYPos),
            )
        }
    }
}

/** Word/syllable-synced karaoke line. */
internal fun DrawScope.drawStandardLine(
    layout: LineLayout,
    lineAnim: LineAnimState,
    lineStartX: Float,
    scrollOffset: Float,
    dynamicY: Float,
    config: RenderConfig,
) {
    val rtl = layout.isRtl
    layout.words.forEach { wLayout ->
        val wordAnim = lineAnim.wordStates.getOrNull(wLayout.sourceWordIndex) ?: return@forEach
        val xPos = lineStartX + wLayout.relativeOffset.x
        val yPos = dynamicY + wLayout.relativeOffset.y
        val textWidth = wLayout.textLayoutResult.size.width.toFloat()
        val textHeight = wLayout.textLayoutResult.size.height.toFloat()

        // Distance-blur silhouette alpha follows the word's state (NotSung dim / Sung bright).
        val stateAlpha = if (wordAnim.state == com.tx24.spicyplayer.lyrics.spicy.animation.ElementState.Sung)
            config.gradientAlphaBright else config.gradientAlphaDim
        val baseShadow = blurShadow(lineAnim, stateAlpha)

        if (wordAnim.isLetterGroup) {
            drawSyllabicLetterFragment(wLayout, wordAnim, lineAnim, xPos, yPos, textWidth, textHeight, scrollOffset, config, baseShadow, rtl)
        } else {
            drawStandardWord(wLayout, wordAnim, lineAnim, xPos, yPos, textWidth, textHeight, scrollOffset, config, baseShadow, rtl)
        }
    }
}

private fun DrawScope.drawSyllabicLetterFragment(
    wLayout: WordLayout,
    wordAnim: WordAnimState,
    lineAnim: LineAnimState,
    xPos: Float,
    yPos: Float,
    textWidth: Float,
    textHeight: Float,
    scrollOffset: Float,
    config: RenderConfig,
    baseShadow: Shadow?,
    rtl: Boolean,
) {
    val lState = wordAnim.letterStates.getOrNull(wLayout.charIndex) ?: return

    val sLYPos = yPos + scrollOffset
    val sPivotX = xPos + textWidth / 2f
    val sPivotY = sLYPos + textHeight / 2f
    // Letter yOffset applied ×2 (reference), on top of the word container's own transform.
    val lYShift = lState.yOffset * textHeight * 2f
    val containerYShift = wordAnim.yOffset * textHeight

    val lGlowBlur = 4f + 12f * lState.glow
    val lGlowOpacity = (lState.glow * 1.85f).coerceIn(0f, 1f)  // LetterGlowMultiplier_Opacity = 185%
    val lShadow = when {
        lGlowOpacity > 0.02f -> Shadow(color = Color.White.copy(alpha = lGlowOpacity * lineAnim.opacity), blurRadius = lGlowBlur)
        else -> baseShadow
    }

    val bright = config.gradientAlphaBright * lineAnim.opacity
    val dim = config.gradientAlphaDim * lineAnim.opacity

    withTransform({
        // The reference nests letter spans inside the word element: the word's own
        // scale/translate wraps every letter's individual scale/translate.
        scale(wordAnim.scale, wordAnim.scale, Offset(sPivotX, sPivotY))
        translate(top = containerYShift)
        scale(lState.scale, lState.scale, Offset(sPivotX, sPivotY))
        translate(top = lYShift)
    }) {
        drawWipeText(
            layoutResult = wLayout.textLayoutResult,
            xPos = xPos,
            yPos = yPos + scrollOffset,
            fragmentWidth = textWidth,
            fullWidth = textWidth,
            startXOffset = 0f,
            gradientPositionPercent = lState.gradientPosition,
            brightAlpha = bright,
            dimAlpha = dim,
            shadow = lShadow,
            rtl = rtl,
        )
    }
}

private fun DrawScope.drawStandardWord(
    wLayout: WordLayout,
    wordAnim: WordAnimState,
    lineAnim: LineAnimState,
    xPos: Float,
    yPos: Float,
    textWidth: Float,
    textHeight: Float,
    scrollOffset: Float,
    config: RenderConfig,
    baseShadow: Shadow?,
    rtl: Boolean,
) {
    val glowBlur = 4f + 2f * wordAnim.glow
    val glowOpacity = (wordAnim.glow * 0.35f).coerceIn(0f, 1f)
    val shadow = when {
        glowOpacity > 0.02f -> Shadow(color = Color.White.copy(alpha = glowOpacity * lineAnim.opacity), blurRadius = glowBlur)
        else -> baseShadow
    }

    val wordScale = wordAnim.scale
    val wordYShift = wordAnim.yOffset * textHeight
    val pivotX = xPos + textWidth / 2f
    val pivotY = yPos + textHeight / 2f

    val isBg = lineAnim.isBackground
    val bright = (if (isBg) 0.6f else config.gradientAlphaBright) * lineAnim.opacity
    val dim = (if (isBg) 0.3f else config.gradientAlphaDim) * lineAnim.opacity

    withTransform({
        translate(top = scrollOffset)
        scale(scaleX = wordScale, scaleY = wordScale, pivot = Offset(pivotX, pivotY))
        translate(top = wordYShift)
    }) {
        drawWipeText(
            layoutResult = wLayout.textLayoutResult,
            xPos = xPos,
            yPos = yPos,
            fragmentWidth = textWidth,
            fullWidth = wLayout.fullWordWidth,
            startXOffset = wLayout.startXOffset,
            gradientPositionPercent = wordAnim.gradientPosition,
            brightAlpha = bright,
            dimAlpha = dim,
            shadow = shadow,
            rtl = rtl,
        )
    }
}

/** Whole-line gradient sweep for [com.tx24.spicyplayer.lyrics.spicy.models.LyricsType.Line]. */
internal fun DrawScope.drawLineModeLine(
    layout: LineLayout,
    lineAnim: LineAnimState,
    lineStartX: Float,
    scrollOffset: Float,
    dynamicY: Float,
    config: RenderConfig,
) {
    val bright = config.gradientAlphaBright * lineAnim.opacity
    val dim = config.lineGradientAlphaDim * lineAnim.opacity

    // Whole-line glow spring (reference Line-mode: shadow blur 4 + 8·glow, alpha glow·0.5),
    // layered with the inactive-line distance blur when present.
    val glowAlpha = (lineAnim.lineGlow * 0.5f).coerceIn(0f, 1f)
    val lineStateAlpha = if (lineAnim.lineGradientPercent >= 100f)
        config.gradientAlphaBright else config.lineGradientAlphaDim
    val shadow = when {
        glowAlpha > 0.02f -> Shadow(
            color = Color.White.copy(alpha = glowAlpha * lineAnim.opacity),
            blurRadius = 4f + 8f * lineAnim.lineGlow,
        )
        else -> blurShadow(lineAnim, lineStateAlpha)
    }
    val lineWidth = layout.maxRowWidth.coerceAtLeast(1f)

    // Active Line-mode lines scale to 1.05 with transform-origin left-center
    // (right-center for duet/RTL lines), per the reference CSS.
    val pivot = Offset(
        if (layout.oppositeAligned || layout.isRtl) lineStartX + layout.totalWidth else lineStartX,
        dynamicY + scrollOffset + layout.height / 2f,
    )
    withTransform({
        scale(lineAnim.scale, lineAnim.scale, pivot)
    }) {
        layout.words.forEach { wLayout ->
            val xPos = lineStartX + wLayout.relativeOffset.x
            val yPos = dynamicY + wLayout.relativeOffset.y + scrollOffset
            val textWidth = wLayout.textLayoutResult.size.width.toFloat()
            val textHeight = wLayout.textLayoutResult.size.height.toFloat()
            if (layout.isRtl) {
                // RTL lines keep the horizontal right→left sweep (.line.rtl -90deg !important).
                drawWipeText(
                    layoutResult = wLayout.textLayoutResult,
                    xPos = xPos,
                    yPos = yPos,
                    fragmentWidth = textWidth,
                    fullWidth = lineWidth,
                    startXOffset = wLayout.relativeOffset.x,
                    gradientPositionPercent = lineAnim.lineGradientPercent,
                    brightAlpha = bright,
                    dimAlpha = dim,
                    shadow = shadow,
                    rtl = true,
                )
            } else {
                // Line-mode fills top→bottom across the line's full height (180deg on .line).
                drawVerticalWipeText(
                    layoutResult = wLayout.textLayoutResult,
                    xPos = xPos,
                    yPos = yPos,
                    fragmentHeight = textHeight,
                    fullHeight = layout.height.coerceAtLeast(1f),
                    startYOffset = wLayout.relativeOffset.y,
                    gradientPositionPercent = lineAnim.lineGradientPercent,
                    brightAlpha = bright,
                    dimAlpha = dim,
                    shadow = shadow,
                )
            }
        }
    }
}

/** Plain, non-interactive text for [com.tx24.spicyplayer.lyrics.spicy.models.LyricsType.Static]. */
internal fun DrawScope.drawStaticLine(
    layout: LineLayout,
    lineAnim: LineAnimState,
    lineStartX: Float,
    scrollOffset: Float,
    dynamicY: Float,
) {
    layout.words.forEach { wLayout ->
        val xPos = lineStartX + wLayout.relativeOffset.x
        val yPos = dynamicY + wLayout.relativeOffset.y + scrollOffset
        drawText(
            textLayoutResult = wLayout.textLayoutResult,
            color = Color.White,
            alpha = lineAnim.opacity.coerceIn(0f, 1f),
            topLeft = Offset(xPos, yPos),
        )
    }
}
