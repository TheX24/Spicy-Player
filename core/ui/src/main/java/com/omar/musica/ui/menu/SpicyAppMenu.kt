package com.omar.musica.ui.menu

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun SpicyAppMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val transitionState = remember { MutableTransitionState(false) }
    
    LaunchedEffect(visible) {
        transitionState.targetState = visible
    }

    if (transitionState.currentState || transitionState.targetState) {
        val config = LocalConfiguration.current
        val screenHeight = config.screenHeightDp.dp
        
        var offsetY by remember { mutableFloatStateOf(0f) }
        val animatedOffsetY by animateFloatAsState(
            targetValue = offsetY,
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
            label = "Offset"
        )

        val scrollState = rememberScrollState()
        val nestedScrollConnection = remember {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val delta = available.y
                    if (delta > 0 && scrollState.value == 0) {
                        offsetY += delta
                        return Offset(0f, delta)
                    }
                    if (offsetY > 0 && delta < 0) {
                        val consumed = if (offsetY + delta < 0) -offsetY else delta
                        offsetY += consumed
                        return Offset(0f, consumed)
                    }
                    return Offset.Zero
                }

                override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                    if (offsetY > 150) {
                        onDismiss()
                    } else {
                        offsetY = 0f
                    }
                    return super.onPostFling(consumed, available)
                }
            }
        }

        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // Background Dim
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { onDismiss() }
                    )
                }

                // Sheet Content
                AnimatedVisibility(
                    visibleState = transitionState,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
                    ),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                    ),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset { IntOffset(0, animatedOffsetY.roundToInt()) }
                            .nestedScroll(nestedScrollConnection)
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = screenHeight * 0.95f)
                        ) {
                            // Fixed Header
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures(
                                            onDragEnd = {
                                                if (offsetY > 150) {
                                                    onDismiss()
                                                } else {
                                                    offsetY = 0f
                                                }
                                            },
                                            onVerticalDrag = { change, dragAmount ->
                                                change.consume()
                                                offsetY = (offsetY.plus(dragAmount)).coerceAtLeast(0f)
                                            }
                                        )
                                    }
                            ) {
                                Spacer(Modifier.height(12.dp))
                                Box(
                                    modifier = Modifier
                                        .width(32.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        .align(Alignment.CenterHorizontally)
                                )
                                Spacer(Modifier.height(16.dp))

                                if (title != null) {
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleLarge,
                                        modifier = Modifier.padding(bottom = if (subtitle == null) 16.dp else 4.dp, start = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                if (subtitle != null) {
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(bottom = 16.dp, start = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            // Scrollable Content
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .verticalScroll(scrollState)
                                    .padding(horizontal = 16.dp)
                                    .navigationBarsPadding()
                            ) {
                                content()
                                ListItem(
                                    headlineContent = { Spacer(modifier = Modifier.height(100.dp)) },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SpicyActionMenu(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    subtitle: String? = null,
    items: List<MenuActionItem>
) {
    SpicyAppMenu(
        visible = visible,
        onDismiss = onDismiss,
        title = title,
        subtitle = subtitle
    ) {
        items.forEachIndexed { index, item ->
            ListItem(
                headlineContent = { Text(item.title) },
                leadingContent = { Icon(item.icon, contentDescription = null) },
                modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                    onDismiss()
                    item.callback()
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )
            if (index < items.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f)
                )
            }
        }
    }
}
