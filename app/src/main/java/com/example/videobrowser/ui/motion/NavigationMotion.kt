package com.example.videobrowser.ui.motion

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Types of page-level navigation transitions.
 */
enum class NavigationTransitionType {
    NEW_PAGE,
    BACK,
    FORWARD,
    RELOAD
}

/**
 * State holder that coordinates subtle GPU-accelerated page navigation animations.
 * Operates purely on RenderNode transform/opacity layers to maintain steady 60 FPS,
 * never delaying or blocking the underlying WebView page loading or DOM rendering.
 */
@Stable
class PageMotionState {
    val alpha = Animatable(1.0f)
    val translationX = Animatable(0f)
    val translationY = Animatable(0f)
    val scale = Animatable(1.0f)

    suspend fun animateNavigation(
        type: NavigationTransitionType,
        density: Density,
        isReducedMotion: Boolean
    ) {
        if (isReducedMotion) {
            alpha.snapTo(1f)
            translationX.snapTo(0f)
            translationY.snapTo(0f)
            scale.snapTo(1f)
            return
        }

        when (type) {
            NavigationTransitionType.BACK -> {
                val offsetPx = with(density) { -14.dp.toPx() }
                translationX.snapTo(offsetPx)
                alpha.snapTo(0.86f)
                scale.snapTo(1.0f)
                translationY.snapTo(0f)

                coroutineScope {
                    launch {
                        translationX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 1.0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                }
            }
            NavigationTransitionType.FORWARD -> {
                val offsetPx = with(density) { 14.dp.toPx() }
                translationX.snapTo(offsetPx)
                alpha.snapTo(0.86f)
                scale.snapTo(1.0f)
                translationY.snapTo(0f)

                coroutineScope {
                    launch {
                        translationX.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 1.0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                }
            }
            NavigationTransitionType.NEW_PAGE -> {
                val offsetPx = with(density) { 8.dp.toPx() }
                translationX.snapTo(0f)
                translationY.snapTo(offsetPx)
                alpha.snapTo(0.88f)
                scale.snapTo(0.988f)

                coroutineScope {
                    launch {
                        translationY.animateTo(
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                    launch {
                        scale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 1.0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                }
            }
            NavigationTransitionType.RELOAD -> {
                translationX.snapTo(0f)
                translationY.snapTo(0f)
                scale.snapTo(0.993f)
                alpha.snapTo(0.76f)

                coroutineScope {
                    launch {
                        scale.animateTo(
                            targetValue = 1.0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                    launch {
                        alpha.animateTo(
                            targetValue = 1.0f,
                            animationSpec = tween(
                                durationMillis = MotionDurations.Fast,
                                easing = MotionEasing.EmphasizedDecelerate
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Modifier extension applying the GPU transform/opacity layers of [PageMotionState].
 */
fun Modifier.pageMotionLayer(state: PageMotionState): Modifier = this.graphicsLayer {
    this.alpha = state.alpha.value
    this.translationX = state.translationX.value
    this.translationY = state.translationY.value
    this.scaleX = state.scale.value
    this.scaleY = state.scale.value
}

/**
 * Remembers a [PageMotionState] instance across recompositions.
 */
@Composable
fun rememberPageMotionState(): PageMotionState = remember { PageMotionState() }
