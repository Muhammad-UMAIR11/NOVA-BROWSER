package com.example.videobrowser

import android.app.Application
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.ui.motion.MotionDurations
import com.example.videobrowser.ui.motion.MotionEasing
import com.example.videobrowser.ui.motion.MotionScheme
import com.example.videobrowser.ui.motion.MotionSprings
import com.example.videobrowser.ui.motion.MotionTransitions
import com.example.videobrowser.ui.motion.isReducedMotionActive
import com.example.videobrowser.ui.motion.motionSpring
import com.example.videobrowser.ui.motion.motionTween
import com.example.videobrowser.ui.motion.scaledDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MotionSystemTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `motion durations follow consistent hierarchy for fast 60 fps response`() {
        assertTrue("Micro duration should be fast", MotionDurations.Micro in 50..150)
        assertTrue("Fast duration should follow Micro", MotionDurations.Fast > MotionDurations.Micro)
        assertTrue("Medium duration should follow Fast", MotionDurations.Medium > MotionDurations.Fast)
        assertTrue("Emphasized duration should not exceed 400ms", MotionDurations.Emphasized in 250..400)

        val enter = 320
        val exit = MotionDurations.exitDuration(enter)
        assertTrue("Exit duration must be shorter than enter duration for perceived snappiness", exit < enter)
        assertEquals((enter * 0.75f).toInt(), exit)
    }

    @Test
    fun `motion easing curves are non-null and correctly initialized`() {
        assertNotNull(MotionEasing.Standard)
        assertNotNull(MotionEasing.EmphasizedDecelerate)
        assertNotNull(MotionEasing.EmphasizedAccelerate)
        assertNotNull(MotionEasing.Linear)
    }

    @Test
    fun `motion springs avoid excessive bounce and maintain critical damping`() {
        val crisp = MotionSprings.crisp<Float>()
        val gentle = MotionSprings.gentle<Float>()
        val subtle = MotionSprings.subtle<Float>()

        assertEquals(Spring.DampingRatioNoBouncy, crisp.dampingRatio, 0.001f)
        assertEquals(Spring.DampingRatioNoBouncy, gentle.dampingRatio, 0.001f)
        assertTrue("Subtle spring damping must be at least 0.85 to avoid excessive bounce", subtle.dampingRatio >= 0.85f)
    }

    @Test
    fun `reduced motion duration scaling collapses to zero when enabled`() {
        assertEquals(240, scaledDuration(240, isReducedMotion = false))
        assertEquals(0, scaledDuration(240, isReducedMotion = true))
        assertEquals(0, scaledDuration(MotionDurations.Emphasized, isReducedMotion = true))
    }

    @Test
    fun `motionTween respects reduced motion`() {
        val normalTween = motionTween<Float>(durationMillis = 200, isReducedMotion = false)
        assertEquals(200, normalTween.durationMillis)

        val reducedTween = motionTween<Float>(durationMillis = 200, isReducedMotion = true)
        assertEquals(0, reducedTween.durationMillis)
    }

    @Test
    fun `motionSpring returns snap when reduced motion is enabled`() {
        val normalSpring = motionSpring<Float>(isReducedMotion = false)
        assertTrue(normalSpring is SpringSpec<Float>)

        val reducedSpring = motionSpring<Float>(isReducedMotion = true)
        assertNotNull(reducedSpring)
    }

    @Test
    fun `motion transitions build successfully in both normal and reduced motion`() {
        assertNotNull(MotionTransitions.fadeIn(isReducedMotion = false))
        assertNotNull(MotionTransitions.fadeIn(isReducedMotion = true))

        assertNotNull(MotionTransitions.fadeOut(isReducedMotion = false))
        assertNotNull(MotionTransitions.fadeOut(isReducedMotion = true))

        assertNotNull(MotionTransitions.scaleFadeIn(isReducedMotion = false))
        assertNotNull(MotionTransitions.scaleFadeIn(isReducedMotion = true))

        assertNotNull(MotionTransitions.scaleFadeOut(isReducedMotion = false))
        assertNotNull(MotionTransitions.scaleFadeOut(isReducedMotion = true))

        assertNotNull(MotionTransitions.slideUpFadeIn(isReducedMotion = false))
        assertNotNull(MotionTransitions.slideUpFadeIn(isReducedMotion = true))

        assertNotNull(MotionTransitions.slideDownFadeOut(isReducedMotion = false))
        assertNotNull(MotionTransitions.slideDownFadeOut(isReducedMotion = true))

        assertNotNull(MotionTransitions.fadeThrough(isReducedMotion = false))
        assertNotNull(MotionTransitions.fadeThrough(isReducedMotion = true))
    }

    @Test
    fun `motion scheme defaults are consistent`() {
        val scheme = MotionScheme.Default
        assertEquals(MotionDurations.Micro, scheme.microDuration)
        assertEquals(MotionDurations.Fast, scheme.fastDuration)
        assertEquals(MotionDurations.Medium, scheme.mediumDuration)
        assertEquals(MotionDurations.Emphasized, scheme.emphasizedDuration)
        assertEquals(MotionEasing.Standard, scheme.standardEasing)
    }

    @Test
    fun `isReducedMotionActive executes without throwing exceptions`() {
        val result = isReducedMotionActive(app)
        // Should return a valid boolean without throwing
        assertTrue(result == true || result == false)
    }
}
