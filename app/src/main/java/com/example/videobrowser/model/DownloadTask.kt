package com.example.videobrowser.model

import java.util.Locale

enum class DownloadStatus {
    PENDING,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

data class DownloadTask(
    val id: String,
    val title: String,
    val mediaUrl: String,
    val pageUrl: String,
    val fileName: String,
    val mimeType: String,
    val format: String,
    val quality: String,
    val totalBytes: Long = -1L,
    val downloadedBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.PENDING,
    val progress: Float = 0f, // 0.0 to 1.0
    val speedFormatted: String = "",
    val filePath: String? = null,
    val errorMessage: String? = null,
    val systemDownloadId: Long? = null,
    val headers: Map<String, String> = emptyMap(),
    val downloadedSegments: Int = 0,
    val totalSegments: Int = 0,
    val isResumable: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
) {
    val formattedProgress: String
        get() = "${(progress * 100).toInt()}%"

    val isPartiallyDownloaded: Boolean
        get() = downloadedBytes > 0L && status != DownloadStatus.COMPLETED

    val canResume: Boolean
        get() = status == DownloadStatus.PAUSED || (status == DownloadStatus.FAILED && isPartiallyDownloaded)

    val formattedDownloadedSize: String
        get() {
            val dKb = downloadedBytes / 1024.0
            val dMb = dKb / 1024.0
            return if (totalBytes > 0) {
                val tMb = totalBytes / (1024.0 * 1024.0)
                String.format(Locale.US, "%.1f MB / %.1f MB", dMb, tMb)
            } else {
                String.format(Locale.US, "%.1f MB", dMb)
            }
        }

    val formattedResumableOffset: String
        get() {
            val dMb = (downloadedBytes / 1024.0) / 1024.0
            return String.format(Locale.US, "%.1f MB", dMb)
        }
}
