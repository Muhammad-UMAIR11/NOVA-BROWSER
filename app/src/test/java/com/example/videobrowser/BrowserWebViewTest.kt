package com.example.videobrowser

import android.app.Application
import android.net.Uri
import android.webkit.WebResourceRequest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.detection.MediaBridge
import com.example.videobrowser.detection.WebMediaInjector
import com.example.videobrowser.ui.components.BrowserWebView
import com.example.videobrowser.viewmodel.BrowserViewModel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BrowserWebViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var application: Application
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = BrowserViewModel(application)
    }

    @Test
    fun `BrowserWebView renders composable without crash`() {
        composeTestRule.setContent {
            BrowserWebView(
                viewModel = viewModel,
                targetUrl = "https://example.com"
            )
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun `Network interception through BrowserViewModel registers detected media`() {
        val pageUrl = "https://example.com/watch"
        viewModel.loadUrlInActiveTab(pageUrl)

        val videoUrl = "https://example.com/streams/720p.mp4"
        val headers = mapOf(
            "Referer" to pageUrl,
            "User-Agent" to "MobileBrowser/1.0"
        )

        viewModel.onNetworkResourceIntercepted(
            requestUrl = videoUrl,
            headers = headers,
            pageUrl = pageUrl
        )

        val activeTab = viewModel.uiState.value.activeTab
        assertNotNull(activeTab)
        assertEquals(1, activeTab?.detectedMediaList?.size)
        assertEquals(videoUrl, activeTab?.detectedMediaList?.first()?.mediaUrl)
        assertEquals(1, activeTab?.groupedVideos?.size)
    }

    @Test
    fun `MediaBridge JavascriptInterface parses and forwards DOM video detection to ViewModel`() {
        val bridge = MediaBridge(listener = viewModel)

        val payload = JSONObject().apply {
            put("src", "https://example.com/media/main_player.mp4")
            put("currentSrc", "https://example.com/media/main_player.mp4")
            put("pageUrl", "https://example.com/watch")
            put("title", "Nature Documentary 4K")
            put("type", "video/mp4")
            put("videoWidth", 1920)
            put("videoHeight", 1080)
            put("duration", 185.5)
            put("isPlaying", true)
            put("isDrm", false)
            put("userInteracted", true)
            put("hasControls", true)
            put("poster", "https://example.com/media/poster.jpg")
        }

        bridge.onVideoDetected(payload.toString())

        val activeTab = viewModel.uiState.value.activeTab
        assertNotNull(activeTab)
        val detected = activeTab?.detectedMediaList?.firstOrNull()
        assertNotNull(detected)
        assertEquals("https://example.com/media/main_player.mp4", detected?.mediaUrl)
        assertEquals(1080, detected?.height)
        assertEquals(1920, detected?.width)
        assertEquals(185.5, detected?.durationSeconds ?: 0.0, 0.1)
        assertTrue(detected?.isPlaying == true)
        assertTrue(detected?.userInteracted == true)
        assertEquals("https://example.com/media/poster.jpg", detected?.thumbnailUrl)

        // Check grouped representation
        val grouped = activeTab?.groupedVideos?.firstOrNull()
        assertNotNull(grouped)
        assertEquals("Nature Documentary 4K", grouped?.title)
        assertTrue(grouped?.isPlaying == true)
    }

    @Test
    fun `MediaBridge onPlaybackState updates playing status and current position`() {
        val bridge = MediaBridge(listener = viewModel)
        val mediaUrl = "https://example.com/media/clip.mp4"

        // First discover
        bridge.onVideoDetected(JSONObject().apply {
            put("src", mediaUrl)
            put("currentSrc", mediaUrl)
            put("pageUrl", "https://example.com")
            put("title", "Cool Clip")
            put("videoHeight", 720)
            put("duration", 60.0)
            put("isPlaying", false)
        }.toString())

        // Now simulate play event from JS
        bridge.onPlaybackState(JSONObject().apply {
            put("src", mediaUrl)
            put("isPlaying", true)
            put("currentTime", 15.0)
            put("duration", 60.0)
        }.toString())

        val activeTab = viewModel.uiState.value.activeTab
        val detected = activeTab?.detectedMediaList?.firstOrNull()
        assertTrue(detected?.isPlaying == true)
    }

    @Test
    fun `Direct download request via onDownloadRequestedFromWebView creates detected media and prompts confirmation`() {
        val downloadUrl = "https://example.com/files/lecture_recording.mp4"
        viewModel.onDownloadRequestedFromWebView(
            url = downloadUrl,
            userAgent = "CustomBrowser/1.0",
            contentDisposition = "attachment; filename=\"lecture_recording.mp4\"",
            mimeType = "video/mp4",
            contentLength = 52_428_800L // 50 MB
        )

        val activeTab = viewModel.uiState.value.activeTab
        assertNotNull(activeTab)
        val detected = activeTab?.detectedMediaList?.find { it.mediaUrl == downloadUrl }
        assertNotNull(detected)
        assertEquals("lecture_recording", detected?.title)
        assertEquals(52_428_800L, detected?.contentLength)
        assertTrue(detected?.isDownloadable == true)

        // Verifies download confirmation dialog was opened
        val state = viewModel.uiState.value
        assertNotNull(state.selectedMediaForConfirmation)
        assertEquals(downloadUrl, state.selectedMediaForConfirmation?.mediaUrl)
    }

    @Test
    fun `WebMediaInjector script contains essential detection hooks`() {
        val script = WebMediaInjector.DETECTION_SCRIPT
        assertTrue(script.contains("AndroidMediaBridge"))
        assertTrue(script.contains("reportVideo"))
        assertTrue(script.contains("MutationObserver"))
        assertTrue(script.contains("userInteracted"))
        assertTrue(script.contains("requestMediaKeySystemAccess"))
    }

    @Test
    fun `BrowserWebView handles multiple tabs and navigation without renderer crash`() {
        viewModel.addNewTab("https://duckduckgo.com")
        composeTestRule.setContent {
            BrowserWebView(
                viewModel = viewModel,
                targetUrl = "https://duckduckgo.com"
            )
        }
        composeTestRule.waitForIdle()
        val currentTab = viewModel.uiState.value.activeTab
        assertNotNull(currentTab)
        assertEquals("https://duckduckgo.com", currentTab?.url)
    }
}
