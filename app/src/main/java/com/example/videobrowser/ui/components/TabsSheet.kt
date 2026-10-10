package com.example.videobrowser.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videobrowser.model.BrowserTab
import com.example.videobrowser.ui.glass.GlassBottomSheet
import com.example.videobrowser.ui.glass.GlassCard
import com.example.videobrowser.ui.glass.GlassTokens
import com.example.videobrowser.ui.motion.AnimatedDropdownMenuItem
import com.example.videobrowser.ui.motion.AnimatedDropdownPanel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabsSheet(
    sheetState: SheetState? = null,
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelectTab: (String) -> Unit,
    onCloseTab: (String) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit,
    visible: Boolean = true
) {
    val clipboardManager = LocalClipboardManager.current
    var menuExpandedForTabId by remember { mutableStateOf<String?>(null) }

    GlassBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Browser Tabs",
        subtitle = "${tabs.size} open tabs • 1-tap switch",
        icon = Icons.Default.Layers,
        testTag = "tabs_bottom_sheet",
        headerActions = {
            Button(
                onClick = onNewTab,
                colors = ButtonDefaults.buttonColors(
                    containerColor = GlassTokens.AccentCyan,
                    contentColor = Color(0xFF0F172A)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("tabs_add_new_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Tab", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                items(tabs, key = { it.id }) { tab ->
                    val isActive = tab.id == activeTabId

                    GlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = if (isActive) Color(0x3838BDF8) else Color(0x2E1E293B),
                        borderColor = if (isActive) GlassTokens.AccentCyan else GlassTokens.GlassBorderDim,
                        onClick = {
                            onDismiss()
                            onSelectTab(tab.id)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isActive) GlassTokens.AccentCyan.copy(alpha = 0.25f) else Color(0x2B94A3B8),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = if (isActive) GlassTokens.AccentCyan else GlassTokens.TextSecondary,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isActive) GlassTokens.AccentCyan else GlassTokens.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tab.url,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GlassTokens.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (tab.detectedMediaList.isNotEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.25f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GlassTokens.AccentCyan.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = "${tab.detectedMediaList.size} vids",
                                        color = GlassTokens.AccentCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Box {
                                IconButton(
                                    onClick = {
                                        menuExpandedForTabId = if (menuExpandedForTabId == tab.id) null else tab.id
                                    },
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Tab options",
                                        tint = GlassTokens.TextSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                AnimatedDropdownPanel(
                                    expanded = menuExpandedForTabId == tab.id,
                                    onDismissRequest = { menuExpandedForTabId = null }
                                ) {
                                    AnimatedDropdownMenuItem(
                                        text = "Close Tab",
                                        icon = Icons.Default.Close,
                                        onClick = {
                                            menuExpandedForTabId = null
                                            onCloseTab(tab.id)
                                        }
                                    )
                                    AnimatedDropdownMenuItem(
                                        text = "Copy Address",
                                        icon = Icons.Default.ContentCopy,
                                        onClick = {
                                            menuExpandedForTabId = null
                                            clipboardManager.setText(AnnotatedString(tab.url))
                                        }
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onCloseTab(tab.id) },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close tab",
                                    tint = GlassTokens.TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
