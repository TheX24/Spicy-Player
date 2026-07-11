package com.tx24.spicyplayer.uiLibrary.albums.ui.albumsscreen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.Checkbox
import com.tx24.spicyplayer.ui.menu.SpicyAppMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.model.AlbumsSortOption
import com.tx24.spicyplayer.model.prefs.IsAscending


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsTopBar(
    scrollBehavior: TopAppBarScrollBehavior,
    gridSize: Int,
    sortOrder: Pair<AlbumsSortOption, IsAscending>,
    onChangeSortOption: (Pair<AlbumsSortOption, IsAscending>) -> Unit,
    onChangeGridSize: (Int) -> Unit
) {

    var gridSizeDropdownShown by remember {
        mutableStateOf(false)
    }

    var sortOptionDropdownMenuShown by remember {
        mutableStateOf(false)
    }

    TopAppBar(
        title = {
            Text(text = "Albums", fontWeight = FontWeight.SemiBold)
        },
        scrollBehavior = scrollBehavior,
        actions = {
            IconButton(onClick = { gridSizeDropdownShown = !gridSizeDropdownShown }) {
                Icon(imageVector = Icons.Rounded.GridView, contentDescription = "Grid Size")
                GridSizeDropDownMenu(
                    visible = gridSizeDropdownShown,
                    currentSize = gridSize,
                    onSizeSelected = { gridSizeDropdownShown = false; onChangeGridSize(it) },
                    onDismissRequest = { gridSizeDropdownShown = false }
                )
            }

            IconButton(onClick = { sortOptionDropdownMenuShown = true }) {
                Icon(imageVector = Icons.AutoMirrored.Rounded.Sort, contentDescription = "Sort")
                SortOptionDropdownMenu(
                    visible = sortOptionDropdownMenuShown,
                    sortOption = sortOrder.first,
                    isAscending = sortOrder.second,
                    onChangeSortCriteria = {
                        onChangeSortOption(it to sortOrder.second); sortOptionDropdownMenuShown =
                        false
                    },
                    onChangeAscending = { onChangeSortOption(sortOrder.first to it) },
                    onDismissRequest = { sortOptionDropdownMenuShown = false }
                )
            }
        }
    )

}

@Composable
fun GridSizeDropDownMenu(
    visible: Boolean,
    currentSize: Int,
    onSizeSelected: (Int) -> Unit,
    onDismissRequest: () -> Unit,
) {

    SpicyAppMenu(
        visible = visible,
        onDismiss = onDismissRequest,
        title = "Grid Size"
    ) {
        (1 until 5).forEach {
            ListItem(
                headlineContent = { Text(it.toString()) },
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onSizeSelected(it) },
                trailingContent = {
                    RadioButton(selected = it == currentSize, onClick = null)
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
        }
    }
}

@Composable
fun SortOptionDropdownMenu(
    visible: Boolean,
    sortOption: AlbumsSortOption,
    isAscending: IsAscending,
    onChangeSortCriteria: (AlbumsSortOption) -> Unit,
    onChangeAscending: (IsAscending) -> Unit,
    onDismissRequest: () -> Unit,
) {

    SpicyAppMenu(
        visible = visible,
        onDismiss = onDismissRequest,
        title = "Sort Albums"
    ) {
        ListItem(
            headlineContent = { Text(text = "Ascending") },
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onChangeAscending(!isAscending) },
            trailingContent = {
                Checkbox(
                    checked = isAscending,
                    onCheckedChange = null
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

        ListItem(
            headlineContent = { Text("Name") },
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onChangeSortCriteria(AlbumsSortOption.NAME) },
            trailingContent = {
                RadioButton(selected = sortOption == AlbumsSortOption.NAME, onClick = null)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        ListItem(
            headlineContent = { Text("Artist") },
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onChangeSortCriteria(AlbumsSortOption.ARTIST) },
            trailingContent = {
                RadioButton(selected = sortOption == AlbumsSortOption.ARTIST, onClick = null)
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
        ListItem(
            headlineContent = { Text("Number of Songs") },
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable { onChangeSortCriteria(AlbumsSortOption.NUMBER_OF_SONGS) },
            trailingContent = {
                RadioButton(
                    selected = sortOption == AlbumsSortOption.NUMBER_OF_SONGS,
                    onClick = null
                )
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}