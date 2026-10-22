package com.example.videobrowser.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring

/**
 * iOS-optimized fluid tactile press effect.
 * Uses GPU [graphicsLayer] on the RenderNode without triggering relayout or measure passes,
 * ensuring steady 60/120 FPS buttery smoothness without frame drops or lag.
 *
 * Implements iOS touch physics:
 * - Instant responsive depression on touch-down (StiffnessHigh)
 * - Elastic, organic spring rebound on release (dampingRatio = 0.78f, StiffnessMediumLow)
 *
 * Automatically respects [isReducedMotion].
 */
fun Modifier.iosPressEffect(
    pressedScale: Float = 0.96f,
    interactionSource: MutableInteractionSource? = null,
    isReducedMotion: Boolean = false
): Modifier = composed {
    if (isReducedMotion) return@composed this

    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = if (isPressed) {
            spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessHigh
            )
        } else {
            spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            )
        },
        label = "iosPressScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Lightweight, GPU-accelerated touch feedback modifier using [graphicsLayer].
 * Modifies scale directly on the RenderNode without triggering relayout or remeasure passes,
 * ensuring strict 60 FPS fluidity with minimal battery impact.
 *
 * Automatically respects [isReducedMotion].
 */
fun Modifier.motionPressEffect(
    pressedScale: Float = 0.97f,
    interactionSource: MutableInteractionSource? = null,
    isReducedMotion: Boolean = false
): Modifier = iosPressEffect(
    pressedScale = pressedScale,
    interactionSource = interactionSource,
    isReducedMotion = isReducedMotion
)

/**
 * GPU-accelerated smooth alpha transition.
 * Uses [graphicsLayer] to avoid full recomposition and layout overhead during fading.
 */
fun Modifier.motionAlpha(
    targetAlpha: Float,
    durationMillis: Int = MotionDurations.Fast,
    isReducedMotion: Boolean = false
): Modifier = composed {
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        animationSpec = motionTween(
            durationMillis = durationMillis,
            easing = MotionEasing.Standard,
            isReducedMotion = isReducedMotion
        ),
        label = "motionAlpha"
    )

    this.graphicsLayer {
        this.alpha = alpha
    }
}
