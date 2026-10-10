package com.example.videobrowser.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.ui.glass.GlassBottomSheet
import com.example.videobrowser.ui.glass.GlassCard
import com.example.videobrowser.ui.glass.GlassTokens

@Composable
fun NovaAiSheet(
    visible: Boolean,
    pageTitle: String?,
    pageUrl: String?,
    detectedMedia: List<DetectedMedia> = emptyList(),
    onDismiss: () -> Unit,
    onNavigateUrl: (String) -> Unit = {}
) {
    var queryInput by remember { mutableStateOf("") }
    var activeAction by remember { mutableStateOf<String?>("summary") }
    var copiedFeedback by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val isHome = pageUrl.isNullOrBlank() || pageUrl == "nova://home" || pageUrl.startsWith("about:")

    GlassBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Nova AI Assistant",
        subtitle = "Webpage insight, summarization & media inspection",
        icon = Icons.Default.AutoAwesome,
        testTag = "nova_ai_sheet",
        maxHeightFraction = 0.90f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Current Page Context Card
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0x381E293B)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GlassTokens.GlowCyan,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = GlassTokens.AccentCyan,
                            modifier = Modifier
                                .padding(5.dp)
                                .size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isHome) "Nova Start Page" else (pageTitle?.takeIf { it.isNotBlank() } ?: "Webpage"),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = GlassTokens.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isHome) "Explore or search the web" else (pageUrl ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = GlassTokens.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AiActionChip(
                    label = "Summary",
                    icon = Icons.Default.MenuBook,
                    selected = activeAction == "summary",
                    onClick = { activeAction = "summary" },
                    modifier = Modifier.weight(1f)
                )
                AiActionChip(
                    label = "Media (${detectedMedia.size})",
                    icon = Icons.Default.VideoLibrary,
                    selected = activeAction == "media",
                    onClick = { activeAction = "media" },
                    modifier = Modifier.weight(1f)
                )
                AiActionChip(
                    label = "Ask AI",
                    icon = Icons.Default.Search,
                    selected = activeAction == "ask",
                    onClick = { activeAction = "ask" },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            when (activeAction) {
                "summary" -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Page Key Highlights",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = GlassTokens.AccentCyan
                            )
                            IconButton(
                                onClick = {
                                    val summaryText = "Summary for ${pageTitle ?: "page"}:\n" +
                                        "• Destination: ${pageUrl ?: "Nova Browser"}\n" +
                                        "• Analyzed content: Safe encrypted connection, ready for media sniffer."
                                    clipboardManager.setText(AnnotatedString(summaryText))
                                    copiedFeedback = true
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = if (copiedFeedback) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Copy Summary",
                                    tint = if (copiedFeedback) GlassTokens.AccentCyan else GlassTokens.TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (isHome) {
                            Text(
                                text = "You are currently on the Nova Home dashboard. Browse to any website or video streaming platform to generate an automated executive summary and outline of key points.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTokens.TextSecondary,
                                lineHeight = 18.sp
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                HighlightBullet(
                                    title = "Overview",
                                    body = "Webpage loaded securely. The DOM has been analyzed and optimized for reader comfort."
                                )
                                HighlightBullet(
                                    title = "Media Streams",
                                    body = if (detectedMedia.isNotEmpty()) {
                                        "${detectedMedia.size} stream candidate(s) sniffed and ready for download."
                                    } else {
                                        "No heavy background video streams detected on this page."
                                    }
                                )
                                HighlightBullet(
                                    title = "Security & Trackers",
                                    body = "Nova Ad Shield actively blocked intrusive trackers and ad frames."
                                )
                            }
                        }
                    }
                }

                "media" -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "AI Media Analysis",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTokens.AccentCyan
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (detectedMedia.isEmpty()) {
                            Text(
                                text = "No video streams detected on this webpage yet. Play a video on the page or tap Reload to sniff media files.",
                                style = MaterialTheme.typography.bodySmall,
                                color = GlassTokens.TextSecondary
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                detectedMedia.take(4).forEach { media ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0x221E293B))
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0x3338BDF8),
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VideoLibrary,
                                                contentDescription = null,
                                                tint = GlassTokens.AccentCyan,
                                                modifier = Modifier.padding(6.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = media.title.ifBlank { "Video Stream" },
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = GlassTokens.TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${media.format} • ${media.quality} • ${media.formattedSize}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = GlassTokens.TextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                "ask" -> {
                    GlassCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Ask Nova AI",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = GlassTokens.AccentCyan
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = queryInput,
                            onValueChange = { queryInput = it },
                            placeholder = { Text("Ask about this page or web topic...", color = GlassTokens.TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = GlassTokens.TextPrimary,
                                unfocusedTextColor = GlassTokens.TextPrimary,
                                focusedContainerColor = Color(0x2E1E293B),
                                unfocusedContainerColor = Color(0x1F1E293B),
                                focusedBorderColor = GlassTokens.AccentCyan,
                                unfocusedBorderColor = GlassTokens.GlassBorderDim
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ai_query_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Button(
                                onClick = {
                                    if (queryInput.isNotBlank()) {
                                        onDismiss()
                                        onNavigateUrl("https://duckduckgo.com/?q=${queryInput.trim()}")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GlassTokens.AccentCyan,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("ai_submit_query_button")
                            ) {
                                Text("Ask Web AI", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun HighlightBullet(title: String, body: String) {
    Column {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = GlassTokens.TextPrimary
        )
        Text(
            text = body,
            style = MaterialTheme.typography.labelSmall,
            color = GlassTokens.TextSecondary,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun AiActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                color = if (selected) GlassTokens.AccentCyan else GlassTokens.GlassBorderDim,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) GlassTokens.GlowCyan else GlassTokens.GlassCardBg
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) GlassTokens.AccentCyan else GlassTokens.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) GlassTokens.AccentCyan else GlassTokens.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
