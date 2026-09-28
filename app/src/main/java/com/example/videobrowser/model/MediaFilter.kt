package com.example.videobrowser.model

import android.net.Uri

object MediaFilter {

    private val VIDEO_EXTENSIONS = listOf(
        ".mp4", ".webm", ".mkv", ".m4v", ".mov", ".3gp", ".flv",
        ".m3u8", ".mpd", ".ts"
    )

    private val VIDEO_MIME_TYPES = listOf(
        "video/mp4",
        "video/webm",
        "video/x-matroska",
        "video/quicktime",
        "video/3gpp",
        "video/x-flv",
        "video/mp2t",
        "application/x-mpegurl",
        "application/vnd.apple.mpegurl",
        "application/dash+xml"
    )

    private val AD_AND_TRACKING_HOSTS = listOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adnxs.com",
        "scorecardresearch.com",
        "outbrain.com",
        "taboola.com",
        "adsystem.com",
        "adsafeprotected.com",
        "amazon-adsystem.com",
        "advertising.com",
        "rubiconproject.com",
        "pubmatic.com",
        "criteo.com",
        "moatads.com",
        "applovin.com",
        "unityads.unity3d.com",
        "vungle.com",
        "adcolony.com",
        "inmobi.com",
        "chartboost.com",
        "openx.net",
        "smartadserver.com"
    )

    private val AD_PATH_KEYWORDS = listOf(
        "/ads/", "/ad/", "/advertisement/",
        "vast.xml", "vpaid", "/pixel",
        "pagead", "tracking", "telemetry",
        "analytics", "beacon", "ad_break"
    )

    private val IGNORED_EXTENSIONS = listOf(
        ".js", ".css", ".html", ".htm", ".json", ".xml",
        ".png", ".jpg", ".jpeg", ".webp", ".svg", ".gif", ".ico",
        ".woff", ".woff2", ".ttf", ".otf", ".eot",
        ".mp3", ".aac", ".wav", ".ogg", ".flac"
    )

    private val SOCIAL_MEDIA_NON_VIDEO_PATTERNS = listOf(
        "profile_images", "avatar", "user_avatar", "emoji", "reactions",
        "story_thumb", "story_preview", "live_preview", "cover_photo",
        "sprite", "/graphql/query", "/logging/", "/telemetry/", "preview.redd.it",
        "/stickers/", "/stickers_v2/", "badge_icon"
    )

    private val GIF_PATTERNS = listOf(
        ".gif", "mime=image/gif", "image/gif",
        "giphy.com/media", "tenor.com/view", "gfycat.com",
        "/emojis/", "/stickers/", "format=gif", "animated_gif"
    )

    /**
     * Checks if a URL or MIME type matches a video candidate.
     */
    fun isCandidateVideo(url: String, mimeType: String? = null): Boolean {
        if (url.isBlank()) return false

        // Check if explicitly blocked as an ad or tracker
        if (isLikelyAdvertisementOrTracker(url)) {
            return false
        }

        val lowerUrl = url.lowercase()

        // Filter out obvious GIF or emoji/sticker media
        if (lowerUrl.endsWith(".gif") || mimeType?.equals("image/gif", ignoreCase = true) == true) {
            return false
        }

        // Check social media non-video assets (avatars, emojis, previews)
        if (SOCIAL_MEDIA_NON_VIDEO_PATTERNS.any { lowerUrl.contains(it) }) {
            return false
        }

        // Check ignored non-video static extensions
        val cleanPath = url.substringBefore('?').lowercase()
        if (IGNORED_EXTENSIONS.any { cleanPath.endsWith(it) }) {
            return false
        }

        // Check explicit MIME type
        if (!mimeType.isNullOrBlank()) {
            val lowerMime = mimeType.lowercase()
            if (VIDEO_MIME_TYPES.any { lowerMime.contains(it) } || lowerMime.startsWith("video/")) {
                return true
            }
        }

        // Check video extension in path
        if (VIDEO_EXTENSIONS.any { cleanPath.endsWith(it) }) {
            return true
        }

        // Check query params often containing video formats or indicators
        val queryIndications = listOf(
            "mime=video",
            "video_id=",
            ".mp4?",
            ".webm?",
            ".m3u8?",
            "format=mp4",
            "videoplayback",
            "stream/video"
        )
        if (queryIndications.any { lowerUrl.contains(it) }) {
            return true
        }

        return false
    }

    /**
     * Identifies if a media element is likely an animated GIF, short preview loop, or decorative asset.
     */
    fun isLikelyGifOrDecorative(
        url: String,
        mimeType: String? = null,
        duration: Double = 0.0,
        width: Int = 0,
        height: Int = 0,
        isLooping: Boolean = false,
        isMuted: Boolean = false,
        hasControls: Boolean = true
    ): Boolean {
        val lowerUrl = url.lowercase()
        if (GIF_PATTERNS.any { lowerUrl.contains(it) }) return true

        // Looping + muted + short duration (< 4.5 seconds) without native controls is characteristic of a GIF/preview
        if (isLooping && isMuted && !hasControls && duration > 0.0 && duration < 4.5) {
            return true
        }

        // Extremely small decorative video (< 200x200 and < 3s)
        if (width in 1..199 && height in 1..199 && duration in 0.1..3.0) {
            return true
        }

        return false
    }

    /**
     * Computes a multi-signal priority score to rank candidates, ensuring the video
     * the user is actively playing or watching is strictly prioritized first.
     */
    fun calculatePriorityScore(media: DetectedMedia): Int {
        var score = 0

        // 1. User Interaction & Active Playback (Highest Priority)
        if (media.isPlaying) {
            score += 1500
        }
        if (media.userInteracted) {
            score += 800
        }

        // 2. Duration signals (Full content vs micro clip)
        when {
            media.durationSeconds >= 60.0 -> score += 500
            media.durationSeconds >= 20.0 -> score += 300
            media.durationSeconds >= 5.0 -> score += 100
            media.durationSeconds in 0.1..3.0 -> score -= 300 // Micro clip penalty
        }

        // 3. Resolution & Dimensions
        when {
            media.height >= 1080 -> score += 400
            media.height >= 720 -> score += 300
            media.height >= 480 -> score += 150
            media.height in 1..240 -> score -= 200
        }

        // 4. Player Controls vs Looping Decoration
        if (media.hasControls) {
            score += 150
        }
        if (media.isLooping && media.isMuted && media.durationSeconds in 0.1..5.0) {
            score -= 600 // Heavy GIF loop penalty
        }

        // 5. Downloadability / Direct Stream
        if (media.isDownloadable) {
            score += 100
        }

        // 6. Source Signals
        if (media.source == DetectionSource.PLAYBACK_EVENT) {
            score += 400
        } else if (media.source == DetectionSource.DOM_INSPECTION) {
            score += 100
        }

        // 7. Suspicious Keywords in URL
        val lowerUrl = media.mediaUrl.lowercase()
        if (lowerUrl.contains("preview") || lowerUrl.contains("thumb") || lowerUrl.contains("trailer_thumb") || lowerUrl.contains("teaser")) {
            score -= 350
        }

        return score
    }

    /**
     * Filters known advertisement networks, tracking pixels, and telemetry beacons.
     */
    fun isLikelyAdvertisementOrTracker(url: String): Boolean {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: ""
            if (AD_AND_TRACKING_HOSTS.any { host.contains(it) }) {
                return true
            }
            val lowerUrl = url.lowercase()
            AD_PATH_KEYWORDS.any { lowerUrl.contains(it) }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Extracts a human-friendly format tag (e.g., MP4, WEBM, HLS).
     */
    fun inferFormat(url: String, mimeType: String?): String {
        val lowerUrl = url.lowercase()
        val lowerMime = mimeType?.lowercase() ?: ""

        return when {
            lowerMime.contains("mp4") || lowerUrl.contains(".mp4") -> "MP4"
            lowerMime.contains("webm") || lowerUrl.contains(".webm") -> "WEBM"
            lowerMime.contains("mpegurl") || lowerUrl.contains(".m3u8") -> "HLS (m3u8)"
            lowerMime.contains("dash") || lowerUrl.contains(".mpd") -> "DASH (mpd)"
            lowerMime.contains("matroska") || lowerUrl.contains(".mkv") -> "MKV"
            lowerMime.contains("quicktime") || lowerUrl.contains(".mov") -> "MOV"
            lowerMime.contains("3gpp") || lowerUrl.contains(".3gp") -> "3GP"
            lowerMime.startsWith("video/") -> lowerMime.removePrefix("video/").uppercase()
            else -> "Video"
        }
    }

    /**
     * Identifies if media is an adaptive live/segmented stream.
     */
    fun isStreamManifest(url: String, mimeType: String?): Boolean {
        val lowerUrl = url.lowercase()
        val lowerMime = mimeType?.lowercase() ?: ""
        return lowerUrl.contains(".m3u8") ||
                lowerUrl.contains(".mpd") ||
                lowerMime.contains("mpegurl") ||
                lowerMime.contains("dash")
    }

    /**
     * Cleans up video title by stripping common web suffix junk and noisy tags.
     */
    fun cleanTitle(title: String?): String {
        if (title.isNullOrBlank()) return "Detected Video"
        return title
            .replace(Regex("""\s*[-|–]\s*(YouTube|Vimeo|Dailymotion|Facebook|Instagram|Twitter|X|Watch.*|Free.*).*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\(Official\s*(HD|Video|Trailer|Audio)?\s*\)""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s*\[(Official\s*)?(HD|1080p|720p|4K)?\s*\]""", RegexOption.IGNORE_CASE), "")
            .trim()
            .ifEmpty { "Detected Video" }
    }
}
