package com.tx24.spicyplayer.ui.menu

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun SongDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    actions: List<MenuActionItem>
) {

    SpicyActionMenu(
        visible = expanded,
        onDismiss = onDismissRequest,
        items = actions
    )


}