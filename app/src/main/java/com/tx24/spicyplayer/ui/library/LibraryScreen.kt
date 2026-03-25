package com.tx24.spicyplayer.ui.library

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.tx24.spicyplayer.models.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class LibrarySubPage {
    PLAYLISTS, ALBUMS, SONGS, ARTISTS, FOLDERS,
    ALBUM_DETAIL, ARTIST_DETAIL, FOLDER_DETAIL
}

sealed class LibraryScreenState {
    abstract val depth: Int
    object Menu : LibraryScreenState() { override val depth = 0 }
    data class Detail(val subPage: LibrarySubPage, val selectedItem: String? = null) : LibraryScreenState() {
        override val depth = when(subPage) {
            LibrarySubPage.ALBUM_DETAIL, LibrarySubPage.ARTIST_DETAIL, LibrarySubPage.FOLDER_DETAIL -> 2
            else -> 1
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongSelected: (Int) -> Unit
) {
    var currentScreenState by remember { mutableStateOf<LibraryScreenState>(LibraryScreenState.Menu) }
    
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = currentScreenState,
            transitionSpec = {
                val initialDepth = initialState.depth
                val targetDepth = targetState.depth
                val goingDeeper = targetDepth > initialDepth
                
                if (goingDeeper) {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn() togetherWith slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeOut()
                } else {
                    slideInHorizontally(
                        initialOffsetX = { -it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeIn() togetherWith slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    ) + fadeOut()
                }
            },
            label = "LibraryNavigation"
        ) { screenState ->
            if (screenState is LibraryScreenState.Menu) {
                LibraryMenu(
                    colorScheme = colorScheme,
                    bottomPadding = bottomPadding,
                    onNavigate = { currentScreenState = LibraryScreenState.Detail(it) }
                )
            } else if (screenState is LibraryScreenState.Detail) {
                val subPage = screenState.subPage
                val selectedItem = screenState.selectedItem
                LibrarySubPageView(
                    subPage = subPage,
                    selectedItem = selectedItem,
                    songs = songs,
                    colorScheme = colorScheme,
                    bottomPadding = bottomPadding,
                    onBack = { 
                        currentScreenState = when (subPage) {
                            LibrarySubPage.ALBUM_DETAIL -> LibraryScreenState.Detail(LibrarySubPage.ALBUMS)
                            LibrarySubPage.ARTIST_DETAIL -> LibraryScreenState.Detail(LibrarySubPage.ARTISTS)
                            LibrarySubPage.FOLDER_DETAIL -> LibraryScreenState.Detail(LibrarySubPage.FOLDERS)
                            else -> LibraryScreenState.Menu
                        }
                    },
                    onNavigateToDetail = { detailSubPage, item ->
                        currentScreenState = LibraryScreenState.Detail(detailSubPage, item)
                    },
                    onSongSelected = onSongSelected
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryMenu(
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigate: (LibrarySubPage) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp))
            val menuItems = listOf(
                Triple("Playlists", Icons.AutoMirrored.Rounded.PlaylistPlay, LibrarySubPage.PLAYLISTS),
                Triple("Albums", Icons.Rounded.Album, LibrarySubPage.ALBUMS),
                Triple("Songs", Icons.Rounded.MusicNote, LibrarySubPage.SONGS),
                Triple("Artists", Icons.Rounded.Person, LibrarySubPage.ARTISTS),
                Triple("Folders", Icons.Rounded.Folder, LibrarySubPage.FOLDERS)
            )
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = bottomPadding + 24.dp)
            ) {
                items(menuItems.size) { index ->
                    val (title, icon, page) = menuItems[index]
                    LibraryMenuItem(
                        title = title,
                        icon = icon,
                        colorScheme = colorScheme,
                        onClick = { onNavigate(page) }
                    )
                }
            }
        }
        
        TopAppBar(
            modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
            title = {
                Text(
                    text = "Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurface
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }
}
@Composable
fun LibraryMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorScheme: ColorScheme,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp), // XL Shape
        color = colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun LibrarySubPageView(
    subPage: LibrarySubPage,
    selectedItem: String?,
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onBack: () -> Unit,
    onNavigateToDetail: (LibrarySubPage, String) -> Unit,
    onSongSelected: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        val detailTitle = selectedItem ?: subPage.name.lowercase().replaceFirstChar { it.uppercase() }
        
        Box(modifier = Modifier.fillMaxSize()) {
            when (subPage) {
                LibrarySubPage.PLAYLISTS -> PlaylistsList(colorScheme, bottomPadding)
                LibrarySubPage.ALBUMS -> AlbumsGrid(songs, colorScheme, bottomPadding, onNavigateToDetail)
                LibrarySubPage.SONGS -> SongsList(songs, colorScheme, bottomPadding, onSongSelected)
                LibrarySubPage.ARTISTS -> ArtistsGrid(songs, colorScheme, bottomPadding, onNavigateToDetail)
                LibrarySubPage.FOLDERS -> FoldersList(songs, colorScheme, bottomPadding, onNavigateToDetail)
                LibrarySubPage.ALBUM_DETAIL -> AlbumDetail(selectedItem!!, songs, colorScheme, bottomPadding, onSongSelected)
                LibrarySubPage.ARTIST_DETAIL -> ArtistDetail(selectedItem!!, songs, colorScheme, bottomPadding, onSongSelected)
                LibrarySubPage.FOLDER_DETAIL -> FolderDetail(selectedItem!!, songs, colorScheme, bottomPadding, onSongSelected)
            }
        }

        LibraryHeader(
            title = detailTitle,
            onBack = onBack,
            colorScheme = colorScheme
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHeader(
    title: String,
    onBack: () -> Unit,
    colorScheme: ColorScheme
) {
    TopAppBar(
        modifier = Modifier.padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        navigationIcon = {
            Surface(
                shape = CircleShape,
                color = colorScheme.surfaceVariant.copy(alpha = 0.8f),
                onClick = onBack,
                modifier = Modifier.padding(start = 16.dp),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(end = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                            tint = colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        },
        title = {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun SongsList(
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongSelected: (Int) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    val sortedSongs = remember(songs) {
        songs.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), // XL Shape top
            color = colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding + 16.dp)
            ) {
                itemsIndexed(sortedSongs) { index, song ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = song.title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colorScheme.onSurface,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold // Emphasized
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "${song.artist} • ${song.album}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colorScheme.primary)
                        },
                        modifier = Modifier.clickable { onSongSelected(songs.indexOf(song)) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
        
        AlphabetIndexer(
            items = sortedSongs.map { it.title },
            onLetterSelected = { letter ->
                val index = sortedSongs.indexOfFirst { it.title.firstOrNull()?.uppercaseChar()?.toString() == letter || (letter == "#" && it.title.firstOrNull()?.isDigit() == true) }
                if (index != -1) {
                    coroutineScope.launch { listState.scrollToItem(index) }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
            colorScheme = colorScheme
        )
    }
}

@Composable
fun AlbumsGrid(
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigateToDetail: (LibrarySubPage, String) -> Unit
) {
    val grouped = remember(songs) {
        songs.groupBy { it.album }
    }
    val albums = remember(grouped) {
        grouped.keys.toList().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp),
            state = gridState,
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums) { album ->
                val firstSong = grouped[album]?.firstOrNull()
                AlbumCard(
                    title = album,
                    subtitle = grouped[album]?.firstOrNull()?.artist ?: "Unknown Artist",
                    song = firstSong,
                    colorScheme = colorScheme,
                    onClick = {
                        onNavigateToDetail(LibrarySubPage.ALBUM_DETAIL, album)
                    }
                )
            }
        }
        
        AlphabetIndexer(
            items = albums,
            onLetterSelected = { letter ->
                val index = albums.indexOfFirst { it.firstOrNull()?.uppercaseChar()?.toString() == letter || (letter == "#" && it.firstOrNull()?.isDigit() == true) }
                if (index != -1) {
                    coroutineScope.launch { gridState.scrollToItem(index) }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
            colorScheme = colorScheme
        )
    }
}

@Composable
fun ArtistsGrid(
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigateToDetail: (LibrarySubPage, String) -> Unit
) {
    val grouped = remember(songs) {
        songs.groupBy { it.artist }
    }
    val artists = remember(grouped) {
        grouped.keys.toList().sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it })
    }
    val gridState = rememberLazyGridState()
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp),
            state = gridState,
            contentPadding = PaddingValues(bottom = bottomPadding + 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(artists) { artist ->
                val firstSong = grouped[artist]?.firstOrNull()
                AlbumCard(
                    title = artist,
                    subtitle = "${grouped[artist]?.size ?: 0} Songs",
                    song = firstSong,
                    colorScheme = colorScheme,
                    isArtist = true,
                    onClick = {
                        onNavigateToDetail(LibrarySubPage.ARTIST_DETAIL, artist)
                    }
                )
            }
        }

        AlphabetIndexer(
            items = artists,
            onLetterSelected = { letter ->
                val index = artists.indexOfFirst { it.firstOrNull()?.uppercaseChar()?.toString() == letter || (letter == "#" && it.firstOrNull()?.isDigit() == true) }
                if (index != -1) {
                    coroutineScope.launch { gridState.scrollToItem(index) }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd),
            colorScheme = colorScheme
        )
    }
}

@Composable
fun AlbumCard(
    title: String,
    subtitle: String,
    song: Song?,
    colorScheme: ColorScheme,
    isArtist: Boolean = false,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(song) {
        if (song != null) {
            withContext(Dispatchers.IO) {
                try {
                    val retriever = MediaMetadataRetriever()
                    retriever.setDataSource(song.file.absolutePath)
                    val art = retriever.embeddedPicture
                    if (art != null) {
                        bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                    }
                    retriever.release()
                } catch (_: Exception) {}
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .clickable { onClick() },
        shape = RoundedCornerShape(28.dp), // Expressive Large
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerHigh)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colorScheme.surfaceVariant)
            ) {
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            if (isArtist) Icons.Rounded.Person else Icons.Rounded.Album,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, // Emphasized
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FoldersList(
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onNavigateToDetail: (LibrarySubPage, String) -> Unit
) {
    val folders = remember(songs) {
        songs.map { it.file.parentFile?.absolutePath ?: "Unknown" }
            .distinct()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.substringAfterLast("/") })
    }
    
    Surface(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding + 16.dp)
        ) {
            items(folders) { folderPath ->
                val folderName = folderPath.substringAfterLast("/")
                LibraryMenuItem(
                    title = folderName,
                    icon = Icons.Rounded.Folder,
                    colorScheme = colorScheme,
                    onClick = { onNavigateToDetail(LibrarySubPage.FOLDER_DETAIL, folderPath) }
                )
            }
        }
    }
}

@Composable
fun AlbumDetail(
    albumName: String,
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongSelected: (Int) -> Unit
) {
    val albumSongs = remember(albumName, songs) {
        songs.filter { it.album == albumName }
            .sortedBy { it.trackNumber ?: 0 }
    }
    SimpleSongList(albumSongs, songs, colorScheme, bottomPadding, onSongSelected)
}

@Composable
fun ArtistDetail(
    artistName: String,
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongSelected: (Int) -> Unit
) {
    val artistSongs = remember(artistName, songs) {
        songs.filter { it.artist == artistName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }
    SimpleSongList(artistSongs, songs, colorScheme, bottomPadding, onSongSelected)
}

@Composable
fun FolderDetail(
    folderPath: String,
    songs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongSelected: (Int) -> Unit
) {
    val folderSongs = remember(folderPath, songs) {
        songs.filter { it.file.parentFile?.absolutePath == folderPath }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.title })
    }
    SimpleSongList(folderSongs, songs, colorScheme, bottomPadding, onSongSelected)
}

@Composable
fun SimpleSongList(
    displaySongs: List<Song>,
    allSongs: List<Song>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp).padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 16.dp, bottom = bottomPadding + 16.dp)
        ) {
            items(displaySongs) { song ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colorScheme.primary.copy(alpha = 0.6f))
                    },
                    modifier = Modifier.clickable { onSongSelected(allSongs.indexOf(song)) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun PlaylistsList(
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 72.dp, bottom = bottomPadding),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            color = colorScheme.surfaceContainerHigh,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.QueueMusic,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = colorScheme.primary.copy(alpha = 0.1f)
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    "Coming Soon",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun AlphabetIndexer(
    items: List<String>,
    onLetterSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    colorScheme: ColorScheme
) {
    val alphabet = "#ABCDEFGHIJKLMNOPQRSTUVWXYZ".map { it.toString() }
    
    Box(
        modifier = modifier
            .padding(end = 6.dp, top = 24.dp, bottom = 24.dp)
            .width(32.dp)
            .fillMaxHeight()
            .clip(CircleShape)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val y = offset.y
                    val index = (y / size.height * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
                    onLetterSelected(alphabet[index])
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val y = change.position.y
                    val index = (y / size.height * alphabet.size).toInt().coerceIn(0, alphabet.lastIndex)
                    onLetterSelected(alphabet[index])
                }
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            alphabet.forEach { letter ->
                Text(
                    text = letter,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}
