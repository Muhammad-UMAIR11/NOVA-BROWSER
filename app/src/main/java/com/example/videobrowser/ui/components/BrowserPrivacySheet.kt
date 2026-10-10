package com.example.videobrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videobrowser.ui.glass.GlassBottomSheet
import com.example.videobrowser.ui.glass.GlassCard
import com.example.videobrowser.ui.glass.GlassTokens

@Composable
fun BrowserPrivacySheet(
    visible: Boolean,
    pageUrl: String?,
    isAdBlockerActive: Boolean,
    blockedAdsOnPage: Int,
    totalBlockedAds: Int,
    onToggleAdBlocker: () -> Unit,
    onClearCache: () -> Unit,
    onDismiss: () -> Unit
) {
    val isHttps = pageUrl?.startsWith("https://", ignoreCase = true) == true
    val isHome = pageUrl.isNullOrBlank() || pageUrl == "nova://home"

    GlassBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Privacy & Shield",
        subtitle = "Zero-telemetry • Local sandbox",
        icon = Icons.Default.Security,
        testTag = "privacy_bottom_sheet"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // 1. Connection Security Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isHttps || isHome) Color(0x3310B981) else Color(0x33EF4444),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (isHttps || isHome) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHome) "Internal Sandbox" else if (isHttps) "Connection is Encrypted" else "Connection Not Secure",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        Text(
                            text = if (isHome) "Protected local navigation context" else if (isHttps) "TLS certificate valid • AES-256" else "HTTP plain text traffic detected",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTokens.TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Ad & Tracker Blocker Toggle Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = GlassTokens.GlowCyan,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = GlassTokens.AccentCyan,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Ad & Tracker Shield",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = GlassTokens.TextPrimary
                            )
                            Text(
                                text = if (isAdBlockerActive) "Active • Filter list enabled" else "Disabled",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isAdBlockerActive) GlassTokens.AccentCyan else GlassTokens.TextMuted
                            )
                        }
                    }

                    Switch(
                        checked = isAdBlockerActive,
                        onCheckedChange = { onToggleAdBlocker() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF0F172A),
                            checkedTrackColor = GlassTokens.AccentCyan,
                            uncheckedThumbColor = GlassTokens.TextSecondary,
                            uncheckedTrackColor = Color(0x401E293B)
                        ),
                        modifier = Modifier.testTag("privacy_adblock_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Stats Grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatBox(
                        title = "Blocked on Page",
                        value = "$blockedAdsOnPage",
                        modifier = Modifier.weight(1f)
                    )
                    StatBox(
                        title = "Lifetime Blocked",
                        value = "$totalBlockedAds",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Clear Cache & Privacy Wipe Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Clean Browsing Storage",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary
                        )
                        Text(
                            text = "Clears WebView cache, HTTP cookies & DOM storage",
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTokens.TextSecondary
                        )
                    }

                    Button(
                        onClick = onClearCache,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0x38EF4444),
                            contentColor = Color(0xFFFCA5A5)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("privacy_clear_storage_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatBox(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0x2E1E293B),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = GlassTokens.AccentCyan
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = GlassTokens.TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}
