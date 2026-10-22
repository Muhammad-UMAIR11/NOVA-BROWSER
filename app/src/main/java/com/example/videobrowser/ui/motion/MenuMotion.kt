package com.example.videobrowser.ui.motion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.videobrowser.ui.glass.GlassDropdownPanel

/**
 * Standard transitions for browser menus, dropdown panels, and context dialogs.
 * Ensures short, snappy, GPU-accelerated enter and exit animations:
 * - Opening: Fade in + slightly translate & scale into position.
 * - Closing: Smoothly reverses the animation.
 */
object MenuMotionSpecs {

    fun menuEnter(
        isReducedMotion: Boolean,
        fromBottom: Boolean = false,
        durationMillis: Int = MotionDurations.Fast
    ): EnterTransition {
        if (isReducedMotion) {
            return fadeIn(animationSpec = motionTween(0, isReducedMotion = true))
        }
        val slideOffsetFactor = if (fromBottom) 0.25f else -0.12f
        return fadeIn(
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        ) + slideInVertically(
            initialOffsetY = { (it * slideOffsetFactor).toInt() },
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        ) + scaleIn(
            initialScale = 0.94f,
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        )
    }

    fun menuExit(
        isReducedMotion: Boolean,
        toBottom: Boolean = false,
        durationMillis: Int = MotionDurations.Fast
    ): ExitTransition {
        if (isReducedMotion) {
            return fadeOut(animationSpec = motionTween(0, isReducedMotion = true))
        }
        val slideOffsetFactor = if (toBottom) 0.25f else -0.12f
        return fadeOut(
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        ) + slideOutVertically(
            targetOffsetY = { (it * slideOffsetFactor).toInt() },
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        ) + scaleOut(
            targetScale = 0.94f,
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        )
    }

    fun dropdownEnter(
        isReducedMotion: Boolean,
        durationMillis: Int = MotionDurations.Fast
    ): EnterTransition {
        if (isReducedMotion) {
            return fadeIn(animationSpec = motionTween(0, isReducedMotion = true))
        }
        return fadeIn(
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        ) + slideInVertically(
            initialOffsetY = { -((it * 0.14f).toInt()) },
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        ) + scaleIn(
            initialScale = 0.93f,
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        )
    }

    fun dropdownExit(
        isReducedMotion: Boolean,
        durationMillis: Int = MotionDurations.Fast
    ): ExitTransition {
        if (isReducedMotion) {
            return fadeOut(animationSpec = motionTween(0, isReducedMotion = true))
        }
        return fadeOut(
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        ) + slideOutVertically(
            targetOffsetY = { -((it * 0.14f).toInt()) },
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        ) + scaleOut(
            targetScale = 0.93f,
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        )
    }

    fun dialogEnter(
        isReducedMotion: Boolean,
        durationMillis: Int = MotionDurations.Fast
    ): EnterTransition {
        if (isReducedMotion) {
            return fadeIn(animationSpec = motionTween(0, isReducedMotion = true))
        }
        return fadeIn(
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        ) + slideInVertically(
            initialOffsetY = { (it * 0.08f).toInt() },
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        ) + scaleIn(
            initialScale = 0.92f,
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = false)
        )
    }

    fun dialogExit(
        isReducedMotion: Boolean,
        durationMillis: Int = MotionDurations.Fast
    ): ExitTransition {
        if (isReducedMotion) {
            return fadeOut(animationSpec = motionTween(0, isReducedMotion = true))
        }
        return fadeOut(
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        ) + slideOutVertically(
            targetOffsetY = { (it * 0.08f).toInt() },
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        ) + scaleOut(
            targetScale = 0.92f,
            animationSpec = motionTween(durationMillis, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = false)
        )
    }
}

/**
 * Polished, animated dropdown panel with glassmorphism surface, border, and glow.
 */
@Composable
fun AnimatedDropdownPanel(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    GlassDropdownPanel(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        content = content
    )
}

/**
 * Standard menu item for dropdown and context menus with accessible 48dp touch targets.
 */
@Composable
fun AnimatedDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) tint else tint.copy(alpha = 0.38f),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) tint else tint.copy(alpha = 0.38f)
        )
    }
}
