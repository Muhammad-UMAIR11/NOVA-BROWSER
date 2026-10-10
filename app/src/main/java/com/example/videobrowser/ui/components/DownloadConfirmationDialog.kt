package com.example.videobrowser.ui.components

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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.ui.glass.GlassCard
import com.example.videobrowser.ui.glass.GlassDialog
import com.example.videobrowser.ui.glass.GlassTokens

@Composable
fun DownloadConfirmationDialog(
    media: DetectedMedia,
    fileNameInput: String,
    useSystemDownloadManager: Boolean,
    onFileNameChange: (String) -> Unit,
    onToggleSystemDownloadManager: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    GlassDialog(
        visible = true,
        onDismissRequest = onDismiss,
        testTag = "download_confirmation_dialog_surface"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            // Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = GlassTokens.GlowCyan,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = GlassTokens.AccentCyan,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Confirm Video Download",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Verify stream metadata before queuing",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTokens.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Glass Summary Card
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                DetailRow("Format:", media.format)
                DetailRow("Quality:", media.displayQuality)
                DetailRow("Estimated Size:", media.formattedSize)
                DetailRow("MIME Type:", media.mimeType)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // File Name Input
            Text(
                text = "Save as file name:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = GlassTokens.TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = fileNameInput,
                onValueChange = onFileNameChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filename_input_field"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = GlassTokens.TextPrimary,
                    unfocusedTextColor = GlassTokens.TextPrimary,
                    focusedContainerColor = Color(0x2E1E293B),
                    unfocusedContainerColor = Color(0x1F1E293B),
                    focusedBorderColor = GlassTokens.AccentCyan,
                    unfocusedBorderColor = GlassTokens.GlassBorderDim
                ),
                shape = RoundedCornerShape(10.dp),
                suffix = {
                    val ext = when {
                        media.format.equals("WEBM", ignoreCase = true) -> ".webm"
                        media.format.equals("MKV", ignoreCase = true) -> ".mkv"
                        media.format.equals("3GP", ignoreCase = true) -> ".3gp"
                        media.format.equals("MOV", ignoreCase = true) -> ".mov"
                        else -> ".mp4"
                    }
                    Text(ext, style = MaterialTheme.typography.bodySmall, color = GlassTokens.TextSecondary)
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Storage destination info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = GlassTokens.AccentCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Destination: Device Downloads",
                    style = MaterialTheme.typography.labelSmall,
                    color = GlassTokens.TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Option: System DownloadManager vs In-App Streamer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = useSystemDownloadManager,
                    onCheckedChange = onToggleSystemDownloadManager,
                    colors = CheckboxDefaults.colors(
                        checkedColor = GlassTokens.AccentCyan,
                        checkmarkColor = Color(0xFF0F172A),
                        uncheckedColor = GlassTokens.TextMuted
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Use Android System Downloader",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = GlassTokens.TextPrimary
                    )
                    Text(
                        text = "Downloads in system background notification",
                        style = MaterialTheme.typography.labelSmall,
                        color = GlassTokens.TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("dialog_cancel_button")
                ) {
                    Text("Cancel", color = GlassTokens.TextSecondary)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GlassTokens.AccentCyan,
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("dialog_confirm_download_button")
                ) {
                    Text("Start Download", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = GlassTokens.TextSecondary,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = GlassTokens.TextPrimary
        )
    }
}
