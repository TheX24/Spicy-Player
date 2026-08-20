package com.tx24.spicyplayer.lyrics.spicy.canvas

import com.tx24.spicyplayer.R

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
import com.tx24.spicyplayer.lyrics.spicy.models.Line
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsType
import com.tx24.spicyplayer.lyrics.spicy.models.Word
import com.tx24.spicyplayer.lyrics.spicy.parser.RtlDetector

internal object LyricsLayoutCalculator {

    private val spicyFontFamily = FontFamily(
        Font(R.font.lyrics_regular, FontWeight.Normal),
        Font(R.font.lyrics_medium, FontWeight.Medium),
        Font(R.font.lyrics_semibold, FontWeight.SemiBold),
        Font(R.font.lyrics_bold, FontWeight.Bold)
    )
    private val vazirmatnFontFamily = FontFamily(
        Font(R.font.vazirmatn_variable, FontWeight.Normal),
        Font(R.font.vazirmatn_variable, FontWeight.Medium),
        Font(R.font.vazirmatn_variable, FontWeight.SemiBold),
        Font(R.font.vazirmatn_variable, FontWeight.Bold),
    )
    private val georgianFontFamily = FontFamily(
        Font(R.font.noto_sans_georgian_variable, FontWeight.Normal),
        Font(R.font.noto_sans_georgian_variable, FontWeight.Medium),
        Font(R.font.noto_sans_georgian_variable, FontWeight.SemiBold),
        Font(R.font.noto_sans_georgian_variable, FontWeight.Bold),
    )

    private fun fontFamilyFor(text: String): FontFamily = when (ScriptFontSelector.select(text)) {
        LyricScriptFont.DEFAULT -> spicyFontFamily
        LyricScriptFont.VAZIRMATN -> vazirmatnFontFamily
        LyricScriptFont.NOTO_SANS_GEORGIAN -> georgianFontFamily
    }

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

    /** The string to render for a word: its romanization when [romanize] is on and available. */
    private fun displayText(word: Word, romanize: Boolean): String =
        if (romanize) (word.romanizedText ?: word.text) else word.text

    /**
     * Whether a line's block should sit on the right edge of the lyrics column.
     *
     * Plain lines: right-aligned only if RTL. Duet lines normally put the primary voice (v1,
     * `!oppositeAligned`) on the left and the guest on the right — but the reference CSS swaps
     * that for RTL duets (`.line.rtl.OppositeAligned` gets the padding a plain `.line.rtl` would,
     * and vice versa), so an RTL duet mirrors instead of stacking both voices on the same side.
     */
    private fun resolveRightAligned(hasDuet: Boolean, isRtl: Boolean, oppositeAligned: Boolean, isSongwriter: Boolean): Boolean {
        if (isSongwriter) return false
        if (!hasDuet) return isRtl
        return if (isRtl) !oppositeAligned else oppositeAligned
    }

    fun calculateLineLayouts(
        lines: List<Line>,
        canvasWidth: Float,
        textMeasurer: TextMeasurer,
        density: Float,
        lyricsType: LyricsType,
        fontSizeScale: Float = 1.0f,
        romanize: Boolean = false,
    ): List<LineLayout> {

        val layouts = mutableListOf<LineLayout>()
        var currentY = 0f
        val metrics = LyricsLayoutMetrics(canvasWidth, density, lyricsType, fontSizeScale)
        val lineSpacing = metrics.lineGapPx
        
        val hasDuet = lines.any { it.oppositeAligned }
        val baseFontSize = metrics.baseFontSizeSp.sp
        val bgFontSize = baseFontSize * 0.75f

        for (line in lines) {
            val isInterlude = line.isInterlude
            val isBg = line.isBackground
            val fontSize = if (isBg) bgFontSize else baseFontSize
            val fontWeight = when {
                lyricsType == LyricsType.Static -> FontWeight.Medium
                isBg -> FontWeight.SemiBold
                else -> FontWeight.Bold
            }

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
                            fontWeight = fontWeight,
                            color = Color.White,
                            // Distinct per-dot identity, same rationale as the word-level hack below:
                            // three identical "•" glyphs would otherwise share one cached
                            // TextLayoutResult (and its highlight/paint state) across dots.
                            letterSpacing = (dotIdx * 0.0000001f).sp,
                        )
                    )
                    val dotW = result.size.width.toFloat()
                    val dotGap = 4f // FIXED: tight gaps
                    WordLayout(dotWord, result, Offset(dotIdx * (dotW + dotGap), 0f))
                }
                val dotH = dotLayouts.maxOfOrNull { it.textLayoutResult.size.height.toFloat() } ?: 0f
                val totalDotsW = dotLayouts.lastOrNull()?.let { it.relativeOffset.x + it.textLayoutResult.size.width } ?: 0f

                // Inherit alignment (and RTL-ness) from the next non-interlude, non-background line.
                val nextLine = lines
                    .firstOrNull { it.startMs > line.startMs && !it.isInterlude && !it.isBackground && !it.isSongwriter }
                val nextLineAlignment = nextLine?.oppositeAligned ?: false
                val nextLineIsRtl = nextLine != null &&
                    RtlDetector.isRtl(nextLine.words.joinToString(" ") { displayText(it, romanize) })
                val dotsRightAligned = resolveRightAligned(hasDuet, nextLineIsRtl, nextLineAlignment, isSongwriter = false)

                val slot = metrics.contentSlot(hasDuet, nextLineIsRtl, nextLineAlignment)
                layouts.add(LineLayout(line, dotLayouts, currentY, dotH, totalDotsW, totalDotsW, true, isBg,
                    nextLineAlignment, false, nextLineIsRtl, dotsRightAligned, slot.startPx, slot.widthPx))
                currentY += 0f // Interludes collapse when not active.
                continue
            }


            // Standard lyric line layout.
            val lineIsRtl = RtlDetector.isRtl(line.words.joinToString(" ") { displayText(it, romanize) })
            val lineFontFamily = fontFamilyFor(line.words.joinToString(" ") { displayText(it, romanize) })
            val contentSlot = metrics.contentSlot(hasDuet, lineIsRtl, line.oppositeAligned)
            val lineMaxWidth = contentSlot.widthPx
            // Inter-word gap of 0.32ch (width of "0"), matching the reference's `margin-right: 0.32ch`.
            val chWidth = textMeasurer.measure(
                text = AnnotatedString("0"),
                style = TextStyle(
                    fontFamily = lineFontFamily,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = Color.White,
                )
            ).size.width.toFloat()
            val wordGap = chWidth * 0.32f

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
                    fontFamily = lineFontFamily,
                    fontSize = fontSize,
                    fontWeight = fontWeight,
                    color = Color.White,
                    // Use a tiny unique letter spacing based on the Word object's identity.
                    // This prevents Compose from sharing cached TextLayoutResults (and highlights) 
                    // between identical words in different lines.
                    letterSpacing = (System.identityHashCode(word) % 1000 * 0.0000001f).sp
                )
                
                val text = displayText(word, romanize)
                val fullResult = textMeasurer.measure(text, style)
                val fullW = fullResult.size.width.toFloat()

                // `word.isPartOfWord` records that the ORIGINAL text had no whitespace before this
                // token — true both for real hyphen-continuations ("Hel-"+"lo") and, just as often,
                // for CJK/Hangul syllable spans (which never have inter-word whitespace in the source
                // TTML). Romanization must preserve the source's spacing exactly: a syllable glued to
                // its neighbour in the original script stays glued when romanized (こんにちは → "konnichiwa",
                // not "kon nichi wa"), and only tokens that had real whitespace in the source get a gap.
                val effectiveIsPartOfWord = word.isPartOfWord

                if (word.isLetterGroup) {
                    var currentX = 0f
                    val graphemes = com.tx24.spicyplayer.lyrics.spicy.parser.GraphemeSegmenter.segment(text)
                    for (charIdx in graphemes.indices) {
                        val charText = graphemes[charIdx]
                        // Two instances of the same letter within one word (e.g. the two "a"s in
                        // "california") would otherwise measure with identical (text, style) and
                        // share one cached TextLayoutResult, coupling their highlight state — mix
                        // the char index into letterSpacing on top of the word's own identity.
                        val charStyle = style.copy(
                            letterSpacing = (style.letterSpacing.value + charIdx * 0.00000001f).sp
                        )
                        val charResult = textMeasurer.measure(charText, charStyle)
                        pieces.add(Piece(
                            word = word,
                            text = charText,
                            layout = charResult,
                            sourceIdx = wIdx,
                            charIdx = charIdx,
                            fullWidth = fullW,
                            startX = currentX,
                            isCjkPiece = charText.firstOrNull()?.let(::isCjk) == true,
                            isPartOfWord = charIdx > 0 || effectiveIsPartOfWord
                        ))
                        currentX += charResult.size.width
                    }
                } else {
                    pieces.add(Piece(
                        word = word,
                        text = text,
                        layout = fullResult,
                        sourceIdx = wIdx,
                        charIdx = 0,
                        fullWidth = fullW,
                        startX = 0f,
                        isCjkPiece = false,
                        isPartOfWord = effectiveIsPartOfWord
                    ))
                }
            }

            // Word-wrapping logic on pieces.
            val numPieces = pieces.size
            val lineBreaks = mutableListOf<Int>()

            // A run of glued pieces (isPartOfWord chain — CJK char-splits, held-word letters, and
            // romanized syllables alike) should wrap as one unit, like the reference's adjacent
            // inline spans with no whitespace between them. Only break inside a run when the run
            // itself can never fit a row on its own — otherwise it'd overflow off-screen forever.
            val runFits = BooleanArray(numPieces)
            run {
                var runStart = 0
                var runWidth = 0f
                for (idx in 0 until numPieces) {
                    if (idx > 0 && !pieces[idx].isPartOfWord) {
                        val fits = runWidth <= lineMaxWidth
                        for (k in runStart until idx) runFits[k] = fits
                        runStart = idx
                        runWidth = 0f
                    }
                    runWidth += pieces[idx].layout.size.width.toFloat()
                }
                val fits = runWidth <= lineMaxWidth
                for (k in runStart until numPieces) runFits[k] = fits
            }

            // Greedy wrap for every line. The reference is CSS `flex-wrap: wrap`, which is always
            // greedy — it fills each row until the next item doesn't fit, then breaks, and never
            // balances row lengths. Duet/opposite-aligned lines wrap greedily too (right-alignment
            // is applied afterwards), matching the reference instead of a balanced pass.
            run {
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

                    // A glued piece (isPartOfWord — CJK char-split, held-word letter, or romanized
                    // syllable) prefers to stay attached to its run, same as adjacent no-whitespace
                    // inline spans in the reference. Falls back to breakable when the run can't
                    // possibly fit a row (runFits == false), so an overlong glued run still wraps
                    // instead of overflowing off-screen.
                    val isForcedSyllable = i > 0 && piece.isPartOfWord && runFits[i] &&
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
            }


            // Assemble WordLayouts into rows.
            val allRows = mutableListOf<Pair<Float, List<WordLayout>>>()
            var currentRowY = 0f
            var maxRowWidth = 0f
            val explicitRowHeight = metrics.lineHeightPx(fontSize.value)

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
                if (b < lineBreaks.size - 2) {
                    currentRowY += explicitRowHeight
                }
            }
            
            val totalHeight = if (allRows.isEmpty()) explicitRowHeight else currentRowY + explicitRowHeight
            val totalWidth = maxRowWidth

            // Apply alignment and flatten. RTL duet lines mirror the LTR duet convention (primary
            // right, guest left) instead of both stacking on the right — see resolveRightAligned.
            val isRightAligned = resolveRightAligned(hasDuet, lineIsRtl, line.oppositeAligned, line.isSongwriter)
            val wordLayouts = mutableListOf<WordLayout>()
            for ((rWidth, rowPieces) in allRows) {
                val alignmentShift = if (isRightAligned) maxRowWidth - rWidth else 0f
                for (wLayout in rowPieces) {
                    // Our layout builds each row left-to-right in source (logical reading) order.
                    // For LTR that's also visual order, but RTL reading order is right-to-left, so
                    // the row must be mirrored within its own width — otherwise words still read
                    // left-to-right, just shifted as a block (the bug: "words are ordered left to
                    // right" instead of right to left).
                    val mirroredX = if (lineIsRtl) {
                        rWidth - wLayout.relativeOffset.x - wLayout.textLayoutResult.size.width
                    } else {
                        wLayout.relativeOffset.x
                    }
                    wordLayouts.add(wLayout.copy(
                        relativeOffset = Offset(mirroredX + alignmentShift, wLayout.relativeOffset.y)
                    ))
                }
            }

            val prevIsInterlude = layouts.lastOrNull()?.isInterlude ?: false
            val drawY = if (isBg && !prevIsInterlude) currentY - lineSpacing else currentY

            layouts.add(LineLayout(line, wordLayouts, drawY, totalHeight, totalWidth, maxRowWidth, false, isBg,
                line.oppositeAligned, line.isSongwriter, lineIsRtl, isRightAligned,
                contentSlot.startPx, contentSlot.widthPx))
            
            val bottomY = drawY + totalHeight
            currentY = maxOf(currentY, bottomY + lineSpacing)
        }
        return layouts
    }
}
