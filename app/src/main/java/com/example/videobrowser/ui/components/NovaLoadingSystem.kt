package com.example.videobrowser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videobrowser.ui.glass.GlassTokens
import com.example.videobrowser.ui.motion.LocalReducedMotion
import com.example.videobrowser.ui.motion.MotionDurations
import com.example.videobrowser.ui.motion.motionTween

/**
 * Creates a smooth, GPU-accelerated horizontal shimmer gradient brush.
 * Reflects actual UI loading state while respecting user's Reduced Motion preferences.
 */
@Composable
fun rememberShimmerBrush(
    targetColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    shimmerColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
): Brush {
    val isReducedMotion = LocalReducedMotion.current
    if (isReducedMotion) {
        return SolidColor(targetColor.copy(alpha = 0.6f))
    }

    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = -400f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    return Brush.linearGradient(
        colors = listOf(
            targetColor.copy(alpha = 0.35f),
            shimmerColor,
            targetColor.copy(alpha = 0.35f)
        ),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim + 300f, 0f)
    )
}

/**
 * Modifier extension that applies an animated shimmer to any shape.
 */
fun Modifier.shimmerBackground(shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(8.dp)): Modifier = composed {
    val brush = rememberShimmerBrush()
    this
        .clip(shape)
        .background(brush)
}

/**
 * Lightweight, elegant webpage loading skeleton.
 * Displayed while the browser tab is loading a new URL (until first contentful render),
 * displaying the ACTUAL progress value from the Android WebView engine.
 */
@Composable
fun WebPageLoadingSkeleton(
    progress: Int,
    url: String,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val shimmerBrush = rememberShimmerBrush()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .testTag("webpage_loading_skeleton")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Real Progress Line at the very top of skeleton
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Loading ${if (url.length > 36) url.take(36) + "…" else url}",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTokens.TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "$progress%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.AccentCyan,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { (progress.coerceIn(5, 100)) / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(1.5.dp)),
                    color = GlassTokens.AccentCyan,
                    trackColor = Color(0x3338BDF8)
                )
            }

            // Hero Banner Skeleton Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(shimmerBrush)
                    .border(1.dp, GlassTokens.GlassBorderDim, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Subtle glowing orb representing media inspection
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(GlassTokens.GlowCyan)
                )
            }

            // Headline skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.72f)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(shimmerBrush)
            )

            // Subtitle skeleton
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Paragraph body skeletons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Two card skeletons side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerBrush)
                        .border(1.dp, GlassTokens.GlassBorderDim, RoundedCornerShape(12.dp))
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerBrush)
                        .border(1.dp, GlassTokens.GlassBorderDim, RoundedCornerShape(12.dp))
                )
            }
        }
    }
}

/**
 * Animated Radar Sweep for video detection states.
 * Uses real mathematical angles and smooth continuous rotation to visually indicate
 * that the background sniffer is currently inspecting network requests and DOM elements.
 */
@Composable
fun VideoDetectionRadarSweep(
    modifier: Modifier = Modifier,
    tint: Color = GlassTokens.AccentCyan
) {
    val isReducedMotion = LocalReducedMotion.current

    if (isReducedMotion) {
        Icon(
            imageVector = Icons.Default.Radar,
            contentDescription = "Detecting video streams",
            tint = tint,
            modifier = modifier
        )
        return
    }

    val infiniteTransition = rememberInfiniteTransition(label = "radarSweep")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarRotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Expanding sonar ripple
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (size.minDimension / 2f) * pulseScale
            drawCircle(
                color = tint.copy(alpha = (1.25f - pulseScale).coerceIn(0f, 0.45f)),
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Rotating radar beam
        Icon(
            imageVector = Icons.Default.Radar,
            contentDescription = "Sniffing video streams",
            tint = tint,
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        )
    }
}

/**
 * Video detection loading placeholder shown inside DetectedMediaSheet when
 * the webpage is still actively sniffing for video files.
 */
@Composable
fun VideoDetectionSkeleton(
    pageTitle: String,
    modifier: Modifier = Modifier
) {
    val shimmerBrush = rememberShimmerBrush()

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(1.dp, GlassTokens.GlassBorderDim),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Rotating scanner radar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(GlassTokens.GlowCyan),
                contentAlignment = Alignment.Center
            ) {
                VideoDetectionRadarSweep(
                    modifier = Modifier.size(32.dp),
                    tint = GlassTokens.AccentCyan
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Scanning for video streams…",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Intercepting network playlists (HLS, MP4, WebM) on $pageTitle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Shimmer quality option skeletons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

/**
 * Animated Search Loading Beam.
 * Runs smoothly across the bottom of the search/address bar while network navigation or query dispatch is active.
 */
@Composable
fun SearchLoadingBeam(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    beamColor: Color = GlassTokens.AccentCyan
) {
    val isReducedMotion = LocalReducedMotion.current

    AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)),
        exit = fadeOut(motionTween(MotionDurations.Medium, isReducedMotion = isReducedMotion))
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "searchBeam")
        val offsetX by infiniteTransition.animateFloat(
            initialValue = -0.5f,
            targetValue = 1.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "searchBeamOffset"
        )

        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(2.5.dp)
        ) {
            val width = size.width
            val startX = (offsetX - 0.35f) * width
            val endX = (offsetX + 0.35f) * width

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        beamColor.copy(alpha = 0.85f),
                        beamColor,
                        beamColor.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    startX = startX,
                    endX = endX
                )
            )
        }
    }
}

/**
 * Animated sound wave / equalizer bars specifically for the Audio-Only download option.
 * Displays subtle animated rhythm that gives the audio-only option a distinctive, modern character.
 */
@Composable
fun AudioWaveAnimation(
    modifier: Modifier = Modifier,
    tint: Color = Color(0xFFA855F7),
    isAnimating: Boolean = true
) {
    val isReducedMotion = LocalReducedMotion.current
    val infiniteTransition = rememberInfiniteTransition(label = "audioWave")

    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )

    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 0.9f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )

    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val heights = if (isAnimating && !isReducedMotion) {
            listOf(bar1Height, bar2Height, bar3Height)
        } else {
            listOf(0.5f, 0.9f, 0.65f)
        }

        heights.forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((14 * fraction).coerceAtLeast(3f).dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(tint)
            )
        }
    }
}

/**
 * Circular progress indicator for real async operations in Settings & Preferences (such as cache clearing).
 */
@Composable
fun AsyncOperationProgressIndicator(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    strokeWidth: Dp = 2.dp
) {
    CircularProgressIndicator(
        modifier = modifier.size(16.dp),
        color = tint,
        strokeWidth = strokeWidth
    )
}
