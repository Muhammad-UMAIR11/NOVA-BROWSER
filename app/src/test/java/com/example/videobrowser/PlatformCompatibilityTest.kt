package com.example.videobrowser

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.detection.PlatformCompatibilityEngine
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DownloadStatus
import com.example.videobrowser.model.MediaCompatibilityCategory
import com.example.videobrowser.model.MediaFilter
import com.example.videobrowser.viewmodel.BrowserViewModel
import com.example.videobrowser.viewmodel.OrbStatus
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
class PlatformCompatibilityTest {

    private lateinit var app: Application
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = BrowserViewModel(app)
    }

    @Test
    fun `direct MP4 stream is categorized as DIRECT_DOWNLOADABLE`() {
        val result = PlatformCompatibilityEngine.evaluate(
            mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            pageUrl = "https://example.com/videos",
            mimeType = "video/mp4",
            isStream = false,
            isDrmReported = false
        )

        assertEquals(MediaCompatibilityCategory.DIRECT_DOWNLOADABLE, result.category)
        assertTrue(result.isDownloadable)
        assertEquals(null, result.restrictionReason)
    }

    @Test
    fun `HLS m3u8 stream is categorized as ADAPTIVE_STREAM and is downloadable`() {
        val result = PlatformCompatibilityEngine.evaluate(
            mediaUrl = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8",
            pageUrl = "https://example.com/live",
            mimeType = "application/x-mpegURL",
            isStream = true,
            isDrmReported = false
        )

        assertEquals(MediaCompatibilityCategory.ADAPTIVE_STREAM, result.category)
        assertTrue(result.isDownloadable)
        assertNull(result.restrictionReason)
    }

    @Test
    fun `DASH mpd stream is categorized as ADAPTIVE_STREAM and requires demuxing pipeline`() {
        val result = PlatformCompatibilityEngine.evaluate(
            mediaUrl = "https://example.com/dash/manifest.mpd",
            pageUrl = "https://example.com/watch",
            mimeType = "application/dash+xml",
            isStream = true,
            isDrmReported = false
        )

        assertEquals(MediaCompatibilityCategory.ADAPTIVE_STREAM, result.category)
        assertFalse(result.isDownloadable)
        assertNotNull(result.restrictionReason)
        assertTrue(result.restrictionReason!!.contains("DASH adaptive stream"))
    }

    @Test
    fun `DRM reported media is categorized as RESTRICTED_OR_PROTECTED`() {
        val result = PlatformCompatibilityEngine.evaluate(
            mediaUrl = "https://stream.netflix.com/playback/manifest.mpd",
            pageUrl = "https://netflix.com/watch/12345",
            mimeType = "application/dash+xml",
            isStream = true,
            isDrmReported = true
        )

        assertEquals(MediaCompatibilityCategory.RESTRICTED_OR_PROTECTED, result.category)
        assertFalse(result.isDownloadable)
        assertNotNull(result.restrictionReason)
        assertTrue(result.restrictionReason!!.contains("Digital Rights Management (DRM"))
    }

    @Test
    fun `YouTube page is categorized as RESTRICTED_OR_PROTECTED with clear reason`() {
        val result = PlatformCompatibilityEngine.evaluate(
            mediaUrl = "https://rr3---sn-4g5ednle.googlevideo.com/videoplayback?expire=12345&sig=abc",
            pageUrl = "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            mimeType = "video/webm",
            isStream = false,
            isDrmReported = false
        )

        assertEquals(MediaCompatibilityCategory.RESTRICTED_OR_PROTECTED, result.category)
        assertFalse(result.isDownloadable)
        assertNotNull(result.restrictionReason)
        assertTrue(result.restrictionReason!!.contains("YouTube"))
    }

    @Test
    fun `social media stickers and avatars are rejected by MediaFilter`() {
        assertFalse(MediaFilter.isCandidateVideo("https://static.xx.fbcdn.net/rsrc.php/v3/y1/r/sticker_123.png"))
        assertFalse(MediaFilter.isCandidateVideo("https://instagram.com/static/emoji/heart.png"))
        assertFalse(MediaFilter.isCandidateVideo("https://tiktokcdn.com/avatar/user123.jpg"))
        assertFalse(MediaFilter.isCandidateVideo("https://abs.twimg.com/emoji/v2/svg/1f600.svg"))
    }

    @Test
    fun `orbStatus reflects detection state accurately`() {
        // Initial: No videos found -> OrbStatus.HIDDEN
        assertEquals(OrbStatus.HIDDEN, viewModel.uiState.value.orbStatus)
        assertFalse(viewModel.uiState.value.isOrbVisible)

        // Add 1 downloadable video -> OrbStatus.SUPPORTED_FOUND
        viewModel.onDomVideoFound(
            mediaUrl = "https://example.com/video.mp4",
            pageUrl = "https://example.com",
            title = "Sample Video",
            mimeType = "video/mp4",
            width = 1920,
            height = 1080,
            duration = 60.0,
            isPlaying = false,
            isDrm = false
        )

        assertEquals(OrbStatus.SUPPORTED_FOUND, viewModel.uiState.value.orbStatus)
        assertTrue(viewModel.uiState.value.isOrbVisible)

        // Add 2nd downloadable video -> OrbStatus.MULTIPLE_CANDIDATES
        viewModel.onDomVideoFound(
            mediaUrl = "https://example.com/video2.mp4",
            pageUrl = "https://example.com",
            title = "Sample Video 2",
            mimeType = "video/mp4",
            width = 1280,
            height = 720,
            duration = 45.0,
            isPlaying = false,
            isDrm = false
        )

        assertEquals(OrbStatus.MULTIPLE_CANDIDATES, viewModel.uiState.value.orbStatus)

        // Clear by navigating to new page
        viewModel.loadUrlInActiveTab("https://duckduckgo.com")
        assertEquals(OrbStatus.HIDDEN, viewModel.uiState.value.orbStatus)
    }

    @Test
    fun `download engine pause and resume transition task status`() {
        val testMedia = DetectedMedia(
            id = "media_pause_test",
            mediaUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
            pageUrl = "https://example.com",
            title = "Big Buck Bunny",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            contentLength = 10485760L // 10 MB
        )

        val taskId = viewModel.downloadEngine.startDownload(
            media = testMedia,
            customFileName = "big_buck_bunny",
            useSystemDownloadManager = false
        )

        val taskBefore = viewModel.downloadTasks.value.find { it.id == taskId }
        assertNotNull(taskBefore)
        assertEquals(DownloadStatus.DOWNLOADING, taskBefore?.status)

        // Pause download
        viewModel.pauseDownload(taskId)
        val taskPaused = viewModel.downloadTasks.value.find { it.id == taskId }
        assertEquals(DownloadStatus.PAUSED, taskPaused?.status)

        // Resume download
        viewModel.resumeDownload(taskId)
        val taskResumed = viewModel.downloadTasks.value.find { it.id == taskId }
        assertEquals(DownloadStatus.DOWNLOADING, taskResumed?.status)

        // Cancel
        viewModel.downloadEngine.cancelDownload(taskId)
        val taskCancelled = viewModel.downloadTasks.value.find { it.id == taskId }
        assertEquals(DownloadStatus.CANCELLED, taskCancelled?.status)
    }
}
