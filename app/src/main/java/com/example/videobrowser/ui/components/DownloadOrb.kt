package com.example.videobrowser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videobrowser.viewmodel.OrbStatus

@Composable
fun DownloadOrb(
    visible: Boolean,
    detectedCount: Int,
    isPlaying: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    orbStatus: OrbStatus = if (detectedCount > 0) OrbStatus.SUPPORTED_FOUND else OrbStatus.HIDDEN
) {
    val isActuallyVisible = visible && orbStatus != OrbStatus.HIDDEN

    AnimatedVisibility(
        visible = isActuallyVisible,
        enter = scaleIn(animationSpec = androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)) + fadeIn(animationSpec = tween(250)),
        exit = scaleOut(animationSpec = tween(180, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(180)),
        modifier = modifier
    ) {
        // Infinite transition for subtle pulse when active
        val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = if (isPlaying) 1.12f else 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.5f,
            targetValue = 0.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )

        val (gradientColors, iconVector, badgeColor) = when (orbStatus) {
            OrbStatus.UNSUPPORTED_MEDIA -> Triple(
                listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
                Icons.Default.Lock,
                Color(0xFFD97706)
            )
            OrbStatus.ERROR -> Triple(
                listOf(Color(0xFFDC2626), Color(0xFFEF4444)),
                Icons.Default.Warning,
                Color(0xFFDC2626)
            )
            OrbStatus.DETECTING -> Triple(
                listOf(Color(0xFF4B5563), Color(0xFF6B7280)),
                Icons.Default.Search,
                Color(0xFF4B5563)
            )
            else -> {
                if (isPlaying) {
                    Triple(
                        listOf(Color(0xFF059669), Color(0xFF10B981)),
                        Icons.Default.PlayArrow,
                        Color(0xFFEF4444)
                    )
                } else {
                    Triple(
                        listOf(Color(0xFF1D4ED8), Color(0xFF0284C7)),
                        Icons.Default.Download,
                        Color(0xFFEF4444)
                    )
                }
            }
        }

        val glowColor = gradientColors.last().copy(alpha = pulseAlpha)

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(72.dp)
                .semantics { contentDescription = "Video Download Orb, $detectedCount videos available, status: $orbStatus" }
                .testTag("download_orb_button")
        ) {
            // Pulsing outer ripple glow
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                glowColor,
                                Color.Transparent
                            )
                        )
                    )
            )

            // Main Orb Body
            Surface(
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true, color = Color.White),
                        onClick = onClick
                    )
            ) {
                Box(
                    modifier = Modifier
                        .background(Brush.linearGradient(colors = gradientColors))
                        .border(
                            width = 1.5.dp,
                            color = Color.White.copy(alpha = 0.6f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconVector,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Count Badge with smooth roll transition
            AnimatedVisibility(
                visible = detectedCount > 0,
                enter = scaleIn(androidx.compose.animation.core.spring(dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = scaleOut() + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(badgeColor)
                        .border(1.5.dp, Color.White, CircleShape)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = if (detectedCount > 99) "99+" else "$detectedCount",
                        transitionSpec = {
                            androidx.compose.animation.slideInVertically { -it } + fadeIn() togetherWith
                                    androidx.compose.animation.slideOutVertically { it } + fadeOut()
                        },
                        label = "badgeCountRoll"
                    ) { countText ->
                        Text(
                            text = countText,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 12.sp
                        )
                    }
                }
            }
        }
    }
}
