package com.example.videobrowser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    detectedMediaCount: Int,
    isToolsOpen: Boolean = false,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onToggleTools: () -> Unit = {},
    onNewTab: () -> Unit,
    onOpenMediaSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Smooth color tint transitions for action buttons
    val backTint by animateColorAsState(
        targetValue = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        animationSpec = tween(180),
        label = "backTint"
    )

    val forwardTint by animateColorAsState(
        targetValue = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        animationSpec = tween(180),
        label = "forwardTint"
    )

    val toolsIconColor by animateColorAsState(
        targetValue = if (isToolsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(180),
        label = "toolsIconColor"
    )

    val mediaIconTint by animateColorAsState(
        targetValue = if (detectedMediaCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(180),
        label = "mediaIconTint"
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                enabled = canGoBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("nav_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate Back",
                    tint = backTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onForward,
                enabled = canGoForward,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("nav_forward_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Navigate Forward",
                    tint = forwardTint,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onHome,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("nav_home_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Navigate Home",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onToggleTools,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("nav_tools_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Widgets,
                    contentDescription = "Nova Tools",
                    tint = toolsIconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onNewTab,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("nav_new_tab_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Tab",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(
                onClick = onOpenMediaSheet,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .size(44.dp)
                    .testTag("nav_media_list_button")
            ) {
                BadgedBox(
                    badge = {
                        if (detectedMediaCount > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("$detectedMediaCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Detected Videos",
                        tint = mediaIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
