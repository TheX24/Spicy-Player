package com.tx24.spicyplayer.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tx24.spicyplayer.ui.AppScreen

@Composable
fun AppNavigationBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    visible: Boolean,
    colorScheme: ColorScheme,
    topRadius: androidx.compose.ui.unit.Dp = 32.dp,
    bottomRadius: androidx.compose.ui.unit.Dp = 32.dp
) {
    if (visible) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(
                topStart = topRadius, 
                topEnd = topRadius, 
                bottomStart = bottomRadius, 
                bottomEnd = bottomRadius
            ),
            color = colorScheme.surfaceVariant,
            tonalElevation = 6.dp,
            shadowElevation = 0.dp
        ) {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp), // Height increased to 80dp for M3E
                containerColor = Color.Transparent,
                contentColor = colorScheme.onSurfaceVariant,
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.QUEUE,
                    onClick = { onNavigate(AppScreen.QUEUE) },
                    icon = { Icon(Icons.Rounded.QueueMusic, contentDescription = "Queue", modifier = Modifier.size(28.dp)) },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colorScheme.primaryContainer,
                        selectedIconColor = colorScheme.onPrimaryContainer,
                        unselectedIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.NOW_PLAYING,
                    onClick = { onNavigate(AppScreen.NOW_PLAYING) },
                    icon = { Icon(Icons.Rounded.PlayCircle, contentDescription = "Now Playing", modifier = Modifier.size(28.dp)) },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colorScheme.primaryContainer,
                        selectedIconColor = colorScheme.onPrimaryContainer,
                        unselectedIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.LIBRARY,
                    onClick = { onNavigate(AppScreen.LIBRARY) },
                    icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Library", modifier = Modifier.size(28.dp)) },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colorScheme.primaryContainer,
                        selectedIconColor = colorScheme.onPrimaryContainer,
                        unselectedIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
                NavigationBarItem(
                    selected = currentScreen == AppScreen.SETTINGS,
                    onClick = { onNavigate(AppScreen.SETTINGS) },
                    icon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings", modifier = Modifier.size(28.dp)) },
                    alwaysShowLabel = false,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = colorScheme.primaryContainer,
                        selectedIconColor = colorScheme.onPrimaryContainer,
                        unselectedIconColor = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}
