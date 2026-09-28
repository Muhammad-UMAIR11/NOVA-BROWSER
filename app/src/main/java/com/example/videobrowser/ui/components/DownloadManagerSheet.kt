package com.example.videobrowser.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.videobrowser.model.DownloadStatus
import com.example.videobrowser.model.DownloadTask
import java.io.File

private enum class DownloadFilter {
    ALL,
    ACTIVE,
    PAUSED_OR_INTERRUPTED,
    COMPLETED
}

/**
 * Download Manager Modal Bottom Sheet.
 * Hosts the comprehensive, scrollable Download Manager UI component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadManagerSheet(
    sheetState: SheetState,
    tasks: List<DownloadTask>,
    onCancelTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onDismiss: () -> Unit,
    onRetryTask: (String) -> Unit = {},
    onPauseTask: (String) -> Unit = {},
    onResumeTask: (String) -> Unit = {},
    onPauseAll: () -> Unit = {},
    onResumeAll: () -> Unit = {},
    onClearCompleted: () -> Unit = {}
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        DownloadManagerComponent(
            tasks = tasks,
            onCancelTask = onCancelTask,
            onRemoveTask = onRemoveTask,
            onRetryTask = onRetryTask,
            onPauseTask = onPauseTask,
            onResumeTask = onResumeTask,
            onPauseAll = onPauseAll,
            onResumeAll = onResumeAll,
            onClearCompleted = onClearCompleted,
            onClose = onDismiss,
            modifier = Modifier.padding(bottom = 24.dp)
        )
    }
}

/**
 * Reusable Download Manager UI Component.
 *
 * Tracks active and queued file downloads in real-time, displaying them in a scrollable list
 * with individual and batch Pause, Resume, and Cancel functionality.
 *
 * Resilient Interruption Handling:
 * If an interruption occurs (network dropped, timeout, paused midway), the download starts
 * exactly where it was paused using HTTP Range headers and segment offsets without data loss.
 */
@Composable
fun DownloadManagerComponent(
    tasks: List<DownloadTask>,
    onCancelTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onRetryTask: (String) -> Unit,
    onPauseTask: (String) -> Unit,
    onResumeTask: (String) -> Unit,
    onPauseAll: () -> Unit = {},
    onResumeAll: () -> Unit = {},
    onClearCompleted: () -> Unit = {},
    onClose: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf(DownloadFilter.ALL) }

    val activeCount = tasks.count { it.status == DownloadStatus.DOWNLOADING }
    val pausedCount = tasks.count { it.status == DownloadStatus.PAUSED || (it.status == DownloadStatus.FAILED && it.isPartiallyDownloaded) }
    val completedCount = tasks.count { it.status == DownloadStatus.COMPLETED }

    val filteredTasks = when (selectedFilter) {
        DownloadFilter.ALL -> tasks
        DownloadFilter.ACTIVE -> tasks.filter { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.PENDING }
        DownloadFilter.PAUSED_OR_INTERRUPTED -> tasks.filter { it.status == DownloadStatus.PAUSED || (it.status == DownloadStatus.FAILED && it.isPartiallyDownloaded) }
        DownloadFilter.COMPLETED -> tasks.filter { it.status == DownloadStatus.COMPLETED }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("download_manager_component")
    ) {
        // --- 1. Header with Stats & Close ---
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Download Manager",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (activeCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.18f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0284C7).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF0284C7))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$activeCount active",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7),
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }

                        Text(
                            text = "${tasks.size} total downloads • Resume supported",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (onClose != null) {
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("download_manager_close_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Download Manager",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Batch Actions Row (Pause All, Resume All, Clear Completed)
                if (tasks.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeCount > 0) {
                            OutlinedButton(
                                onClick = onPauseAll,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("download_manager_pause_all_button")
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pause All", fontSize = 11.sp)
                            }
                        }

                        if (pausedCount > 0) {
                            Button(
                                onClick = onResumeAll,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("download_manager_resume_all_button")
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Resume All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (completedCount > 0) {
                            OutlinedButton(
                                onClick = onClearCompleted,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(34.dp)
                                    .testTag("download_manager_clear_completed_button")
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear Finished", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // --- 2. Interruption Resumption Guarantee Banner ---
        if (pausedCount > 0) {
            Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(10.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Sync,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Interrupted/paused downloads will start right where they paused without restarting from 0.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        // --- 3. Filter Chips Row ---
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                val isSelected = selectedFilter == DownloadFilter.ALL
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = DownloadFilter.ALL },
                    label = { Text("All (${tasks.size})", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    ),
                    modifier = Modifier.testTag("download_filter_all")
                )
            }
            item {
                val isSelected = selectedFilter == DownloadFilter.ACTIVE
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = DownloadFilter.ACTIVE },
                    label = { Text("Active ($activeCount)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    ),
                    modifier = Modifier.testTag("download_filter_active")
                )
            }
            item {
                val isSelected = selectedFilter == DownloadFilter.PAUSED_OR_INTERRUPTED
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = DownloadFilter.PAUSED_OR_INTERRUPTED },
                    label = { Text("Paused ($pausedCount)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    ),
                    modifier = Modifier.testTag("download_filter_paused")
                )
            }
            item {
                val isSelected = selectedFilter == DownloadFilter.COMPLETED
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedFilter = DownloadFilter.COMPLETED },
                    label = { Text("Completed ($completedCount)", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = MaterialTheme.colorScheme.outlineVariant,
                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                        borderWidth = 1.dp,
                        selectedBorderWidth = 1.5.dp
                    ),
                    modifier = Modifier.testTag("download_filter_completed")
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

        // --- 4. Scrollable List of Downloads ---
        if (filteredTasks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 40.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier
                                .padding(14.dp)
                                .size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (tasks.isEmpty()) "No Downloads Yet" else "No matching downloads in this tab",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (tasks.isEmpty()) {
                            "Detected videos from webpages will appear here with live speed, byte tracking, pause, and cancellation."
                        } else {
                            "Select 'All' to view all download tasks."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("download_manager_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredTasks, key = { it.id }) { task ->
                    DownloadTaskCard(
                        task = task,
                        onPause = { onPauseTask(task.id) },
                        onResume = { onResumeTask(task.id) },
                        onCancel = { onCancelTask(task.id) },
                        onRetry = { onRetryTask(task.id) },
                        onRemove = { onRemoveTask(task.id) },
                        onOpenFile = {
                            task.filePath?.let { pathOrUri ->
                                try {
                                    val uri = if (pathOrUri.startsWith("content://") || pathOrUri.startsWith("file://")) {
                                        Uri.parse(pathOrUri)
                                    } else {
                                        val file = File(pathOrUri)
                                        if (file.exists()) {
                                            FileProvider.getUriForFile(
                                                context,
                                                "${context.packageName}.fileprovider",
                                                file
                                            )
                                        } else null
                                    }
                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            setDataAndType(uri, task.mimeType)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                } catch (_: Exception) {}
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * Individual Download Task Card with progress, speed, file metadata,
 * and responsive Pause, Resume, and Cancel buttons.
 */
@Composable
private fun DownloadTaskCard(
    task: DownloadTask,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
    onOpenFile: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when (task.status) {
                DownloadStatus.DOWNLOADING -> Color(0xFF0284C7).copy(alpha = 0.35f)
                DownloadStatus.PAUSED -> Color(0xFFF59E0B).copy(alpha = 0.4f)
                DownloadStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.35f)
                DownloadStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_task_card_${task.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Row with file name and status chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                DownloadStatusBadge(task = task)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle info: format, quality, size transferred
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Text(
                        text = task.format.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                        fontSize = 10.sp
                    )
                }

                if (task.quality.isNotBlank()) {
                    Text(
                        text = "${task.quality} • ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                    )
                }

                Text(
                    text = task.formattedDownloadedSize,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.5.sp
                )
            }

            // Progress bar and live metrics when DOWNLOADING or PAUSED
            if (task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED || task.canResume) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { task.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = if (task.status == DownloadStatus.DOWNLOADING) Color(0xFF0284C7) else Color(0xFFF59E0B),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = task.formattedProgress,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (task.status == DownloadStatus.DOWNLOADING) Color(0xFF0284C7) else Color(0xFFF59E0B)
                    )

                    if (task.status == DownloadStatus.DOWNLOADING && task.speedFormatted.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = task.speedFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else if (task.status == DownloadStatus.PAUSED && task.isPartiallyDownloaded) {
                        Text(
                            text = "Starts at ${task.formattedResumableOffset}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFF59E0B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Error or Interruption notice
            if (task.errorMessage != null && task.status != DownloadStatus.COMPLETED) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (task.canResume) Color(0xFFF59E0B).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (task.canResume) Icons.Default.Sync else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (task.canResume) Color(0xFFF59E0B) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = task.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = if (task.canResume) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Pause, Resume, Cancel, Open, Retry, Remove
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (task.status) {
                    DownloadStatus.DOWNLOADING -> {
                        // Pause Button
                        Button(
                            onClick = onPause,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF59E0B),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("pause_download_button_${task.id}")
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Cancel Button
                        OutlinedButton(
                            onClick = onCancel,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("cancel_download_button_${task.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }

                    DownloadStatus.PAUSED -> {
                        // Resume Button (starts where paused)
                        Button(
                            onClick = onResume,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("resume_download_button_${task.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Cancel Button
                        OutlinedButton(
                            onClick = onCancel,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("cancel_download_button_${task.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }

                    DownloadStatus.PENDING -> {
                        OutlinedButton(
                            onClick = onCancel,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("cancel_download_button_${task.id}")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 12.sp)
                        }
                    }

                    DownloadStatus.COMPLETED -> {
                        Button(
                            onClick = onOpenFile,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("open_file_button_${task.id}")
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    DownloadStatus.FAILED, DownloadStatus.CANCELLED -> {
                        // Retry button (resumes from existing bytes if partial file exists)
                        Button(
                            onClick = onRetry,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("retry_download_button_${task.id}")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (task.isPartiallyDownloaded) "Resume" else "Retry", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("remove_download_button_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete download record",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadStatusBadge(task: DownloadTask) {
    val (color, text) = when (task.status) {
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.outline to "Queued"
        DownloadStatus.DOWNLOADING -> Color(0xFF0284C7) to "Downloading"
        DownloadStatus.PAUSED -> {
            if (task.isPartiallyDownloaded) {
                Color(0xFFF59E0B) to "Paused"
            } else {
                Color(0xFFF59E0B) to "Paused"
            }
        }
        DownloadStatus.COMPLETED -> Color(0xFF10B981) to "Completed"
        DownloadStatus.FAILED -> {
            if (task.isPartiallyDownloaded) {
                Color(0xFFF97316) to "Interrupted"
            } else {
                MaterialTheme.colorScheme.error to "Failed"
            }
        }
        DownloadStatus.CANCELLED -> MaterialTheme.colorScheme.outline to "Cancelled"
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            if (task.status == DownloadStatus.COMPLETED) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
            } else if (task.status == DownloadStatus.DOWNLOADING) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(color)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            Text(
                text = text,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
