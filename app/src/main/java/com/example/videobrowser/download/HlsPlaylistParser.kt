package com.example.videobrowser.download

import java.net.URI

data class HlsVariant(
    val bandwidth: Long,
    val resolution: String,
    val url: String,
    val codecs: String = "",
    val frameRate: Double = 0.0,
    val estimatedBytes: Long = -1L
)

data class HlsSegment(
    val index: Int,
    val durationSeconds: Double,
    val url: String
)

data class HlsPlaylist(
    val isMaster: Boolean,
    val isDrmProtected: Boolean,
    val drmReason: String? = null,
    val variants: List<HlsVariant> = emptyList(),
    val initSegmentUrl: String? = null,
    val segments: List<HlsSegment> = emptyList(),
    val targetDuration: Double = 0.0,
    val isLive: Boolean = false
) {
    val totalDuration: Double
        get() = segments.sumOf { it.durationSeconds }
}

object HlsPlaylistParser {

    private val DRM_KEY_METHODS = listOf("SAMPLE-AES", "SAMPLE-AES-CTR", "SAMPLE-AES-CENC")
    private val DRM_KEY_FORMATS = listOf("urn:uuid:edef8ba9-79d6-4ace-a3c8-27dcd51d21ed", "com.widevine", "com.apple.streamingkeydelivery", "com.microsoft.playready")

    /**
     * Parses an HLS .m3u8 playlist text.
     * @param content Raw text of the m3u8 playlist
     * @param baseUrl Base URL to resolve relative variant or segment paths
     */
    fun parse(content: String, baseUrl: String): HlsPlaylist {
        val lines = content.lines().map { it.trim() }.filter { it.isNotBlank() }
        if (lines.isEmpty() || !lines[0].startsWith("#EXTM3U")) {
            return HlsPlaylist(
                isMaster = false,
                isDrmProtected = false,
                drmReason = "Invalid HLS manifest header"
            )
        }

        var isDrm = false
        var drmReason: String? = null

        // Check for DRM encryption keys
        for (line in lines) {
            if (line.startsWith("#EXT-X-KEY")) {
                val upperLine = line.uppercase()
                val isDrmMethod = DRM_KEY_METHODS.any { upperLine.contains("METHOD=$it") }
                val isDrmFormat = DRM_KEY_FORMATS.any { line.contains(it, ignoreCase = true) }
                if (isDrmMethod || isDrmFormat) {
                    isDrm = true
                    drmReason = "HLS stream requires DRM license decryption (SAMPLE-AES / Widevine / FairPlay)."
                    break
                }
            }
        }

        val hasVariants = lines.any { it.startsWith("#EXT-X-STREAM-INF") }
        if (hasVariants) {
            val variants = mutableListOf<HlsVariant>()
            var currentBandwidth = 0L
            var currentResolution = ""
            var currentCodecs = ""
            var currentFrameRate = 0.0

            for (i in lines.indices) {
                val line = lines[i]
                if (line.startsWith("#EXT-X-STREAM-INF:")) {
                    val attrs = line.removePrefix("#EXT-X-STREAM-INF:")
                    currentBandwidth = extractAttributeLong(attrs, "BANDWIDTH")
                    currentResolution = extractAttributeString(attrs, "RESOLUTION")
                    currentCodecs = extractAttributeString(attrs, "CODECS")
                    currentFrameRate = extractAttributeDouble(attrs, "FRAME-RATE")
                } else if (!line.startsWith("#") && currentBandwidth > 0) {
                    val resolvedUrl = resolveUrl(baseUrl, line)
                    variants.add(
                        HlsVariant(
                            bandwidth = currentBandwidth,
                            resolution = currentResolution.ifBlank { "Adaptive" },
                            url = resolvedUrl,
                            codecs = currentCodecs,
                            frameRate = currentFrameRate
                        )
                    )
                    currentBandwidth = 0L
                    currentResolution = ""
                    currentCodecs = ""
                    currentFrameRate = 0.0
                }
            }

            // Sort variants highest quality/bandwidth first
            variants.sortByDescending { it.bandwidth }

            return HlsPlaylist(
                isMaster = true,
                isDrmProtected = isDrm,
                drmReason = drmReason,
                variants = variants
            )
        }

        // Media playlist containing segments
        val segments = mutableListOf<HlsSegment>()
        var initSegmentUrl: String? = null
        var currentDuration = 0.0
        var targetDuration = 0.0
        var hasEndList = false

        for (line in lines) {
            when {
                line.startsWith("#EXT-X-TARGETDURATION:") -> {
                    targetDuration = line.removePrefix("#EXT-X-TARGETDURATION:").toDoubleOrNull() ?: 0.0
                }
                line.startsWith("#EXT-X-MAP:") -> {
                    val uriAttr = extractAttributeString(line.removePrefix("#EXT-X-MAP:"), "URI")
                    if (uriAttr.isNotBlank()) {
                        initSegmentUrl = resolveUrl(baseUrl, uriAttr)
                    }
                }
                line.startsWith("#EXTINF:") -> {
                    val durStr = line.removePrefix("#EXTINF:").substringBefore(',').trim()
                    currentDuration = durStr.toDoubleOrNull() ?: 0.0
                }
                line.startsWith("#EXT-X-ENDLIST") -> {
                    hasEndList = true
                }
                !line.startsWith("#") -> {
                    if (line.isNotBlank()) {
                        val segmentUrl = resolveUrl(baseUrl, line)
                        segments.add(
                            HlsSegment(
                                index = segments.size,
                                durationSeconds = currentDuration,
                                url = segmentUrl
                            )
                        )
                        currentDuration = 0.0
                    }
                }
            }
        }

        return HlsPlaylist(
            isMaster = false,
            isDrmProtected = isDrm,
            drmReason = drmReason,
            initSegmentUrl = initSegmentUrl,
            segments = segments,
            targetDuration = targetDuration,
            isLive = !hasEndList
        )
    }

    private fun extractAttributeString(attrs: String, key: String): String {
        // Quoted string: KEY="([^"]*)"
        val quotedPattern = Regex("""$key="([^"]*)"""", RegexOption.IGNORE_CASE)
        val quotedMatch = quotedPattern.find(attrs)
        if (quotedMatch != null) {
            return quotedMatch.groupValues[1]
        }
        // Unquoted string: KEY=([^,\s]+)
        val unquotedPattern = Regex("""$key=([^,\s]+)""", RegexOption.IGNORE_CASE)
        return unquotedPattern.find(attrs)?.groupValues?.get(1) ?: ""
    }

    private fun extractAttributeLong(attrs: String, key: String): Long {
        val pattern = Regex("""$key=(\d+)""", RegexOption.IGNORE_CASE)
        return pattern.find(attrs)?.groupValues?.get(1)?.toLongOrNull() ?: 0L
    }

    private fun extractAttributeDouble(attrs: String, key: String): Double {
        val pattern = Regex("""$key=([0-9.]+)""", RegexOption.IGNORE_CASE)
        return pattern.find(attrs)?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    fun estimateSize(bandwidth: Long, durationSeconds: Double): Long {
        if (bandwidth <= 0L || durationSeconds <= 0.0) return -1L
        return (bandwidth / 8.0 * durationSeconds).toLong()
    }

    fun resolveUrl(baseUrl: String, targetUrl: String): String {
        return try {
            val base = URI(baseUrl)
            base.resolve(targetUrl).toString()
        } catch (_: Exception) {
            if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                targetUrl
            } else {
                val prefix = baseUrl.substringBeforeLast('/', "")
                "$prefix/$targetUrl"
            }
        }
    }
}
