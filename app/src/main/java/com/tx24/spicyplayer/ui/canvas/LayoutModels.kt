package com.tx24.spicyplayer.ui.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import com.tx24.spicyplayer.models.Line
import com.tx24.spicyplayer.models.Word

/**
 * Internal data class representing the layout and position of a single word.
 */
internal data class WordLayout(
    val word: Word,
    val textLayoutResult: TextLayoutResult,
    val relativeOffset: Offset,
    val characterLayouts: List<TextLayoutResult> = emptyList()
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
