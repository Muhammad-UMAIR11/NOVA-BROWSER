package com.example.videobrowser.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrowserTopBar(
    urlInput: String,
    isLoading: Boolean,
    progress: Int,
    activeDownloadCount: Int,
    tabCount: Int,
    searchEngineName: String = "DuckDuckGo",
    blockedAdsCount: Int = 0,
    isAdBlockerActive: Boolean = true,
    onUrlChange: (String) -> Unit,
    onSubmitUrl: (String) -> Unit,
    onRefreshOrStop: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit = {},
    onShieldClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var isFocused by remember { mutableStateOf(false) }

    // Smooth URL container background & border animations
    val containerBgColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "containerBgColor"
    )

    val containerBorderColor by animateColorAsState(
        targetValue = if (isFocused) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "containerBorderColor"
    )

    // Smooth linear progress indicator animation
    val animatedProgress by animateFloatAsState(
        targetValue = if (isLoading) (progress.coerceIn(8, 100) / 100f) else 1f,
        animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing),
        label = "loadingProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // URL / Search Input Bar
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .border(1.dp, containerBorderColor, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                color = containerBgColor
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAdBlockerActive) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (blockedAdsCount > 0) Color(0xFF10B981).copy(alpha = 0.16f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (blockedAdsCount > 0) Color(0xFF10B981).copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onShieldClick() }
                                .testTag("top_bar_shield_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Nova Ad Shield",
                                    tint = if (blockedAdsCount > 0) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                if (blockedAdsCount > 0) {
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "$blockedAdsCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF10B981),
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    val isHttps = urlInput.startsWith("https://")
                    val statusIcon = if (isHttps) Icons.Default.Lock else Icons.Default.Search
                    val statusTint by animateColorAsState(
                        targetValue = if (isHttps) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "statusTint"
                    )

                    Icon(
                        imageVector = statusIcon,
                        contentDescription = "Security Status",
                        tint = statusTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    TextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .weight(1f)
                            .onFocusChanged { isFocused = it.isFocused }
                            .testTag("url_input_field"),
                        placeholder = {
                            Text(
                                "Search with $searchEngineName or type URL",
                                style = TextStyle(fontSize = 13.5.sp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                onSubmitUrl(urlInput)
                            }
                        ),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        )
                    )

                    // Clear button with smooth scale & fade animation
                    AnimatedVisibility(
                        visible = urlInput.isNotEmpty() && isFocused,
                        enter = fadeIn(tween(160)) + scaleIn(tween(160)),
                        exit = fadeOut(tween(140)) + scaleOut(tween(140))
                    ) {
                        IconButton(
                            onClick = { onUrlChange("") },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear address",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Refresh / Stop Action with smooth crossfade
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            onRefreshOrStop()
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        AnimatedContent(
                            targetState = isLoading,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(180)) togetherWith fadeOut(animationSpec = tween(180))
                            },
                            label = "refreshStopCrossfade"
                        ) { loading ->
                            Icon(
                                imageVector = if (loading) Icons.Default.Stop else Icons.Default.Refresh,
                                contentDescription = if (loading) "Stop loading" else "Refresh page",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Active Downloads Action
            IconButton(
                onClick = onOpenDownloads,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("open_downloads_button")
            ) {
                BadgedBox(
                    badge = {
                        if (activeDownloadCount > 0) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Text("$activeDownloadCount")
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Downloads Manager",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Tabs Switcher Action
            IconButton(
                onClick = onOpenTabs,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("open_tabs_button")
            ) {
                BadgedBox(
                    badge = {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                            Text("$tabCount")
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Layers,
                        contentDescription = "Open tabs",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Settings & Preferences Action
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier
                    .size(44.dp)
                    .testTag("open_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Browser Settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Fluid Animated Loading Progress Bar
        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(tween(140)) + expandVertically(tween(180)),
            exit = fadeOut(tween(300)) + shrinkVertically(tween(220))
        ) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}
