package com.example.videobrowser.model

import java.net.URI
import java.util.Locale

enum class VideoDownloadType(val displayName: String) {
    PROGRESSIVE("Progressive"),
    ADAPTIVE("Adaptive")
}

data class VideoQualityOption(
    val id: String,
    val resolutionLabel: String,
    val resolutionShort: String,
    val height: Int = 0,
    val width: Int = 0,
    val format: String,
    val mimeType: String,
    val contentLength: Long = -1L,
    val estimatedBytes: Long = -1L,
    val isEstimatedSize: Boolean = false,
    val formattedSize: String = "Unknown Size",
    val downloadType: VideoDownloadType,
    val mediaUrl: String,
    val headers: Map<String, String> = emptyMap(),
    val isDownloadable: Boolean = true,
    val category: MediaCompatibilityCategory = MediaCompatibilityCategory.DIRECT_DOWNLOADABLE,
    val restrictionReason: String? = null,
    val bandwidth: Long = 0L,
    val sourceMediaId: String = ""
) {
    val effectiveBytes: Long
        get() = if (contentLength > 0) contentLength else estimatedBytes

    val displaySize: String
        get() {
            if (formattedSize.isNotBlank() && formattedSize != "Unknown Size" && formattedSize != "Adaptive Stream") {
                return formattedSize
            }
            val bytes = effectiveBytes
            if (bytes <= 0) {
                return if (downloadType == VideoDownloadType.ADAPTIVE) "Adaptive Stream" else "Unknown Size"
            }
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            val sizeStr = when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
            return if (isEstimatedSize) "~$sizeStr (Est.)" else sizeStr
        }

    fun toDetectedMedia(parentTitle: String, pageUrl: String): DetectedMedia {
        return DetectedMedia(
            id = id,
            mediaUrl = mediaUrl,
            pageUrl = pageUrl,
            title = parentTitle,
            mimeType = mimeType,
            format = format,
            quality = resolutionShort,
            contentLength = if (contentLength > 0) contentLength else effectiveBytes,
            width = width,
            height = height,
            isStream = downloadType == VideoDownloadType.ADAPTIVE,
            isDownloadable = isDownloadable,
            category = category,
            restrictionReason = restrictionReason,
            headers = headers
        )
    }
}

data class GroupedVideoMedia(
    val id: String,
    val title: String,
    val pageUrl: String,
    val domain: String,
    val thumbnailUrl: String? = null,
    val durationSeconds: Double = 0.0,
    val isPlaying: Boolean = false,
    val userInteracted: Boolean = false,
    val hasControls: Boolean = true,
    val isLikelyPreview: Boolean = false,
    val priorityScore: Int = 0,
    val qualities: List<VideoQualityOption> = emptyList(),
    val selectedQualityId: String = "",
    val detectedAt: Long = System.currentTimeMillis(),
    val rawMediaCount: Int = 1
) {
    val selectedQuality: VideoQualityOption?
        get() = qualities.find { it.id == selectedQualityId } ?: qualities.firstOrNull()

    val isDownloadable: Boolean
        get() = selectedQuality?.isDownloadable ?: qualities.any { it.isDownloadable }

    val format: String
        get() = selectedQuality?.format ?: qualities.firstOrNull()?.format ?: "MP4"

    val highestQuality: String
        get() = selectedQuality?.resolutionShort ?: qualities.firstOrNull()?.resolutionShort ?: "Standard"

    val category: MediaCompatibilityCategory
        get() = selectedQuality?.category ?: qualities.firstOrNull()?.category ?: MediaCompatibilityCategory.DIRECT_DOWNLOADABLE

    val restrictionReason: String?
        get() = selectedQuality?.restrictionReason ?: qualities.firstOrNull()?.restrictionReason

    val formattedDuration: String
        get() {
            if (durationSeconds <= 0.0) return ""
            val totalSec = durationSeconds.toInt()
            val minutes = totalSec / 60
            val seconds = totalSec % 60
            val hours = minutes / 60
            return if (hours > 0) {
                String.format(Locale.US, "%d:%02d:%02d", hours, minutes % 60, seconds)
            } else {
                String.format(Locale.US, "%d:%02d", minutes, seconds)
            }
        }

    val displayDomain: String
        get() = if (domain.isNotBlank()) domain else try {
            URI(pageUrl).host?.removePrefix("www.") ?: ""
        } catch (_: Exception) {
            ""
        }

    val downloadableQualitiesCount: Int
        get() = qualities.count { it.isDownloadable }

    val totalQualitiesCount: Int
        get() = qualities.size

    val qualitiesOverviewText: String
        get() {
            if (qualities.isEmpty()) return "Standard"
            val count = qualities.size
            val labels = qualities.map { it.resolutionShort }.distinct().take(4).joinToString(", ")
            return if (count > 1) "$count qualities ($labels)" else labels
        }

    val sizeRangeText: String
        get() {
            val validQualities = qualities.filter { it.effectiveBytes > 0 }
            if (validQualities.isEmpty()) return ""
            val minBytes = validQualities.minOf { it.effectiveBytes }
            val maxBytes = validQualities.maxOf { it.effectiveBytes }
            val hasEstimates = validQualities.any { it.isEstimatedSize }
            val prefix = if (hasEstimates) "~" else ""
            val formatBytes: (Long) -> String = { bytes ->
                val mb = bytes / (1024.0 * 1024.0)
                val gb = mb / 1024.0
                if (gb >= 1.0) String.format(Locale.US, "%.1f GB", gb)
                else String.format(Locale.US, "%.1f MB", mb)
            }
            return if (minBytes == maxBytes) {
                "$prefix${formatBytes(minBytes)}"
            } else {
                "$prefix${formatBytes(minBytes)} – $prefix${formatBytes(maxBytes)}"
            }
        }
}
