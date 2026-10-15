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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

@Composable
fun ColorPickerDialog(
    title: String,
    initialColor: Long,
    defaultColor: Long = initialColor,
    supportsAlpha: Boolean = true,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val initC = Color(initialColor)
    var red by remember { mutableFloatStateOf(initC.red * 255f) }
    var green by remember { mutableFloatStateOf(initC.green * 255f) }
    var blue by remember { mutableFloatStateOf(initC.blue * 255f) }
    var alpha by remember { mutableFloatStateOf(if (supportsAlpha) initC.alpha * 255f else 255f) }

    fun buildCurrentColor(): Color {
        return Color(
            red = red.roundToInt().coerceIn(0, 255) / 255f,
            green = green.roundToInt().coerceIn(0, 255) / 255f,
            blue = blue.roundToInt().coerceIn(0, 255) / 255f,
            alpha = alpha.roundToInt().coerceIn(0, 255) / 255f
        )
    }

    fun toHex(c: Color): String {
        val a = (c.alpha * 255).roundToInt()
        val r = (c.red * 255).roundToInt()
        val g = (c.green * 255).roundToInt()
        val b = (c.blue * 255).roundToInt()
        return if (supportsAlpha && a < 255) {
            String.format("#%02X%02X%02X%02X", a, r, g, b)
        } else {
            String.format("#%02X%02X%02X", r, g, b)
        }
    }

    var hexInput by remember { mutableStateOf(toHex(initC)) }
    var hexError by remember { mutableStateOf(false) }

    val presetSwatches = remember {
        listOf(
            0xFF38BDF8, 0xFFA855F7, 0xFF06B6D4, 0xFF10B981,
            0xFFFACC15, 0xFFEC4899, 0xFFEF4444, 0xFFF97316,
            0xFF0284C7, 0xFF6366F1, 0xFF8B5CF6, 0xFF14B8A6,
            0xFFF8FAFC, 0xFF94A3B8, 0xFF1E293B, 0xFF020617
        )
    }

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
                .fillMaxWidth(0.94f)
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
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "RGB / HEX Color Customizer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                // Comparison preview (Original vs Current)
                val currentColor = buildCurrentColor()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(currentColor)
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Original",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(initialColor))
                                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .clickable {
                                    val c = Color(initialColor)
                                    red = c.red * 255f
                                    green = c.green * 255f
                                    blue = c.blue * 255f
                                    alpha = if (supportsAlpha) c.alpha * 255f else 255f
                                    hexInput = toHex(c)
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // HEX Input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { input ->
                        hexInput = input
                        val cleaned = input.trim().removePrefix("#")
                        try {
                            if (cleaned.length == 6) {
                                val parsed = cleaned.toLong(16)
                                red = ((parsed shr 16) and 0xFF).toFloat()
                                green = ((parsed shr 8) and 0xFF).toFloat()
                                blue = (parsed and 0xFF).toFloat()
                                hexError = false
                            } else if (cleaned.length == 8 && supportsAlpha) {
                                val parsed = cleaned.toLong(16)
                                alpha = ((parsed shr 24) and 0xFF).toFloat()
                                red = ((parsed shr 16) and 0xFF).toFloat()
                                green = ((parsed shr 8) and 0xFF).toFloat()
                                blue = (parsed and 0xFF).toFloat()
                                hexError = false
                            } else {
                                hexError = cleaned.isNotEmpty()
                            }
                        } catch (e: Exception) {
                            hexError = true
                        }
                    },
                    isError = hexError,
                    label = { Text("HEX Code") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Curated Swatches
                Text(
                    text = "Quick Palette",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetSwatches.take(8).forEach { colorLong ->
                        val swatchColor = Color(colorLong)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    red = swatchColor.red * 255f
                                    green = swatchColor.green * 255f
                                    blue = swatchColor.blue * 255f
                                    hexInput = toHex(swatchColor)
                                    hexError = false
                                }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    presetSwatches.takeLast(8).forEach { colorLong ->
                        val swatchColor = Color(colorLong)
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatchColor)
                                .border(1.dp, Color.White.copy(alpha = 0.4f), CircleShape)
                                .clickable {
                                    red = swatchColor.red * 255f
                                    green = swatchColor.green * 255f
                                    blue = swatchColor.blue * 255f
                                    hexInput = toHex(swatchColor)
                                    hexError = false
                                }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                Spacer(modifier = Modifier.height(12.dp))

                // Red Slider
                ColorChannelSlider(
                    label = "Red (R)",
                    value = red,
                    onValueChange = {
                        red = it
                        hexInput = toHex(buildCurrentColor())
                        hexError = false
                    },
                    accentColor = Color(0xFFEF4444)
                )

                // Green Slider
                ColorChannelSlider(
                    label = "Green (G)",
                    value = green,
                    onValueChange = {
                        green = it
                        hexInput = toHex(buildCurrentColor())
                        hexError = false
                    },
                    accentColor = Color(0xFF10B981)
                )

                // Blue Slider
                ColorChannelSlider(
                    label = "Blue (B)",
                    value = blue,
                    onValueChange = {
                        blue = it
                        hexInput = toHex(buildCurrentColor())
                        hexError = false
                    },
                    accentColor = Color(0xFF38BDF8)
                )

                // Alpha Slider (if supported)
                if (supportsAlpha) {
                    ColorChannelSlider(
                        label = "Opacity (Alpha)",
                        value = alpha,
                        onValueChange = {
                            alpha = it
                            hexInput = toHex(buildCurrentColor())
                            hexError = false
                        },
                        accentColor = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val c = Color(defaultColor)
                            red = c.red * 255f
                            green = c.green * 255f
                            blue = c.blue * 255f
                            alpha = if (supportsAlpha) c.alpha * 255f else 255f
                            hexInput = toHex(c)
                            hexError = false
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val finalColorLong = (currentColor.toArgb().toLong() and 0xFFFFFFFFL)
                            onColorSelected(finalColorLong)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .testTag("color_picker_apply_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply Color", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    accentColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value.roundToInt().toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = accentColor
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = accentColor,
                activeTrackColor = accentColor,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
