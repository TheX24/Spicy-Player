package com.tx24.spicyplayer.uiLibrary.songs.analytics



sealed interface AnalyticsScreenState {
    data object Loading: AnalyticsScreenState
    data class Loaded(
        val averageListeningTimePerDay: Int
    ): AnalyticsScreenState
}