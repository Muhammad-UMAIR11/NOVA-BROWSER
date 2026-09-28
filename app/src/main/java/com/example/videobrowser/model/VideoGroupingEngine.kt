package com.example.videobrowser.model

import android.net.Uri
import com.example.videobrowser.download.HlsVariant
import java.util.Locale

object VideoGroupingEngine {

    /**
     * Intelligently groups multiple detected media streams (such as range chunks, multiple quality renditions,
     * HLS playlists, or DOM video elements) into unified, single video entries.
     */
    fun groupMedia(
        rawMediaList: List<DetectedMedia>,
        currentPageUrl: String,
        currentPageTitle: String
    ): List<GroupedVideoMedia> {
        if (rawMediaList.isEmpty()) return emptyList()

        // 1. Pre-filter out advertisements, pure trackers, and tiny decorative loops
        val filtered = rawMediaList.filter { media ->
            !MediaFilter.isLikelyAdvertisementOrTracker(media.mediaUrl) &&
            !MediaFilter.isLikelyGifOrDecorative(
                url = media.mediaUrl,
                mimeType = media.mimeType,
                duration = media.durationSeconds,
                width = media.width,
                height = media.height,
                isLooping = media.isLooping,
                isMuted = media.isMuted,
                hasControls = media.hasControls
            )
        }

        if (filtered.isEmpty()) return emptyList()

        // 2. Identify and group media streams that belong to the same video
        val groupsMap = LinkedHashMap<String, MutableList<DetectedMedia>>()
        for (media in filtered) {
            val key = computeFamilyKey(media, currentPageUrl)
            val list = groupsMap.getOrPut(key) { mutableListOf() }
            list.add(media)
        }

        // 3. For each group, construct a unified GroupedVideoMedia
        val groupedList = groupsMap.map { (groupKey, members) ->
            createGroupedVideo(groupKey, members, currentPageUrl, currentPageTitle)
        }

        // 4. Sort grouped videos:
        // - Currently playing video first
        // - User interacted video second
        // - Priority score (duration, controls, resolution)
        // - Creation timestamp
        return groupedList.sortedWith(
            compareByDescending<GroupedVideoMedia> { it.isPlaying }
                .thenByDescending { it.userInteracted }
                .thenByDescending { it.priorityScore }
                .thenByDescending { it.detectedAt }
        )
    }

    /**
     * Derives a canonical family key to unite duplicate streams, range requests, and quality renditions.
     */
    fun computeFamilyKey(media: DetectedMedia, pageUrl: String): String {
        // If explicit video family key is set, use it
        if (!media.videoFamilyKey.isNullOrBlank()) {
            return media.videoFamilyKey
        }

        val url = media.mediaUrl
        try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase() ?: ""
            val rawPath = uri.path ?: ""

            // Segment chunks (.ts, .m4s) belong to their parent directory/stream
            val cleanPath = if (rawPath.endsWith(".ts", ignoreCase = true) ||
                rawPath.endsWith(".m4s", ignoreCase = true) ||
                rawPath.contains("/seg", ignoreCase = true) ||
                rawPath.contains("/chunk", ignoreCase = true)
            ) {
                rawPath.substringBeforeLast('/')
            } else {
                rawPath
            }

            // Strip common resolution tokens from path: e.g. "bunny_1080p.mp4" -> "bunny"
            val strippedPath = cleanPath
                .replace(Regex("""[-_](1080p?|720p?|480p?|360p?|240p?|144p?|4k|2k|hd|sd|fhd|uhd)""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""/(1080p?|720p?|480p?|360p?|240p?)/""", RegexOption.IGNORE_CASE), "/")
                .substringBeforeLast('.') // strip extension for family matching

            // If the title is specific and non-generic, combine with title
            val cleanTitle = MediaFilter.cleanTitle(media.title)
            val isSpecificTitle = cleanTitle.length >= 6 &&
                !cleanTitle.equals("Detected Video", ignoreCase = true) &&
                !cleanTitle.equals("Webpage", ignoreCase = true)

            if (isSpecificTitle) {
                val titleKey = cleanTitle.lowercase().replace(Regex("[^a-z0-9]"), "").take(30)
                return "title:$titleKey"
            }

            if (host.isNotEmpty() && strippedPath.isNotEmpty()) {
                return "host:$host/path:$strippedPath"
            }
        } catch (_: Exception) {}

        // Fallback to mediaUrl without query params
        return url.substringBefore('?')
    }

    /**
     * Builds the GroupedVideoMedia and all its constituent quality options.
     */
    private fun createGroupedVideo(
        groupKey: String,
        members: List<DetectedMedia>,
        currentPageUrl: String,
        currentPageTitle: String
    ): GroupedVideoMedia {
        // Select best representative metadata:
        // Prefer member that is playing, or interacted, or has the longest title / best thumbnail
        val primary = members.maxByOrNull {
            var score = 0
            if (it.isPlaying) score += 1000
            if (it.userInteracted) score += 500
            if (it.hasControls) score += 200
            if (!it.thumbnailUrl.isNullOrBlank()) score += 100
            if (it.title.length > 5 && !it.title.contains("Detected Video", ignoreCase = true)) score += 100
            if (it.height > 0) score += it.height
            score
        } ?: members.first()

        val isPlaying = members.any { it.isPlaying }
        val userInteracted = members.any { it.userInteracted }
        val hasControls = members.any { it.hasControls }
        val isLikelyPreview = members.all { it.isLikelyPreview }
        val maxDuration = members.maxOfOrNull { it.durationSeconds } ?: 0.0
        val maxPriority = members.maxOfOrNull { it.priorityScore } ?: primary.priorityScore

        val bestTitle = members.map { it.title }
            .firstOrNull { it.isNotBlank() && !it.equals("Detected Video", ignoreCase = true) }
            ?: if (currentPageTitle.isNotBlank()) MediaFilter.cleanTitle(currentPageTitle) else "Detected Video"

        val bestThumbnail = members.mapNotNull { it.thumbnailUrl }.firstOrNull { it.isNotBlank() }

        val domain = try {
            val uri = Uri.parse(if (primary.pageUrl.isNotBlank()) primary.pageUrl else currentPageUrl)
            uri.host?.removePrefix("www.") ?: ""
        } catch (_: Exception) {
            ""
        }

        // Collect and build qualities
        val qualities = buildQualities(members, bestTitle, currentPageUrl)

        // Select default quality:
        // 1. If an item is playing, pick its quality
        // 2. Otherwise pick the highest resolution downloadable quality
        val defaultQualityId = qualities.find { q ->
            members.any { m -> m.isPlaying && (m.mediaUrl == q.mediaUrl || m.height == q.height) }
        }?.id ?: qualities.firstOrNull { it.isDownloadable }?.id ?: qualities.firstOrNull()?.id ?: ""

        return GroupedVideoMedia(
            id = primary.id.ifBlank { groupKey.hashCode().toString() },
            title = bestTitle,
            pageUrl = primary.pageUrl.ifBlank { currentPageUrl },
            domain = domain,
            thumbnailUrl = bestThumbnail,
            durationSeconds = maxDuration,
            isPlaying = isPlaying,
            userInteracted = userInteracted,
            hasControls = hasControls,
            isLikelyPreview = isLikelyPreview,
            priorityScore = maxPriority + (if (isPlaying) 1500 else 0) + (if (userInteracted) 800 else 0),
            qualities = qualities,
            selectedQualityId = defaultQualityId,
            detectedAt = members.maxOf { it.detectedAt },
            rawMediaCount = members.size
        )
    }

    /**
     * Extracts and synthesizes all available qualities across the grouped items,
     * including expanding parsed HLS variants if present.
     */
    fun buildQualities(
        members: List<DetectedMedia>,
        parentTitle: String,
        pageUrl: String
    ): List<VideoQualityOption> {
        val options = mutableListOf<VideoQualityOption>()

        // 1. Check for HLS streams with variants
        for (member in members) {
            if (member.isStream && member.parsedHlsVariants.isNotEmpty()) {
                for (variant in member.parsedHlsVariants) {
                    val resInfo = resolveResolutionInfo(variant.resolution, variant.bandwidth, 0, 0)
                    val estBytes = if (variant.estimatedBytes > 0) {
                        variant.estimatedBytes
                    } else if (variant.bandwidth > 0 && member.durationSeconds > 0) {
                        (variant.bandwidth / 8.0 * member.durationSeconds).toLong()
                    } else {
                        -1L
                    }

                    val isEstimated = estBytes > 0
                    val formattedSize = if (estBytes > 0) {
                        val mb = estBytes / (1024.0 * 1024.0)
                        val gb = mb / 1024.0
                        if (gb >= 1.0) String.format(Locale.US, "~%.2f GB", gb)
                        else String.format(Locale.US, "~%.1f MB", mb)
                    } else {
                        "Adaptive Stream"
                    }

                    options.add(
                        VideoQualityOption(
                            id = "${member.id}_hls_${resInfo.shortLabel}_${variant.bandwidth}",
                            resolutionLabel = resInfo.displayLabel,
                            resolutionShort = resInfo.shortLabel,
                            height = resInfo.height,
                            width = resInfo.width,
                            format = "HLS (m3u8)",
                            mimeType = "application/x-mpegURL",
                            contentLength = -1L,
                            estimatedBytes = estBytes,
                            isEstimatedSize = isEstimated,
                            formattedSize = formattedSize,
                            downloadType = VideoDownloadType.ADAPTIVE,
                            mediaUrl = variant.url,
                            headers = member.headers,
                            isDownloadable = member.isDownloadable,
                            category = member.category,
                            restrictionReason = member.restrictionReason,
                            bandwidth = variant.bandwidth,
                            sourceMediaId = member.id
                        )
                    )
                }
            }
        }

        // 2. Process progressive / direct media entries and standalone streams
        for (member in members) {
            // If HLS variants were already expanded from this member, skip adding the raw master manifest as an extra option
            if (member.isStream && member.parsedHlsVariants.isNotEmpty()) {
                continue
            }

            // If it's a raw .ts or .m4s chunk and we already have a stream manifest, skip standalone chunk
            val isChunk = member.mediaUrl.contains(".ts") || member.mediaUrl.contains(".m4s")
            val hasManifest = members.any { it.isStream && it.mediaUrl.contains(".m3u8") }
            if (isChunk && hasManifest) continue

            val downloadType = if (member.isStream) VideoDownloadType.ADAPTIVE else VideoDownloadType.PROGRESSIVE
            val resInfo = resolveResolutionInfo("", 0L, member.width, member.height)
            val shortRes = if (resInfo.shortLabel.isNotBlank()) resInfo.shortLabel else member.quality.ifBlank { "Standard" }
            val labelRes = if (resInfo.displayLabel.isNotBlank()) resInfo.displayLabel else member.displayQuality
            val formattedSize = if (member.contentLength > 0) member.formattedSize else "Unknown Size"

            val option = VideoQualityOption(
                id = "${member.id}_${shortRes}_${member.format}",
                resolutionLabel = labelRes,
                resolutionShort = shortRes,
                height = if (resInfo.height > 0) resInfo.height else member.height,
                width = if (resInfo.width > 0) resInfo.width else member.width,
                format = member.format,
                mimeType = member.mimeType,
                contentLength = member.contentLength,
                estimatedBytes = -1L,
                isEstimatedSize = false,
                formattedSize = formattedSize,
                downloadType = downloadType,
                mediaUrl = member.mediaUrl,
                headers = member.headers,
                isDownloadable = member.isDownloadable,
                category = member.category,
                restrictionReason = member.restrictionReason,
                sourceMediaId = member.id
            )
            options.add(option)
        }

        // 3. Deduplicate qualities:
        // Group by (resolutionShort, format, downloadType).
        // For duplicates, keep the one with known contentLength or larger size.
        val deduplicated = options
            .groupBy { "${it.resolutionShort.lowercase()}_${it.format.lowercase()}_${it.downloadType}" }
            .values
            .map { group ->
                group.maxByOrNull { it.effectiveBytes } ?: group.first()
            }

        // 4. Sort qualities: highest resolution first, then bandwidth, then effectiveBytes
        return deduplicated.sortedWith(
            compareByDescending<VideoQualityOption> { it.height }
                .thenByDescending { it.bandwidth }
                .thenByDescending { it.effectiveBytes }
                .thenBy { it.format }
        )
    }

    data class ResolutionInfo(
        val displayLabel: String,
        val shortLabel: String,
        val width: Int,
        val height: Int
    )

    fun resolveResolutionInfo(
        resString: String,
        bandwidth: Long,
        width: Int,
        height: Int
    ): ResolutionInfo {
        var w = width
        var h = height

        if (resString.isNotBlank()) {
            val parts = resString.split("x", "X")
            if (parts.size == 2) {
                w = parts[0].toIntOrNull() ?: w
                h = parts[1].toIntOrNull() ?: h
            }
        }

        if (h > 0) {
            val (label, short) = when {
                h >= 2160 -> "4K UHD (2160p)" to "2160p"
                h >= 1440 -> "2K QHD (1440p)" to "1440p"
                h >= 1080 -> "1080p Full HD" to "1080p"
                h >= 720 -> "720p HD" to "720p"
                h >= 480 -> "480p SD" to "480p"
                h >= 360 -> "360p" to "360p"
                h >= 240 -> "240p" to "240p"
                else -> "${h}p" to "${h}p"
            }
            return ResolutionInfo(label, short, w, h)
        }

        if (bandwidth > 0) {
            val mbps = String.format(Locale.US, "%.1f Mbps", bandwidth / 1_000_000.0)
            return when {
                bandwidth >= 4_500_000 -> ResolutionInfo("1080p Full HD (~$mbps)", "1080p", 1920, 1080)
                bandwidth >= 2_200_000 -> ResolutionInfo("720p HD (~$mbps)", "720p", 1280, 720)
                bandwidth >= 1_000_000 -> ResolutionInfo("480p SD (~$mbps)", "480p", 854, 480)
                bandwidth >= 500_000 -> ResolutionInfo("360p (~$mbps)", "360p", 640, 360)
                else -> ResolutionInfo("240p (~$mbps)", "240p", 426, 240)
            }
        }

        return ResolutionInfo("Standard Quality", "Standard", w, h)
    }
}
