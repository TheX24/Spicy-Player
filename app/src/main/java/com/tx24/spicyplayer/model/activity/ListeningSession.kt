package com.tx24.spicyplayer.model.activity

import java.util.Date


data class ListeningSession(
    val startTime: Date,
    val durationSeconds: Int,
    val songUri: String = "",
    val songName: String = "",
    val albumName: String = "",
)