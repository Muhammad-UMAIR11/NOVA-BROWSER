package com.example.videobrowser

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.detection.VideoSniffer
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DetectionSource
import com.example.videobrowser.model.DownloadStatus
import com.example.videobrowser.model.MediaFilter
import com.example.videobrowser.viewmodel.BrowserViewModel
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
@Config(sdk = [36])
class VideoDetectionTest {

    private lateinit var application: Application
    private lateinit var viewModel: BrowserViewModel
    private val sniffer = VideoSniffer()

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = BrowserViewModel(application)
    }

    // --- 1. MediaFilter & Ad/Tracker Filtering Tests ---

    @Test
    fun `isCandidateVideo accepts valid video URLs and MIME types`() {
        assertTrue(MediaFilter.isCandidateVideo("https://example.com/videos/movie.mp4"))
        assertTrue(MediaFilter.isCandidateVideo("https://example.com/stream.webm"))
        assertTrue(MediaFilter.isCandidateVideo("https://example.com/playlist.m3u8"))
        assertTrue(MediaFilter.isCandidateVideo("https://example.com/media/manifest.mpd"))
        assertTrue(MediaFilter.isCandidateVideo("https://example.com/dynamic-video?format=mp4"))
        assertTrue(MediaFilter.isCandidateVideo("https://example.com/get_stream?mime=video%2Fmp4"))
        assertTrue(MediaFilter.isCandidateVideo("https://example.com/raw-stream", mimeType = "video/mp4"))
    }

    @Test
    fun `isCandidateVideo rejects advertisements, tracking pixels, and non-video assets`() {
        // Ads and tracking domains
        assertFalse(MediaFilter.isCandidateVideo("https://securepubads.g.doubleclick.net/gampad/ads?video=1"))
        assertFalse(MediaFilter.isCandidateVideo("https://pagead2.googlesyndication.com/pagead/ads?client=ca-video"))
        assertFalse(MediaFilter.isCandidateVideo("https://ib.adnxs.com/seg?add=1"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/ad_break/vast.xml"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/telemetry/beacon.mp4"))

        // Static non-video resources
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/image.jpg"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/thumb.png"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/script.js"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/styles.css"))
        assertFalse(MediaFilter.isCandidateVideo("https://example.com/font.woff2"))
    }

    @Test
    fun `inferFormat correctly categorizes media formats`() {
        assertEquals("MP4", MediaFilter.inferFormat("https://example.com/video.mp4", "video/mp4"))
        assertEquals("WEBM", MediaFilter.inferFormat("https://example.com/video.webm", "video/webm"))
        assertEquals("HLS (m3u8)", MediaFilter.inferFormat("https://example.com/stream.m3u8", "application/x-mpegURL"))
        assertEquals("DASH (mpd)", MediaFilter.inferFormat("https://example.com/stream.mpd", "application/dash+xml"))
    }

    @Test
    fun `isStreamManifest identifies adaptive manifests`() {
        assertTrue(MediaFilter.isStreamManifest("https://example.com/stream.m3u8", null))
        assertTrue(MediaFilter.isStreamManifest("https://example.com/stream.mpd", null))
        assertFalse(MediaFilter.isStreamManifest("https://example.com/video.mp4", "video/mp4"))
    }

    // --- 2. URL and Search Query Resolution ---

    @Test
    fun `submitUrl resolves valid domains and converts searches to DuckDuckGo query`() {
        viewModel.submitUrl("https://example.com/watch")
        assertEquals("https://example.com/watch", viewModel.uiState.value.urlInput)

        viewModel.submitUrl("example.org")
        assertEquals("https://example.org", viewModel.uiState.value.urlInput)

        viewModel.submitUrl("cats playing piano")
        assertTrue(viewModel.uiState.value.urlInput.startsWith("https://duckduckgo.com/?q="))
    }

    // --- 3. Browser Tab Management ---

    @Test
    fun `tabs can be added, switched, and closed`() {
        val initialTabsCount = viewModel.uiState.value.tabs.size
        assertEquals(1, initialTabsCount)

        viewModel.addNewTab("https://example.com")
        assertEquals(2, viewModel.uiState.value.tabs.size)
        assertEquals("https://example.com", viewModel.uiState.value.activeTab?.url)

        val firstTabId = viewModel.uiState.value.tabs.first().id
        viewModel.switchTab(firstTabId)
        assertEquals(firstTabId, viewModel.uiState.value.activeTabId)

        // Close a tab
        val tabToClose = viewModel.uiState.value.tabs.last().id
        viewModel.closeTab(tabToClose)
        assertEquals(1, viewModel.uiState.value.tabs.size)
    }

    // --- 4. Video Sniffing & Detection State ---

    @Test
    fun `DOM video detection updates detected list and activates floating orb`() {
        val tab = viewModel.uiState.value.activeTab
        assertNotNull(tab)
        assertFalse(viewModel.uiState.value.isOrbVisible)

        // Simulate DOM video detection
        viewModel.onDomVideoFound(
            mediaUrl = "https://example.com/stream/sample.mp4",
            pageUrl = "https://example.com/watch?v=123",
            title = "Sample Bunny Video",
            mimeType = "video/mp4",
            width = 1920,
            height = 1080,
            duration = 60.0,
            isPlaying = true
        )

        val videos = viewModel.uiState.value.detectedVideos
        assertEquals(1, videos.size)
        assertTrue(viewModel.uiState.value.isOrbVisible)

        val first = videos.first()
        assertEquals("Sample Bunny Video", first.title)
        assertEquals("1080p Full HD", first.displayQuality)
        assertTrue(first.isPlaying)
        assertTrue(first.isDownloadable)
    }

    @Test
    fun `playback state changes properly update active playing media`() {
        val mediaUrl = "https://example.com/stream/action.mp4"
        viewModel.onDomVideoFound(
            mediaUrl = mediaUrl,
            pageUrl = "https://example.com/watch",
            title = "Action Trailer",
            mimeType = "video/mp4",
            width = 1280,
            height = 720,
            duration = 120.0,
            isPlaying = false
        )

        assertNull(viewModel.uiState.value.activePlayingMedia)

        // User starts playback
        viewModel.onVideoPlaybackChanged(
            mediaUrl = mediaUrl,
            isPlaying = true,
            currentTime = 5.0,
            duration = 120.0
        )

        assertNotNull(viewModel.uiState.value.activePlayingMedia)
        assertEquals(mediaUrl, viewModel.uiState.value.activePlayingMedia?.mediaUrl)
        assertEquals("720p HD", viewModel.uiState.value.activePlayingMedia?.displayQuality)

        // User pauses
        viewModel.onVideoPlaybackChanged(
            mediaUrl = mediaUrl,
            isPlaying = false,
            currentTime = 20.0,
            duration = 120.0
        )

        assertNull(viewModel.uiState.value.activePlayingMedia)
    }

    @Test
    fun `duplicate streams are updated without bloating list`() {
        val mediaUrl = "https://example.com/sample.mp4"
        viewModel.onDomVideoFound(
            mediaUrl = mediaUrl,
            pageUrl = "https://example.com",
            title = "Video 1",
            mimeType = "video/mp4",
            width = 640,
            height = 360,
            duration = 30.0,
            isPlaying = false
        )
        // Same URL discovered via network interception
        viewModel.onNetworkResourceIntercepted(
            requestUrl = mediaUrl,
            headers = mapOf("Referer" to "https://example.com"),
            pageUrl = "https://example.com"
        )

        assertEquals(1, viewModel.uiState.value.detectedVideos.size)
    }

    // --- 5. Download Confirmation & Engine Dispatch ---

    @Test
    fun `media selection opens confirmation and enqueues real download`() {
        val testMedia = DetectedMedia(
            id = "test_media_1",
            mediaUrl = "https://example.com/test.mp4",
            pageUrl = "https://example.com",
            title = "Test Episode 1",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            contentLength = 15728640L // 15 MB
        )

        viewModel.selectMediaForConfirmation(testMedia)

        assertNotNull(viewModel.uiState.value.selectedMediaForConfirmation)
        assertEquals("15.0 MB", testMedia.formattedSize)

        // Confirm download
        viewModel.confirmDownload()

        assertNull(viewModel.uiState.value.selectedMediaForConfirmation)
        val tasks = viewModel.downloadTasks.value
        assertEquals(1, tasks.size)

        val task = tasks.first()
        assertEquals("Test Episode 1", task.title)
        assertTrue(task.fileName.endsWith(".mp4"))
        assertEquals(DownloadStatus.DOWNLOADING, task.status)
    }

    // --- 6. Network Error & Reset on Page Navigation ---

    @Test
    fun `navigating to a new page clears detection state of previous page`() {
        viewModel.onDomVideoFound(
            mediaUrl = "https://example.com/clip.mp4",
            pageUrl = "https://example.com/p1",
            title = "Clip",
            mimeType = "video/mp4",
            width = 1280,
            height = 720,
            duration = 10.0,
            isPlaying = true
        )
        assertEquals(1, viewModel.uiState.value.detectedVideos.size)

        // User navigates to a new page
        viewModel.loadUrlInActiveTab("https://example.com/p2")
        assertEquals(0, viewModel.uiState.value.detectedVideos.size)
        assertFalse(viewModel.uiState.value.isOrbVisible)
    }
}
