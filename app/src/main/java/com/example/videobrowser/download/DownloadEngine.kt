package com.example.videobrowser.download

import android.app.DownloadManager
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DownloadStatus
import com.example.videobrowser.model.DownloadTask
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resilient, high-performance Download Engine with complete Pause, Cancel,
 * and Interruption-Resumption support via HTTP Range requests and HLS segment offsets.
 *
 * Core Guarantee:
 * When a download is paused or interrupted (network loss, timeout, app backgrounding),
 * it starts directly from where it was paused without re-downloading existing bytes.
 */
class DownloadEngine(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    companion object {
        private const val TAG = "DownloadEngine"
    }

    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val _tasks = MutableStateFlow<List<DownloadTask>>(emptyList())
    val tasks: StateFlow<List<DownloadTask>> = _tasks.asStateFlow()

    private val downloadManager: DownloadManager? by lazy {
        context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
    }

    /**
     * Enqueues a video for download.
     * @param useSystemDownloadManager whether to delegate to Android's system DownloadManager
     * or use the app's internal chunk/stream engine with live byte tracking.
     */
    fun startDownload(
        media: DetectedMedia,
        customFileName: String? = null,
        useSystemDownloadManager: Boolean = false
    ): String {
        val rawName = (customFileName?.takeIf { it.isNotBlank() } ?: media.title).trim()
        val sanitizedBaseName = rawName
            .replace(Regex("""[/\\?%*:|"<>]+"""), "_")
            .trim()
            .take(60)

        val isHls = media.isStream || media.mediaUrl.contains(".m3u8") || media.mimeType.contains("mpegurl")
        val extension = when {
            isHls -> ".ts"
            media.format.equals("WEBM", ignoreCase = true) -> ".webm"
            media.format.equals("MKV", ignoreCase = true) -> ".mkv"
            media.format.equals("3GP", ignoreCase = true) -> ".3gp"
            media.format.equals("MOV", ignoreCase = true) -> ".mov"
            else -> ".mp4"
        }

        val initialFileName = if (sanitizedBaseName.endsWith(extension, ignoreCase = true)) {
            sanitizedBaseName
        } else {
            "$sanitizedBaseName$extension"
        }

        val destDir = try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        } catch (_: Exception) {
            context.filesDir
        }
        if (!destDir.exists()) destDir.mkdirs()

        val finalFileName = resolveUniqueFileName(destDir, initialFileName)

        val taskId = UUID.randomUUID().toString()
        val newTask = DownloadTask(
            id = taskId,
            title = media.title,
            mediaUrl = media.mediaUrl,
            pageUrl = media.pageUrl,
            fileName = finalFileName,
            mimeType = if (isHls) "video/mp2t" else media.mimeType,
            format = media.format,
            quality = media.quality,
            totalBytes = media.contentLength,
            headers = media.headers,
            status = DownloadStatus.DOWNLOADING,
            filePath = File(destDir, finalFileName).absolutePath
        )

        _tasks.update { listOf(newTask) + it }

        if (useSystemDownloadManager && downloadManager != null && !isHls) {
            enqueueWithSystemDownloadManager(newTask, media)
        } else {
            val job = scope.launch {
                if (isHls) {
                    executeHlsDownload(newTask, media)
                } else {
                    executeInternalDownload(newTask, media)
                }
            }
            activeJobs[taskId] = job
        }

        return taskId
    }

    private fun resolveUniqueFileName(destDir: File, desiredFileName: String): String {
        var file = File(destDir, desiredFileName)
        if (!file.exists()) return desiredFileName
        val dotIndex = desiredFileName.lastIndexOf('.')
        val nameWithoutExt = if (dotIndex > 0) desiredFileName.substring(0, dotIndex) else desiredFileName
        val ext = if (dotIndex > 0) desiredFileName.substring(dotIndex) else ""
        var counter = 1
        while (file.exists()) {
            val candidate = "$nameWithoutExt ($counter)$ext"
            file = File(destDir, candidate)
            counter++
        }
        return file.name
    }

    private fun enqueueWithSystemDownloadManager(task: DownloadTask, media: DetectedMedia) {
        try {
            val request = DownloadManager.Request(Uri.parse(task.mediaUrl))
                .setTitle(task.fileName)
                .setDescription(task.title)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, task.fileName)

            media.headers["Referer"]?.let { request.addRequestHeader("Referer", it) }
            media.headers["User-Agent"]?.let { request.addRequestHeader("User-Agent", it) }
            media.headers["Cookie"]?.let { request.addRequestHeader("Cookie", it) }

            val systemId = downloadManager?.enqueue(request)
            _tasks.update { current ->
                current.map {
                    if (it.id == task.id) it.copy(systemDownloadId = systemId) else it
                }
            }

            // Start polling system download progress
            systemId?.let { id ->
                val job = scope.launch {
                    trackSystemDownloadProgress(task.id, id)
                }
                activeJobs[task.id] = job
            }
        } catch (e: Exception) {
            Log.e(TAG, "System DownloadManager failed: ${e.message}", e)
            _tasks.update { current ->
                current.map {
                    if (it.id == task.id) it.copy(
                        status = DownloadStatus.FAILED,
                        errorMessage = "DownloadManager error: ${e.localizedMessage ?: "Unknown"}"
                    ) else it
                }
            }
        }
    }

    private suspend fun trackSystemDownloadProgress(taskId: String, systemId: Long) {
        val dm = downloadManager ?: return
        val query = DownloadManager.Query().setFilterById(systemId)
        var running = true

        while (running && scope.isActive) {
            val currentTask = _tasks.value.find { it.id == taskId }
            if (currentTask == null || currentTask.status == DownloadStatus.CANCELLED) {
                running = false
                break
            }

            delay(1000)
            try {
                dm.query(query)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                val localUriStr = try {
                                    dm.getUriForDownloadedFile(systemId)?.toString()
                                        ?: cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                                } catch (_: Exception) {
                                    null
                                }
                                _tasks.update { current ->
                                    current.map {
                                        if (it.id == taskId) it.copy(
                                            status = DownloadStatus.COMPLETED,
                                            downloadedBytes = bytesTotal,
                                            totalBytes = bytesTotal,
                                            progress = 1.0f,
                                            speedFormatted = "",
                                            filePath = localUriStr ?: it.filePath,
                                            completedAt = System.currentTimeMillis()
                                        ) else it
                                    }
                                }
                                running = false
                            }
                            DownloadManager.STATUS_FAILED -> {
                                val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                                _tasks.update { current ->
                                    current.map {
                                        if (it.id == taskId) it.copy(
                                            status = DownloadStatus.FAILED,
                                            errorMessage = "System download failed (code $reason)"
                                        ) else it
                                    }
                                }
                                running = false
                            }
                            DownloadManager.STATUS_RUNNING -> {
                                val progress = if (bytesTotal > 0) bytesDownloaded.toFloat() / bytesTotal.toFloat() else 0f
                                _tasks.update { current ->
                                    current.map {
                                        if (it.id == taskId) it.copy(
                                            downloadedBytes = bytesDownloaded,
                                            totalBytes = bytesTotal,
                                            progress = progress,
                                            status = DownloadStatus.DOWNLOADING
                                        ) else it
                                    }
                                }
                            }
                        }
                    } else {
                        running = false
                    }
                }
            } catch (e: CancellationException) {
                running = false
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Polling system download: ${e.message}")
            }
        }
    }

    /**
     * Downloads file stream directly using OkHttpClient and writes to app-accessible storage.
     * Supports resilient HTTP Range resumption so paused or interrupted downloads continue
     * where they stopped.
     */
    private suspend fun executeInternalDownload(task: DownloadTask, media: DetectedMedia) = withContext(Dispatchers.IO) {
        val destDir = try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        } catch (_: Exception) {
            context.filesDir
        }
        if (!destDir.exists()) destDir.mkdirs()

        val destFile = File(destDir, task.fileName)
        val existingBytes = if (destFile.exists()) destFile.length() else 0L
        val isResuming = existingBytes > 0L
        val startByte = if (isResuming) existingBytes else 0L

        try {
            val requestBuilder = Request.Builder().url(task.mediaUrl)
            media.headers["Referer"]?.let { requestBuilder.header("Referer", it) }
            media.headers["User-Agent"]?.let { requestBuilder.header("User-Agent", it) }
            media.headers["Cookie"]?.let { requestBuilder.header("Cookie", it) }

            // Range header to resume from the exact byte where it paused/stopped
            if (isResuming && startByte > 0L) {
                requestBuilder.header("Range", "bytes=$startByte-")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful && response.code != 206) {
                    // Check if file is already fully downloaded
                    if (response.code == 416 && destFile.exists() && destFile.length() > 0L &&
                        task.totalBytes > 0L && destFile.length() >= task.totalBytes) {
                        completeTask(destFile, task)
                        return@withContext
                    }

                    val errorMsg = "HTTP ${response.code}: ${response.message}"
                    _tasks.update { current ->
                        current.map {
                            if (it.id == task.id) it.copy(
                                status = DownloadStatus.FAILED,
                                errorMessage = errorMsg
                            ) else it
                        }
                    }
                    return@withContext
                }

                val body = response.body ?: throw IllegalStateException("Empty response body")
                val isPartial = response.code == 206
                val appendMode = isResuming && isPartial

                val totalBytes = if (isPartial) {
                    val contentRange = response.header("Content-Range")
                    val parsedTotal = contentRange?.substringAfterLast('/')?.toLongOrNull()
                    parsedTotal ?: (startByte + body.contentLength())
                } else {
                    body.contentLength().let { if (it > 0) it else task.totalBytes }
                }

                var downloaded: Long = if (appendMode) startByte else 0L

                _tasks.update { current ->
                    current.map {
                        if (it.id == task.id) it.copy(
                            totalBytes = totalBytes,
                            downloadedBytes = downloaded,
                            filePath = destFile.absolutePath,
                            errorMessage = null
                        ) else it
                    }
                }

                val buffer = ByteArray(16384)
                var lastTime = System.currentTimeMillis()
                var lastDownloaded: Long = downloaded

                body.byteStream().use { input ->
                    FileOutputStream(destFile, appendMode).use { output ->
                        var read = input.read(buffer)
                        while (read != -1) {
                            output.write(buffer, 0, read)
                            downloaded += read

                            val now = System.currentTimeMillis()
                            if (now - lastTime >= 400) {
                                val speed = ((downloaded - lastDownloaded) / 1024.0) / ((now - lastTime) / 1000.0).coerceAtLeast(0.05)
                                val speedStr = if (speed > 1024.0) {
                                    String.format(Locale.US, "%.1f MB/s", speed / 1024.0)
                                } else {
                                    String.format(Locale.US, "%.1f KB/s", speed)
                                }
                                val progress = if (totalBytes > 0) (downloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f

                                _tasks.update { current ->
                                    current.map {
                                        if (it.id == task.id) it.copy(
                                            downloadedBytes = downloaded,
                                            progress = progress,
                                            speedFormatted = speedStr,
                                            status = DownloadStatus.DOWNLOADING
                                        ) else it
                                    }
                                }
                                lastTime = now
                                lastDownloaded = downloaded
                            }
                            read = input.read(buffer)
                        }
                        output.flush()
                    }
                }

                // Verify file integrity
                if (!destFile.exists() || destFile.length() == 0L) {
                    throw IllegalStateException("Downloaded file is empty or corrupted")
                }

                completeTask(destFile, task)
            }
        } catch (e: CancellationException) {
            val currentTask = _tasks.value.find { it.id == task.id }
            if (currentTask?.status == DownloadStatus.PAUSED) {
                // Keep partial file for resume!
                val currentLen = if (destFile.exists()) destFile.length() else task.downloadedBytes
                _tasks.update { current ->
                    current.map {
                        if (it.id == task.id) it.copy(
                            downloadedBytes = currentLen,
                            progress = if (it.totalBytes > 0) (currentLen.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) else it.progress,
                            speedFormatted = "",
                            errorMessage = "Paused at ${(currentLen / 1024.0 / 1024.0).let { String.format(Locale.US, "%.1f MB", it) }}"
                        ) else it
                    }
                }
            } else {
                destFile.delete()
                _tasks.update { current ->
                    current.map {
                        if (it.id == task.id) it.copy(
                            status = DownloadStatus.CANCELLED,
                            errorMessage = "Download cancelled"
                        ) else it
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Download interrupted: ${e.message}", e)
            val currentLen = if (destFile.exists()) destFile.length() else task.downloadedBytes
            // Interruption preservation: Preserve file and mark as PAUSED so user can resume where it paused!
            _tasks.update { current ->
                current.map {
                    if (it.id == task.id) it.copy(
                        status = DownloadStatus.PAUSED,
                        downloadedBytes = currentLen,
                        progress = if (it.totalBytes > 0) (currentLen.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) else it.progress,
                        speedFormatted = "",
                        errorMessage = "Interrupted (${e.localizedMessage ?: "Connection dropped"}). Tap Resume to continue."
                    ) else it
                }
            }
        } finally {
            activeJobs.remove(task.id)
        }
    }

    private fun completeTask(destFile: File, task: DownloadTask) {
        try {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(destFile.absolutePath),
                arrayOf(task.mimeType),
                null
            )
        } catch (_: Exception) {}

        _tasks.update { current ->
            current.map {
                if (it.id == task.id) it.copy(
                    status = DownloadStatus.COMPLETED,
                    downloadedBytes = destFile.length(),
                    totalBytes = destFile.length(),
                    progress = 1.0f,
                    speedFormatted = "",
                    completedAt = System.currentTimeMillis()
                ) else it
            }
        }
    }

    /**
     * Downloads an HLS multi-segment stream. Supports resuming from the last downloaded segment!
     */
    private suspend fun executeHlsDownload(task: DownloadTask, media: DetectedMedia) = withContext(Dispatchers.IO) {
        val destDir = try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
        } catch (_: Exception) {
            context.filesDir
        }
        if (!destDir.exists()) destDir.mkdirs()

        val destFile = File(destDir, task.fileName)

        try {
            // 1. Fetch playlist manifest
            val playlistReq = Request.Builder().url(task.mediaUrl)
            media.headers["Referer"]?.let { playlistReq.header("Referer", it) }
            media.headers["User-Agent"]?.let { playlistReq.header("User-Agent", it) }
            media.headers["Cookie"]?.let { playlistReq.header("Cookie", it) }

            val manifestText = client.newCall(playlistReq.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    throw IllegalStateException("Failed to load HLS manifest (HTTP ${resp.code})")
                }
                resp.body?.string() ?: throw IllegalStateException("Empty manifest body")
            }

            var playlist = HlsPlaylistParser.parse(manifestText, task.mediaUrl)

            if (playlist.isDrmProtected) {
                val drmMsg = playlist.drmReason ?: "Content is encrypted with DRM license (SAMPLE-AES)."
                _tasks.update { current ->
                    current.map {
                        if (it.id == task.id) it.copy(
                            status = DownloadStatus.FAILED,
                            errorMessage = drmMsg
                        ) else it
                    }
                }
                return@withContext
            }

            // If Master Playlist, select best variant and fetch its media playlist
            var activeMediaUrl = task.mediaUrl
            if (playlist.isMaster) {
                val bestVariant = playlist.variants.firstOrNull()
                    ?: throw IllegalStateException("No media variants available in HLS master playlist")
                activeMediaUrl = bestVariant.url
                val variantReq = Request.Builder().url(activeMediaUrl)
                media.headers["Referer"]?.let { variantReq.header("Referer", it) }
                media.headers["User-Agent"]?.let { variantReq.header("User-Agent", it) }
                media.headers["Cookie"]?.let { variantReq.header("Cookie", it) }

                val variantManifest = client.newCall(variantReq.build()).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        throw IllegalStateException("Failed to load variant playlist (HTTP ${resp.code})")
                    }
                    resp.body?.string() ?: throw IllegalStateException("Empty variant manifest body")
                }
                playlist = HlsPlaylistParser.parse(variantManifest, activeMediaUrl)
            }

            val segments = playlist.segments
            if (segments.isEmpty()) {
                throw IllegalStateException("No media segments found in HLS stream")
            }

            val totalSegments = segments.size
            var downloadedBytes = if (destFile.exists()) destFile.length() else 0L

            _tasks.update { current ->
                current.map {
                    if (it.id == task.id) it.copy(
                        filePath = destFile.absolutePath,
                        downloadedBytes = downloadedBytes,
                        totalSegments = totalSegments
                    ) else it
                }
            }

            // Download init segment if present and file is new
            if (playlist.initSegmentUrl != null && (!destFile.exists() || destFile.length() == 0L)) {
                val initReq = Request.Builder().url(playlist.initSegmentUrl)
                media.headers["Referer"]?.let { initReq.header("Referer", it) }
                media.headers["User-Agent"]?.let { initReq.header("User-Agent", it) }
                client.newCall(initReq.build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        resp.body?.byteStream()?.use { input ->
                            FileOutputStream(destFile, true).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }

            val buffer = ByteArray(16384)
            var lastTime = System.currentTimeMillis()
            var lastDownloaded = downloadedBytes

            // Resume directly from the segment index where it paused!
            val startSegmentIndex = task.downloadedSegments.coerceIn(0, totalSegments)

            for (index in startSegmentIndex until totalSegments) {
                val segment = segments[index]
                val currentTask = _tasks.value.find { it.id == task.id }
                if (currentTask?.status == DownloadStatus.PAUSED || currentTask?.status == DownloadStatus.CANCELLED) {
                    return@withContext
                }

                val segReq = Request.Builder().url(segment.url)
                media.headers["Referer"]?.let { segReq.header("Referer", it) }
                media.headers["User-Agent"]?.let { segReq.header("User-Agent", it) }
                media.headers["Cookie"]?.let { segReq.header("Cookie", it) }

                var success = false
                var attempts = 0
                while (!success && attempts < 3) {
                    attempts++
                    try {
                        client.newCall(segReq.build()).execute().use { segResp ->
                            if (!segResp.isSuccessful) throw IllegalStateException("Segment HTTP ${segResp.code}")
                            segResp.body?.byteStream()?.use { input ->
                                FileOutputStream(destFile, true).use { output ->
                                    var read = input.read(buffer)
                                    while (read != -1) {
                                        output.write(buffer, 0, read)
                                        downloadedBytes += read
                                        read = input.read(buffer)
                                    }
                                    output.flush()
                                }
                            }
                        }
                        success = true
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        if (attempts >= 3) throw e
                        delay(600)
                    }
                }

                val now = System.currentTimeMillis()
                if (now - lastTime >= 400 || index == totalSegments - 1) {
                    val speed = ((downloadedBytes - lastDownloaded) / 1024.0) / ((now - lastTime) / 1000.0).coerceAtLeast(0.05)
                    val speedStr = String.format(Locale.US, "%.1f KB/s", speed)
                    val progress = ((index + 1).toFloat() / totalSegments.toFloat()).coerceIn(0f, 1f)

                    _tasks.update { current ->
                        current.map {
                            if (it.id == task.id) it.copy(
                                downloadedBytes = downloadedBytes,
                                totalBytes = downloadedBytes,
                                downloadedSegments = index + 1,
                                totalSegments = totalSegments,
                                progress = progress,
                                speedFormatted = speedStr,
                                status = DownloadStatus.DOWNLOADING
                            ) else it
                        }
                    }
                    lastTime = now
                    lastDownloaded = downloadedBytes
                }
            }

            // Verify file integrity
            if (!destFile.exists() || destFile.length() == 0L) {
                throw IllegalStateException("Downloaded HLS video is empty or incomplete")
            }

            completeTask(destFile, task)
        } catch (e: CancellationException) {
            val currentTask = _tasks.value.find { it.id == task.id }
            if (currentTask?.status == DownloadStatus.PAUSED) {
                // Keep partial file
                val currentLen = if (destFile.exists()) destFile.length() else task.downloadedBytes
                _tasks.update { current ->
                    current.map {
                        if (it.id == task.id) it.copy(
                            downloadedBytes = currentLen,
                            speedFormatted = "",
                            errorMessage = "Paused at segment ${it.downloadedSegments}/${it.totalSegments}"
                        ) else it
                    }
                }
            } else {
                destFile.delete()
                _tasks.update { current ->
                    current.map {
                        if (it.id == task.id) it.copy(
                            status = DownloadStatus.CANCELLED,
                            errorMessage = "Download cancelled"
                        ) else it
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "HLS download interrupted: ${e.message}", e)
            val currentLen = if (destFile.exists()) destFile.length() else task.downloadedBytes
            _tasks.update { current ->
                current.map {
                    if (it.id == task.id) it.copy(
                        status = DownloadStatus.PAUSED, // Interruption pauses gracefully so it continues from segment offset!
                        downloadedBytes = currentLen,
                        speedFormatted = "",
                        errorMessage = "Interrupted at segment ${it.downloadedSegments}/${it.totalSegments}. Tap Resume to continue."
                    ) else it
                }
            }
        } finally {
            activeJobs.remove(task.id)
        }
    }

    /**
     * Pauses an active download, preserving downloaded bytes on disk for resumption.
     */
    fun pauseDownload(taskId: String) {
        val job = activeJobs.remove(taskId)
        val task = _tasks.value.find { it.id == taskId }
        val destDir = try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        } catch (_: Exception) {
            context.filesDir
        }
        val fileLen = if (task != null) {
            val f = File(destDir, task.fileName)
            if (f.exists()) f.length() else task.downloadedBytes
        } else 0L

        _tasks.update { current ->
            current.map {
                if (it.id == taskId && it.status == DownloadStatus.DOWNLOADING) {
                    it.copy(
                        status = DownloadStatus.PAUSED,
                        downloadedBytes = fileLen,
                        progress = if (it.totalBytes > 0) (fileLen.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) else it.progress,
                        speedFormatted = ""
                    )
                } else it
            }
        }
        job?.cancel()
    }

    /**
     * Resumes a paused or interrupted download from where it paused.
     */
    fun resumeDownload(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        if (task.status != DownloadStatus.PAUSED && task.status != DownloadStatus.FAILED) return

        val destDir = try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        } catch (_: Exception) {
            context.filesDir
        }
        val destFile = File(destDir, task.fileName)
        val existingBytes = if (destFile.exists()) destFile.length() else task.downloadedBytes

        _tasks.update { current ->
            current.map {
                if (it.id == taskId) it.copy(
                    status = DownloadStatus.DOWNLOADING,
                    downloadedBytes = existingBytes,
                    errorMessage = null,
                    speedFormatted = "Resuming..."
                ) else it
            }
        }

        val updatedTask = _tasks.value.find { it.id == taskId } ?: return
        val isHls = updatedTask.mediaUrl.contains(".m3u8") || updatedTask.mimeType.contains("mpegurl") || updatedTask.format.contains("HLS")
        val media = DetectedMedia(
            id = UUID.randomUUID().toString(),
            mediaUrl = updatedTask.mediaUrl,
            pageUrl = updatedTask.pageUrl,
            mimeType = updatedTask.mimeType,
            format = updatedTask.format,
            title = updatedTask.title,
            quality = updatedTask.quality,
            contentLength = updatedTask.totalBytes,
            headers = updatedTask.headers,
            isStream = isHls
        )

        val job = scope.launch {
            if (isHls) {
                executeHlsDownload(updatedTask, media)
            } else {
                executeInternalDownload(updatedTask, media)
            }
        }
        activeJobs[taskId] = job
    }

    /**
     * Cancels an active or pending download and cleans up resources.
     */
    fun cancelDownload(taskId: String) {
        activeJobs.remove(taskId)?.cancel()
        val task = _tasks.value.find { it.id == taskId }
        if (task?.systemDownloadId != null) {
            try {
                downloadManager?.remove(task.systemDownloadId)
            } catch (_: Exception) {}
        }

        // Delete partial file on explicit user cancellation
        task?.filePath?.let { path ->
            try {
                val f = File(path)
                if (f.exists()) f.delete()
            } catch (_: Exception) {}
        }

        _tasks.update { current ->
            current.map {
                if (it.id == taskId) {
                    it.copy(
                        status = DownloadStatus.CANCELLED,
                        speedFormatted = "",
                        errorMessage = "Download cancelled"
                    )
                } else it
            }
        }
    }

    /**
     * Retries a failed or cancelled download, keeping existing downloaded bytes if resumable!
     */
    fun retryDownload(taskId: String) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        if (task.status != DownloadStatus.FAILED && task.status != DownloadStatus.CANCELLED) return

        val destDir = try {
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        } catch (_: Exception) {
            context.filesDir
        }
        val destFile = File(destDir, task.fileName)
        val existingBytes = if (destFile.exists()) destFile.length() else 0L

        _tasks.update { current ->
            current.map {
                if (it.id == taskId) it.copy(
                    status = DownloadStatus.DOWNLOADING,
                    downloadedBytes = existingBytes,
                    progress = if (it.totalBytes > 0) (existingBytes.toFloat() / it.totalBytes.toFloat()).coerceIn(0f, 1f) else 0f,
                    errorMessage = null,
                    speedFormatted = if (existingBytes > 0L) "Resuming..." else ""
                ) else it
            }
        }

        val updatedTask = _tasks.value.find { it.id == taskId } ?: return
        val isHls = updatedTask.mediaUrl.contains(".m3u8") || updatedTask.mimeType.contains("mpegurl") || updatedTask.format.contains("HLS")
        val media = DetectedMedia(
            id = UUID.randomUUID().toString(),
            mediaUrl = updatedTask.mediaUrl,
            pageUrl = updatedTask.pageUrl,
            mimeType = updatedTask.mimeType,
            format = updatedTask.format,
            title = updatedTask.title,
            quality = updatedTask.quality,
            contentLength = updatedTask.totalBytes,
            headers = updatedTask.headers,
            isStream = isHls
        )

        val job = scope.launch {
            if (isHls) {
                executeHlsDownload(updatedTask, media)
            } else {
                executeInternalDownload(updatedTask, media)
            }
        }
        activeJobs[taskId] = job
    }

    fun pauseAll() {
        val downloadingIds = _tasks.value
            .filter { it.status == DownloadStatus.DOWNLOADING }
            .map { it.id }
        downloadingIds.forEach { pauseDownload(it) }
    }

    fun resumeAll() {
        val pausedIds = _tasks.value
            .filter { it.status == DownloadStatus.PAUSED || (it.status == DownloadStatus.FAILED && it.downloadedBytes > 0) }
            .map { it.id }
        pausedIds.forEach { resumeDownload(it) }
    }

    fun clearCompleted() {
        _tasks.update { current ->
            current.filterNot { it.status == DownloadStatus.COMPLETED || it.status == DownloadStatus.CANCELLED }
        }
    }

    fun removeTask(taskId: String) {
        cancelDownload(taskId)
        _tasks.update { current -> current.filterNot { it.id == taskId } }
    }
}
