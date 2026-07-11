package com.tx24.spicyplayer.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import com.tx24.spicyplayer.uiLibrary.playlists.navigation.PLAYLIST_DETAILS_ROUTE
import com.tx24.spicyplayer.uiLibrary.albums.navigation.ALBUM_DETAIL_ROUTE
import com.tx24.spicyplayer.settings.navigation.SETTINGS_ROUTE
import com.tx24.spicyplayer.uiLibrary.songs.navigation.SEARCH_ROUTE
import com.tx24.spicyplayer.tageditor.navigation.TAG_EDITOR_SCREEN
import com.tx24.spicyplayer.uiNowPlaying.NowPlayingState
import com.tx24.spicyplayer.uiNowPlaying.viewmodel.NowPlayingViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach


@Composable
fun rememberSpicyAppState(
    navHostController: NavHostController,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    isNowPlayingExpanded: Boolean,
    nowPlayingViewModel: NowPlayingViewModel,
    nowPlayingScreenOffset: () -> Float
): SpicyAppState {
    return remember(
        navHostController,
        coroutineScope,
        isNowPlayingExpanded,
        nowPlayingScreenOffset
    ) {
        SpicyAppState(
            navHostController,
            coroutineScope,
            isNowPlayingExpanded,
            nowPlayingViewModel,
            nowPlayingScreenOffset
        )
    }
}


@Stable
class SpicyAppState(
    val navHostController: NavHostController,
    val coroutineScope: CoroutineScope,
    val isNowPlayingExpanded: Boolean,
    val nowPlayingViewModel: NowPlayingViewModel,
    val nowPlayingScreenOffset: () -> Float
) {


    /**
     * Whether we should show the NowPlaying Screen or not.
     */
    val shouldShowNowPlayingScreen = nowPlayingViewModel.state.map { it is NowPlayingState.Playing }

    val shouldShowBottomBar = navHostController.currentBackStackEntryFlow.onEach { delay(100) }.map {
        val route = it.destination.route ?: return@map true
        return@map !(
                        route.contains(PLAYLIST_DETAILS_ROUTE) ||
                        route.contains(SEARCH_ROUTE) ||
                        route.contains(TAG_EDITOR_SCREEN) ||
                        route.contains(ALBUM_DETAIL_ROUTE) ||
                        route.contains(SETTINGS_ROUTE)
                )
    }


}