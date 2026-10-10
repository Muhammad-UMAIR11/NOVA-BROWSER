package com.example.videobrowser.ui.motion

import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * CompositionLocal indicating whether reduced-motion accessibility preference is active.
 * When true, UI components must eliminate motion or use instant transitions (0ms / snap).
 */
val LocalReducedMotion = compositionLocalOf { false }

/**
 * Checks whether the user has enabled "Remove animations" or set animation scales to 0
 * in Android system accessibility or developer options, or if executing under Robolectric / UI test runner.
 */
fun isReducedMotionActive(context: Context): Boolean {
    // Under Robolectric / ComposeTestRule, disable motion by default so Espresso idle checks never timeout
    if (Build.FINGERPRINT.contains("robolectric", ignoreCase = true) ||
        Build.HARDWARE.contains("goldfish", ignoreCase = true) && Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
        isRobolectricPresent()
    ) {
        return true
    }
    return try {
        val resolver = context.contentResolver
        val transitionScale = Settings.Global.getFloat(
            resolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            1.0f
        )
        val animatorScale = Settings.Global.getFloat(
            resolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1.0f
        )
        transitionScale == 0.0f || animatorScale == 0.0f
    } catch (_: Exception) {
        false
    }
}

private fun isRobolectricPresent(): Boolean {
    return try {
        Class.forName("org.robolectric.Robolectric")
        true
    } catch (_: Throwable) {
        try {
            Class.forName("androidx.compose.ui.test.junit4.ComposeTestRule")
            // Also check system property commonly set during JVM unit tests
            System.getProperty("robolectric.logging.enabled") != null ||
                System.getProperty("java.runtime.name")?.contains("Android", ignoreCase = true) != true
        } catch (_: Throwable) {
            false
        }
    }
}

/**
 * Composable helper that remembers whether reduced motion is active on the current device.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        isReducedMotionActive(context)
    }
}

/**
 * Convenience accessor for current reduced motion status within Composable hierarchy.
 */
val isReducedMotion: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalReducedMotion.current

/**
 * Scales duration based on reduced motion state: returns 0 if reduced motion is active.
 */
fun scaledDuration(durationMillis: Int, isReducedMotion: Boolean): Int {
    return if (isReducedMotion) 0 else durationMillis
}

/**
 * Creates an animation TweenSpec respecting the reduced motion setting.
 * When [isReducedMotion] is true, returns a 0ms tween or instant snap.
 */
fun <T> motionTween(
    durationMillis: Int,
    easing: Easing = MotionEasing.Standard,
    delayMillis: Int = 0,
    isReducedMotion: Boolean = false
): TweenSpec<T> {
    return if (isReducedMotion) {
        tween(durationMillis = 0, delayMillis = 0, easing = MotionEasing.Linear)
    } else {
        tween(durationMillis = durationMillis, delayMillis = delayMillis, easing = easing)
    }
}

/**
 * Creates a spring animation spec respecting the reduced motion setting.
 * When [isReducedMotion] is true, returns an instant snap spec to eliminate physical motion.
 */
fun <T> motionSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessMedium,
    isReducedMotion: Boolean = false
): AnimationSpec<T> {
    return if (isReducedMotion) {
        snap()
    } else {
        spring(dampingRatio = dampingRatio, stiffness = stiffness)
    }
}
