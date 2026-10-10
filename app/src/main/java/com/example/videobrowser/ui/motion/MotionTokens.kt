package com.example.videobrowser.ui.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Standard duration tokens for browser UI motion.
 * Designed for responsive 60 FPS interactions with fast response and zero lag.
 */
object MotionDurations {
    /** Ultra-fast micro interactions (toggles, button taps, subtle state flips): 120ms */
    const val Micro: Int = 120

    /** Fast transitions (icon color tints, chip state changes, badge pop): 180ms */
    const val Fast: Int = 180

    /** Standard transitions (tab switching, card expansions, dropdown menus): 240ms */
    const val Medium: Int = 240

    /** Emphasized transitions (bottom sheets, dialogs, fullscreen overlays): 320ms */
    const val Emphasized: Int = 320

    /** Outgoing multiplier: elements exiting the screen leave 25% faster to feel snappy */
    const val ExitDiscount: Float = 0.75f

    /** Calculates exit duration based on entering duration */
    fun exitDuration(enterDuration: Int): Int = (enterDuration * ExitDiscount).toInt()
}

/**
 * Natural easing curves adhering to modern motion principles.
 * Avoids mechanical linear motion and erratic bounces.
 */
object MotionEasing {
    /**
     * Standard easing curve (Material 3 standard).
     * Gentle acceleration into an ultra-smooth deceleration.
     */
    val Standard: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

    /**
     * Emphasized Decelerate curve for incoming elements (sheets, popups, tooltips).
     * The element enters swiftly and glides naturally to its resting position.
     */
    val EmphasizedDecelerate: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

    /**
     * Emphasized Accelerate curve for outgoing/dismissing elements.
     * Starts calmly and accelerates cleanly off-screen without lingering.
     */
    val EmphasizedAccelerate: Easing = CubicBezierEasing(0.3f, 0.0f, 0.8f, 0.15f)

    /** Linear easing for uniform progress / spinners */
    val Linear: Easing = LinearEasing
}

/**
 * Natural spring curves configured strictly without excessive bounce or wobbles.
 * Battery-conscious and physics-based for smooth touch interactions.
 */
object MotionSprings {
    /**
     * Crisp spring with zero bounce (critically damped) for fast UI state transitions.
     */
    fun <T> crisp() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /**
     * Gentle spring with zero bounce (critically damped) for smooth position transitions.
     */
    fun <T> gentle() = spring<T>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /**
     * Subtle spring with a slight natural cushion (dampingRatio = 0.88f) without wobble.
     */
    fun <T> subtle() = spring<T>(
        dampingRatio = 0.88f,
        stiffness = Spring.StiffnessMedium
    )
}

/**
 * Immutable bundle of motion tokens for consistent component-level motion governance.
 */
@Immutable
data class MotionScheme(
    val microDuration: Int = MotionDurations.Micro,
    val fastDuration: Int = MotionDurations.Fast,
    val mediumDuration: Int = MotionDurations.Medium,
    val emphasizedDuration: Int = MotionDurations.Emphasized,
    val standardEasing: Easing = MotionEasing.Standard,
    val decelerateEasing: Easing = MotionEasing.EmphasizedDecelerate,
    val accelerateEasing: Easing = MotionEasing.EmphasizedAccelerate
) {
    companion object {
        val Default = MotionScheme()
    }
}

/**
 * CompositionLocal providing the ambient motion scheme across the browser UI.
 */
val LocalMotionScheme = staticCompositionLocalOf { MotionScheme.Default }
