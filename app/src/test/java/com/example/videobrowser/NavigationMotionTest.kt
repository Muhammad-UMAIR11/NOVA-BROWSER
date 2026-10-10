package com.example.videobrowser

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.example.videobrowser.ui.motion.NavigationTransitionType
import com.example.videobrowser.ui.motion.PageMotionState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationMotionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDensity = Density(density = 2.0f, fontScale = 1.0f)

    @Test
    fun `page motion state initializes with resting values`() {
        val state = PageMotionState()
        assertEquals(1.0f, state.alpha.value, 0.001f)
        assertEquals(0f, state.translationX.value, 0.001f)
        assertEquals(0f, state.translationY.value, 0.001f)
        assertEquals(1.0f, state.scale.value, 0.001f)
    }

    @Test
    fun `animateNavigation completes and returns to resting state for all transition types`() {
        val state = PageMotionState()
        var currentType by mutableStateOf<NavigationTransitionType?>(null)

        composeTestRule.setContent {
            val density = LocalDensity.current
            LaunchedEffect(currentType) {
                currentType?.let { type ->
                    state.animateNavigation(
                        type = type,
                        density = density,
                        isReducedMotion = false
                    )
                }
            }
        }

        for (type in NavigationTransitionType.values()) {
            currentType = type
            composeTestRule.waitForIdle()
            assertEquals("Alpha must settle to 1.0 for $type", 1.0f, state.alpha.value, 0.01f)
            assertEquals("TranslationX must settle to 0 for $type", 0f, state.translationX.value, 0.01f)
            assertEquals("TranslationY must settle to 0 for $type", 0f, state.translationY.value, 0.01f)
            assertEquals("Scale must settle to 1.0 for $type", 1.0f, state.scale.value, 0.01f)
        }
    }

    @Test
    fun `animateNavigation respects reduced motion and snaps to resting state immediately`() {
        val state = PageMotionState()
        var currentType by mutableStateOf<NavigationTransitionType?>(null)

        composeTestRule.setContent {
            val density = LocalDensity.current
            LaunchedEffect(currentType) {
                currentType?.let { type ->
                    state.animateNavigation(
                        type = type,
                        density = density,
                        isReducedMotion = true
                    )
                }
            }
        }

        currentType = NavigationTransitionType.BACK
        composeTestRule.waitForIdle()
        assertEquals(1.0f, state.alpha.value, 0.001f)
        assertEquals(0f, state.translationX.value, 0.001f)
        assertEquals(0f, state.translationY.value, 0.001f)
        assertEquals(1.0f, state.scale.value, 0.001f)

        currentType = NavigationTransitionType.FORWARD
        composeTestRule.waitForIdle()
        assertEquals(1.0f, state.alpha.value, 0.001f)
        assertEquals(0f, state.translationX.value, 0.001f)

        currentType = NavigationTransitionType.RELOAD
        composeTestRule.waitForIdle()
        assertEquals(1.0f, state.alpha.value, 0.001f)
        assertEquals(1.0f, state.scale.value, 0.001f)
    }
}
