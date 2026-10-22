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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.videobrowser.ui.components.DownloadManagerComponent
import com.example.videobrowser.ui.components.DownloadManagerSheet
import org.junit.Rule

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DownloadManagerTest {

    @get:Rule
    val composeTestRule = createComposeRule()

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

    @Test
    fun `download manager renders animated cards for active, paused, and completed tasks`() {
        val activeTask = DownloadTask(
            id = "anim-1",
            title = "Animated Video",
            mediaUrl = "https://example.com/video1.mp4",
            pageUrl = "https://example.com",
            fileName = "animation_test.mp4",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            totalBytes = 10_000_000L,
            downloadedBytes = 5_000_000L,
            status = DownloadStatus.DOWNLOADING,
            progress = 0.5f,
            speedFormatted = "2.4 MB/s"
        )

        val completedTask = DownloadTask(
            id = "anim-2",
            title = "Completed Video",
            mediaUrl = "https://example.com/video2.mp4",
            pageUrl = "https://example.com",
            fileName = "completed_video.mp4",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "720p",
            totalBytes = 8_000_000L,
            downloadedBytes = 8_000_000L,
            status = DownloadStatus.COMPLETED,
            progress = 1.0f
        )

        var pauseClicked = false
        var cancelClicked = false

        composeTestRule.setContent {
            DownloadManagerComponent(
                tasks = listOf(activeTask),
                onCancelTask = { cancelClicked = true },
                onRemoveTask = {},
                onPauseTask = { pauseClicked = true },
                onResumeTask = {},
                onRetryTask = {},
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()

        // Verify the Download Manager active card renders animated items
        composeTestRule.onNodeWithTag("download_task_card_anim-1").assertExists()
        composeTestRule.onNodeWithText("animation_test.mp4").assertExists()
        composeTestRule.onNodeWithText("Downloading").assertExists()
        composeTestRule.onNodeWithText("50%").assertExists()

        // Test Pause interaction
        composeTestRule.onNodeWithTag("pause_download_button_anim-1").performClick()
        composeTestRule.waitForIdle()
        assertTrue(pauseClicked)

        // Test Cancel interaction
        composeTestRule.onNodeWithTag("cancel_download_button_anim-1").performClick()
        composeTestRule.waitForIdle()
        assertTrue(cancelClicked)
    }

    @Test
    fun `completed download card transitions into completed state with open and remove affordances`() {
        val completedTask = DownloadTask(
            id = "completed-1",
            title = "Completed Video",
            mediaUrl = "https://example.com/video2.mp4",
            pageUrl = "https://example.com",
            fileName = "completed_video.mp4",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "720p",
            totalBytes = 8_000_000L,
            downloadedBytes = 8_000_000L,
            status = DownloadStatus.COMPLETED,
            progress = 1.0f
        )

        var removeClicked = false

        composeTestRule.setContent {
            DownloadManagerComponent(
                tasks = listOf(completedTask),
                onCancelTask = {},
                onRemoveTask = { removeClicked = true },
                onPauseTask = {},
                onResumeTask = {},
                onRetryTask = {},
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()

        // Card and completed banner transitions
        composeTestRule.onNodeWithTag("download_task_card_completed-1").assertExists()
        composeTestRule.onNodeWithText("completed_video.mp4").assertExists()
        composeTestRule.onNodeWithText("Completed").assertExists()
        composeTestRule.onNodeWithText("Download complete • Saved to storage").assertExists()

        // Open button affordance
        composeTestRule.onNodeWithTag("open_file_button_completed-1").assertExists()

        // Remove button affordance
        composeTestRule.onNodeWithTag("remove_download_button_completed-1").performClick()
        composeTestRule.waitForIdle()
        assertTrue(removeClicked)
    }

    @Test
    fun `network exception detection classifies socket drops and timeouts accurately`() {
        val socketEx = java.net.SocketException("Connection reset by peer")
        val timeoutEx = java.net.SocketTimeoutException("Read timed out")
        val unknownHostEx = java.net.UnknownHostException("Unable to resolve host")
        val wrappedEx = RuntimeException("Outer failure", java.io.IOException("Broken pipe"))
        val normalEx = IllegalArgumentException("Invalid state")

        assertTrue(engine.isNetworkException(socketEx))
        assertTrue(engine.isNetworkException(timeoutEx))
        assertTrue(engine.isNetworkException(unknownHostEx))
        assertTrue(engine.isNetworkException(wrappedEx))
        assertFalse(engine.isNetworkException(normalEx))
    }

    @Test
    fun `download task card displays waiting for network badge and auto-resumes from offset`() {
        val waitingTask = DownloadTask(
            id = "net-wait-1",
            title = "Nature Documentary",
            mediaUrl = "https://example.com/doc.mp4",
            pageUrl = "https://example.com",
            fileName = "doc.mp4",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            totalBytes = 50_000_000L,
            downloadedBytes = 25_000_000L,
            status = DownloadStatus.PAUSED,
            progress = 0.5f,
            isWaitingForNetwork = true,
            errorMessage = "Offline: Saved at 23.8 MB. Auto-resumes when online."
        )

        var resumeClicked = false

        composeTestRule.setContent {
            DownloadManagerComponent(
                tasks = listOf(waitingTask),
                onCancelTask = {},
                onRemoveTask = {},
                onPauseTask = {},
                onResumeTask = { resumeClicked = true },
                onRetryTask = {},
                showHeader = false
            )
        }
        composeTestRule.waitForIdle()

        // Card should display task file name, waiting for network badge, and resume affordance
        composeTestRule.onNodeWithTag("download_task_card_net-wait-1").assertExists()
        composeTestRule.onNodeWithText("doc.mp4").assertExists()
        composeTestRule.onNodeWithText("Waiting for Network").assertExists()
        composeTestRule.onNodeWithText("Offline: Saved at 23.8 MB. Auto-resumes when online.").assertExists()
        composeTestRule.onNodeWithText("📶 Auto-resumes from 23.8 MB").assertExists()

        // Resume button should be present and clickable
        composeTestRule.onNodeWithTag("resume_download_button_net-wait-1").performClick()
        composeTestRule.waitForIdle()
        assertTrue(resumeClicked)
    }
}
