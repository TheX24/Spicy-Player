package com.tx24.spicyplayer.ui.library

import android.graphics.Bitmap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.tx24.spicyplayer.ui.canvas.DynamicBackgroundView
import java.io.File

@Composable
fun LibraryScreen(
    songPairs: List<Pair<File, File?>>,
    colorScheme: ColorScheme,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onSongSelected: (Int) -> Unit
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Songs", "Folders")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 32.dp)
            .padding(bottom = bottomPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.Transparent,
                contentColor = colorScheme.onSurface,
                divider = { },
                indicator = { tabPositions ->
                    if (selectedTabIndex < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            color = colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { 
                            Text(
                                title, 
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            when (selectedTabIndex) {
                0 -> SongsList(songPairs, colorScheme, onSongSelected)
                1 -> FoldersList(songPairs, colorScheme, onSongSelected)
            }
        }
    }
}

@Composable
fun SongsList(
    songPairs: List<Pair<File, File?>>,
    colorScheme: ColorScheme,
    onSongSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
            itemsIndexed(songPairs) { index, pair ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = pair.first.nameWithoutExtension,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = colorScheme.onSurface
                        )
                    },
                    supportingContent = {
                        Text(
                            text = pair.first.parentFile?.name ?: "Unknown Folder",
                            color = colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = colorScheme.primary)
                    },
                    modifier = Modifier.clickable { onSongSelected(index) },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
            }
        }
    }
}

@Composable
fun FoldersList(
    songPairs: List<Pair<File, File?>>,
    colorScheme: ColorScheme,
    onSongSelected: (Int) -> Unit
) {
    // Group songs by folder
    val grouped = remember(songPairs) {
        songPairs.mapIndexed { idx, pair -> idx to pair }
            .groupBy { it.second.first.parentFile?.name ?: "Unknown" }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        color = colorScheme.surfaceContainerHigh,
        tonalElevation = 0.dp
    ) {
        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)) {
            grouped.forEach { (folderName, items) ->
                item {
                    Text(
                        text = folderName,
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
                items(items.size) { index ->
                    val (originalIndex, pair) = items[index]
                    ListItem(
                        headlineContent = {
                            Text(
                                text = pair.first.nameWithoutExtension,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = colorScheme.secondary)
                        },
                        modifier = Modifier.clickable { onSongSelected(originalIndex) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}
