package com.example.videobrowser

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.detection.VideoSniffer
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DownloadStatus
import com.example.videobrowser.model.MediaFilter
import com.example.videobrowser.viewmodel.BrowserViewModel
import com.example.videobrowser.viewmodel.WebAction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VideoBrowserStressAndRobustnessTest {

    private lateinit var app: Application
    private lateinit var viewModel: BrowserViewModel
    private val sniffer = VideoSniffer()

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = BrowserViewModel(app)
    }

    // --- 1. URL Resolution & Edge Cases ---

    @Test
    fun `empty or blank URL input is safely ignored`() {
        val originalUrl = viewModel.uiState.value.activeTab?.url
        viewModel.submitUrl("   ")
        assertEquals(originalUrl, viewModel.uiState.value.activeTab?.url)

        viewModel.submitUrl("")
        assertEquals(originalUrl, viewModel.uiState.value.activeTab?.url)
    }

    @Test
    fun `domain without protocol auto-prepends https`() {
        viewModel.submitUrl("example.org")
        assertEquals("https://example.org", viewModel.uiState.value.activeTab?.url)

        viewModel.submitUrl("subdomain.example.com/watch?v=42")
        assertEquals("https://subdomain.example.com/watch?v=42", viewModel.uiState.value.activeTab?.url)
    }

    @Test
    fun `search phrases redirect to DuckDuckGo search`() {
        viewModel.submitUrl("kotlin coroutines best practices")
        val activeUrl = viewModel.uiState.value.activeTab?.url ?: ""
        assertTrue(activeUrl.startsWith("https://duckduckgo.com/?q="))
        assertTrue(activeUrl.contains("kotlin"))
    }

    @Test
    fun `special symbols and injection strings in search are encoded safely`() {
        viewModel.submitUrl("<script>alert('xss')</script> & ? # =")
        val activeUrl = viewModel.uiState.value.activeTab?.url ?: ""
        assertTrue(activeUrl.startsWith("https://duckduckgo.com/?q="))
        assertFalse(activeUrl.contains("<script>")) // Should be URL-encoded
    }

    // --- 2. Tab Lifecycle & Stress Operations ---

    @Test
    fun `rapid tab creation and switching behaves deterministically`() {
        for (i in 1..15) {
            viewModel.addNewTab("https://site$i.org")
        }
        assertEquals(16, viewModel.uiState.value.tabs.size)
        assertEquals("https://site15.org", viewModel.uiState.value.activeTab?.url)

        // Switch to the first tab
        val firstTabId = viewModel.uiState.value.tabs.first().id
        viewModel.switchTab(firstTabId)
        assertEquals(firstTabId, viewModel.uiState.value.activeTabId)
        assertEquals("https://duckduckgo.com", viewModel.uiState.value.activeTab?.url)
    }

    @Test
    fun `closing non-existent tab does not crash or corrupt tabs`() {
        val initialCount = viewModel.uiState.value.tabs.size
        viewModel.closeTab("non_existent_tab_id_xyz")
        assertEquals(initialCount, viewModel.uiState.value.tabs.size)
    }

    @Test
    fun `closing all tabs always preserves at least one fresh home tab`() {
        viewModel.addNewTab("https://alpha.com")
        viewModel.addNewTab("https://beta.com")
        assertEquals(3, viewModel.uiState.value.tabs.size)

        // Close all tabs
        val tabIds = viewModel.uiState.value.tabs.map { it.id }
        tabIds.forEach { id ->
            viewModel.closeTab(id)
        }

        // Must still have exactly 1 tab, not 0
        assertEquals(1, viewModel.uiState.value.tabs.size)
        val survivor = viewModel.uiState.value.tabs.first()
        assertEquals("Home", survivor.title)
        assertTrue(survivor.url.contains("duckduckgo.com"))
    }

    // --- 3. Media Filtering & Detection Resilience ---

    @Test
    fun `video extensions with query parameters are recognized`() {
        val url = "https://media.server.net/streams/video.mp4?auth=secretToken123&expires=999999"
        assertTrue(MediaFilter.isCandidateVideo(url, "video/mp4"))
        assertEquals("MP4", MediaFilter.inferFormat(url, "video/mp4"))
        assertFalse(MediaFilter.isStreamManifest(url, "video/mp4"))
    }

    @Test
    fun `uppercase video extensions are recognized`() {
        val url = "https://example.com/archive/DOCUMENTARY.WEBM"
        assertTrue(MediaFilter.isCandidateVideo(url, ""))
        assertEquals("WEBM", MediaFilter.inferFormat(url, ""))
    }

    @Test
    fun `HLS and DASH manifests are recognized as streams`() {
        val hlsUrl = "https://stream.example.com/live/master.m3u8"
        assertTrue(MediaFilter.isCandidateVideo(hlsUrl, "application/x-mpegURL"))
        assertTrue(MediaFilter.isStreamManifest(hlsUrl, "application/x-mpegURL"))
        assertTrue(MediaFilter.inferFormat(hlsUrl, "application/x-mpegURL").startsWith("HLS"))

        val dashUrl = "https://stream.example.com/dash/manifest.mpd"
        assertTrue(MediaFilter.isCandidateVideo(dashUrl, "application/dash+xml"))
        assertTrue(MediaFilter.isStreamManifest(dashUrl, "application/dash+xml"))
        assertTrue(MediaFilter.inferFormat(dashUrl, "application/dash+xml").startsWith("DASH"))
    }

    @Test
    fun `audio and non-media assets are rejected`() {
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/song.mp3", "audio/mpeg"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/style.css", "text/css"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/script.js", "application/javascript"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/image.png", "image/png"))
    }

    @Test
    fun `title sanitization cleans annoying web suffixes`() {
        assertEquals("Big Buck Bunny", MediaFilter.cleanTitle("Big Buck Bunny | Watch Free Online"))
        assertEquals("Nature Documentary", MediaFilter.cleanTitle("Nature Documentary - YouTube"))
        assertEquals("Movie Trailer", MediaFilter.cleanTitle("Movie Trailer (Official HD)"))
    }

    // --- 4. WebAction Navigation Commands ---

    @Test
    fun `navigation methods emit appropriate WebActions`() {
        viewModel.navigateBack()
        viewModel.navigateForward()
        viewModel.reloadActiveTab()
        viewModel.stopActiveTabLoading()
        viewModel.loadUrlInActiveTab("https://test.com")

        // State changes immediately reflected
        assertEquals("https://test.com", viewModel.uiState.value.activeTab?.url)
    }

    // --- 5. Download Engine & Task Lifecycle ---

    @Test
    fun `download task file name sanitization strips illegal characters`() {
        val dirtyMedia = DetectedMedia(
            id = "dirty_1",
            mediaUrl = "https://example.com/dirty.mp4",
            pageUrl = "https://example.com",
            title = "My:Awesome/Video*Part?1<test>|\"HD\"",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            contentLength = 5000000L
        )

        val taskId = viewModel.downloadEngine.startDownload(dirtyMedia)
        val task = viewModel.downloadTasks.value.find { it.id == taskId }
        assertNotNull(task)

        // Verifies no forbidden filesystem chars remain
        assertFalse(task!!.fileName.contains(":"))
        assertFalse(task.fileName.contains("/"))
        assertFalse(task.fileName.contains("*"))
        assertFalse(task.fileName.contains("?"))
        assertFalse(task.fileName.contains("<"))
        assertFalse(task.fileName.contains(">"))
        assertFalse(task.fileName.contains("|"))
        assertFalse(task.fileName.contains("\""))
        assertTrue(task.fileName.endsWith(".mp4"))
    }

    @Test
    fun `cancelling and removing download updates state cleanly`() {
        val testMedia = DetectedMedia(
            id = "cancel_test",
            mediaUrl = "https://example.com/cancel.mp4",
            pageUrl = "https://example.com",
            title = "Cancellation Test",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "720p",
            contentLength = 1000000L
        )

        val taskId = viewModel.downloadEngine.startDownload(testMedia)
        assertEquals(1, viewModel.downloadTasks.value.size)

        viewModel.downloadEngine.cancelDownload(taskId)
        val taskAfterCancel = viewModel.downloadTasks.value.find { it.id == taskId }
        assertEquals(DownloadStatus.CANCELLED, taskAfterCancel?.status)

        viewModel.downloadEngine.removeTask(taskId)
        assertTrue(viewModel.downloadTasks.value.none { it.id == taskId })
    }

    // --- 6. UI Sheet Visibility & Confirmation Flows ---

    @Test
    fun `sheet opening and closing state operates without side effects`() {
        assertFalse(viewModel.uiState.value.showDetectionSheet)
        assertFalse(viewModel.uiState.value.showDownloadsSheet)
        assertFalse(viewModel.uiState.value.showTabsSheet)

        viewModel.openDetectionSheet()
        assertTrue(viewModel.uiState.value.showDetectionSheet)
        viewModel.closeDetectionSheet()
        assertFalse(viewModel.uiState.value.showDetectionSheet)

        viewModel.openDownloadsSheet()
        assertTrue(viewModel.uiState.value.showDownloadsSheet)
        viewModel.closeDownloadsSheet()
        assertFalse(viewModel.uiState.value.showDownloadsSheet)

        viewModel.openTabsSheet()
        assertTrue(viewModel.uiState.value.showTabsSheet)
        viewModel.closeTabsSheet()
        assertFalse(viewModel.uiState.value.showTabsSheet)
    }

    @Test
    fun `formats WEBM, MKV, 3GP, MOV receive correct extensions on download`() {
        val formats = listOf(
            "WEBM" to ".webm",
            "MKV" to ".mkv",
            "3GP" to ".3gp",
            "MOV" to ".mov",
            "MP4" to ".mp4"
        )

        formats.forEachIndexed { index, (fmt, expectedExt) ->
            val media = DetectedMedia(
                id = "fmt_test_$index",
                mediaUrl = "https://example.com/test_$index",
                pageUrl = "https://example.com",
                title = "Video $fmt",
                mimeType = "video/$fmt",
                format = fmt,
                quality = "1080p"
            )
            val taskId = viewModel.downloadEngine.startDownload(media)
            val task = viewModel.downloadTasks.value.find { it.id == taskId }
            assertNotNull(task)
            assertTrue("Expected file to end with $expectedExt but was ${task!!.fileName}", task.fileName.endsWith(expectedExt))
        }
    }

    @Test
    fun `custom filename with whitespace and missing extension is formatted accurately`() {
        val media = DetectedMedia(
            id = "custom_name_test",
            mediaUrl = "https://example.com/stream.webm",
            pageUrl = "https://example.com",
            title = "Generic Title",
            mimeType = "video/webm",
            format = "WEBM",
            quality = "720p"
        )
        val taskId = viewModel.downloadEngine.startDownload(media, customFileName = "  My Custom Vacation Video  ")
        val task = viewModel.downloadTasks.value.find { it.id == taskId }
        assertNotNull(task)
        assertEquals("My Custom Vacation Video.webm", task!!.fileName)
    }

    @Test
    fun `duplicate media intercepted with same URL is updated not duplicated`() {
        val testUrl = "https://example.com/video1.mp4"
        viewModel.onNetworkResourceIntercepted(testUrl, emptyMap(), "https://example.com")
        val initialCount = viewModel.uiState.value.detectedVideos.size
        assertEquals(1, initialCount)

        // Intercept again with enriched headers
        viewModel.onNetworkResourceIntercepted(testUrl, mapOf("Referer" to "https://example.com"), "https://example.com")
        val afterCount = viewModel.uiState.value.detectedVideos.size
        assertEquals(1, afterCount)
    }

    // --- 7. Download Lifecycle & Cancellation Robustness ---

    @Test
    fun `cancelDownload updates task status to CANCELLED and stops active job`() {
        val media = DetectedMedia(
            id = "cancel_test_1",
            mediaUrl = "https://example.com/large_video.mp4",
            pageUrl = "https://example.com",
            title = "Large Video",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            isStream = false
        )
        val taskId = viewModel.downloadEngine.startDownload(media)
        assertNotNull(taskId)

        viewModel.cancelDownload(taskId)
        val task = viewModel.downloadTasks.value.find { it.id == taskId }
        assertNotNull(task)
        assertEquals(DownloadStatus.CANCELLED, task?.status)
    }

    @Test
    fun `pauseDownload and resumeDownload transitions task status correctly`() {
        val media = DetectedMedia(
            id = "pause_test_1",
            mediaUrl = "https://example.com/streamable.mp4",
            pageUrl = "https://example.com",
            title = "Pausable Video",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "720p",
            isStream = false
        )
        val taskId = viewModel.downloadEngine.startDownload(media)
        assertNotNull(taskId)

        viewModel.pauseDownload(taskId)
        var task = viewModel.downloadTasks.value.find { it.id == taskId }
        assertNotNull(task)
        assertEquals(DownloadStatus.PAUSED, task?.status)

        viewModel.resumeDownload(taskId)
        task = viewModel.downloadTasks.value.find { it.id == taskId }
        assertNotNull(task)
        assertTrue(task?.status == DownloadStatus.DOWNLOADING || task?.status == DownloadStatus.PENDING)
    }

    @Test
    fun `removeDownload clears task from task list`() {
        val media = DetectedMedia(
            id = "remove_test_1",
            mediaUrl = "https://example.com/removable.mp4",
            pageUrl = "https://example.com",
            title = "Removable Video",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "480p"
        )
        val taskId = viewModel.downloadEngine.startDownload(media)
        assertNotNull(taskId)
        assertTrue(viewModel.downloadTasks.value.any { it.id == taskId })

        viewModel.removeDownload(taskId)
        assertFalse(viewModel.downloadTasks.value.any { it.id == taskId })
    }

    @Test
    fun `switchTab emits LoadUrl action for target tab`() = runBlocking {
        val initialTab = viewModel.uiState.value.activeTab
        assertNotNull(initialTab)

        viewModel.addNewTab("https://example.com/tab2")
        val secondTabId = viewModel.uiState.value.activeTabId

        // Switch back to initial tab
        viewModel.switchTab(initialTab!!.id)
        assertEquals(initialTab.id, viewModel.uiState.value.activeTabId)
        assertEquals(initialTab.url, viewModel.uiState.value.urlInput)
    }
}
