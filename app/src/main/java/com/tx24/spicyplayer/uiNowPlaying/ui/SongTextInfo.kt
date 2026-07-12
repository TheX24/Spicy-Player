package com.tx24.spicyplayer.uiNowPlaying.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tx24.spicyplayer.library.store.model.song.Song

@Composable
fun SongTextInfo(
    modifier: Modifier,
    song: Song,
    showArtist: Boolean = true,
    showAlbum: Boolean = true,
    marqueeEffect: Boolean = true,
    titleStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    titleFontWeight: FontWeight = FontWeight.Medium,
    titleFontSize: TextUnit = 22.sp,
    titleColor: Color = Color.Unspecified,
    artistStyle: TextStyle = MaterialTheme.typography.labelSmall,
    artistFontWeight: FontWeight = FontWeight.Normal,
    artistFontSize: TextUnit = 14.sp,
    artistColor: Color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
) {


    Column(modifier = modifier) {

        AnimatedContent(
            modifier = Modifier.fillMaxWidth(),
            targetState = song.metadata.title,
            transitionSpec = { fadeIn(tween(delayMillis = 150)) togetherWith fadeOut(tween(durationMillis = 150)) }
        ) { title ->
            if (marqueeEffect) {
                PingPongMarquee(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = title,
                        color = titleColor,
                        fontWeight = titleFontWeight,
                        textAlign = TextAlign.Center,
                        style = titleStyle,
                        fontSize = titleFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false
                    )
                }
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = title,
                    color = titleColor,
                    fontWeight = titleFontWeight,
                    textAlign = TextAlign.Center,
                    style = titleStyle,
                    fontSize = titleFontSize,
                    maxLines = 1
                )
            }
        }
        if (showArtist) {
            Spacer(modifier = Modifier.height(4.dp))
            val artistName = song.metadata.artistName ?: "<unknown>"
            if (marqueeEffect) {
                PingPongMarquee(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = artistName,
                        style = artistStyle,
                        fontWeight = artistFontWeight,
                        textAlign = TextAlign.Center,
                        fontSize = artistFontSize,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false,
                        color = artistColor
                    )
                }
            } else {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = artistName,
                    style = artistStyle,
                    fontWeight = artistFontWeight,
                    textAlign = TextAlign.Center,
                    fontSize = artistFontSize,
                    maxLines = 1,
                    color = artistColor
                )
            }
        }

        if (showAlbum) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = song.metadata.albumName ?: "<unknown>",
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }

}

