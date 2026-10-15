package com.example.videobrowser.ui.components.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.videobrowser.model.theme.BrowserTheme
import java.util.UUID

@Composable
fun CreateThemeDialog(
    baseTheme: BrowserTheme = BrowserTheme.MIDNIGHT,
    onDismiss: () -> Unit,
    onSaveTheme: (BrowserTheme) -> Unit
) {
    var themeName by remember { mutableStateOf("${baseTheme.name} Custom") }
    var isDark by remember { mutableStateOf(baseTheme.isDark) }
    var primaryColor by remember { mutableStateOf(baseTheme.primaryColor) }
    var surfaceColor by remember { mutableStateOf(baseTheme.surfaceColor) }
    var backgroundColor by remember { mutableStateOf(baseTheme.backgroundColor) }

    var activeColorPickerTarget by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Create Custom Theme",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                OutlinedTextField(
                    value = themeName,
                    onValueChange = { themeName = it },
                    label = { Text("Theme Name") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_theme_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Mode Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isDark,
                        onClick = { isDark = true },
                        label = { Text("Dark Aesthetic") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isDark,
                        onClick = { isDark = false },
                        label = { Text("Light Aesthetic") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Accurate Live Mini Browser UI Preview
                Text(
                    text = "Live UI Preview",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Mini Browser Mockup Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(backgroundColor),
                    border = BorderStroke(1.2.dp, Color(primaryColor).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // Mock Top Bar
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(surfaceColor))
                                .padding(horizontal = 8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(primaryColor))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "nova://browser/settings",
                                color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Mock Page Content Card
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(surfaceColor).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(primaryColor).copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = themeName.ifBlank { "Untitled Theme" },
                                        color = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (isDark) "Dark mode surface" else "Light mode surface",
                                        color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        fontSize = 9.sp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(primaryColor)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFF020617) else Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(14.dp))

                // Component Color Config Rows
                Text(
                    text = "Component Palette",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                ColorPickerTriggerRow(
                    label = "Primary Accent",
                    currentColor = primaryColor,
                    onClick = { activeColorPickerTarget = "primary" }
                )
                ColorPickerTriggerRow(
                    label = "Surface / Panels",
                    currentColor = surfaceColor,
                    onClick = { activeColorPickerTarget = "surface" }
                )
                ColorPickerTriggerRow(
                    label = "App Background",
                    currentColor = backgroundColor,
                    onClick = { activeColorPickerTarget = "background" }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val newTheme = BrowserTheme(
                                id = "custom_${UUID.randomUUID().toString().take(8)}",
                                name = themeName.ifBlank { "Custom Theme" },
                                isCustom = true,
                                isDark = isDark,
                                primaryColor = primaryColor,
                                onPrimaryColor = if (isDark) 0xFF02131F else 0xFFFFFFFF,
                                secondaryColor = primaryColor,
                                surfaceColor = surfaceColor,
                                onSurfaceColor = if (isDark) 0xFFF8FAFC else 0xFF0F172A,
                                backgroundColor = backgroundColor,
                                onBackgroundColor = if (isDark) 0xFFF8FAFC else 0xFF0F172A,
                                toolbarColor = surfaceColor,
                                navigationBarColor = surfaceColor,
                                borderColor = Color(primaryColor).copy(alpha = 0.35f).value.toLong(),
                                secondaryTextColor = if (isDark) 0xFF94A3B8 else 0xFF64748B,
                                accentColor = primaryColor
                            )
                            onSaveTheme(newTheme)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("save_custom_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Theme", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Secondary Color Picker Dialog for individual slots
    activeColorPickerTarget?.let { target ->
        val currentSlotColor = when (target) {
            "primary" -> primaryColor
            "surface" -> surfaceColor
            else -> backgroundColor
        }
        val targetTitle = when (target) {
            "primary" -> "Primary Accent Color"
            "surface" -> "Surface / Panel Color"
            else -> "Background Color"
        }
        ColorPickerDialog(
            title = targetTitle,
            initialColor = currentSlotColor,
            supportsAlpha = false,
            onDismiss = { activeColorPickerTarget = null },
            onColorSelected = { selected ->
                when (target) {
                    "primary" -> primaryColor = selected
                    "surface" -> surfaceColor = selected
                    "background" -> backgroundColor = selected
                }
            }
        )
    }
}

@Composable
fun ColorPickerTriggerRow(
    label: String,
    currentColor: Long,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(currentColor))
                        .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = "Pick color",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
