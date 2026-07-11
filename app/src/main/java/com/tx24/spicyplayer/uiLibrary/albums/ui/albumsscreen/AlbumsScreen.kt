package com.tx24.spicyplayer.uiLibrary.albums.ui.albumsscreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import com.tx24.spicyplayer.uiLibrary.albums.ui.menuactions.buildAlbumsMenuActions
import com.tx24.spicyplayer.uiLibrary.albums.viewmodel.AlbumsScreenActions
import com.tx24.spicyplayer.uiLibrary.albums.viewmodel.AlbumsScreenState
import com.tx24.spicyplayer.uiLibrary.albums.viewmodel.AlbumsViewModel
import com.tx24.spicyplayer.library.store.model.album.BasicAlbum
import com.tx24.spicyplayer.ui.anim.OPEN_SCREEN_ENTER_ANIMATION
import com.tx24.spicyplayer.ui.anim.POP_SCREEN_EXIT_ANIMATION
import com.tx24.spicyplayer.ui.common.LocalUserPreferences
import com.tx24.spicyplayer.ui.common.MultiSelectState
import com.tx24.spicyplayer.ui.topbar.SelectionTopAppBarScaffold


@Composable
fun AlbumsScreen(
    modifier: Modifier,
    onAlbumClicked: (albumId: Int) -> Unit,
    viewModel: AlbumsViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsState()

    AlbumsScreen(
        modifier = modifier,
        state = state,
        actions = viewModel,
        onAlbumClicked = onAlbumClicked,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    modifier: Modifier,
    state: AlbumsScreenState,
    actions: AlbumsScreenActions,
    onAlbumClicked: (albumId: Int) -> Unit
) {

    val albums = state.albums

    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    val librarySettings = LocalUserPreferences.current.librarySettings

    val multiSelectState = remember {
        MultiSelectState<BasicAlbum>()
    }
    val multiSelectEnabled by remember {
        derivedStateOf { multiSelectState.selected.size > 0 }
    }

    BackHandler(multiSelectEnabled) {
        multiSelectState.clear()
    }

    Scaffold(
        topBar = {
            SelectionTopAppBarScaffold(
                modifier = Modifier.fillMaxWidth(),
                multiSelectState = multiSelectState,
                isMultiSelectEnabled = multiSelectEnabled,
                actionItems = buildAlbumsMenuActions(
                    onPlay = {
                        actions.playAlbums(multiSelectState.selected)
                    },
                    addToQueue = {
                        actions.addAlbumsToQueue(multiSelectState.selected)
                    },
                    onPlayNext = {
                        actions.playAlbumsNext(multiSelectState.selected)
                    },
                    onShuffle = {
                        actions.shuffleAlbums(multiSelectState.selected)
                    },
                    onShuffleNext = {
                        actions.shuffleAlbumsNext(multiSelectState.selected)
                    },
                    onAddToPlaylists = {

                    }
                ),
                numberOfVisibleIcons = 2,
                scrollBehavior = topAppBarScrollBehavior,
            ) {
                AlbumsTopBar(
                    scrollBehavior = topAppBarScrollBehavior,
                    gridSize = librarySettings.albumsGridSize,
                    sortOrder = librarySettings.albumsSortOrder,
                    { actions.changeSortOptions(it.first, it.second) },
                    actions::changeGridSize
                )
            }
        },
        modifier = modifier,
    ) { paddingValues ->

        AnimatedContent(
            targetState = librarySettings.albumsGridSize, label = "",
            transitionSpec = {
                OPEN_SCREEN_ENTER_ANIMATION togetherWith POP_SCREEN_EXIT_ANIMATION
            }
        ) {
            when (it) {
                1 -> {
                    AlbumsList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                        albums = albums,
                        multiSelectState = multiSelectState,
                        onAlbumClicked = {
                            if (multiSelectEnabled) multiSelectState.toggle(it)
                            else {
                                onAlbumClicked(it.albumInfo.id)
                            }
                        },
                        onAlbumLongClicked = {
                            multiSelectState.toggle(it)
                        }
                    )
                }

                else -> {
                    AlbumsGrid(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                            .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
                        albums = albums,
                        numOfColumns = it,
                        multiSelectState = multiSelectState,
                        onAlbumClicked = {
                            if (multiSelectEnabled) multiSelectState.toggle(it)
                            else {
                                onAlbumClicked(it.albumInfo.id)
                            }
                        },
                        onAlbumLongClicked = {
                            multiSelectState.toggle(it)
                        }
                    )
                }
            }
        }
    }


}