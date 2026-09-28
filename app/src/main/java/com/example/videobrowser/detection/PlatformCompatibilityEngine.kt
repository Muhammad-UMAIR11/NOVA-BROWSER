package com.example.videobrowser.detection

import android.net.Uri
import com.example.videobrowser.model.MediaCompatibilityCategory

data class CompatibilityResult(
    val category: MediaCompatibilityCategory,
    val isDownloadable: Boolean,
    val restrictionReason: String? = null,
    val platformTag: String = "Web"
)

object PlatformCompatibilityEngine {

    private val DRM_KEYWORDS = listOf(
        "widevine", "fairplay", "playready", "clearkey",
        "/drm/", "drm=true", "license_server", "eme-cert"
    )

    /**
     * Determines whether the given media request on the given page can be downloaded,
     * or whether it is an adaptive stream or restricted/DRM protected media.
     */
    fun evaluate(
        mediaUrl: String,
        pageUrl: String,
        mimeType: String? = null,
        isStream: Boolean = false,
        isDrmReported: Boolean = false
    ): CompatibilityResult {
        val lowerMedia = mediaUrl.lowercase()
        val lowerPage = pageUrl.lowercase()
        val lowerMime = mimeType?.lowercase() ?: ""
        val platformTag = detectPlatform(pageUrl, mediaUrl)

        // 1. YouTube Platform Restriction & Access Controls
        if (isYouTube(lowerPage, lowerMedia)) {
            return CompatibilityResult(
                category = MediaCompatibilityCategory.RESTRICTED_OR_PROTECTED,
                isDownloadable = false,
                restrictionReason = "YouTube media uses dynamic signature ciphers and adaptive multi-track streams. In compliance with platform technical protections and policies, direct downloading is not supported.",
                platformTag = "YouTube"
            )
        }

        // 2. Explicit DRM / EME Encryption Detection
        if (isDrmReported || DRM_KEYWORDS.any { lowerMedia.contains(it) }) {
            return CompatibilityResult(
                category = MediaCompatibilityCategory.RESTRICTED_OR_PROTECTED,
                isDownloadable = false,
                restrictionReason = "Content is encrypted with Digital Rights Management (DRM - Widevine/ClearKey). Direct downloading is restricted by content encryption.",
                platformTag = platformTag
            )
        }

        // 3. Adaptive Streaming Manifests (HLS / DASH)
        if (isStream || lowerMedia.contains(".m3u8") || lowerMedia.contains(".mpd") ||
            lowerMime.contains("mpegurl") || lowerMime.contains("dash")
        ) {
            val isHls = lowerMedia.contains(".m3u8") || lowerMime.contains("mpegurl")
            return if (isHls) {
                // Accessible HLS stream without DRM: Downloadable via HLS segment assembler
                CompatibilityResult(
                    category = MediaCompatibilityCategory.ADAPTIVE_STREAM,
                    isDownloadable = true,
                    restrictionReason = null,
                    platformTag = platformTag
                )
            } else {
                // DASH adaptive streams with separate multi-track representations
                CompatibilityResult(
                    category = MediaCompatibilityCategory.ADAPTIVE_STREAM,
                    isDownloadable = false,
                    restrictionReason = "DASH adaptive stream ($platformTag). Multi-track separate streams require custom audio/video demuxing pipeline.",
                    platformTag = platformTag
                )
            }
        }

        // 4. Direct Downloadable Media (MP4, WebM, MKV, MOV, 3GP, etc.)
        return CompatibilityResult(
            category = MediaCompatibilityCategory.DIRECT_DOWNLOADABLE,
            isDownloadable = true,
            restrictionReason = null,
            platformTag = platformTag
        )
    }

    fun isYouTube(pageUrl: String, mediaUrl: String): Boolean {
        val lowerPage = pageUrl.lowercase()
        val lowerMedia = mediaUrl.lowercase()
        return lowerPage.contains("youtube.com") ||
                lowerPage.contains("youtu.be") ||
                lowerMedia.contains("googlevideo.com")
    }

    fun detectPlatform(pageUrl: String, mediaUrl: String): String {
        val lowerPage = pageUrl.lowercase()
        val lowerMedia = mediaUrl.lowercase()

        return when {
            lowerPage.contains("youtube.com") || lowerPage.contains("youtu.be") || lowerMedia.contains("googlevideo.com") -> "YouTube"
            lowerPage.contains("twitter.com") || lowerPage.contains("x.com") || lowerMedia.contains("twimg.com") -> "Twitter / X"
            lowerPage.contains("instagram.com") || lowerMedia.contains("cdninstagram.com") -> "Instagram"
            lowerPage.contains("facebook.com") || lowerPage.contains("fb.watch") || lowerMedia.contains("fbcdn.net") -> "Facebook"
            lowerPage.contains("reddit.com") || lowerMedia.contains("v.redd.it") -> "Reddit"
            lowerPage.contains("tiktok.com") || lowerMedia.contains("tiktokcdn.com") -> "TikTok"
            lowerPage.contains("vimeo.com") -> "Vimeo"
            lowerPage.contains("dailymotion.com") -> "Dailymotion"
            else -> try {
                Uri.parse(pageUrl).host?.removePrefix("www.")?.take(20) ?: "Web"
            } catch (_: Exception) {
                "Web"
            }
        }
    }
}
