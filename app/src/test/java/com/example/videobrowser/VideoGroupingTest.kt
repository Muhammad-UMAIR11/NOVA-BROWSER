package com.example.videobrowser

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.download.HlsVariant
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DetectionSource
import com.example.videobrowser.model.MediaCompatibilityCategory
import com.example.videobrowser.model.VideoDownloadType
import com.example.videobrowser.model.VideoGroupingEngine
import com.example.videobrowser.viewmodel.BrowserViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class VideoGroupingTest {

    private lateinit var application: Application
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        viewModel = BrowserViewModel(application)
    }

    @Test
    fun `groupMedia unites 12 duplicate stream entries into 1 single video entry`() {
        val streamList = (1..12).map { i ->
            DetectedMedia(
                id = "stream_$i",
                mediaUrl = "https://cdn.videohost.com/content/movie123/chunk_part_${i}.mp4?token=abc&range=${i * 1000}",
                pageUrl = "https://videohost.com/watch/movie123",
                title = "Incredible Nature Documentary",
                mimeType = "video/mp4",
                format = "MP4",
                quality = "1080p",
                contentLength = 150_000_000L,
                width = 1920,
                height = 1080,
                durationSeconds = 620.0,
                isStream = false,
                isDownloadable = true,
                source = DetectionSource.NETWORK_INTERCEPTION
            )
        }

        val grouped = VideoGroupingEngine.groupMedia(
            rawMediaList = streamList,
            currentPageUrl = "https://videohost.com/watch/movie123",
            currentPageTitle = "Incredible Nature Documentary - VideoHost"
        )

        // Verifies duplicate streams are combined into exactly ONE video entry
        assertEquals(1, grouped.size)
        val singleEntry = grouped.first()
        assertEquals("Incredible Nature Documentary", singleEntry.title)
        assertEquals("videohost.com", singleEntry.domain)
        assertEquals(12, singleEntry.rawMediaCount)
        assertTrue(singleEntry.isDownloadable)
        assertNotNull(singleEntry.selectedQuality)
        assertEquals("1080p", singleEntry.selectedQuality?.resolutionShort)
    }

    @Test
    fun `groupMedia aggregates multiple resolutions (1080p, 720p, 480p, 360p) with clear quality options`() {
        val resolutions = listOf(
            Triple(1920, 1080, 200_000_000L),
            Triple(1280, 720, 100_000_000L),
            Triple(854, 480, 50_000_000L),
            Triple(640, 360, 25_000_000L)
        )

        val mediaList = resolutions.map { (w, h, size) ->
            DetectedMedia(
                id = "vid_${h}p",
                mediaUrl = "https://cdn.example.com/videos/awesome_presentation_${h}p.mp4",
                pageUrl = "https://example.com/watch/101",
                title = "Awesome Presentation",
                mimeType = "video/mp4",
                format = "MP4",
                quality = "${h}p",
                contentLength = size,
                width = w,
                height = h,
                durationSeconds = 300.0,
                isDownloadable = true
            )
        }

        val grouped = VideoGroupingEngine.groupMedia(
            rawMediaList = mediaList,
            currentPageUrl = "https://example.com/watch/101",
            currentPageTitle = "Awesome Presentation"
        )

        assertEquals(1, grouped.size)
        val video = grouped.first()

        // 4 quality options should be available under this single video
        assertEquals(4, video.qualities.size)

        // Highest quality (1080p) should be the default selected quality
        val topQuality = video.selectedQuality
        assertNotNull(topQuality)
        assertEquals("1080p", topQuality?.resolutionShort)
        assertEquals("MP4", topQuality?.format)
        assertEquals(VideoDownloadType.PROGRESSIVE, topQuality?.downloadType)
        assertTrue(topQuality?.formattedSize?.contains("MB") == true)

        // Verify quality labels and download types
        val qualityLabels = video.qualities.map { it.resolutionShort }
        assertEquals(listOf("1080p", "720p", "480p", "360p"), qualityLabels)
    }

    @Test
    fun `groupMedia expands HLS master playlist variants into selectable qualities`() {
        val masterHls = DetectedMedia(
            id = "master_hls",
            mediaUrl = "https://video.example.com/live/master.m3u8",
            pageUrl = "https://example.com/live",
            title = "Live Stream Broadcast",
            mimeType = "application/x-mpegurl",
            format = "HLS",
            quality = "Adaptive Stream",
            isStream = true,
            isDownloadable = true,
            category = MediaCompatibilityCategory.ADAPTIVE_STREAM,
            parsedHlsVariants = listOf(
                HlsVariant(
                    bandwidth = 5_000_000L,
                    resolution = "1920x1080",
                    url = "https://video.example.com/live/1080p.m3u8"
                ),
                HlsVariant(
                    bandwidth = 2_500_000L,
                    resolution = "1280x720",
                    url = "https://video.example.com/live/720p.m3u8"
                ),
                HlsVariant(
                    bandwidth = 1_000_000L,
                    resolution = "854x480",
                    url = "https://video.example.com/live/480p.m3u8"
                )
            )
        )

        val grouped = VideoGroupingEngine.groupMedia(
            rawMediaList = listOf(masterHls),
            currentPageUrl = "https://example.com/live",
            currentPageTitle = "Live Stream Broadcast"
        )

        assertEquals(1, grouped.size)
        val video = grouped.first()
        assertEquals(3, video.qualities.size)

        val q1080 = video.qualities[0]
        assertEquals("1080p", q1080.resolutionShort)
        assertEquals(VideoDownloadType.ADAPTIVE, q1080.downloadType)
        assertTrue(q1080.isDownloadable)

        val q720 = video.qualities[1]
        assertEquals("720p", q720.resolutionShort)
    }

    @Test
    fun `groupMedia filters out ad banners and tiny looping background snippets`() {
        val validVideo = DetectedMedia(
            id = "valid_1",
            mediaUrl = "https://content.site.com/media/main_lecture.mp4",
            pageUrl = "https://site.com/lecture",
            title = "Physics Lecture 1",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "720p",
            width = 1280,
            height = 720,
            durationSeconds = 1800.0,
            hasControls = true
        )

        val adMedia = DetectedMedia(
            id = "ad_1",
            mediaUrl = "https://pagead2.googlesyndication.com/ads/commercial.mp4",
            pageUrl = "https://site.com/lecture",
            title = "Ad Commercial",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "360p",
            durationSeconds = 15.0
        )

        val loopingDecorative = DetectedMedia(
            id = "decor_1",
            mediaUrl = "https://site.com/assets/bg_decor.mp4",
            pageUrl = "https://site.com/lecture",
            title = "Decorative Background",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "180p",
            width = 200,
            height = 100,
            durationSeconds = 2.0,
            isLooping = true,
            isMuted = true,
            hasControls = false,
            isLikelyPreview = true
        )

        val grouped = VideoGroupingEngine.groupMedia(
            rawMediaList = listOf(validVideo, adMedia, loopingDecorative),
            currentPageUrl = "https://site.com/lecture",
            currentPageTitle = "Physics Lecture 1"
        )

        // Only the valid video should remain; ad and decorative looping snippet must be filtered out
        assertEquals(1, grouped.size)
        assertEquals("Physics Lecture 1", grouped.first().title)
    }

    @Test
    fun `BrowserViewModel user quality selection and download confirmation flow`() {
        val media1080 = DetectedMedia(
            id = "vid_1080",
            mediaUrl = "https://videos.org/talk_1080p.mp4",
            pageUrl = "https://videos.org/watch",
            title = "Tech Talk",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            contentLength = 80_000_000L,
            width = 1920,
            height = 1080,
            durationSeconds = 600.0,
            isDownloadable = true
        )

        val media720 = DetectedMedia(
            id = "vid_720",
            mediaUrl = "https://videos.org/talk_720p.mp4",
            pageUrl = "https://videos.org/watch",
            title = "Tech Talk",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "720p",
            contentLength = 40_000_000L,
            width = 1280,
            height = 720,
            durationSeconds = 600.0,
            isDownloadable = true
        )

        // Simulate page loading and detecting both streams
        viewModel.onPageStarted("https://videos.org/watch")
        viewModel.onPageFinished("https://videos.org/watch", "Tech Talk")

        viewModel.onDomVideoFound(
            mediaUrl = media1080.mediaUrl,
            pageUrl = media1080.pageUrl,
            title = media1080.title,
            mimeType = media1080.mimeType,
            width = media1080.width,
            height = media1080.height,
            duration = media1080.durationSeconds,
            isPlaying = true,
            isDrm = false,
            userInteracted = true,
            hasControls = true,
            isLooping = false,
            isMuted = false,
            posterUrl = "https://videos.org/talk_thumb.jpg"
        )

        viewModel.onDomVideoFound(
            mediaUrl = media720.mediaUrl,
            pageUrl = media720.pageUrl,
            title = media720.title,
            mimeType = media720.mimeType,
            width = media720.width,
            height = media720.height,
            duration = media720.durationSeconds,
            isPlaying = false,
            isDrm = false,
            userInteracted = false,
            hasControls = true,
            isLooping = false,
            isMuted = false,
            posterUrl = "https://videos.org/talk_thumb.jpg"
        )

        val uiState = viewModel.uiState.value
        assertEquals(1, uiState.detectedCount)
        val groupedVideo = uiState.detectedGroupedVideos.first()
        assertEquals("Tech Talk", groupedVideo.title)
        assertEquals("https://videos.org/talk_thumb.jpg", groupedVideo.thumbnailUrl)

        // User switches quality from 1080p to 720p
        val quality720Option = groupedVideo.qualities.find { it.resolutionShort == "720p" }
        assertNotNull(quality720Option)

        viewModel.selectQuality(groupedVideo.id, quality720Option!!.id)

        val updatedState = viewModel.uiState.value
        val updatedGroup = updatedState.detectedGroupedVideos.first()
        assertEquals(quality720Option.id, updatedGroup.selectedQualityId)
        assertEquals("720p", updatedGroup.selectedQuality?.resolutionShort)

        // User confirms download for this grouped video with selected 720p
        viewModel.selectGroupForDownload(updatedGroup)
        val confirmationState = viewModel.uiState.value
        assertNotNull(confirmationState.selectedMediaForConfirmation)
        assertEquals("720p", confirmationState.selectedMediaForConfirmation?.quality)
        assertEquals(media720.mediaUrl, confirmationState.selectedMediaForConfirmation?.mediaUrl)
    }

    @Test
    fun `grouped video accurately presents downloadable qualities count and size range`() {
        // Multi-quality video with 1080p (120MB), 720p (60MB), and 360p (20MB)
        val m1080 = DetectedMedia(
            id = "m1",
            mediaUrl = "https://cdn.example.com/movie_1080p.mp4",
            pageUrl = "https://example.com/watch/1",
            title = "Nature Film",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            contentLength = 120 * 1024 * 1024L,
            width = 1920,
            height = 1080,
            durationSeconds = 120.0,
            isDownloadable = true,
            source = DetectionSource.DOM_INSPECTION
        )
        val m720 = DetectedMedia(
            id = "m2",
            mediaUrl = "https://cdn.example.com/movie_720p.mp4",
            pageUrl = "https://example.com/watch/1",
            title = "Nature Film",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "720p",
            contentLength = 60 * 1024 * 1024L,
            width = 1280,
            height = 720,
            durationSeconds = 120.0,
            isDownloadable = true,
            source = DetectionSource.DOM_INSPECTION
        )
        val m360 = DetectedMedia(
            id = "m3",
            mediaUrl = "https://cdn.example.com/movie_360p.mp4",
            pageUrl = "https://example.com/watch/1",
            title = "Nature Film",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "360p",
            contentLength = 20 * 1024 * 1024L,
            width = 640,
            height = 360,
            durationSeconds = 120.0,
            isDownloadable = true,
            source = DetectionSource.DOM_INSPECTION
        )

        val groups = VideoGroupingEngine.groupMedia(
            rawMediaList = listOf(m1080, m720, m360),
            currentPageUrl = "https://example.com/watch/1",
            currentPageTitle = "Nature Film"
        )

        assertEquals(1, groups.size)
        val group = groups.first()

        assertEquals(3, group.downloadableQualitiesCount)
        assertEquals(3, group.totalQualitiesCount)
        assertEquals("3 qualities (1080p, 720p, 360p)", group.qualitiesOverviewText)
        assertEquals("20.0 MB – 120.0 MB", group.sizeRangeText)

        // Best quality selected by default
        assertEquals("1080p", group.selectedQuality?.resolutionShort)
        assertEquals("120.0 MB", group.selectedQuality?.displaySize)

        // Verify qualities list ordering (highest to lowest)
        assertEquals("1080p", group.qualities[0].resolutionShort)
        assertEquals("720p", group.qualities[1].resolutionShort)
        assertEquals("360p", group.qualities[2].resolutionShort)
    }

    @Test
    fun `HLS stream estimates sizes per variant and presents size range correctly`() {
        val hlsMedia = DetectedMedia(
            id = "hls1",
            mediaUrl = "https://stream.example.com/master.m3u8",
            pageUrl = "https://stream.example.com/watch/live",
            title = "Conference Keynote",
            mimeType = "application/x-mpegURL",
            format = "HLS",
            quality = "1080p",
            durationSeconds = 300.0, // 5 minutes
            isStream = true,
            isDownloadable = true,
            source = DetectionSource.NETWORK_INTERCEPTION,
            parsedHlsVariants = listOf(
                HlsVariant(
                    url = "https://stream.example.com/1080p.m3u8",
                    bandwidth = 6_000_000L, // 6 Mbps -> ~225 MB for 300s
                    resolution = "1920x1080",
                    codecs = "avc1.640028"
                ),
                HlsVariant(
                    url = "https://stream.example.com/720p.m3u8",
                    bandwidth = 2_500_000L, // 2.5 Mbps -> ~93.75 MB for 300s
                    resolution = "1280x720",
                    codecs = "avc1.4d401f"
                ),
                HlsVariant(
                    url = "https://stream.example.com/480p.m3u8",
                    bandwidth = 1_000_000L, // 1 Mbps -> ~37.5 MB for 300s
                    resolution = "854x480",
                    codecs = "avc1.4d401f"
                )
            )
        )

        val groups = VideoGroupingEngine.groupMedia(
            rawMediaList = listOf(hlsMedia),
            currentPageUrl = "https://stream.example.com/watch/live",
            currentPageTitle = "Conference Keynote"
        )

        assertEquals(1, groups.size)
        val group = groups.first()

        assertEquals(3, group.downloadableQualitiesCount)
        assertTrue(group.sizeRangeText.contains("~35.8 MB – ~214.6 MB"))

        val q1080 = group.qualities.find { it.resolutionShort == "1080p" }
        assertNotNull(q1080)
        assertTrue(q1080!!.isEstimatedSize)
        assertEquals("~214.6 MB", q1080.displaySize)

        val q480 = group.qualities.find { it.resolutionShort == "480p" }
        assertNotNull(q480)
        assertTrue(q480!!.isEstimatedSize)
        assertEquals("~35.8 MB", q480.displaySize)
    }
}
