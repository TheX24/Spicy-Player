package com.tx24.spicyplayer.model.lyrics


/**
 * Synced Lyrics containing a list of [SyncedLyricsSegment]s
 */
data class SynchronizedLyrics(
    val originalString: String,
    val segments: List<SyncedLyricsSegment>
) {


    fun constructStringForSharing(): String {
        return segments.joinToString(separator = "\n") { it.text }
    }

    companion object {
        fun fromString(text: String?): SynchronizedLyrics? {
            if (text.isNullOrBlank()) return null

            val segments = mutableListOf<SyncedLyricsSegment>()

            val lines = text.split("\n")
            for (line in lines) {
                if (!line.trim().startsWith("[")) {
                    continue
                }

                val timeInfoLastIndex = line.indexOfFirst { it == ']' }
                if (timeInfoLastIndex == -1) continue

                val timeInfo = line.substring(1, timeInfoLastIndex)
                val timeInfoArray = timeInfo.split(":")

                val minutes = timeInfoArray[0].toIntOrNull() ?: continue

                if (timeInfoArray.size != 2) continue
                val secondsArray = timeInfoArray[1].split(".", limit = 2)
                val seconds = secondsArray[0].toIntOrNull() ?: continue
                val fraction = secondsArray.getOrNull(1).orEmpty()
                val millis = when (fraction.length) {
                    0 -> 0
                    1 -> fraction.toIntOrNull()?.times(100)
                    2 -> fraction.toIntOrNull()?.times(10)
                    else -> fraction.take(3).toIntOrNull()
                } ?: continue

                SyncedLyricsSegment(
                    line.substring(timeInfoLastIndex + 1).trim(),
                    minutes * 60 * 1000 + seconds * 1000 + millis
                ).also { segments.add(it) }
            }

            if (segments.isEmpty()) return null
            return SynchronizedLyrics(text, segments)
        }
    }

}


/**
 * Represents a single line of a synced lyrics text
 *
 * @param text The line of the lyrics
 * @param durationMillis Duration in milliseconds since the start of the song
 */
data class SyncedLyricsSegment(
    val text: String,
    val durationMillis: Int
)
