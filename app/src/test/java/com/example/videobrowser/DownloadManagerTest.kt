package com.example.videobrowser

import android.app.Application
import android.os.Environment
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.download.DownloadEngine
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DownloadStatus
import com.example.videobrowser.model.DownloadTask
import com.example.videobrowser.viewmodel.BrowserViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadManagerTest {

    private lateinit var app: Application
    private lateinit var engine: DownloadEngine
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        engine = DownloadEngine(app)
        viewModel = BrowserViewModel(app)
    }

    @Test
    fun `download task model accurately computes progress and resumption properties`() {
        val task = DownloadTask(
            id = "test-1",
            title = "Test Video",
            mediaUrl = "https://example.com/video.mp4",
            pageUrl = "https://example.com",
            fileName = "video.mp4",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            totalBytes = 20_000_000L,
            downloadedBytes = 10_000_000L,
            status = DownloadStatus.PAUSED,
            progress = 0.5f
        )

        assertEquals("50%", task.formattedProgress)
        assertTrue(task.isPartiallyDownloaded)
        assertTrue(task.canResume)
        assertTrue(task.formattedDownloadedSize.contains("9.5 MB / 19.1 MB"))
        assertTrue(task.formattedResumableOffset.contains("9.5 MB"))
    }

    @Test
    fun `startDownload enqueues active task and tracks in tasks flow`() {
        val media = DetectedMedia(
            id = "media-1",
            mediaUrl = "https://example.com/stream.mp4",
            pageUrl = "https://example.com",
            mimeType = "video/mp4",
            format = "MP4",
            title = "Sample Video Stream",
            quality = "720p",
            contentLength = 15_000_000L
        )

        val taskId = engine.startDownload(media, customFileName = "MySample")
        val tasks = engine.tasks.value

        assertEquals(1, tasks.size)
        val task = tasks.first()
        assertEquals(taskId, task.id)
        assertEquals("MySample.mp4", task.fileName)
        assertEquals(DownloadStatus.DOWNLOADING, task.status)
        assertEquals(15_000_000L, task.totalBytes)
    }

    @Test
    fun `pauseDownload transitions active task to PAUSED without deleting partial progress`() {
        val media = DetectedMedia(
            id = "media-2",
            mediaUrl = "https://example.com/sample.mp4",
            pageUrl = "https://example.com",
            mimeType = "video/mp4",
            format = "MP4",
            title = "Sample Video",
            quality = "1080p",
            contentLength = 25_000_000L
        )

        val taskId = engine.startDownload(media, customFileName = "PausableVideo")
        engine.pauseDownload(taskId)

        val task = engine.tasks.value.find { it.id == taskId }
        assertNotNull(task)
        assertEquals(DownloadStatus.PAUSED, task?.status)
    }

    @Test
    fun `cancelDownload marks task as CANCELLED`() {
        val media = DetectedMedia(
            id = "media-3",
            mediaUrl = "https://example.com/cancel.mp4",
            pageUrl = "https://example.com",
            mimeType = "video/mp4",
            format = "MP4",
            title = "Cancelable Video",
            quality = "480p"
        )

        val taskId = engine.startDownload(media, customFileName = "ToCancel")
        engine.cancelDownload(taskId)

        val task = engine.tasks.value.find { it.id == taskId }
        assertNotNull(task)
        assertEquals(DownloadStatus.CANCELLED, task?.status)
    }

    @Test
    fun `interruption preserves downloaded bytes and allows resuming where it paused`() {
        val media = DetectedMedia(
            id = "media-4",
            mediaUrl = "https://example.com/interrupted.mp4",
            pageUrl = "https://example.com",
            mimeType = "video/mp4",
            format = "MP4",
            title = "Interrupted Video",
            quality = "1080p",
            contentLength = 2_000_000L
        )

        val taskId = engine.startDownload(media, customFileName = "InterruptedVideo")
        val task = engine.tasks.value.find { it.id == taskId }
        assertNotNull(task)

        val destFile = File(task!!.filePath!!)
        destFile.writeBytes(ByteArray(500_000))

        engine.pauseDownload(taskId)

        val pausedTask = engine.tasks.value.find { it.id == taskId }
        assertEquals(DownloadStatus.PAUSED, pausedTask?.status)
        assertTrue(destFile.exists())
        assertEquals(500_000L, destFile.length())
        assertEquals(500_000L, pausedTask?.downloadedBytes)

        // Resume starts where it paused!
        engine.resumeDownload(taskId)
        val resumedTask = engine.tasks.value.find { it.id == taskId }
        assertEquals(DownloadStatus.DOWNLOADING, resumedTask?.status)
        assertEquals(500_000L, resumedTask?.downloadedBytes)
    }

    @Test
    fun `batch pauseAll and resumeAll properly transition all active downloads`() {
        val mediaA = DetectedMedia(
            id = "m-a",
            mediaUrl = "https://example.com/a.mp4",
            pageUrl = "https://example.com",
            mimeType = "video/mp4",
            format = "MP4",
            title = "Video A",
            quality = "720p"
        )
        val mediaB = DetectedMedia(
            id = "m-b",
            mediaUrl = "https://example.com/b.mp4",
            pageUrl = "https://example.com",
            mimeType = "video/mp4",
            format = "MP4",
            title = "Video B",
            quality = "1080p"
        )

        val idA = engine.startDownload(mediaA, customFileName = "BatchA")
        val idB = engine.startDownload(mediaB, customFileName = "BatchB")

        assertEquals(2, engine.tasks.value.count { it.status == DownloadStatus.DOWNLOADING })

        engine.pauseAll()
        assertEquals(2, engine.tasks.value.count { it.status == DownloadStatus.PAUSED })

        engine.resumeAll()
        assertEquals(2, engine.tasks.value.count { it.status == DownloadStatus.DOWNLOADING })
    }

    @Test
    fun `clearCompleted removes cancelled and finished tasks while keeping active ones`() {
        val media = DetectedMedia(
            id = "m-c",
            mediaUrl = "https://example.com/c.mp4",
            pageUrl = "https://example.com",
            mimeType = "video/mp4",
            format = "MP4",
            title = "Video C",
            quality = "720p"
        )

        val taskId = engine.startDownload(media, customFileName = "ToClear")
        engine.cancelDownload(taskId)
        assertEquals(1, engine.tasks.value.size)

        engine.clearCompleted()
        assertEquals(0, engine.tasks.value.size)
    }

    @Test
    fun `viewModel exposes batch download actions and status notifications`() {
        viewModel.pauseAllDownloads()
        assertEquals("All downloads paused", viewModel.uiState.value.statusNotice)

        viewModel.resumeAllDownloads()
        assertEquals("All downloads resumed", viewModel.uiState.value.statusNotice)
    }
}
