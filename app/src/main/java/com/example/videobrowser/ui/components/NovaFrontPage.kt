package com.example.videobrowser.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.io.File

/**
 * Custom Front Page / Home Screen for Nova Browser.
 * Features:
 * - Pure deep black background with animated small white moonlight fireflies drifting in the dark.
 * - Sleek, professional "NOVA" typography styled in moonlight white with subtle celestial glow.
 * - Top Search Bar beside a glowing celestial Moon Settings Button at top right (softly glowing moonlight ball).
 * - All tools cleanly accessed from the bottom bar button, keeping the front page decluttered and immersive.
 */
@Composable
fun NovaFrontPage(
    customWallpaperPath: String?,
    onSearch: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    val moonlightWhite = Color(0xFFF8FAFC)
    val moonlightSilver = Color(0xFFCBD5E1)
    val moonlightMuted = Color(0xFF94A3B8)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Background Layer: Custom Wallpaper (if set in Settings) OR Moonlight Fireflies
        if (customWallpaperPath != null && File(customWallpaperPath).exists()) {
            AsyncImage(
                model = File(customWallpaperPath),
                contentDescription = "Custom theme wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            // Atmospheric dark overlay for readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.75f)
                            )
                        )
                    )
            )
        } else {
            // Pitch-black background with animated small white moonlight fireflies
            MoonlightFirefliesBackground(modifier = Modifier.fillMaxSize())
        }

        // 2. Foreground Content: Top Bar (Search + Moon Settings Button) & Centered NOVA Hero
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // TOP ROW: SEARCH BAR + MOON SETTINGS BUTTON AT TOP RIGHT
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Top Search Bar (Clean, rounded translucent pill with neutral placeholder)
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .clip(RoundedCornerShape(27.dp))
                        .border(
                            1.2.dp,
                            Color(0xFF475569).copy(alpha = 0.6f),
                            RoundedCornerShape(27.dp)
                        )
                        .testTag("nova_front_search_bar"),
                    color = Color(0xFF0B1120).copy(alpha = 0.85f),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = moonlightWhite,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))

                        TextField(
                            value = searchInput,
                            onValueChange = { searchInput = it },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("nova_front_search_input"),
                            placeholder = {
                                Text(
                                    text = "Search or enter URL",
                                    style = TextStyle(fontSize = 14.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = moonlightMuted
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Search
                            ),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    if (searchInput.isNotBlank()) {
                                        focusManager.clearFocus()
                                        onSearch(searchInput)
                                    }
                                }
                            ),
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = moonlightWhite,
                                fontWeight = FontWeight.Medium
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        // Clear button
                        AnimatedVisibility(
                            visible = searchInput.isNotEmpty(),
                            enter = fadeIn(tween(140)) + scaleIn(tween(140)),
                            exit = fadeOut(tween(120)) + scaleOut(tween(120))
                        ) {
                            IconButton(
                                onClick = { searchInput = "" },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    modifier = Modifier.size(18.dp),
                                    tint = moonlightMuted
                                )
                            }
                        }

                        // Go button
                        IconButton(
                            onClick = {
                                if (searchInput.isNotBlank()) {
                                    focusManager.clearFocus()
                                    onSearch(searchInput)
                                }
                            },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = moonlightWhite,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Execute Search",
                                    tint = Color(0xFF0F172A),
                                    modifier = Modifier
                                        .padding(6.dp)
                                        .size(18.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // CELESTIAL MOON SETTINGS BUTTON AT TOP RIGHT BESIDES SEARCH BAR
                // Styled as a moonlight-coloured ball that softly glows (not too much)
                Surface(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .border(
                            1.2.dp,
                            Color(0xFF475569).copy(alpha = 0.6f),
                            CircleShape
                        )
                        .clickable { onOpenSettings() }
                        .testTag("nova_moon_settings_button"),
                    shape = CircleShape,
                    color = Color(0xFF0B1120).copy(alpha = 0.85f),
                    shadowElevation = 6.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        GlowingMoonOrb(
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(110.dp))

            // HERO BRANDING: "NOVA" in professional, majestic moonlight white typography
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("nova_hero_branding")
            ) {
                Text(
                    text = "NOVA",
                    style = TextStyle(
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 66.sp,
                        letterSpacing = 16.sp,
                        color = moonlightWhite,
                        shadow = Shadow(
                            color = Color(0x66FFFFFF),
                            offset = Offset(0f, 2f),
                            blurRadius = 20f
                        )
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "ALWAYS-ON AD BLOCKER • PRIVATE & FAST",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 3.2.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp
                    ),
                    color = moonlightSilver.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Ad Blocker Active Pill
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.45f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Text(
                            text = "Ad & Tracker Shield Active • All Sites",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF6EE7B7),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

/**
 * Glowing Moon Orb:
 * Only a moonlight-coloured ball that softly glows (delicate, elegant, not too much).
 */
@Composable
fun GlowingMoonOrb(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "moonGlowTransition")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.16f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val ballRadius = minOf(w, h) * 0.34f

        // 1. Soft Outer Moonlight Glow Halo (gentle, tasteful, non-overpowering radial falloff)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFF8FAFC).copy(alpha = glowPulse),
                    Color(0xFFE2E8F0).copy(alpha = glowPulse * 0.45f),
                    Color.Transparent
                ),
                center = center,
                radius = ballRadius * 1.55f
            ),
            radius = ballRadius * 1.55f,
            center = center
        )

        // 2. The Moonlight-Coloured Ball (Spherical shading with luminous moonlight whites & pearl silver)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFFFFF),       // pure white specular core
                    Color(0xFFF8FAFC),       // moonlight pearl body
                    Color(0xFFE2E8F0),       // subtle rim transition
                    Color(0xFFCBD5E1)        // soft lunar edge shadow
                ),
                center = Offset(center.x - ballRadius * 0.24f, center.y - ballRadius * 0.24f),
                radius = ballRadius * 1.15f
            ),
            radius = ballRadius,
            center = center
        )

        // 3. Subtle translucent inner rim accent
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = ballRadius,
            center = center,
            style = Stroke(width = 1f)
        )
    }
}
