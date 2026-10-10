package com.example.videobrowser.ui.motion

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

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
): Modifier = composed {
    if (isReducedMotion) return@composed this

    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1.0f,
        animationSpec = motionTween(
            durationMillis = if (isPressed) MotionDurations.Micro else MotionDurations.Fast,
            easing = if (isPressed) MotionEasing.EmphasizedAccelerate else MotionEasing.EmphasizedDecelerate,
            isReducedMotion = false
        ),
        label = "motionPressScale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

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
