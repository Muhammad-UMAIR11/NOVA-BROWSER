package com.example.videobrowser.model

import java.net.URI

object MediaFilter {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "m3u8", "mpd", "webm", "mkv", "mov", "avi", "flv", "3gp", "ts", "m4v", "f4v"
    )

    private val AD_TRACKER_KEYWORDS = listOf(
        "googleads", "googlesyndication", "pagead", "doubleclick", "adservice", "analytics", "tracking", "telemetry",
        "adsystem", "adnxs", "taboola", "outbrain", "adserver", "advert", "sponsor", "commercial", "/ads/", "ad_break", "vast"
    )

    fun isCandidateVideo(url: String, mimeType: String? = null): Boolean {
        if (url.isBlank()) return false
        val lowerUrl = url.lowercase()

        if (isLikelyAdvertisementOrTracker(lowerUrl)) return false

        if (mimeType != null && mimeType.isNotBlank()) {
            val lowerMime = mimeType.lowercase()
            if (lowerMime.startsWith("video/") ||
                lowerMime.contains("mpegurl") ||
                lowerMime.contains("dash+xml") ||
                lowerMime.contains("octet-stream")
            ) {
                return true
            }
        }

        if (lowerUrl.contains("video%2f") || lowerUrl.contains("mime=video")) {
            return true
        }

        val cleanPath = try {
            URI(url).path?.lowercase() ?: lowerUrl
        } catch (_: Exception) {
            lowerUrl
        }

        return VIDEO_EXTENSIONS.any { ext ->
            cleanPath.contains(".$ext") ||
            lowerUrl.contains(".$ext") ||
            lowerUrl.contains("format=$ext") ||
            lowerUrl.contains("format%3d$ext") ||
            lowerUrl.contains("ext=$ext") ||
            lowerUrl.contains("type=$ext")
        } ||
            lowerUrl.contains(".m3u8") ||
            lowerUrl.contains(".mpd") ||
            lowerUrl.contains(".mp4")
    }

    fun isLikelyAdvertisementOrTracker(url: String): Boolean {
        val lower = url.lowercase()
        return AD_TRACKER_KEYWORDS.any { lower.contains(it) }
    }

    fun isLikelyGifOrDecorative(
        url: String,
        width: Int = 0,
        height: Int = 0,
        duration: Double = 0.0,
        mimeType: String? = null,
        isLooping: Boolean = false,
        isMuted: Boolean = false,
        hasControls: Boolean = true
    ): Boolean {
        val lowerUrl = url.lowercase()
        if (lowerUrl.contains(".gif") || lowerUrl.contains("giphy.com") || lowerUrl.contains("tenor.com")) {
            return true
        }
        if (width in 1..100 && height in 1..100) return true
        if (duration in 0.01..2.5 && isLooping && isMuted && !hasControls) return true
        return false
    }

    fun inferFormat(url: String, mimeType: String?): String {
        val lowerUrl = url.lowercase()
        val lowerMime = mimeType?.lowercase().orEmpty()

        return when {
            lowerUrl.contains(".m3u8") || lowerMime.contains("mpegurl") -> "HLS (m3u8)"
            lowerUrl.contains(".mpd") || lowerMime.contains("dash+xml") -> "DASH (mpd)"
            lowerUrl.contains(".webm") || lowerMime.contains("webm") -> "WEBM"
            lowerUrl.contains(".mkv") -> "MKV"
            lowerUrl.contains(".mov") -> "MOV"
            lowerUrl.contains(".3gp") -> "3GP"
            lowerUrl.contains(".flv") -> "FLV"
            else -> "MP4"
        }
    }

    fun isStreamManifest(url: String, mimeType: String?): Boolean {
        val lowerUrl = url.lowercase()
        val lowerMime = mimeType?.lowercase().orEmpty()
        return lowerUrl.contains(".m3u8") ||
            lowerUrl.contains(".mpd") ||
            lowerMime.contains("mpegurl") ||
            lowerMime.contains("dash+xml")
    }

    fun cleanTitle(title: String): String {
        if (title.isBlank()) return "Detected Video"
        var cleaned = title
            .replace(Regex("(?i)https?://\\S+"), "")
            .replace(Regex("(?i)\\s*[\\[(](?:official|hd|4k|hq|full hd|video|clip|trailer)[^\\])]*[\\])]"), "")
            .replace(Regex("\\s*\\|.*$"), "")
            .replace(Regex("(?i)\\s*[-–—]\\s*(?:youtube|vimeo|dailymotion|twitch|facebook|tiktok|twitter|reddit|instagram|watch\\b.*|free\\b.*|online\\b.*).*$"), "")
            .trim()

        cleaned = cleaned.replace(Regex("_+"), " ").trim()
        return cleaned.ifBlank { "Detected Video" }
    }

    fun calculatePriorityScore(media: DetectedMedia): Int {
        var score = 100
        if (media.isPlaying) score += 500
        if (media.userInteracted) score += 300
        if (media.durationSeconds > 60.0) score += 200
        if (media.isStream) score += 150
        if (media.height >= 720) score += 100
        if (media.isLikelyPreview) score -= 300
        return score
    }
}
