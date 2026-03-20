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
    layout.words.forEachIndexed { wordIdx, wLayout ->
        val wordAnim = lineAnim.wordStates.getOrNull(wordIdx) ?: return@forEachIndexed

        val xPos = lineStartX + wLayout.relativeOffset.x
        val yPos = dynamicY + wLayout.relativeOffset.y

        val textWidth = wLayout.textLayoutResult.size.width.toFloat()
        val textHeight = wLayout.textLayoutResult.size.height.toFloat()

        val baseBright = LyricsAnimator.GRADIENT_ALPHA_BRIGHT
        val baseDim = LyricsAnimator.GRADIENT_ALPHA_DIM

        if (wordAnim.isLetterGroup && wordAnim.letterStates.size == wLayout.word.text.length) {
            drawSyllabicLetterGroup(wLayout, wordAnim, lineAnim, xPos, yPos, textHeight, scrollOffset, baseBright, baseDim)
        } else {
            drawStandardWord(wLayout, wordAnim, lineAnim, xPos, yPos, textWidth, textHeight, scrollOffset, baseBright, baseDim)
        }
    }
}

private fun DrawScope.drawSyllabicLetterGroup(
    wLayout: WordLayout,
    wordAnim: com.tx24.spicyplayer.animation.WordAnimState,
    lineAnim: LineAnimState,
    xPos: Float,
    yPos: Float,
    textHeight: Float,
    scrollOffset: Float,
    baseBright: Float,
    baseDim: Float
) {
    val textLen = wLayout.word.text.length
    for (li in 0 until textLen) {
        val lState = wordAnim.letterStates[li]
        val rect = wLayout.textLayoutResult.getBoundingBox(li)

        val lXPos = xPos + rect.left
        val lYPos = yPos + rect.top
        val lWidth = rect.width
        val lHeight = rect.height

        val sLYPos = lYPos + scrollOffset
        val sPivotX = lXPos + lWidth / 2f
        val sPivotY = sLYPos + lHeight / 2f
        val lYShift = lState.yOffset * textHeight * 2f

        val lGlowBlur = 4f + 12f * lState.glow
        val lGlowOpacity = (lState.glow * LyricsAnimator.LETTER_GLOW_MULTIPLIER_OPACITY).coerceIn(0f, 1f)
        val lShadow = if (lGlowOpacity > 0.02f) Shadow(
            color = Color.White.copy(alpha = lGlowOpacity * lineAnim.opacity),
            blurRadius = lGlowBlur,
        ) else null

        val brightAlpha = (baseDim + (baseBright - baseDim) * wordAnim.activeAnimFactor) * lineAnim.opacity
        val dimAlpha = baseDim * lineAnim.opacity
        val gradientFraction = ((lState.gradientPosition + 20f) / 120f).coerceIn(0f, 1f)

        withTransform({
            scale(lState.scale, lState.scale, Offset(sPivotX, sPivotY))
            translate(top = lYShift)
        }) {
            // Clipped base layer
            clipRect(
                left = lXPos - 2f,
                top = sLYPos - lHeight * 0.75f,
                right = lXPos + lWidth + 2f,
                bottom = sLYPos + lHeight * 1.75f,
            ) {
                drawText(
                    textLayoutResult = wLayout.textLayoutResult,
                    color = Color.White,
                    alpha = dimAlpha.coerceIn(0f, 1f),
                    topLeft = Offset(xPos, yPos + scrollOffset),
                )
            }

            // Unclipped (or widely clipped) highlight layer for glowing shadow
            if (brightAlpha > dimAlpha + 0.01f && gradientFraction > 0.001f) {
                val charResult = wLayout.characterLayouts.getOrNull(li)
                val edgeSoftness = 0.15f
                val startFade = maxOf(0f, gradientFraction - edgeSoftness / 2f)
                val endFade = minOf(1f, gradientFraction + edgeSoftness / 2f)
                val highlightColor = Color.White
                
                val overlayAlpha = if (dimAlpha < 1f) {
                    ((brightAlpha - dimAlpha) / (1f - dimAlpha)).coerceIn(0f, 1f)
                } else 0f

                    if (charResult != null) {
                        val charHighlightBrush = Brush.horizontalGradient(
                            0f to highlightColor,
                            startFade to highlightColor,
                            endFade to Color.Transparent,
                            1f to Color.Transparent,
                            startX = rect.left,
                            endX = rect.right
                        )

                        drawText(
                            textLayoutResult = charResult,
                            brush = charHighlightBrush,
                            alpha = overlayAlpha,
                            shadow = lShadow,
                            topLeft = Offset(xPos, yPos + scrollOffset),
                        )
                    }
            }
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
        val gradientFraction = ((wordAnim.gradientPosition + 20f) / 120f).coerceIn(0f, 1f)
        val clipX = xPos + textWidth * gradientFraction

        val brightAlpha = (baseDim + (baseBright - baseDim) * wordAnim.activeAnimFactor) * lineAnim.opacity
        val dimAlpha = baseDim * lineAnim.opacity

        drawText(
            textLayoutResult = wLayout.textLayoutResult,
            color = Color.White,
            alpha = dimAlpha.coerceIn(0f, 1f),
            topLeft = Offset(xPos, yPos),
        )

        if (brightAlpha > dimAlpha + 0.01f && gradientFraction > 0.001f) {
            val edgeSoftness = 0.15f
            val startFade = maxOf(0f, gradientFraction - edgeSoftness / 2f)
            val endFade = minOf(1f, gradientFraction + edgeSoftness / 2f)
            
            val highlightColor = Color.White
            val gradientBrush = Brush.horizontalGradient(
                0f to highlightColor,
                startFade to highlightColor,
                endFade to Color.Transparent,
                1f to Color.Transparent,
                startX = 0f,
                endX = textWidth
            )

            val overlayAlpha = if (dimAlpha < 1f) {
                ((brightAlpha - dimAlpha) / (1f - dimAlpha)).coerceIn(0f, 1f)
            } else 0f

            // No clipRect needed since the brush handles horizontal masking beautifully at 0f locally
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
