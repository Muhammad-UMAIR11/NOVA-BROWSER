package com.example.videobrowser.ui.motion

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith

/**
 * Unified transitions for the browser UI.
 * Employs transform (scale/translate) and opacity (alpha) properties, which run on
 * the RenderNode layer on the GPU for a guaranteed 60/120 FPS feel without layout thrashing.
 */
object MotionTransitions {

    /**
     * Subtle, clean fade-in with natural deceleration.
     */
    fun fadeIn(
        isReducedMotion: Boolean = false,
        durationMillis: Int = MotionDurations.Medium
    ): EnterTransition {
        return if (isReducedMotion) {
            fadeIn(animationSpec = snap())
        } else {
            fadeIn(
                animationSpec = motionTween(
                    durationMillis = durationMillis,
                    easing = MotionEasing.EmphasizedDecelerate
                )
            )
        }
    }

    /**
     * Subtle, clean fade-out with snappy acceleration.
     */
    fun fadeOut(
        isReducedMotion: Boolean = false,
        durationMillis: Int = MotionDurations.Fast
    ): ExitTransition {
        return if (isReducedMotion) {
            fadeOut(animationSpec = snap())
        } else {
            fadeOut(
                animationSpec = motionTween(
                    durationMillis = durationMillis,
                    easing = MotionEasing.EmphasizedAccelerate
                )
            )
        }
    }

    /**
     * Subtle GPU-accelerated scale-and-fade in (from 0.96 scale to 1.0).
     * Ideal for dialogs, cards, popups, and dropdown menus.
     */
    fun scaleFadeIn(
        isReducedMotion: Boolean = false,
        initialScale: Float = 0.96f,
        durationMillis: Int = MotionDurations.Medium
    ): EnterTransition {
        if (isReducedMotion) return fadeIn(animationSpec = snap())
        return fadeIn(
            animationSpec = motionTween(
                durationMillis = durationMillis,
                easing = MotionEasing.EmphasizedDecelerate
            )
        ) + scaleIn(
            initialScale = initialScale,
            animationSpec = motionTween(
                durationMillis = durationMillis,
                easing = MotionEasing.EmphasizedDecelerate
            )
        )
    }

    /**
     * Subtle GPU-accelerated scale-and-fade out (from 1.0 to 0.96 scale).
     */
    fun scaleFadeOut(
        isReducedMotion: Boolean = false,
        targetScale: Float = 0.96f,
        durationMillis: Int = MotionDurations.Fast
    ): ExitTransition {
        if (isReducedMotion) return fadeOut(animationSpec = snap())
        return fadeOut(
            animationSpec = motionTween(
                durationMillis = durationMillis,
                easing = MotionEasing.EmphasizedAccelerate
            )
        ) + scaleOut(
            targetScale = targetScale,
            animationSpec = motionTween(
                durationMillis = durationMillis,
                easing = MotionEasing.EmphasizedAccelerate
            )
        )
    }

    /**
     * Subtle vertical slide + fade in.
     * Uses a subtle fractional offset (default 12%) rather than entire screen offset
     * to avoid unnecessary sweeping movements.
     */
    fun slideUpFadeIn(
        isReducedMotion: Boolean = false,
        offsetFraction: Float = 0.12f,
        durationMillis: Int = MotionDurations.Medium
    ): EnterTransition {
        if (isReducedMotion) return fadeIn(animationSpec = snap())
        return fadeIn(
            animationSpec = motionTween(
                durationMillis = durationMillis,
                easing = MotionEasing.EmphasizedDecelerate
            )
        ) + slideInVertically(
            initialOffsetY = { (it * offsetFraction).toInt() },
            animationSpec = motionTween(
                durationMillis = durationMillis,
                easing = MotionEasing.EmphasizedDecelerate
            )
        )
    }

    /**
     * Subtle vertical slide + fade out.
     */
    fun slideDownFadeOut(
        isReducedMotion: Boolean = false,
        offsetFraction: Float = 0.12f,
        durationMillis: Int = MotionDurations.Fast
    ): ExitTransition {
        if (isReducedMotion) return fadeOut(animationSpec = snap())
        return fadeOut(
            animationSpec = motionTween(
                durationMillis = durationMillis,
                easing = MotionEasing.EmphasizedAccelerate
            )
        ) + slideOutVertically(
            targetOffsetY = { (it * offsetFraction).toInt() },
            animationSpec = motionTween(
                durationMillis = durationMillis,
                easing = MotionEasing.EmphasizedAccelerate
            )
        )
    }

    /**
     * Standard fade-through ContentTransform for switching tabs, views, or mode states.
     * Elements fade out swiftly and new content fades in smoothly.
     */
    fun fadeThrough(
        isReducedMotion: Boolean = false
    ): ContentTransform {
        return if (isReducedMotion) {
            fadeIn(animationSpec = snap()) togetherWith fadeOut(animationSpec = snap())
        } else {
            scaleFadeIn(
                isReducedMotion = false,
                initialScale = 0.98f,
                durationMillis = MotionDurations.Medium
            ) togetherWith scaleFadeOut(
                isReducedMotion = false,
                targetScale = 0.98f,
                durationMillis = MotionDurations.Fast
            )
        }
    }
}
