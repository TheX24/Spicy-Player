package com.tx24.spicyplayer.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import com.tx24.spicyplayer.R
import com.tx24.spicyplayer.models.Line
import com.tx24.spicyplayer.models.Word

internal object LyricsLayoutCalculator {

    private val spicyFontFamily = FontFamily(
        Font(R.font.lyrics_regular, FontWeight.Normal),
        Font(R.font.lyrics_medium, FontWeight.Medium),
        Font(R.font.lyrics_semibold, FontWeight.SemiBold),
        Font(R.font.lyrics_bold, FontWeight.Bold)
    )

    fun calculateLineLayouts(
        lines: List<Line>,
        canvasWidth: Float,
        textMeasurer: TextMeasurer
    ): List<LineLayout> {
        val layouts = mutableListOf<LineLayout>()
        var currentY = 0f
        val lineSpacing = 32f
        val horizontalPadding = 40f
        
        // Choose your desired main lyrics font weight right here:
        // Options: FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold
        val mainFontWeight = FontWeight.Bold
        
        val hasDuet = lines.any { it.oppositeAligned }
        val maxLineWidth = if (hasDuet) {
            (canvasWidth - (horizontalPadding * 2)) * 0.85f
        } else {
            canvasWidth - (horizontalPadding * 2)
        }
        
        val baseFontSize = (canvasWidth / 20f).coerceIn(16f, 32f).sp
        val bgFontSize = baseFontSize * 0.7f
        val songwriterFontSize = baseFontSize * 0.5f

        for (line in lines) {
            val isInterlude = line.isInterlude
            val isBg = line.isBackground
            val fontSize = if (line.isSongwriter) songwriterFontSize else if (isBg) bgFontSize else baseFontSize

            if (isInterlude) {
                // Instrumental interludes are rendered as three dots.
                // You can adjust the multiplier here to make the dots bigger or smaller:
                val dotFontSize = baseFontSize * 1.35f
                // Finish the sequence a little earlier than the true line.endMs (give it a 150ms breather)
                val effectiveDuration = maxOf(30L, line.duration - 250L)
                val dotLayouts = (0 until 3).map { dotIdx ->
                    val chunkDuration = effectiveDuration / 3L
                    val dotStart = line.startMs + dotIdx * chunkDuration
                    val dotEnd = dotStart + chunkDuration
                    val dotWord = Word("•", dotStart, dotEnd)
                    val result = textMeasurer.measure(
                        text = AnnotatedString("•"),
                        style = TextStyle(
                            fontFamily = spicyFontFamily,
                            fontSize = dotFontSize,
                            fontWeight = mainFontWeight,
                            color = Color.White,
                        )
                    )
                    val dotW = result.size.width.toFloat()
                    val dotGap = (0.007f * canvasWidth).coerceIn(8f, 40f)
                    WordLayout(dotWord, result, Offset(dotIdx * (dotW + dotGap), 0f))
                }
                val dotH = dotLayouts.maxOfOrNull { it.textLayoutResult.size.height.toFloat() } ?: 0f
                val totalDotsW = dotLayouts.lastOrNull()?.let { it.relativeOffset.x + it.textLayoutResult.size.width } ?: 0f
                
                layouts.add(LineLayout(line, dotLayouts, currentY, dotH, totalDotsW, totalDotsW, true, isBg, line.oppositeAligned, false))
                currentY += 0f // Interludes collapse when not active.
                continue
            }

            // Standard lyric line layout.
            val spaceWidth = textMeasurer.measure(
                text = AnnotatedString(" "),
                style = TextStyle(
                    fontFamily = spicyFontFamily,
                    fontSize = fontSize,
                    fontWeight = mainFontWeight,
                    color = Color.White,
                )
            ).size.width.toFloat()
            val wordGap = spaceWidth

            val measuredWords = line.words.mapIndexed { wIdx, word ->
                val result = textMeasurer.measure(
                    text = AnnotatedString(word.text),
                    style = TextStyle(
                        fontFamily = spicyFontFamily,
                        fontSize = fontSize,
                        fontWeight = if (line.isSongwriter) FontWeight.Normal else mainFontWeight,
                        color = Color.White,
                        letterSpacing = (wIdx * 0.001f).sp
                    )
                )
                val characterLayouts = if (word.isLetterGroup) {
                    word.text.mapIndexed { idx, _ ->
                        val charStyle = TextStyle(
                            fontFamily = spicyFontFamily,
                            fontSize = fontSize,
                            fontWeight = if (line.isSongwriter) FontWeight.Normal else mainFontWeight,
                            color = Color.Transparent,
                            letterSpacing = (wIdx * 0.001f).sp
                        )
                        val textWithOneCharVisible = AnnotatedString.Builder().apply {
                            withStyle(SpanStyle(color = Color.Transparent)) {
                                append(word.text.substring(0, idx))
                            }
                            withStyle(SpanStyle(color = Color.White)) {
                                append(word.text[idx])
                            }
                            withStyle(SpanStyle(color = Color.Transparent)) {
                                append(word.text.substring(idx + 1))
                            }
                        }.toAnnotatedString()

                        textMeasurer.measure(
                            text = textWithOneCharVisible,
                            style = charStyle
                        )
                    }
                } else emptyList()
                Triple(word, result, characterLayouts)
            }

            // Word-wrapping logic.
            val isSongwriterLine = line.isSongwriter
            var R = 1
            var currentW = 0f
            val lineMaxWidth = if (isSongwriterLine) canvasWidth - (horizontalPadding * 2) else maxLineWidth
            
            for (i in measuredWords.indices) {
                val w = measuredWords[i].second.size.width.toFloat()
                val hspace = if (currentW > 0f && !measuredWords[i].first.isPartOfWord) wordGap else 0f
                if (currentW + hspace + w > lineMaxWidth && currentW > 0f) {
                    R++
                    currentW = w
                } else {
                    currentW += hspace + w
                }
            }

            val numWords = measuredWords.size
            val lineBreaks = mutableListOf<Int>()
            
            if (!hasDuet || isSongwriterLine) {
                // Greedy wrap.
                var currentLineW = 0f
                var lastBreakCandidate = 0
                lineBreaks.add(0)
                
                var i = 0
                while (i < numWords) {
                    val wordW = measuredWords[i].second.size.width.toFloat()
                    val hspace = if (currentLineW > 0f && !measuredWords[i].first.isPartOfWord) wordGap else 0f
                    
                    val isForcedSyllable = i > 0 && measuredWords[i].first.isPartOfWord && !measuredWords[i - 1].first.text.endsWith("-")
                    if (!isForcedSyllable) {
                        lastBreakCandidate = i
                    }
                    
                    if (currentLineW + hspace + wordW > lineMaxWidth && currentLineW > 0f) {
                        val breakIdx = if (lastBreakCandidate > lineBreaks.last()) lastBreakCandidate else i
                        lineBreaks.add(breakIdx)
                        i = breakIdx
                        currentLineW = 0f
                    } else {
                        currentLineW += hspace + wordW
                        i++
                    }
                }
                lineBreaks.add(numWords)
            } else {
                // Balanced wrap using dynamic programming.
                val dp = IntArray(numWords + 1) { Int.MAX_VALUE / 2 }
                val breaks = IntArray(numWords + 1)
                dp[0] = 0

                val targetWordCount = numWords.toFloat() / R

                for (i in 1..numWords) {
                    var w = 0f
                    var j = i - 1
                    while (j >= 0) {
                        val wordW = measuredWords[j].second.size.width.toFloat()
                        val hspace = if (j < i - 1 && !measuredWords[j + 1].first.isPartOfWord) wordGap else 0f
                        w += wordW + hspace
                        if (w > lineMaxWidth && i - j > 1) {
                            break
                        }
                        
                        val isForcedSyllableBreak = j > 0 && measuredWords[j].first.isPartOfWord && !measuredWords[j - 1].first.text.endsWith("-")
                        
                        val wordsInLine = i - j
                        val variancePenalty = kotlin.math.abs(wordsInLine - targetWordCount)
                        val penalty = if (isForcedSyllableBreak) {
                            Int.MAX_VALUE / 2
                        } else {
                            (variancePenalty * 1000f).toInt() + (lineMaxWidth - w).toInt()
                        }

                        if (dp[j] + penalty < dp[i]) {
                            dp[i] = dp[j] + penalty
                            breaks[i] = j
                        }
                        j--
                    }
                }

                var curr = numWords
                while (curr > 0) {
                    lineBreaks.add(0, curr)
                    curr = breaks[curr]
                }
                lineBreaks.add(0, 0)
            }

            // Assemble WordLayouts into rows.
            val wordLayouts = mutableListOf<WordLayout>()
            var rowY = 0f
            var maxRowWidth = 0f
            var rowHeight = 0f
            val rowWidths = mutableMapOf<Float, Float>()

            for (b in 0 until lineBreaks.size - 1) {
                val startIdx = lineBreaks[b]
                val endIdx = lineBreaks[b+1]
                var rowX = 0f
                rowHeight = 0f
                for (idx in startIdx until endIdx) {
                    val (word, result, charLayouts) = measuredWords[idx]
                    val wordWidth = result.size.width.toFloat()
                    val actualGap = if (rowX > 0f && !word.isPartOfWord) wordGap else 0f
                    wordLayouts.add(WordLayout(word, result, Offset(rowX + actualGap, rowY), charLayouts))
                    rowX += wordWidth + actualGap
                    rowHeight = maxOf(rowHeight, result.size.height.toFloat())
                }
                rowWidths[rowY] = rowX
                maxRowWidth = maxOf(maxRowWidth, rowX)
                if (b < lineBreaks.size - 2) {
                    rowY += rowHeight
                }
            }
            
            // Handle right-alignment for duet parts.
            val isRightAligned = hasDuet && !line.oppositeAligned && !line.isSongwriter
            if (isRightAligned) {
                for (j in wordLayouts.indices) {
                    val wLayout = wordLayouts[j]
                    val rWidth = rowWidths[wLayout.relativeOffset.y] ?: maxRowWidth
                    val alignmentShift = maxRowWidth - rWidth
                    wordLayouts[j] = wLayout.copy(relativeOffset = Offset(wLayout.relativeOffset.x + alignmentShift, wLayout.relativeOffset.y))
                }
            }

            val totalHeight = rowY + rowHeight
            val totalWidth = maxRowWidth
            
            val drawY = if (isBg) currentY - 32f else if (line.isSongwriter) currentY + lineSpacing * 0.5f else currentY

            layouts.add(LineLayout(line, wordLayouts, drawY, totalHeight, totalWidth, maxRowWidth, false, isBg, line.oppositeAligned, line.isSongwriter))
            
            val bottomY = drawY + totalHeight
            currentY = maxOf(currentY, bottomY + (if (isBg) 32f else lineSpacing))
        }
        return layouts
    }
}
