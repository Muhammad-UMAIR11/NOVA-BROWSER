package com.example.videobrowser.ui.glass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.videobrowser.ui.motion.LocalReducedMotion
import com.example.videobrowser.ui.motion.MenuMotionSpecs
import com.example.videobrowser.ui.motion.MotionDurations
import com.example.videobrowser.ui.motion.MotionEasing
import com.example.videobrowser.ui.motion.iosPressEffect
import com.example.videobrowser.ui.motion.motionIosSpring
import com.example.videobrowser.ui.motion.motionTween
import kotlin.math.roundToInt

/**
 * Premium Glassmorphism Design Tokens.
 * Deep translucent midnight glass palette keeping high contrast and futuristic dark identity.
 */
object GlassTokens {
    // 78% alpha deep obsidian slate allows underlying webpage to remain softly visible
    val GlassSurface = Color(0xC70E172A)
    val GlassSurfaceVariant = Color(0xB8131E36)
    val GlassCardBg = Color(0x3D1E293B)
    val GlassCardBgHover = Color(0x59334155)

    // Specular border highlights
    val GlassBorderLight = Color(0x59FFFFFF)
    val GlassBorderDim = Color(0x2E94A3B8)
    val GlassBorderAccent = Color(0x4D38BDF8)

    // Glowing accents
    val GlowCyan = Color(0x3338BDF8)
    val GlowIndigo = Color(0x386366F1)
    val AccentCyan = Color(0xFF38BDF8)
    val AccentIndigo = Color(0xFF818CF8)

    // Scrim
    val GlassScrim = Color(0x66020617)

    // Text with guaranteed contrast
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val TextMuted = Color(0xFF64748B)

    fun glassBorderBrush(shape: Shape = RoundedCornerShape(28.dp)) = Brush.verticalGradient(
        colors = listOf(
            GlassBorderLight,
            GlassBorderAccent,
            GlassBorderDim
        )
    )

    fun glassSurfaceBrush() = Brush.verticalGradient(
        colors = listOf(
            Color(0xCE131F38), // ~80% alpha
            Color(0xD90A1124)  // ~85% alpha
        )
    )

    fun glassHighlightBrush() = Brush.verticalGradient(
        colors = listOf(
            Color(0x35FFFFFF),
            Color(0x08FFFFFF),
            Color.Transparent
        )
    )
}

/**
 * Consistent reusable Glass Bottom Sheet overlay component.
 * Features:
 * - Frosted backdrop scrim keeping underlying webpage visible but softly blurred.
 * - Translucent obsidian glass container floating above the webpage.
 * - Specular border highlight with subtle ambient cyan glow.
 * - Smooth GPU-accelerated enter and exit animations (slide/scale/fade).
 * - Interactive downward drag-to-dismiss gesture.
 */
@Composable
fun GlassBottomSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    testTag: String = "glass_bottom_sheet",
    maxHeightFraction: Float = 0.88f,
    headerActions: @Composable (RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current
    val dragOffsetAnim = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    LaunchedEffect(visible) {
        if (!visible) {
            dragOffsetAnim.snapTo(0f)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(motionTween(MotionDurations.Medium, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = isReducedMotion)),
        exit = fadeOut(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("${testTag}_overlay"),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Translucent glass scrim: webpage remains recognizable behind
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(GlassTokens.GlassScrim)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            // Floating Translucent Glass Panel with coordinated enter/exit
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(maxHeightFraction)
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { if (isReducedMotion) 0 else it },
                            animationSpec = motionIosSpring(dampingRatio = 0.84f, stiffness = Spring.StiffnessMediumLow, isReducedMotion = isReducedMotion)
                        ) + fadeIn(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = isReducedMotion)),
                        exit = slideOutVertically(
                            targetOffsetY = { if (isReducedMotion) 0 else it },
                            animationSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)
                        ) + fadeOut(
                            animationSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)
                        )
                    )
            ) {
                val panelShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .offset { IntOffset(0, dragOffsetAnim.value.roundToInt()) }
                        .shadow(
                            elevation = 28.dp,
                            shape = panelShape,
                            ambientColor = GlassTokens.GlowCyan,
                            spotColor = GlassTokens.GlowIndigo
                        )
                        .clip(panelShape)
                        .border(
                            width = 1.2.dp,
                            brush = GlassTokens.glassBorderBrush(panelShape),
                            shape = panelShape
                        )
                        .background(
                            brush = GlassTokens.glassSurfaceBrush(),
                            shape = panelShape
                        )
                        .testTag(testTag),
                    color = Color.Transparent,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        // DRAG HANDLE & HEADER (Interactive swipe-down area)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            if (dragAmount > 0 || dragOffsetAnim.value > 0) {
                                                change.consume()
                                                coroutineScope.launch {
                                                    dragOffsetAnim.snapTo((dragOffsetAnim.value + dragAmount).coerceAtLeast(0f))
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val dismissThresholdPx = with(density) { 65.dp.toPx() }
                                            coroutineScope.launch {
                                                if (dragOffsetAnim.value > dismissThresholdPx) {
                                                    onDismiss()
                                                } else {
                                                    dragOffsetAnim.animateTo(
                                                        targetValue = 0f,
                                                        animationSpec = spring(
                                                            dampingRatio = 0.80f, // iOS rubber-band spring recovery
                                                            stiffness = Spring.StiffnessMediumLow
                                                        )
                                                    )
                                                }
                                            }
                                        },
                                        onDragCancel = {
                                            coroutineScope.launch {
                                                dragOffsetAnim.animateTo(
                                                    targetValue = 0f,
                                                    animationSpec = spring(
                                                        dampingRatio = 0.80f, // iOS rubber-band spring recovery
                                                        stiffness = Spring.StiffnessMediumLow
                                                    )
                                                )
                                            }
                                        }
                                    )
                                }
                        ) {
                            // Frosted Glass Drag Bar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 4.dp)
                                    .size(width = 42.dp, height = 4.5.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.35f))
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Header row with Icon, Title, Subtitle and Action/Close
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (icon != null) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = GlassTokens.GlassCardBg,
                                            border = BorderStroke(1.dp, GlassTokens.GlassBorderDim),
                                            modifier = Modifier.size(38.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = GlassTokens.AccentCyan,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(22.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = GlassTokens.TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (subtitle != null) {
                                            Text(
                                                text = subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = GlassTokens.TextSecondary,
                                                fontSize = 11.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    headerActions?.invoke(this)

                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .testTag("${testTag}_close_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
                                            tint = GlassTokens.TextSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Panel Body Content
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Reusable Frosted Glass Card for grouping secondary options, tiles, and rows.
 * Features iOS tactile spring depression on tap (0.97 scale with elastic release)
 * and GPU-accelerated rendering on RenderNode.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = GlassTokens.GlassCardBg,
    borderColor: Color = GlassTokens.GlassBorderDim,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current
    val interactionSource = remember { MutableInteractionSource() }
    val clickableModifier = if (onClick != null) {
        Modifier
            .iosPressEffect(
                pressedScale = 0.97f,
                interactionSource = interactionSource,
                isReducedMotion = isReducedMotion
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick
            )
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(color = backgroundColor, shape = shape)
            .then(clickableModifier),
        shape = shape,
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

/**
 * Reusable Frosted Glass Dialog overlay component for confirmation and alerts.
 */
@Composable
fun GlassDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "glass_dialog",
    content: @Composable ColumnScope.() -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current

    if (visible) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("${testTag}_scrim"),
                contentAlignment = Alignment.Center
            ) {
                // Smooth frosted backdrop scrim
                val scrimAlpha by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.Standard, isReducedMotion = isReducedMotion),
                    label = "dialogScrimAlpha"
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(GlassTokens.GlassScrim.copy(alpha = GlassTokens.GlassScrim.alpha * scrimAlpha))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismissRequest
                        )
                )

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = isReducedMotion)) +
                        scaleIn(
                            initialScale = if (isReducedMotion) 1f else 0.92f,
                            animationSpec = motionIosSpring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMedium, isReducedMotion = isReducedMotion)
                        ),
                    exit = fadeOut(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)) +
                        scaleOut(
                            targetScale = if (isReducedMotion) 1f else 0.95f,
                            animationSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)
                        )
                ) {
                    val dialogShape = RoundedCornerShape(26.dp)

                    Surface(
                        modifier = modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .shadow(
                                elevation = 24.dp,
                                shape = dialogShape,
                                ambientColor = GlassTokens.GlowCyan,
                                spotColor = GlassTokens.GlowIndigo
                            )
                            .clip(dialogShape)
                            .border(
                                width = 1.2.dp,
                                brush = GlassTokens.glassBorderBrush(dialogShape),
                                shape = dialogShape
                            )
                            .background(
                                brush = GlassTokens.glassSurfaceBrush(),
                                shape = dialogShape
                            )
                            .testTag(testTag),
                        color = Color.Transparent
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            content = content
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable Frosted Glass Dropdown Panel for popup menus and actions.
 */
@Composable
fun GlassDropdownPanel(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 8.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current
    var isShown by remember { mutableStateOf(expanded) }

    LaunchedEffect(expanded) {
        if (expanded) {
            isShown = true
        } else if (isShown) {
            if (isReducedMotion) {
                isShown = false
            } else {
                delay(MotionDurations.Fast.toLong())
                isShown = false
            }
        }
    }

    if (isShown) {
        Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(offset.x.value.toInt(), offset.y.value.toInt()),
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            AnimatedVisibility(
                visible = expanded,
                enter = MenuMotionSpecs.dropdownEnter(isReducedMotion),
                exit = MenuMotionSpecs.dropdownExit(isReducedMotion)
            ) {
                val menuShape = RoundedCornerShape(16.dp)
                Surface(
                    shape = menuShape,
                    color = Color.Transparent,
                    modifier = modifier
                        .widthIn(min = 180.dp, max = 280.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = menuShape,
                            ambientColor = GlassTokens.GlowCyan,
                            spotColor = GlassTokens.GlowIndigo
                        )
                        .clip(menuShape)
                        .border(
                            width = 1.dp,
                            brush = GlassTokens.glassBorderBrush(menuShape),
                            shape = menuShape
                        )
                        .background(
                            brush = GlassTokens.glassSurfaceBrush(),
                            shape = menuShape
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        content = content
                    )
                }
            }
        }
    }
}

