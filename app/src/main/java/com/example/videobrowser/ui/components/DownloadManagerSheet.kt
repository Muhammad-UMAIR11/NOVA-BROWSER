package com.example.videobrowser.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import com.example.videobrowser.ui.glass.GlassBottomSheet
import com.example.videobrowser.ui.glass.GlassTokens
import com.example.videobrowser.ui.motion.LocalReducedMotion
import com.example.videobrowser.ui.motion.MotionDurations
import com.example.videobrowser.ui.motion.MotionEasing
import com.example.videobrowser.ui.motion.motionTween
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
    sheetState: SheetState? = null,
    tasks: List<DownloadTask>,
    onCancelTask: (String) -> Unit,
    onRemoveTask: (String) -> Unit,
    onDismiss: () -> Unit,
    onRetryTask: (String) -> Unit = {},
    onPauseTask: (String) -> Unit = {},
    onResumeTask: (String) -> Unit = {},
    onPauseAll: () -> Unit = {},
    onResumeAll: () -> Unit = {},
    onClearCompleted: () -> Unit = {},
    visible: Boolean = true
) {
    GlassBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Download Manager",
        subtitle = "${tasks.size} total tasks • Local storage",
        icon = Icons.Default.Download,
        testTag = "downloads_bottom_sheet"
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
            showHeader = false,
            modifier = Modifier.padding(bottom = 12.dp)
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
    showHeader: Boolean = true,
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
        if (showHeader) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(
                                fadeInSpec = motionTween(MotionDurations.Medium, easing = MotionEasing.EmphasizedDecelerate),
                                fadeOutSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate),
                                placementSpec = motionTween(MotionDurations.Medium, easing = MotionEasing.Standard)
                            )
                            .testTag("download_task_item_${task.id}")
                    ) {
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
}

/**
 * Individual Download Task Card with progress, speed, file metadata,
 * and responsive Pause, Resume, and Cancel buttons with fluid transitions.
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
    val isReducedMotion = LocalReducedMotion.current

    // Smooth border color interpolation reacting dynamically to status changes
    val targetBorderColor = when (task.status) {
        DownloadStatus.DOWNLOADING -> Color(0xFF0284C7).copy(alpha = 0.45f)
        DownloadStatus.PAUSED -> Color(0xFFF59E0B).copy(alpha = 0.45f)
        DownloadStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.45f)
        DownloadStatus.FAILED -> MaterialTheme.colorScheme.error.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }
    val animatedBorderColor by animateColorAsState(
        targetValue = targetBorderColor,
        animationSpec = motionTween(MotionDurations.Medium, isReducedMotion = isReducedMotion),
        label = "taskCardBorderColor_${task.id}"
    )

    // Subtle container background transition
    val targetContainerColor = when (task.status) {
        DownloadStatus.COMPLETED -> Color(0xFF10B981).copy(alpha = 0.08f)
        DownloadStatus.DOWNLOADING -> Color(0xFF0284C7).copy(alpha = 0.06f)
        DownloadStatus.PAUSED -> Color(0xFFF59E0B).copy(alpha = 0.06f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val animatedContainerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = motionTween(MotionDurations.Medium, isReducedMotion = isReducedMotion),
        label = "taskCardContainerColor_${task.id}"
    )

    // Smooth progress bar animations: animate smoothly between progress percentages
    val rawProgress = task.progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = if (isReducedMotion) {
            motionTween(0, isReducedMotion = true)
        } else {
            tween(durationMillis = 280, easing = FastOutSlowInEasing)
        },
        label = "taskProgress_${task.id}"
    )

    // Progress bar color smoothly adapts when toggling between Downloading (cyan-blue) and Paused (amber)
    val targetProgressBarColor = if (task.status == DownloadStatus.DOWNLOADING) {
        Color(0xFF0284C7)
    } else {
        Color(0xFFF59E0B)
    }
    val animatedProgressBarColor by animateColorAsState(
        targetValue = targetProgressBarColor,
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "progressBarColor_${task.id}"
    )

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = animatedContainerColor
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            animatedBorderColor
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

            // Progress bar and live metrics when DOWNLOADING or PAUSED (Smooth animated appearance & completion transition)
            val showProgressSection = task.status == DownloadStatus.DOWNLOADING || task.status == DownloadStatus.PAUSED || task.canResume
            AnimatedVisibility(
                visible = showProgressSection,
                enter = fadeIn(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)) +
                    expandVertically(animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)),
                exit = fadeOut(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)) +
                    shrinkVertically(animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion))
            ) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = animatedProgressBarColor,
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
                            color = animatedProgressBarColor
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
            }

            // Completion Banner (Smooth natural transition when entering completed state)
            AnimatedVisibility(
                visible = task.status == DownloadStatus.COMPLETED,
                enter = fadeIn(motionTween(MotionDurations.Medium, isReducedMotion = isReducedMotion)) +
                    expandVertically(animationSpec = motionTween(MotionDurations.Medium, isReducedMotion = isReducedMotion)),
                exit = fadeOut(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)) +
                    shrinkVertically(animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion))
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Download complete • Saved to storage",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF059669)
                        )
                    }
                }
            }

            // Error or Interruption notice
            AnimatedVisibility(
                visible = task.errorMessage != null && task.status != DownloadStatus.COMPLETED,
                enter = fadeIn(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)) +
                    expandVertically(animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)),
                exit = fadeOut(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)) +
                    shrinkVertically(animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion))
            ) {
                task.errorMessage?.let { errorMsg ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (task.canResume) Color(0xFFF59E0B).copy(alpha = 0.15f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp)
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
                                text = errorMsg,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (task.canResume) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons: Pause, Resume, Cancel, Open, Retry, Remove with fluid AnimatedContent transitions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedContent(
                    targetState = task.status,
                    transitionSpec = {
                        if (isReducedMotion) {
                            fadeIn(motionTween(0, isReducedMotion = true)) togetherWith fadeOut(motionTween(0, isReducedMotion = true))
                        } else {
                            (fadeIn(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedDecelerate)) +
                                scaleIn(initialScale = 0.94f, animationSpec = motionTween(MotionDurations.Fast))) togetherWith
                                (fadeOut(motionTween(MotionDurations.Micro, easing = MotionEasing.EmphasizedAccelerate)) +
                                    scaleOut(targetScale = 0.94f, animationSpec = motionTween(MotionDurations.Micro)))
                        }
                    },
                    label = "taskActionButtons_${task.id}"
                ) { currentStatus ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        when (currentStatus) {
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
    val isReducedMotion = LocalReducedMotion.current
    val (targetColor, text) = when (task.status) {
        DownloadStatus.PENDING -> MaterialTheme.colorScheme.outline to "Queued"
        DownloadStatus.DOWNLOADING -> Color(0xFF0284C7) to "Downloading"
        DownloadStatus.PAUSED -> Color(0xFFF59E0B) to "Paused"
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

    val animatedBadgeColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "statusBadgeColor_${task.id}"
    )

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = animatedBadgeColor.copy(alpha = 0.16f),
        border = androidx.compose.foundation.BorderStroke(1.dp, animatedBadgeColor.copy(alpha = 0.35f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        ) {
            AnimatedContent(
                targetState = task.status,
                transitionSpec = {
                    if (isReducedMotion) {
                        fadeIn(motionTween(0, isReducedMotion = true)) togetherWith fadeOut(motionTween(0, isReducedMotion = true))
                    } else {
                        (fadeIn(motionTween(MotionDurations.Fast)) + scaleIn(initialScale = 0.8f)) togetherWith
                            (fadeOut(motionTween(MotionDurations.Micro)) + scaleOut(targetScale = 0.8f))
                    }
                },
                label = "badgeIconTransition_${task.id}"
            ) { status ->
                when (status) {
                    DownloadStatus.COMPLETED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = animatedBadgeColor,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                    }
                    DownloadStatus.DOWNLOADING -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(animatedBadgeColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                    }
                    DownloadStatus.PAUSED -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = null,
                                tint = animatedBadgeColor,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                        }
                    }
                    else -> {}
                }
            }

            Text(
                text = text,
                color = animatedBadgeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
