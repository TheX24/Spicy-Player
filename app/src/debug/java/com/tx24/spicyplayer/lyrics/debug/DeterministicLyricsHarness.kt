package com.tx24.spicyplayer.lyrics.debug

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.lyrics.spicy.RenderConfig
import com.tx24.spicyplayer.lyrics.spicy.SimpleAnimationStyle
import com.tx24.spicyplayer.lyrics.spicy.canvas.SpicyLyricsView
import com.tx24.spicyplayer.lyrics.spicy.models.LyricsDocument

data class DebugLyricsHarnessState(
    val document: LyricsDocument,
    val timestampMs: Long,
    val viewportWidthDp: Int,
    val viewportHeightDp: Int,
    val simpleLyricsMode: Boolean = false,
    val minimalLyricsMode: Boolean = false,
    val simpleAnimationStyle: SimpleAnimationStyle = SimpleAnimationStyle.CALCULATE,
)

/** Fixed-input surface used by instrumented renderer captures. */
@Composable
fun DeterministicLyricsHarness(state: DebugLyricsHarnessState) {
    Box(
        Modifier
            .requiredSize(state.viewportWidthDp.dp, state.viewportHeightDp.dp)
            .background(Color.Black),
    ) {
        SpicyLyricsView(
            lines = state.document.lines,
            documentId = state.document.documentId,
            footer = state.document.footer,
            currentTimeMs = { state.timestampMs },
            onSeekWord = {},
            lyricsType = state.document.type,
            config = RenderConfig.create(
                simpleLyricsMode = state.simpleLyricsMode,
                minimalLyricsMode = state.minimalLyricsMode,
                simpleAnimationStyle = state.simpleAnimationStyle,
            ),
        )
    }
}
