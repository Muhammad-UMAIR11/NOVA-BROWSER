package com.example.videobrowser.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.videobrowser.ui.glass.GlassBottomSheet
import com.example.videobrowser.ui.glass.GlassTokens
import com.example.videobrowser.ui.motion.LocalReducedMotion
import com.example.videobrowser.ui.motion.MotionDurations
import com.example.videobrowser.ui.motion.MotionEasing
import com.example.videobrowser.ui.motion.motionSpring
import com.example.videobrowser.ui.motion.motionTween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.GroupedVideoMedia
import com.example.videobrowser.model.MediaCompatibilityCategory
import com.example.videobrowser.model.VideoGroupingEngine
import com.example.videobrowser.model.VideoQualityOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectedMediaSheet(
    sheetState: SheetState? = null,
    pageTitle: String,
    pageUrl: String,
    groupedVideos: List<GroupedVideoMedia> = emptyList(),
    mediaList: List<DetectedMedia> = emptyList(),
    onDismiss: () -> Unit,
    onSelectMedia: (DetectedMedia) -> Unit,
    onSelectQuality: (groupId: String, qualityId: String) -> Unit = { _, _ -> },
    visible: Boolean = true
) {
    val context = LocalContext.current

    // Resolve active video groups: use provided groupedVideos, or derive from raw mediaList
    val displayGroups = remember(groupedVideos, mediaList, pageUrl, pageTitle) {
        if (groupedVideos.isNotEmpty()) {
            groupedVideos
        } else if (mediaList.isNotEmpty()) {
            VideoGroupingEngine.groupMedia(mediaList, pageUrl, pageTitle)
        } else {
            emptyList()
        }
    }

    GlassBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Detected Videos",
        subtitle = pageTitle.ifBlank { pageUrl },
        icon = Icons.Default.VideoFile,
        testTag = "detected_media_bottom_sheet"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {

            if (displayGroups.isEmpty()) {
                if (pageUrl.isNotBlank() && pageUrl != "nova://home") {
                    VideoDetectionSkeleton(
                        pageTitle = pageTitle.ifBlank { "this webpage" },
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    // Empty state for home or pages without media
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No media detected on this page yet",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Play a video to begin detection. Background GIFs and audio previews are automatically filtered out.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(displayGroups, key = { it.id }) { group ->
                        GroupedVideoItemCard(
                            group = group,
                            onSelectQuality = { qualityId -> onSelectQuality(group.id, qualityId) },
                            onDownloadSelected = { selectedQ ->
                                val media = selectedQ.toDetectedMedia(group.title, group.pageUrl)
                                onSelectMedia(media)
                            },
                            onPreviewSelected = { selectedQ ->
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(Uri.parse(selectedQ.mediaUrl), selectedQ.mimeType)
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {
                                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(selectedQ.mediaUrl))
                                    context.startActivity(browserIntent)
                                }
                            }
                        )
                    }
                }
            }

            // Real-world limitations & transparency note
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Identified video and grouped stream qualities. Direct MP4/WebM and HLS streams are downloadable. DRM/DASH streams are reported transparently.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    lineHeight = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GroupedVideoItemCard(
    group: GroupedVideoMedia,
    onSelectQuality: (String) -> Unit,
    onDownloadSelected: (VideoQualityOption) -> Unit,
    onPreviewSelected: (VideoQualityOption) -> Unit
) {
    val activeQuality = group.selectedQuality
    val isDownloadable = activeQuality?.isDownloadable ?: group.isDownloadable
    val isNowPlaying = group.isPlaying
    var isQualityMenuExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isReducedMotion = LocalReducedMotion.current

    val cardBorder = if (isNowPlaying) {
        BorderStroke(1.5.dp, Color(0xFF10B981))
    } else {
        null
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        border = cardBorder,
        colors = CardDefaults.cardColors(
            containerColor = if (isNowPlaying) {
                Color(0xFF10B981).copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Section: Thumbnail + Title & Status Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                // Video Thumbnail / Placeholder
                Box(
                    modifier = Modifier
                        .size(width = 80.dp, height = 56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (!group.thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = group.thumbnailUrl,
                            contentDescription = "Video Thumbnail",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.VideoFile,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Duration overlay if available
                    if (group.durationSeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(3.dp)
                        ) {
                            Text(
                                text = group.formattedDuration,
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title, Domain, & Playing Status
                Column(modifier = Modifier.weight(1f)) {
                    // Domain and status pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (group.domain.isNotBlank()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Public,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = group.domain,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (isNowPlaying) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF10B981))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Now Playing",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF059669),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        } else if (group.userInteracted) {
                            Text(
                                text = "Watched Video",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (group.rawMediaCount > 1) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${group.rawMediaCount} duplicate streams grouped",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Available qualities count and file size range
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = if (group.downloadableQualitiesCount > 0) {
                                    "${group.downloadableQualitiesCount} Downloadable ${if (group.downloadableQualitiesCount == 1) "Quality" else "Qualities"}"
                                } else {
                                    "${group.qualities.size} Qualities"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp
                            )
                        }

                        if (group.sizeRangeText.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.85f)
                            ) {
                                Text(
                                    text = "Size: ${group.sizeRangeText}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Quality Chips Row with entrance and selection animations
            if (group.qualities.isNotEmpty()) {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    group.qualities.forEachIndexed { index, option ->
                        val isSelected = option.id == (activeQuality?.id ?: group.qualities.firstOrNull()?.id)
                        AnimatedVisibility(
                            visible = true,
                            enter = if (isReducedMotion) fadeIn(animationSpec = androidx.compose.animation.core.snap()) else (
                                fadeIn(animationSpec = tween(durationMillis = 180, delayMillis = (index * 25).coerceAtMost(150), easing = MotionEasing.EmphasizedDecelerate)) +
                                    scaleIn(initialScale = 0.92f, animationSpec = tween(durationMillis = 180, delayMillis = (index * 25).coerceAtMost(150), easing = MotionEasing.EmphasizedDecelerate))
                            )
                        ) {
                            QualityChip(
                                option = option,
                                isSelected = isSelected,
                                onSelect = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onSelectQuality(option.id)
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Quality & Format Selector Section
            if (group.qualities.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { isQualityMenuExpanded = !isQualityMenuExpanded }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Quality:",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))

                            // Selected Quality Summary with smooth fluid transitions
                            AnimatedContent(
                                targetState = activeQuality,
                                transitionSpec = {
                                    (slideInVertically(animationSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = isReducedMotion)) { height -> height / 3 } +
                                        fadeIn(animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion))) togetherWith
                                        (slideOutVertically(animationSpec = motionTween(MotionDurations.Micro, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)) { height -> -height / 3 } +
                                            fadeOut(animationSpec = motionTween(MotionDurations.Micro, isReducedMotion = isReducedMotion)))
                                },
                                label = "activeQualityHeaderTransition",
                                modifier = Modifier.weight(1f)
                            ) { q ->
                                if (q != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (q.isAudioOnly) {
                                            Icon(
                                                imageVector = Icons.Default.Headphones,
                                                contentDescription = null,
                                                tint = Color(0xFFA855F7),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = "${q.resolutionLabel} • ${q.format} • ${q.displaySize}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (q.isAudioOnly) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "Select Quality",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Icon(
                                imageVector = if (isQualityMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = if (isQualityMenuExpanded) "Collapse quality options" else "Expand quality options",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Expandable Qualities List
                        AnimatedVisibility(
                            visible = isQualityMenuExpanded,
                            enter = fadeIn(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = isReducedMotion)) +
                                expandVertically(animationSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = isReducedMotion)),
                            exit = fadeOut(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)) +
                                shrinkVertically(animationSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(bottom = 6.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                )

                                group.qualities.forEach { option ->
                                    val isSelected = option.id == (activeQuality?.id ?: group.qualities.firstOrNull()?.id)
                                    QualityOptionRow(
                                        option = option,
                                        isSelected = isSelected,
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onSelectQuality(option.id)
                                            isQualityMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
            }

            // Current Quality Metadata Badges (Category, Format, Size, Type)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category badge
                val category = activeQuality?.category ?: group.category
                when (category) {
                    MediaCompatibilityCategory.DIRECT_DOWNLOADABLE -> {
                        BadgeChip(label = "Direct Download", color = Color(0xFFD1FAE5), textColor = Color(0xFF065F46))
                    }
                    MediaCompatibilityCategory.ADAPTIVE_STREAM -> {
                        if (isDownloadable) {
                            BadgeChip(label = "HLS Stream", color = Color(0xFFE0E7FF), textColor = Color(0xFF3730A3))
                        } else {
                            BadgeChip(label = "Adaptive Stream", color = Color(0xFFFEF3C7), textColor = Color(0xFF92400E))
                        }
                    }
                    MediaCompatibilityCategory.RESTRICTED_OR_PROTECTED -> {
                        BadgeChip(label = "Restricted / DRM", color = Color(0xFFFEE2E2), textColor = Color(0xFF991B1B))
                    }
                }

                activeQuality?.let { q ->
                    if (q.isAudioOnly) {
                        BadgeChip(label = "Audio Only", color = Color(0xFFF3E8FF), textColor = Color(0xFF7E22CE))
                    }
                    BadgeChip(label = q.format, color = MaterialTheme.colorScheme.primaryContainer)
                    BadgeChip(label = q.resolutionShort, color = MaterialTheme.colorScheme.secondaryContainer)
                    BadgeChip(label = q.displaySize, color = MaterialTheme.colorScheme.tertiaryContainer)
                    BadgeChip(label = q.downloadType.displayName, color = MaterialTheme.colorScheme.surfaceVariant)
                } ?: run {
                    BadgeChip(label = group.format, color = MaterialTheme.colorScheme.primaryContainer)
                    BadgeChip(label = group.highestQuality, color = MaterialTheme.colorScheme.secondaryContainer)
                }
            }

            // Restriction Notice if not downloadable
            val restrictionReason = activeQuality?.restrictionReason ?: group.restrictionReason
            if (!isDownloadable && restrictionReason != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFEF3C7).copy(alpha = 0.6f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFB45309),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = restrictionReason,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF92400E),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons (Preview and Download) with animated button state transitions
            val isAudioSelected = activeQuality?.isAudioOnly == true

            val downloadButtonContainerColor by animateColorAsState(
                targetValue = when {
                    !isDownloadable -> MaterialTheme.colorScheme.surfaceVariant
                    isAudioSelected -> Color(0xFF9333EA)
                    else -> MaterialTheme.colorScheme.primary
                },
                animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
                label = "downloadButtonContainerColor"
            )
            val downloadButtonContentColor by animateColorAsState(
                targetValue = when {
                    !isDownloadable -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    isAudioSelected -> Color.White
                    else -> MaterialTheme.colorScheme.onPrimary
                },
                animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
                label = "downloadButtonContentColor"
            )
            val downloadButtonScale by animateFloatAsState(
                targetValue = if (isDownloadable && !isReducedMotion) 1f else 0.98f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                label = "downloadButtonScale"
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val q = activeQuality ?: group.qualities.firstOrNull()
                        if (q != null) onPreviewSelected(q)
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Preview video",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Preview", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = {
                        val q = activeQuality ?: group.qualities.firstOrNull()
                        if (q != null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDownloadSelected(q)
                        }
                    },
                    enabled = isDownloadable,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = downloadButtonContainerColor,
                        contentColor = downloadButtonContentColor,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .graphicsLayer {
                            scaleX = downloadButtonScale
                            scaleY = downloadButtonScale
                        }
                        .testTag("select_download_button_${group.id}")
                ) {
                    // Animated Icon transition
                    AnimatedContent(
                        targetState = Triple(isDownloadable, isAudioSelected, activeQuality?.id),
                        transitionSpec = {
                            (scaleIn(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)) + fadeIn(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion))) togetherWith
                                (scaleOut() + fadeOut(motionTween(MotionDurations.Micro, isReducedMotion = isReducedMotion)))
                        },
                        label = "downloadButtonIconTransition"
                    ) { (downloadable, isAudio, _) ->
                        Icon(
                            imageVector = when {
                                !downloadable -> Icons.Default.Lock
                                isAudio -> Icons.Default.Headphones
                                else -> Icons.Default.Download
                            },
                            contentDescription = if (isAudio) "Download audio-only stream" else "Download video",
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Animated Label transition
                    val buttonLabel = when (activeQuality?.category ?: group.category) {
                        MediaCompatibilityCategory.DIRECT_DOWNLOADABLE -> {
                            val qLabel = activeQuality?.resolutionShort ?: ""
                            val sLabel = activeQuality?.displaySize ?: ""
                            when {
                                isAudioSelected && sLabel.isNotBlank() && sLabel != "Unknown Size" -> "Download Audio ($sLabel)"
                                isAudioSelected -> "Download Audio"
                                qLabel.isNotBlank() && sLabel.isNotBlank() && sLabel != "Unknown Size" -> "Download $qLabel ($sLabel)"
                                qLabel.isNotBlank() -> "Download $qLabel"
                                else -> "Download"
                            }
                        }
                        MediaCompatibilityCategory.ADAPTIVE_STREAM -> {
                            if (isDownloadable) {
                                val qLabel = activeQuality?.resolutionShort ?: ""
                                val sLabel = activeQuality?.displaySize ?: ""
                                if (isAudioSelected) {
                                    "Download HLS Audio"
                                } else if (qLabel.isNotBlank() && sLabel.isNotBlank() && sLabel != "Adaptive Stream") {
                                    "Download $qLabel ($sLabel)"
                                } else if (qLabel.isNotBlank()) {
                                    "Download HLS ($qLabel)"
                                } else {
                                    "Download HLS"
                                }
                            } else {
                                "Unsupported Stream"
                            }
                        }
                        MediaCompatibilityCategory.RESTRICTED_OR_PROTECTED -> "Restricted"
                    }

                    AnimatedContent(
                        targetState = buttonLabel,
                        transitionSpec = {
                            (slideInVertically(animationSpec = motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = isReducedMotion)) { height -> height / 2 } +
                                fadeIn(animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion))) togetherWith
                                (slideOutVertically(animationSpec = motionTween(MotionDurations.Micro, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)) { height -> -height / 2 } +
                                    fadeOut(animationSpec = motionTween(MotionDurations.Micro, isReducedMotion = isReducedMotion)))
                        },
                        label = "downloadButtonLabelTransition"
                    ) { text ->
                        Text(
                            text = text,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated quality chip with spring selection bounce, adaptive color transitions,
 * audio-only visual equalizer and animated checkmark indicator.
 */
@Composable
private fun QualityChip(
    option: VideoQualityOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isReducedMotion = LocalReducedMotion.current
    val isAudio = option.isAudioOnly

    val chipScale by animateFloatAsState(
        targetValue = if (isSelected && !isReducedMotion) 1.05f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "chipScale_${option.id}"
    )

    val chipBgColor by animateColorAsState(
        targetValue = when {
            isSelected && isAudio -> Color(0xFF9333EA).copy(alpha = 0.24f)
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isAudio -> Color(0xFF9333EA).copy(alpha = 0.08f)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        },
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "chipBgColor_${option.id}"
    )

    val chipBorderColor by animateColorAsState(
        targetValue = when {
            isSelected && isAudio -> Color(0xFFA855F7)
            isSelected -> MaterialTheme.colorScheme.primary
            isAudio -> Color(0xFFA855F7).copy(alpha = 0.4f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        },
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "chipBorderColor_${option.id}"
    )

    val chipBorderWidth by animateDpAsState(
        targetValue = if (isSelected) 1.5.dp else 1.dp,
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "chipBorderWidth_${option.id}"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = chipBgColor,
        border = BorderStroke(chipBorderWidth, chipBorderColor),
        modifier = modifier
            .graphicsLayer {
                scaleX = chipScale
                scaleY = chipScale
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Animated selection checkmark indicator
            AnimatedVisibility(
                visible = isSelected,
                enter = scaleIn(motionSpring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium, isReducedMotion = isReducedMotion)) +
                    fadeIn(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)),
                exit = scaleOut(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)) +
                    fadeOut(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = if (isAudio) Color(0xFFA855F7) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
            }

            // Audio-only distinctive sound wave and headphones icon
            if (isAudio) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Audio only",
                    tint = if (isSelected) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                AudioWaveAnimation(
                    tint = if (isSelected) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    isAnimating = isSelected,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }

            Text(
                text = if (isAudio) "Audio" else option.resolutionShort,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = when {
                    isSelected && isAudio -> Color(0xFFA855F7)
                    isSelected -> MaterialTheme.colorScheme.primary
                    isAudio -> Color(0xFFA855F7).copy(alpha = 0.9f)
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "•",
                fontSize = 9.sp,
                color = if (isSelected) {
                    if (isAudio) Color(0xFFA855F7).copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = option.displaySize,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = if (isSelected) {
                    if (isAudio) Color(0xFFA855F7).copy(alpha = 0.9f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun QualityOptionRow(
    option: VideoQualityOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current
    val isAudio = option.isAudioOnly

    val rowScale by animateFloatAsState(
        targetValue = if (isSelected && !isReducedMotion) 1.01f else 1.0f,
        animationSpec = motionSpring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium, isReducedMotion = isReducedMotion),
        label = "rowScale_${option.id}"
    )

    val rowBgColor by animateColorAsState(
        targetValue = when {
            isSelected && isAudio -> Color(0xFF9333EA).copy(alpha = 0.2f)
            isSelected -> MaterialTheme.colorScheme.primaryContainer
            isAudio -> Color(0xFF9333EA).copy(alpha = 0.05f)
            else -> Color.Transparent
        },
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "rowBgColor_${option.id}"
    )

    val rowBorderColor by animateColorAsState(
        targetValue = when {
            isSelected && isAudio -> Color(0xFFA855F7)
            isSelected -> MaterialTheme.colorScheme.primary
            isAudio -> Color(0xFFA855F7).copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion),
        label = "rowBorderColor_${option.id}"
    )

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = rowBgColor,
        border = if (isSelected || isAudio) BorderStroke(if (isSelected) 1.5.dp else 1.dp, rowBorderColor) else null,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = rowScale
                scaleY = rowScale
            }
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio-only icon and wave animation
            if (isAudio) {
                Icon(
                    imageVector = Icons.Default.Headphones,
                    contentDescription = "Audio-only option",
                    tint = if (isSelected) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                AudioWaveAnimation(
                    tint = if (isSelected) Color(0xFFA855F7) else Color(0xFFA855F7).copy(alpha = 0.6f),
                    isAnimating = isSelected,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }

            // Resolution and Format
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = option.resolutionLabel,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            isSelected && isAudio -> Color(0xFFA855F7)
                            isSelected -> MaterialTheme.colorScheme.primary
                            isAudio -> Color(0xFFA855F7).copy(alpha = 0.9f)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isAudio) Color(0xFF9333EA).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = option.format,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAudio) Color(0xFFA855F7) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (option.isDownloadable) Color(0xFFD1FAE5) else Color(0xFFFEF3C7)
                    ) {
                        Text(
                            text = option.downloadType.displayName,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (option.isDownloadable) Color(0xFF065F46) else Color(0xFF92400E),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // File Size or stream
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = when {
                    isSelected && isAudio -> Color(0xFF9333EA).copy(alpha = 0.2f)
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                }
            ) {
                Text(
                    text = option.displaySize,
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        isSelected && isAudio -> Color(0xFFA855F7)
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Smooth Animated Selection Indicator
            AnimatedContent(
                targetState = isSelected,
                transitionSpec = {
                    (scaleIn(motionSpring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium, isReducedMotion = isReducedMotion)) +
                        fadeIn(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion))) togetherWith
                    (scaleOut(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)) +
                        fadeOut(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)))
                },
                label = "selectionIndicator_${option.id}"
            ) { selected ->
                if (selected) {
                    Surface(
                        shape = CircleShape,
                        color = if (isAudio) Color(0xFFA855F7).copy(alpha = 0.18f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, if (isAudio) Color(0xFFA855F7) else MaterialTheme.colorScheme.primary),
                        modifier = Modifier.size(20.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = if (isAudio) Color(0xFFA855F7) else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
private fun BadgeChip(
    label: String,
    color: Color,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = textColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}
