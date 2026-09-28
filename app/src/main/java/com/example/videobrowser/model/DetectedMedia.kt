package com.example.videobrowser.model

import java.util.Locale

/**
 * Source of the media detection.
 */
enum class DetectionSource {
    NETWORK_INTERCEPTION,
    DOM_INSPECTION,
    PLAYBACK_EVENT
}

/**
 * Capability-based categorization for media compatibility.
 */
enum class MediaCompatibilityCategory {
    DIRECT_DOWNLOADABLE,     // Direct accessible MP4/WebM/MKV/MOV/3GP
    ADAPTIVE_STREAM,         // HLS (m3u8) / DASH (mpd) segmented stream
    RESTRICTED_OR_PROTECTED  // DRM protected (Widevine/FairPlay) or platform cipher-restricted
}

/**
 * Represents a verified or candidate media resource detected on a web page.
 */
data class DetectedMedia(
    val id: String,
    val mediaUrl: String,
    val pageUrl: String,
    val title: String,
    val mimeType: String,
    val format: String, // e.g., "MP4", "WEBM", "HLS (m3u8)", "DASH (mpd)"
    val quality: String, // e.g., "1080p", "720p", "480p", "Adaptive Stream", or "Unknown"
    val contentLength: Long = -1L, // -1 if unknown or stream
    val durationSeconds: Double = 0.0,
    val width: Int = 0,
    val height: Int = 0,
    val isPlaying: Boolean = false,
    val isStream: Boolean = false,
    val isDownloadable: Boolean = true,
    val category: MediaCompatibilityCategory = MediaCompatibilityCategory.DIRECT_DOWNLOADABLE,
    val restrictionReason: String? = null,
    val isDrmProtected: Boolean = false,
    val source: DetectionSource = DetectionSource.NETWORK_INTERCEPTION,
    val headers: Map<String, String> = emptyMap(),
    val detectedAt: Long = System.currentTimeMillis(),
    val userInteracted: Boolean = false,
    val hasControls: Boolean = true,
    val isLooping: Boolean = false,
    val isMuted: Boolean = false,
    val isLikelyPreview: Boolean = false,
    val priorityScore: Int = 0,
    val availableQualities: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val parsedHlsVariants: List<com.example.videobrowser.download.HlsVariant> = emptyList(),
    val videoFamilyKey: String? = null
) {
    val formattedSize: String
        get() {
            if (contentLength <= 0) {
                return if (isStream) "Live / Stream" else "Unknown Size"
            }
            val kb = contentLength / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
                mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
                else -> String.format(Locale.US, "%.0f KB", kb)
            }
        }

    val displayQuality: String
        get() = when {
            height >= 2160 -> "4K ($width×$height)"
            height >= 1440 -> "2K ($width×$height)"
            height >= 1080 -> "1080p Full HD"
            height >= 720 -> "720p HD"
            height >= 480 -> "480p SD"
            height > 0 -> "${height}p"
            quality.isNotEmpty() -> quality
            isStream -> "Adaptive Stream"
            else -> "Standard Quality"
        }
}
