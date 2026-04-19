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

    private fun isCjk(c: Char): Boolean {
        val block = Character.UnicodeBlock.of(c)
        return block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
            block == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
            block == Character.UnicodeBlock.HIRAGANA ||
            block == Character.UnicodeBlock.KATAKANA ||
            block == Character.UnicodeBlock.HANGUL_SYLLABLES ||
            block == Character.UnicodeBlock.HANGUL_JAMO ||
            block == Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
    }

    fun calculateLineLayouts(
        lines: List<Line>,
        canvasWidth: Float,
        textMeasurer: TextMeasurer,
        fontSizeScale: Float = 1.0f,
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
        
        val baseFontSize = (canvasWidth / 20f).coerceIn(16f, 32f).sp * fontSizeScale
        val bgFontSize = baseFontSize * 0.7f
        val songwriterFontSize = baseFontSize * 0.5f

        for (line in lines) {
            val isInterlude = line.isInterlude
            val isBg = line.isBackground
            val fontSize = if (line.isSongwriter) songwriterFontSize else if (isBg) bgFontSize else baseFontSize

            if (isInterlude) {
                // Instrumental interludes are rendered as three dots.
                // You can adjust the multiplier here to make the dots bigger or smaller:
                val dotFontSize = baseFontSize * 1.3f // FIXED: dots=1.3x font
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
                    val dotGap = 4f // FIXED: tight gaps
                    WordLayout(dotWord, result, Offset(dotIdx * (dotW + dotGap), 0f))
                }
                val dotH = dotLayouts.maxOfOrNull { it.textLayoutResult.size.height.toFloat() } ?: 0f
                val totalDotsW = dotLayouts.lastOrNull()?.let { it.relativeOffset.x + it.textLayoutResult.size.width } ?: 0f

                // Inherit alignment from the next non-interlude, non-background line.
                val nextLineAlignment = lines
                    .firstOrNull { it.startMs > line.startMs && !it.isInterlude && !it.isBackground && !it.isSongwriter }
                    ?.oppositeAligned ?: false

                layouts.add(LineLayout(line, dotLayouts, currentY, dotH, totalDotsW, totalDotsW, true, isBg, nextLineAlignment, false))
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

            data class Piece(
                val word: Word,
                val text: String,
                val layout: androidx.compose.ui.text.TextLayoutResult,
                val sourceIdx: Int,
                val charIdx: Int,
                val fullWidth: Float,
                val startX: Float,
                val isCjkPiece: Boolean,
                val isPartOfWord: Boolean
            )

            val pieces = mutableListOf<Piece>()
            for (wIdx in line.words.indices) {
                val word = line.words[wIdx]
                val style = TextStyle(
                    fontFamily = spicyFontFamily,
                    fontSize = fontSize,
                    fontWeight = if (line.isSongwriter) FontWeight.Normal else mainFontWeight,
                    color = Color.White,
                    // Use a tiny unique letter spacing based on the Word object's identity.
                    // This prevents Compose from sharing cached TextLayoutResults (and highlights) 
                    // between identical words in different lines.
                    letterSpacing = (System.identityHashCode(word) % 1000 * 0.0000001f).sp
                )
                
                val fullResult = textMeasurer.measure(word.text, style)
                val fullW = fullResult.size.width.toFloat()
                
                if (word.text.any { isCjk(it) } || word.isLetterGroup) {
                    var currentX = 0f
                    for (charIdx in word.text.indices) {
                        val charText = word.text[charIdx].toString()
                        val charResult = textMeasurer.measure(charText, style)
                        pieces.add(Piece(
                            word = word,
                            text = charText,
                            layout = charResult,
                            sourceIdx = wIdx,
                            charIdx = charIdx,
                            fullWidth = fullW,
                            startX = currentX,
                            isCjkPiece = isCjk(word.text[charIdx]),
                            isPartOfWord = charIdx > 0 || word.isPartOfWord
                        ))
                        currentX += charResult.size.width
                    }
                } else {
                    pieces.add(Piece(
                        word = word,
                        text = word.text,
                        layout = fullResult,
                        sourceIdx = wIdx,
                        charIdx = 0,
                        fullWidth = fullW,
                        startX = 0f,
                        isCjkPiece = false,
                        isPartOfWord = word.isPartOfWord
                    ))
                }
            }

            // Word-wrapping logic on pieces.
            val isSongwriterLine = line.isSongwriter
            var R = 1
            var currentW = 0f
            val lineMaxWidth = if (isSongwriterLine) canvasWidth - (horizontalPadding * 2) else maxLineWidth
            
            for (i in pieces.indices) {
                val w = pieces[i].layout.size.width.toFloat()
                val hspace = if (currentW > 0f && !pieces[i].isPartOfWord) wordGap else 0f
                if (currentW + hspace + w > lineMaxWidth && currentW > 0f) {
                    R++
                    currentW = w
                } else {
                    currentW += hspace + w
                }
            }

            val numPieces = pieces.size
            val lineBreaks = mutableListOf<Int>()
            
            if (!hasDuet || isSongwriterLine) {
                // Greedy wrap.
                var currentLineW = 0f
                var lastBreakCandidate = 0
                lineBreaks.add(0)
                
                var i = 0
                while (i < numPieces) {
                    val piece = pieces[i]
                    val prevPiece = if (i > 0) pieces[i - 1] else null
                    val wordW = piece.layout.size.width.toFloat()
                    val hspace = if (currentLineW > 0f && !piece.isPartOfWord) wordGap else 0f
                    
                    val isCjkBoundary = if (i > 0 && prevPiece != null) {
                        (isCjk(prevPiece.text.lastOrNull() ?: ' ')) || (isCjk(piece.text.firstOrNull() ?: ' '))
                    } else false

                    val isForcedSyllable = i > 0 && piece.isPartOfWord && 
                                           !(prevPiece?.text?.endsWith("-") == true) && !isCjkBoundary
                                           
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
                lineBreaks.add(numPieces)
            } else {
                // Balanced wrap using dynamic programming.
                val dp = IntArray(numPieces + 1) { Int.MAX_VALUE / 2 }
                val breaks = IntArray(numPieces + 1)
                dp[0] = 0

                val targetWordCount = numPieces.toFloat() / R

                for (i in 1..numPieces) {
                    var w = 0f
                    var j = i - 1
                    while (j >= 0) {
                        val piece = pieces[j]
                        val nextPiece = if (j < numPieces - 1) pieces[j + 1] else null
                        val wordW = piece.layout.size.width.toFloat()
                        val hspace = if (j < i - 1 && nextPiece != null && !nextPiece.isPartOfWord) wordGap else 0f
                        w += wordW + hspace
                        if (w > lineMaxWidth && i - j > 1) {
                            break
                        }
                        
                        val isCjkBoundary = if (j > 0) {
                            val curP = piece
                            val prevP = pieces[j - 1]
                            (isCjk(prevP.text.lastOrNull() ?: ' ')) || (isCjk(curP.text.firstOrNull() ?: ' '))
                        } else false

                        val isForcedSyllableBreak = j > 0 && piece.isPartOfWord && 
                                                    !(pieces[j-1].text.endsWith("-")) && !isCjkBoundary
                        
                        val piecesInLine = i - j
                        val variancePenalty = kotlin.math.abs(piecesInLine - targetWordCount)
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

                var curr = numPieces
                while (curr > 0) {
                    lineBreaks.add(0, curr)
                    curr = breaks[curr]
                }
                lineBreaks.add(0, 0)
            }


            // Assemble WordLayouts into rows.
            val allRows = mutableListOf<Pair<Float, List<WordLayout>>>()
            var currentRowY = 0f
            var maxRowWidth = 0f
            var lastRowHeight = 0f

            for (b in 0 until lineBreaks.size - 1) {
                val startIdx = lineBreaks[b]
                val endIdx = lineBreaks[b+1]
                
                var maxBaseline = 0f
                for (idx in startIdx until endIdx) {
                    val piece = pieces[idx]
                    val baseline = piece.layout.firstBaseline
                    if (!baseline.isNaN()) {
                        maxBaseline = kotlin.math.max(maxBaseline, baseline)
                    }
                }

                val rowPieces = mutableListOf<WordLayout>()
                var rowMaxBottom = 0f
                var rowX = 0f
                for (idx in startIdx until endIdx) {
                    val piece = pieces[idx]
                    val pieceWidth = piece.layout.size.width.toFloat()
                    val actualGap = if (rowX > 0f && !piece.isPartOfWord) wordGap else 0f
                    
                    val baseline = piece.layout.firstBaseline
                    val yShift = if (!baseline.isNaN() && maxBaseline > 0f) maxBaseline - baseline else 0f
                    
                    rowPieces.add(WordLayout(
                        word = piece.word,
                        textLayoutResult = piece.layout,
                        relativeOffset = Offset(rowX + actualGap, currentRowY + yShift),
                        sourceWordIndex = piece.sourceIdx,
                        charIndex = piece.charIdx,
                        fullWordWidth = piece.fullWidth,
                        startXOffset = piece.startX
                    ))
                    rowX += pieceWidth + actualGap
                    rowMaxBottom = kotlin.math.max(rowMaxBottom, yShift + piece.layout.size.height.toFloat())
                }
                
                allRows.add(rowX to rowPieces)
                maxRowWidth = maxOf(maxRowWidth, rowX)
                lastRowHeight = rowMaxBottom
                
                if (b < lineBreaks.size - 2) {
                    currentRowY += lastRowHeight
                }
            }
            
            val totalHeight = currentRowY + lastRowHeight
            val totalWidth = maxRowWidth

            // Apply alignment and flatten.
            val isRightAligned = hasDuet && line.oppositeAligned && !line.isSongwriter
            val wordLayouts = mutableListOf<WordLayout>()
            for ((rWidth, rowPieces) in allRows) {
                val alignmentShift = if (isRightAligned) maxRowWidth - rWidth else 0f
                for (wLayout in rowPieces) {
                    wordLayouts.add(wLayout.copy(
                        relativeOffset = Offset(wLayout.relativeOffset.x + alignmentShift, wLayout.relativeOffset.y)
                    ))
                }
            }
            
            val prevIsInterlude = layouts.lastOrNull()?.isInterlude ?: false
            val drawY = if (isBg && !prevIsInterlude) currentY - 32f else if (line.isSongwriter) currentY + lineSpacing * 0.5f else currentY

            layouts.add(LineLayout(line, wordLayouts, drawY, totalHeight, totalWidth, maxRowWidth, false, isBg, line.oppositeAligned, line.isSongwriter))
            
            val bottomY = drawY + totalHeight
            currentY = maxOf(currentY, bottomY + (if (isBg) 32f else lineSpacing))
        }
        return layouts
    }
}
