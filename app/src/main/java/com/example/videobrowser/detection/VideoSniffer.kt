package com.example.videobrowser.detection

import android.util.Log
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DetectionSource
import com.example.videobrowser.model.MediaCompatibilityCategory
import com.example.videobrowser.model.MediaFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class VideoSniffer(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    companion object {
        private const val TAG = "VideoSniffer"
    }

    /**
     * Inspects a requested URL from WebView interception.
     * Returns a candidate DetectedMedia immediately or null if not a candidate.
     */
    fun createCandidateFromNetwork(
        url: String,
        headers: Map<String, String>,
        pageUrl: String,
        pageTitle: String
    ): DetectedMedia? {
        if (!MediaFilter.isCandidateVideo(url)) {
            return null
        }

        val format = MediaFilter.inferFormat(url, null)
        val isStream = MediaFilter.isStreamManifest(url, null)
        val id = generateMediaId(url)
        val compat = PlatformCompatibilityEngine.evaluate(
            mediaUrl = url,
            pageUrl = pageUrl,
            mimeType = null,
            isStream = isStream,
            isDrmReported = false
        )

        return DetectedMedia(
            id = id,
            mediaUrl = url,
            pageUrl = pageUrl,
            title = MediaFilter.cleanTitle(pageTitle),
            mimeType = if (isStream) "application/x-mpegURL" else "video/mp4",
            format = format,
            quality = if (isStream) "Adaptive Stream" else "Standard",
            isStream = isStream,
            isDownloadable = compat.isDownloadable,
            category = compat.category,
            restrictionReason = compat.restrictionReason,
            source = DetectionSource.NETWORK_INTERCEPTION,
            headers = headers
        )
    }

    /**
     * Performs an asynchronous probe for Content-Type, Content-Length, HLS variants, and accessibility.
     * Uses HEAD first, with a fallback to a byte-range GET request if HEAD is rejected or lacks length.
     */
    suspend fun probeMediaHeaders(candidate: DetectedMedia): DetectedMedia = withContext(Dispatchers.IO) {
        var contentType: String = candidate.mimeType
        var contentLength: Long = candidate.contentLength
        var streamDuration: Double = candidate.durationSeconds

        try {
            // 1. Attempt lightweight HEAD request
            val headBuilder = Request.Builder()
                .url(candidate.mediaUrl)
                .head()
            candidate.headers["Referer"]?.let { headBuilder.header("Referer", it) }
            candidate.headers["User-Agent"]?.let { headBuilder.header("User-Agent", it) }
            candidate.headers["Cookie"]?.let { headBuilder.header("Cookie", it) }

            try {
                client.newCall(headBuilder.build()).execute().use { response ->
                    if (response.isSuccessful) {
                        response.header("Content-Type")?.let { contentType = it }
                        response.header("Content-Length")?.toLongOrNull()?.let {
                            if (it > 0) contentLength = it
                        }
                    }
                }
            } catch (_: Exception) {}

            // 2. If Content-Length unknown and not an m3u8 stream, fallback to Range: bytes=0-1024
            if (contentLength <= 0L && !candidate.mediaUrl.contains(".m3u8", ignoreCase = true)) {
                try {
                    val rangeBuilder = Request.Builder()
                        .url(candidate.mediaUrl)
                        .get()
                        .header("Range", "bytes=0-1024")
                    candidate.headers["Referer"]?.let { rangeBuilder.header("Referer", it) }
                    candidate.headers["User-Agent"]?.let { rangeBuilder.header("User-Agent", it) }
                    candidate.headers["Cookie"]?.let { rangeBuilder.header("Cookie", it) }

                    client.newCall(rangeBuilder.build()).execute().use { rangeResp ->
                        if (rangeResp.code == 206 || rangeResp.isSuccessful) {
                            rangeResp.header("Content-Type")?.let { contentType = it }
                            val contentRange = rangeResp.header("Content-Range")
                            if (!contentRange.isNullOrBlank()) {
                                // Content-Range format: "bytes 0-1024/52428800"
                                val total = contentRange.substringAfterLast('/').toLongOrNull()
                                if (total != null && total > 0) {
                                    contentLength = total
                                }
                            } else {
                                val cl = rangeResp.header("Content-Length")?.toLongOrNull()
                                if (cl != null && cl > 1024) {
                                    contentLength = cl
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            val format = MediaFilter.inferFormat(candidate.mediaUrl, contentType)
            val isStream = MediaFilter.isStreamManifest(candidate.mediaUrl, contentType)
            var isDrmStream = candidate.isDrmProtected
            var drmReason = candidate.restrictionReason
            var parsedVariants = candidate.parsedHlsVariants

            // 3. Inspect HLS master playlists and variant streams
            if (isStream && candidate.mediaUrl.contains(".m3u8", ignoreCase = true)) {
                try {
                    val getReq = Request.Builder().url(candidate.mediaUrl).get()
                    candidate.headers["Referer"]?.let { getReq.header("Referer", it) }
                    candidate.headers["User-Agent"]?.let { getReq.header("User-Agent", it) }
                    candidate.headers["Cookie"]?.let { getReq.header("Cookie", it) }

                    client.newCall(getReq.build()).execute().use { getResp ->
                        if (getResp.isSuccessful) {
                            val body = getResp.body?.string() ?: ""
                            val parsed = com.example.videobrowser.download.HlsPlaylistParser.parse(body, candidate.mediaUrl)
                            if (parsed.isDrmProtected) {
                                isDrmStream = true
                                drmReason = parsed.drmReason
                            }
                            if (parsed.isMaster && parsed.variants.isNotEmpty()) {
                                parsedVariants = parsed.variants

                                // If stream duration is not yet known from DOM, probe the first variant playlist to read segment durations
                                if (streamDuration <= 0.0 && parsedVariants.isNotEmpty()) {
                                    try {
                                        val firstUrl = parsedVariants.first().url
                                        val vReq = Request.Builder().url(firstUrl).get()
                                        candidate.headers["Referer"]?.let { vReq.header("Referer", it) }
                                        candidate.headers["User-Agent"]?.let { vReq.header("User-Agent", it) }
                                        candidate.headers["Cookie"]?.let { vReq.header("Cookie", it) }
                                        client.newCall(vReq.build()).execute().use { vResp ->
                                            if (vResp.isSuccessful) {
                                                val vBody = vResp.body?.string() ?: ""
                                                val vParsed = com.example.videobrowser.download.HlsPlaylistParser.parse(vBody, firstUrl)
                                                if (vParsed.totalDuration > 0.0) {
                                                    streamDuration = vParsed.totalDuration
                                                }
                                            }
                                        }
                                    } catch (_: Exception) {}
                                }

                                // If duration is available, compute estimated file size for every variant
                                if (streamDuration > 0.0) {
                                    parsedVariants = parsedVariants.map { variant ->
                                        val est = com.example.videobrowser.download.HlsPlaylistParser.estimateSize(variant.bandwidth, streamDuration)
                                        variant.copy(estimatedBytes = est)
                                    }
                                }
                            } else if (!parsed.isMaster && parsed.totalDuration > 0.0) {
                                streamDuration = parsed.totalDuration
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            val compat = PlatformCompatibilityEngine.evaluate(
                mediaUrl = candidate.mediaUrl,
                pageUrl = candidate.pageUrl,
                mimeType = contentType,
                isStream = isStream,
                isDrmReported = isDrmStream
            )

            return@withContext candidate.copy(
                mimeType = contentType,
                contentLength = contentLength,
                durationSeconds = if (streamDuration > 0) streamDuration else candidate.durationSeconds,
                format = format,
                isStream = isStream,
                isDownloadable = compat.isDownloadable && !isDrmStream,
                category = if (isDrmStream) MediaCompatibilityCategory.RESTRICTED_OR_PROTECTED else compat.category,
                restrictionReason = drmReason ?: compat.restrictionReason,
                isDrmProtected = isDrmStream,
                parsedHlsVariants = parsedVariants
            )
        } catch (e: Exception) {
            Log.d(TAG, "Header probe failed for ${candidate.mediaUrl}: ${e.message}")
        }
        return@withContext candidate
    }

    fun generateMediaId(url: String): String {
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val hash = digest.digest(url.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }.take(12)
        } catch (_: Exception) {
            url.hashCode().toString()
        }
    }
}
