package com.tx24.spicyplayer.lyrics

internal const val LYRICS_OFFSET_CONVENTION_VERSION = 1

internal fun migrateLyricsOffset(storedOffsetMs: Int, storedVersion: Int): Int =
    if (storedVersion < LYRICS_OFFSET_CONVENTION_VERSION) -storedOffsetMs else storedOffsetMs
