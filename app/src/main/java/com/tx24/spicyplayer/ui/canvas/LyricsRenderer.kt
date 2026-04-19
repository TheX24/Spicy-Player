package com.tx24.spicyplayer.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.drawText
import com.tx24.spicyplayer.animation.LineAnimState
import com.tx24.spicyplayer.animation.LyricsAnimator

internal fun DrawScope.drawInterludeGroup(
    layout: LineLayout,
    lineAnim: LineAnimState,
    lineStartX: Float,
    scrollOffset: Float,
    dynamicY: Float
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

        val dotGlowAlpha = (dotOpacity * 0.85f).coerceIn(0f, 1f)
        val dotShadow = if (dotGlowAlpha > 0.02f) {
            Shadow(
                color = Color.White.copy(alpha = dotGlowAlpha * lineAnim.opacity),
                blurRadius = 12f
            )
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

internal fun DrawScope.drawStandardLine(
    layout: LineLayout,
    lineAnim: LineAnimState,
    lineStartX: Float,
    scrollOffset: Float,
    dynamicY: Float
) {
    layout.words.forEachIndexed { _, wLayout ->
        val wordAnim = lineAnim.wordStates.getOrNull(wLayout.sourceWordIndex) ?: return@forEachIndexed

        val xPos = lineStartX + wLayout.relativeOffset.x
        val yPos = dynamicY + wLayout.relativeOffset.y

        val textWidth = wLayout.textLayoutResult.size.width.toFloat()
        val textHeight = wLayout.textLayoutResult.size.height.toFloat()

        val baseBright = LyricsAnimator.GRADIENT_ALPHA_BRIGHT
        val baseDim = LyricsAnimator.GRADIENT_ALPHA_DIM

        if (wordAnim.isLetterGroup) {
            drawSyllabicLetterFragment(wLayout, wordAnim, lineAnim, xPos, yPos, textWidth, textHeight, scrollOffset, baseBright, baseDim)
        } else {
            drawStandardWord(wLayout, wordAnim, lineAnim, xPos, yPos, textWidth, textHeight, scrollOffset, baseBright, baseDim)
        }
    }
}

private fun DrawScope.drawSyllabicLetterFragment(
    wLayout: WordLayout,
    wordAnim: com.tx24.spicyplayer.animation.WordAnimState,
    lineAnim: LineAnimState,
    xPos: Float,
    yPos: Float,
    textWidth: Float,
    textHeight: Float,
    scrollOffset: Float,
    baseBright: Float,
    baseDim: Float
) {
    val lState = wordAnim.letterStates.getOrNull(wLayout.charIndex) ?: return
    
    val sLYPos = yPos + scrollOffset
    val sPivotX = xPos + textWidth / 2f
    val sPivotY = sLYPos + textHeight / 2f
    val lYShift = lState.yOffset * textHeight * 2f

    val lGlowBlur = 4f + 12f * lState.glow
    val lGlowOpacity = (lState.glow * LyricsAnimator.LETTER_GLOW_MULTIPLIER_OPACITY).coerceIn(0f, 1f)
    val lShadow = if (lGlowOpacity > 0.02f) Shadow(
        color = Color.White.copy(alpha = lGlowOpacity * lineAnim.opacity),
        blurRadius = lGlowBlur,
    ) else null

    val brightAlpha = (baseDim + (baseBright - baseDim) * wordAnim.activeAnimFactor) * lineAnim.opacity
    val dimAlpha = baseDim * lineAnim.opacity
    
    // Even sharper fade-in for individual letters/chars (2 units instead of 20)
    val gradientFraction = ((lState.gradientPosition + 10f) / 60f).coerceIn(0f, 1f)

    withTransform({
        scale(lState.scale, lState.scale, Offset(sPivotX, sPivotY))
        translate(top = lYShift)
    }) {
        // Base layer
        drawText(
            textLayoutResult = wLayout.textLayoutResult,
            color = Color.White,
            alpha = dimAlpha.coerceIn(0f, 1f),
            topLeft = Offset(xPos, yPos + scrollOffset),
        )

        // Highlight layer
        if (brightAlpha > dimAlpha + 0.01f && gradientFraction > 0.001f) {
            val overlayAlpha = if (dimAlpha < 1f) {
                ((brightAlpha - dimAlpha) / (1f - dimAlpha)).coerceIn(0f, 1f)
            } else 0f

            val finalAlpha = (overlayAlpha * gradientFraction).coerceIn(0f, 1f)
            drawText(
                textLayoutResult = wLayout.textLayoutResult,
                alpha = finalAlpha,
                shadow = lShadow,
                topLeft = Offset(xPos, yPos + scrollOffset),
            )
        }
    }
}

private fun DrawScope.drawStandardWord(
    wLayout: WordLayout,
    wordAnim: com.tx24.spicyplayer.animation.WordAnimState,
    lineAnim: LineAnimState,
    xPos: Float,
    yPos: Float,
    textWidth: Float,
    textHeight: Float,
    scrollOffset: Float,
    baseBright: Float,
    baseDim: Float
) {
    val glowBlur = 4f + 2f * wordAnim.glow
    val glowOpacity = (wordAnim.glow * 0.35f).coerceIn(0f, 1f)
    val shadow = if (glowOpacity > 0.02f) {
        Shadow(
            color = Color.White.copy(alpha = glowOpacity * lineAnim.opacity),
            blurRadius = glowBlur,
        )
    } else null

    val wordScale = wordAnim.scale
    val wordYShift = wordAnim.yOffset * textHeight
    val pivotX = xPos + textWidth / 2f
    val pivotY = yPos + textHeight / 2f

    withTransform({
        translate(top = scrollOffset)
        scale(scaleX = wordScale, scaleY = wordScale, pivot = Offset(pivotX, pivotY))
        translate(top = wordYShift)
    }) {
        val brightAlpha = (baseDim + (baseBright - baseDim) * wordAnim.activeAnimFactor) * lineAnim.opacity
        val dimAlpha = baseDim * lineAnim.opacity

        drawText(
            textLayoutResult = wLayout.textLayoutResult,
            color = Color.White,
            alpha = dimAlpha.coerceIn(0f, 1f),
            topLeft = Offset(xPos, yPos),
        )

        if (brightAlpha > dimAlpha + 0.01f && wordAnim.activeAnimFactor > 0.001f) {
            val edgeSoftnessInPixels = textHeight * 0.25f 
            
            // Map the 0..1 gradientFraction to an expanded range [-margin, textWidth + margin]
            // so the softness feathering clears the edges at 0% and 100%.
            val margin = edgeSoftnessInPixels 
            val wipeProgress = ((wordAnim.gradientPosition + 20f) / 120f).coerceIn(0f, 1f)
            
            val fullWordXWithMargins = (wLayout.fullWordWidth + 2 * margin) * wipeProgress - margin
            val fragmentXHighlight = fullWordXWithMargins - wLayout.startXOffset
            
            val startFade = fragmentXHighlight - edgeSoftnessInPixels / 2f
            val endFade = fragmentXHighlight + edgeSoftnessInPixels / 2f
            
            val highlightColor = Color.White
            val gradientBrush = Brush.horizontalGradient(
                0f to highlightColor,
                (maxOf(0f, startFade) / textWidth).coerceIn(0f, 1f) to highlightColor,
                (minOf(textWidth, endFade) / textWidth).coerceIn(0f, 1f) to Color.Transparent,
                1f to Color.Transparent,
                startX = 0f,
                endX = textWidth
            )

            val overlayAlpha = if (dimAlpha < 1f) {
                ((brightAlpha - dimAlpha) / (1f - dimAlpha)).coerceIn(0f, 1f)
            } else 0f

            drawText(
                textLayoutResult = wLayout.textLayoutResult,
                brush = gradientBrush,
                alpha = overlayAlpha,
                shadow = shadow,
                topLeft = Offset(xPos, yPos),
            )
        }
    }
}
