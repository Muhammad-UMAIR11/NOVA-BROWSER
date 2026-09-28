package com.example.videobrowser.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Sliding Tools Sheet hidden behind a dedicated button in the bottom bar.
 * Features:
 * - Professional layout optimized for efficiency and ergonomics.
 * - Natural downward swipe-to-dismiss gesture handling with real-time translation tracking.
 * - Direct Video Downloader with clipboard paste and sniffing.
 * - Live webpage quick controls (Reload, Desktop/Mobile mode toggle, Share).
 * - Core utilities grid with real-time badges (Downloads, Tabs, Private Shield, Theme).
 * - Modular reserved slots for upcoming capabilities.
 */
@Composable
fun NovaToolsSheet(
    visible: Boolean,
    activeTabTitle: String? = null,
    activeTabUrl: String? = null,
    detectedMediaCount: Int = 0,
    activeDownloadsCount: Int = 0,
    openTabsCount: Int = 1,
    isDesktopMode: Boolean = false,
    isAdBlockerActive: Boolean = true,
    blockedAdsOnPage: Int = 0,
    totalBlockedAds: Int = 0,
    onDismiss: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    onClearCache: () -> Unit,
    onSniffUrl: (String) -> Unit,
    onReloadPage: () -> Unit = {},
    onToggleDesktopMode: () -> Unit = {},
    onOpenDetectedMedia: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val moonlightWhite = Color(0xFFF8FAFC)
    val moonlightMuted = Color(0xFF94A3B8)
    val sheetBackground = Color(0xFF090D16)
    val cardBackground = Color(0xFF131B2E).copy(alpha = 0.85f)
    val cardBorder = Color(0xFF334155).copy(alpha = 0.6f)

    var directUrlInput by remember { mutableStateOf("") }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    val isWebpageActive = activeTabUrl != null &&
        activeTabUrl.isNotBlank() &&
        !activeTabUrl.startsWith("about:") &&
        activeTabUrl != "nova://home"

    // Reset drag offset when visibility changes
    LaunchedEffect(visible) {
        if (!visible) {
            dragOffsetY = 0f
        }
    }

    // Nested scroll connection for naturally sliding down to dismiss when scrolled to top
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < 0 && dragOffsetY > 0f) {
                    val newOffset = (dragOffsetY + available.y).coerceAtLeast(0f)
                    val consumed = dragOffsetY - newOffset
                    dragOffsetY = newOffset
                    return Offset(0f, -consumed)
                }
                return Offset.Zero
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (available.y > 0 && scrollState.value == 0) {
                    dragOffsetY = (dragOffsetY + available.y).coerceAtLeast(0f)
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val dismissThresholdPx = with(density) { 70.dp.toPx() }
                if (dragOffsetY > dismissThresholdPx || (available.y > 750f && dragOffsetY > 10f)) {
                    onDismiss()
                    dragOffsetY = 0f
                    return available
                }
                dragOffsetY = 0f
                return Velocity.Zero
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(220)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .testTag("nova_tools_overlay"),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Scrim (tap outside to smoothly dismiss)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )

            // Sliding Tools Sheet Container with real-time gesture offset
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset { IntOffset(0, dragOffsetY.roundToInt()) }
                        .nestedScroll(nestedScrollConnection)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .border(
                            1.dp,
                            Color(0xFF334155).copy(alpha = 0.45f),
                            RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
                        )
                        .testTag("nova_tools_sheet_surface"),
                    color = sheetBackground,
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .verticalScroll(scrollState)
                    ) {
                        // DRAG HANDLE & HEADER (Interactive swipe-down area)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            if (dragAmount > 0 || dragOffsetY > 0) {
                                                change.consume()
                                                dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                                            }
                                        },
                                        onDragEnd = {
                                            val dismissThresholdPx = with(density) { 70.dp.toPx() }
                                            if (dragOffsetY > dismissThresholdPx) {
                                                onDismiss()
                                            }
                                            dragOffsetY = 0f
                                        },
                                        onDragCancel = {
                                            dragOffsetY = 0f
                                        }
                                    )
                                }
                        ) {
                            // Smooth Drag Handle Bar
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(vertical = 4.dp)
                                    .size(width = 44.dp, height = 5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF64748B).copy(alpha = 0.8f))
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Header Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF1E293B),
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Widgets,
                                            contentDescription = null,
                                            tint = moonlightWhite,
                                            modifier = Modifier
                                                .padding(7.dp)
                                                .size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Nova Tools Hub",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = moonlightWhite
                                        )
                                        Text(
                                            text = "Slide down to close • Browser utilities",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = moonlightMuted,
                                            fontSize = 11.5.sp
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("close_tools_sheet_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close Tools",
                                        tint = moonlightMuted,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // ACTIVE WEBPAGE CONTROLS (Displayed when viewing a webpage)
                        if (isWebpageActive) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = activeTabTitle?.takeIf { it.isNotBlank() } ?: "Current Page",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = moonlightWhite,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Reload
                                        PageActionPill(
                                            icon = Icons.Default.Refresh,
                                            label = "Reload",
                                            onClick = {
                                                onDismiss()
                                                onReloadPage()
                                            },
                                            modifier = Modifier.weight(1f)
                                        )

                                        // 2. Desktop Mode Toggle
                                        PageActionPill(
                                            icon = if (isDesktopMode) Icons.Default.DesktopWindows else Icons.Default.PhoneAndroid,
                                            label = if (isDesktopMode) "Desktop ON" else "Mobile Site",
                                            active = isDesktopMode,
                                            onClick = {
                                                onToggleDesktopMode()
                                            },
                                            modifier = Modifier.weight(1.2f)
                                        )

                                        // 3. Share Page
                                        PageActionPill(
                                            icon = Icons.Default.Share,
                                            label = "Share",
                                            onClick = {
                                                if (activeTabUrl != null) {
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, activeTabUrl)
                                                    }
                                                    context.startActivity(Intent.createChooser(shareIntent, "Share Page Link"))
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    // If media detected on this active page, show direct inspection button
                                    if (detectedMediaCount > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF38BDF8).copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    onDismiss()
                                                    onOpenDetectedMedia()
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.VideoLibrary,
                                                        contentDescription = null,
                                                        tint = Color(0xFF38BDF8),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "$detectedMediaCount videos captured on this page",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Medium,
                                                        color = moonlightWhite
                                                    )
                                                }
                                                Text(
                                                    text = "Inspect →",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF38BDF8)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFF1E293B))
                        Spacer(modifier = Modifier.height(14.dp))

                        // NOVA AD & TRACKER SHIELD (Primary Core Feature)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF064E3B).copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tools_ad_shield_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFF10B981).copy(alpha = 0.2f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier
                                                    .padding(7.dp)
                                                    .size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Always-On Ad Blocker",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = moonlightWhite
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (isAdBlockerActive) Color(0xFF10B981) else Color(0xFF64748B)
                                                ) {
                                                    Text(
                                                        text = if (isAdBlockerActive) "ACTIVE" else "PAUSED",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF022C22),
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Blocks ads, banners & tracking on all sites",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF6EE7B7),
                                                fontSize = 11.5.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF022C22).copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(
                                                text = "This Page",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF6EE7B7).copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "$blockedAdsOnPage blocked",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = moonlightWhite
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFF022C22).copy(alpha = 0.6f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.25f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                            Text(
                                                text = "Lifetime Total",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF6EE7B7).copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                            Text(
                                                text = "$totalBlockedAds total ads",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = moonlightWhite
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SMART VIDEO DOWNLOADER & SNIFFER
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "VIDEO DOWNLOADER & SNIFFER",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.2.sp
                            )

                            // Quick Paste from Clipboard button
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF1E293B),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        val clipText = clipboardManager.getText()?.text
                                        if (!clipText.isNullOrBlank()) {
                                            directUrlInput = clipText.trim()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentPaste,
                                        contentDescription = "Paste from clipboard",
                                        tint = moonlightMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Paste",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = moonlightWhite,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBackground,
                            border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    tint = Color(0xFF38BDF8),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                TextField(
                                    value = directUrlInput,
                                    onValueChange = { directUrlInput = it },
                                    placeholder = {
                                        Text(
                                            "Paste video URL to inspect or stream",
                                            color = moonlightMuted,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Uri,
                                        imeAction = ImeAction.Go
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onGo = {
                                            if (directUrlInput.isNotBlank()) {
                                                val url = directUrlInput.trim()
                                                directUrlInput = ""
                                                onDismiss()
                                                onSniffUrl(url)
                                            }
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("tools_sheet_video_url_input"),
                                    textStyle = TextStyle(color = moonlightWhite, fontSize = 13.5.sp),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent
                                    )
                                )

                                Button(
                                    onClick = {
                                        if (directUrlInput.isNotBlank()) {
                                            val url = directUrlInput.trim()
                                            directUrlInput = ""
                                            onDismiss()
                                            onSniffUrl(url)
                                        }
                                    },
                                    enabled = directUrlInput.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF38BDF8),
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Text("Sniff", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // CORE UTILITIES GRID (Enhanced with live badges)
                        Text(
                            text = "CORE UTILITIES",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = moonlightMuted,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Downloads Hub
                            SheetToolItem(
                                icon = Icons.Default.Download,
                                title = "Downloads",
                                subtitle = "Manage files & speeds",
                                badgeText = if (activeDownloadsCount > 0) "$activeDownloadsCount active" else null,
                                iconTint = Color(0xFF4ADE80),
                                cardBg = cardBackground,
                                cardBorder = cardBorder,
                                textColor = moonlightWhite,
                                subtextColor = moonlightMuted,
                                onClick = {
                                    onDismiss()
                                    onOpenDownloads()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tools_sheet_item_downloads")
                            )

                            // 2. Tabs Switcher
                            SheetToolItem(
                                icon = Icons.Default.Layers,
                                title = "Browser Tabs",
                                subtitle = "Open & manage sessions",
                                badgeText = "$openTabsCount tabs",
                                iconTint = Color(0xFFA78BFA),
                                cardBg = cardBackground,
                                cardBorder = cardBorder,
                                textColor = moonlightWhite,
                                subtextColor = moonlightMuted,
                                onClick = {
                                    onDismiss()
                                    onOpenTabs()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tools_sheet_item_tabs")
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 3. Private Shield & Cache Cleaner
                            SheetToolItem(
                                icon = Icons.Default.Security,
                                title = "Private Shield",
                                subtitle = "Purge cache & cookies",
                                badgeText = "Active",
                                iconTint = Color(0xFFFBBF24),
                                cardBg = cardBackground,
                                cardBorder = cardBorder,
                                textColor = moonlightWhite,
                                subtextColor = moonlightMuted,
                                onClick = {
                                    onDismiss()
                                    onClearCache()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tools_sheet_item_privacy")
                            )

                            // 4. Themes & Settings
                            SheetToolItem(
                                icon = Icons.Default.Palette,
                                title = "Themes & Looks",
                                subtitle = "Wallpaper, colors, fonts",
                                iconTint = Color(0xFFF472B6),
                                cardBg = cardBackground,
                                cardBorder = cardBorder,
                                textColor = moonlightWhite,
                                subtextColor = moonlightMuted,
                                onClick = {
                                    onDismiss()
                                    onOpenSettings()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("tools_sheet_item_settings")
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        // EXPANDABLE EXTENSIONS / RESERVED FUNCTION SLOTS
                        Text(
                            text = "EXPANDABLE EXTENSIONS (RESERVED)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = moonlightMuted.copy(alpha = 0.8f),
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReservedSheetSlot(
                                title = "Reader Mode",
                                subtitle = "Distraction-free reading view",
                                status = "Reserved",
                                cardBg = Color(0xFF0F172A).copy(alpha = 0.4f),
                                cardBorder = Color(0xFF334155).copy(alpha = 0.3f),
                                textColor = moonlightWhite.copy(alpha = 0.7f),
                                subtextColor = moonlightMuted.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            )

                            ReservedSheetSlot(
                                title = "Tracker Shield",
                                subtitle = "Zero telemetry protection",
                                status = "Active",
                                cardBg = Color(0xFF0F172A).copy(alpha = 0.4f),
                                cardBorder = Color(0xFF334155).copy(alpha = 0.3f),
                                textColor = moonlightWhite.copy(alpha = 0.7f),
                                subtextColor = moonlightMuted.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun PageActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (active) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF1E293B),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (active) Color(0xFF38BDF8).copy(alpha = 0.6f) else Color(0xFF334155).copy(alpha = 0.5f)
        ),
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Color(0xFF38BDF8) else Color(0xFFCBD5E1),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.5.sp,
                color = if (active) Color(0xFF38BDF8) else Color(0xFFF8FAFC),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SheetToolItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeText: String? = null,
    iconTint: Color,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = iconTint.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }

                if (badgeText != null) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = iconTint.copy(alpha = 0.18f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, iconTint.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = iconTint,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReservedSheetSlot(
    title: String,
    subtitle: String,
    status: String,
    cardBg: Color,
    cardBorder: Color,
    textColor: Color,
    subtextColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, cardBorder, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        color = cardBg
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.05f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = subtextColor,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF334155).copy(alpha = 0.3f)
                ) {
                    Text(
                        text = status,
                        style = MaterialTheme.typography.labelSmall,
                        color = subtextColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = subtextColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
