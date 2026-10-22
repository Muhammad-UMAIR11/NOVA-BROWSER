package com.example.videobrowser.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.videobrowser.ui.motion.LocalReducedMotion
import com.example.videobrowser.ui.motion.MotionDurations
import com.example.videobrowser.ui.motion.MotionEasing
import com.example.videobrowser.ui.motion.iosPressEffect
import com.example.videobrowser.ui.motion.motionSpring
import com.example.videobrowser.ui.motion.motionTween

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
    val isReducedMotion = LocalReducedMotion.current

    // Smooth color tint transitions for action buttons
    val backTint by animateColorAsState(
        targetValue = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "backTint"
    )

    val forwardTint by animateColorAsState(
        targetValue = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "forwardTint"
    )

    val toolsIconColor by animateColorAsState(
        targetValue = if (isToolsOpen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "toolsIconColor"
    )

    val mediaIconTint by animateColorAsState(
        targetValue = if (detectedMediaCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
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
            // Interactive Back Button with directional micro-nudge & press feedback
            NavigationControlButton(
                icon = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Navigate Back",
                enabled = canGoBack,
                tint = backTint,
                testTag = "nav_back_button",
                nudgeDirection = -1f,
                onClick = onBack
            )

            // Interactive Forward Button with directional micro-nudge & press feedback
            NavigationControlButton(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Navigate Forward",
                enabled = canGoForward,
                tint = forwardTint,
                testTag = "nav_forward_button",
                nudgeDirection = 1f,
                onClick = onForward
            )

            IconButton(
                onClick = onHome,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.Transparent
                ),
                modifier = Modifier
                    .size(44.dp)
                    .iosPressEffect(pressedScale = 0.88f, isReducedMotion = isReducedMotion)
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
                    .iosPressEffect(pressedScale = 0.88f, isReducedMotion = isReducedMotion)
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
                    .iosPressEffect(pressedScale = 0.88f, isReducedMotion = isReducedMotion)
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
                    .iosPressEffect(pressedScale = 0.88f, isReducedMotion = isReducedMotion)
                    .testTag("nav_media_button")
            ) {
                BadgedBox(
                    badge = {
                        if (detectedMediaCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ) {
                                Text(
                                    text = if (detectedMediaCount > 99) "99+" else detectedMediaCount.toString(),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Detected Media",
                        tint = mediaIconTint,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * Micro-animated navigation button with immediate visual feedback, spring compression,
 * directional tactile nudge, and zero delay to navigation dispatch.
 */
@Composable
private fun NavigationControlButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    tint: Color,
    testTag: String,
    nudgeDirection: Float, // -1f for Back (leftwards), +1f for Forward (rightwards)
    onClick: () -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    var isTriggered by remember { mutableStateOf(false) }
    LaunchedEffect(isTriggered) {
        if (isTriggered) {
            kotlinx.coroutines.delay(160)
            isTriggered = false
        }
    }

    // Immediate button micro-scale on touch press / tap
    val buttonScale by animateFloatAsState(
        targetValue = when {
            isPressed && enabled && !isReducedMotion -> 0.88f
            isTriggered && enabled && !isReducedMotion -> 0.94f
            else -> 1.0f
        },
        animationSpec = motionSpring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
            isReducedMotion = isReducedMotion
        ),
        label = "navButtonScale_$testTag"
    )

    // Directional nudge when pressed or tapped (-2.5dp for back, +2.5dp for forward)
    val nudgeX by animateFloatAsState(
        targetValue = when {
            (isPressed || isTriggered) && enabled && !isReducedMotion -> nudgeDirection * 2.5f
            else -> 0f
        },
        animationSpec = motionSpring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium,
            isReducedMotion = isReducedMotion
        ),
        label = "navNudgeX_$testTag"
    )

    // Immediate subtle highlight halo behind the button when pressed
    val haloAlpha by animateFloatAsState(
        targetValue = if ((isPressed || isTriggered) && enabled && !isReducedMotion) 0.16f else 0f,
        animationSpec = motionTween(MotionDurations.Micro, easing = MotionEasing.Standard, isReducedMotion = isReducedMotion),
        label = "navHaloAlpha_$testTag"
    )

    Box(
        modifier = Modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
                translationX = nudgeX
            }
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = haloAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    isTriggered = true
                    onClick()
                }
            )
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
}
