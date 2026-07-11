package com.tx24.spicyplayer.uiNowPlaying.spicy.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import com.tx24.spicyplayer.uiNowPlaying.spicy.models.Line
import com.tx24.spicyplayer.uiNowPlaying.spicy.models.Word

/**
 * Internal data class representing the layout and position of a single word.
 */
internal data class WordLayout(
    val word: Word,
    val textLayoutResult: TextLayoutResult,
    val relativeOffset: Offset,
    val characterLayouts: List<TextLayoutResult> = emptyList(),
    /** Index of the original word in the Line.words list. */
    val sourceWordIndex: Int = -1,
    /** Index of the character offset within the original word. */
    val charIndex: Int = 0,
    /** Original total width of the word (including all fragments). */
    val fullWordWidth: Float = 0f,
    /** Horizontal start position of this fragment within the full word. */
    val startXOffset: Float = 0f
)

/**
 * Internal data class representing the layout and position of a full line of lyrics.
 */
internal data class LineLayout(
    val line: Line,
    val words: List<WordLayout>,
    /** Absolute vertical position of the line. */
    val yOffset: Float,
    val height: Float,
    val totalWidth: Float,
    val maxRowWidth: Float,
    val isInterlude: Boolean,
    val isBackground: Boolean,
    val oppositeAligned: Boolean,
    val isSongwriter: Boolean,
)
