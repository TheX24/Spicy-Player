package com.tx24.spicyplayer.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AppMenuBottomSheet(
    showMenu: Boolean,
    onDismiss: () -> Unit,
    colorScheme: ColorScheme,
    onNavigateToEqualizer: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onClearQueue: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = showMenu,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { onDismiss() }
            )
        }

        AnimatedVisibility(
            visible = showMenu,
            enter = slideInVertically { it },
            exit = slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = colorScheme.surfaceVariant,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .navigationBarsPadding()
                ) {
                    Box(
                        modifier = Modifier
                            .width(32.dp)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "More Options",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
                        color = colorScheme.onSurfaceVariant
                    )
                    ListItem(
                        headlineContent = { Text("Equalizer") },
                        leadingContent = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onDismiss()
                            onNavigateToEqualizer()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    ListItem(
                        headlineContent = { Text("Settings") },
                        leadingContent = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        modifier = Modifier.clickable {
                            onDismiss()
                            onNavigateToSettings()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp), color = colorScheme.onSurfaceVariant.copy(alpha = 0.1f))

                    ListItem(
                        headlineContent = { Text("Clear Queue", color = colorScheme.error) },
                        leadingContent = { Icon(Icons.Rounded.DeleteSweep, contentDescription = null, tint = colorScheme.error) },
                        modifier = Modifier.clickable {
                            onDismiss()
                            onClearQueue()
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}
