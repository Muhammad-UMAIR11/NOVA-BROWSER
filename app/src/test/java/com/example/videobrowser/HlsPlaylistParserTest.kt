package com.example.videobrowser

import com.example.videobrowser.download.HlsPlaylistParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HlsPlaylistParserTest {

    @Test
    fun `parse master playlist extracts variants sorted by bandwidth`() {
        val masterContent = """
            #EXTM3U
            #EXT-X-VERSION:3
            #EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360
            360p.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720
            720p.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080
            1080p.m3u8
        """.trimIndent()

        val playlist = HlsPlaylistParser.parse(masterContent, "https://cdn.example.com/videos/master.m3u8")

        assertTrue(playlist.isMaster)
        assertFalse(playlist.isDrmProtected)
        assertEquals(3, playlist.variants.size)

        // Best quality should be first
        assertEquals(5000000L, playlist.variants[0].bandwidth)
        assertEquals("1920x1080", playlist.variants[0].resolution)
        assertEquals("https://cdn.example.com/videos/1080p.m3u8", playlist.variants[0].url)

        assertEquals(2500000L, playlist.variants[1].bandwidth)
        assertEquals("https://cdn.example.com/videos/720p.m3u8", playlist.variants[1].url)
    }

    @Test
    fun `parse media playlist extracts segments and detects end of stream`() {
        val mediaContent = """
            #EXTM3U
            #EXT-X-TARGETDURATION:10
            #EXT-X-VERSION:3
            #EXTINF:9.009,
            segment_000.ts
            #EXTINF:9.009,
            segment_001.ts
            #EXTINF:5.000,
            segment_002.ts
            #EXT-X-ENDLIST
        """.trimIndent()

        val playlist = HlsPlaylistParser.parse(mediaContent, "https://cdn.example.com/stream/index.m3u8")

        assertFalse(playlist.isMaster)
        assertFalse(playlist.isDrmProtected)
        assertFalse(playlist.isLive)
        assertEquals(10.0, playlist.targetDuration, 0.001)
        assertEquals(3, playlist.segments.size)

        assertEquals("https://cdn.example.com/stream/segment_000.ts", playlist.segments[0].url)
        assertEquals(9.009, playlist.segments[0].durationSeconds, 0.001)

        assertEquals("https://cdn.example.com/stream/segment_002.ts", playlist.segments[2].url)
        assertEquals(5.000, playlist.segments[2].durationSeconds, 0.001)
    }

    @Test
    fun `parse detects DRM protected stream with SAMPLE-AES key`() {
        val drmContent = """
            #EXTM3U
            #EXT-X-VERSION:5
            #EXT-X-KEY:METHOD=SAMPLE-AES,URI="skd://drm.example.com/key",KEYFORMAT="com.apple.streamingkeydelivery"
            #EXTINF:6.0,
            seg_0.ts
            #EXT-X-ENDLIST
        """.trimIndent()

        val playlist = HlsPlaylistParser.parse(drmContent, "https://cdn.example.com/stream/drm.m3u8")

        assertTrue(playlist.isDrmProtected)
        assertNotNull(playlist.drmReason)
        assertTrue(playlist.drmReason!!.contains("DRM license decryption"))
    }

    @Test
    fun `parse detects fragmented MP4 initialization segment`() {
        val fmp4Content = """
            #EXTM3U
            #EXT-X-TARGETDURATION:6
            #EXT-X-VERSION:7
            #EXT-X-MAP:URI="init.mp4"
            #EXTINF:6.0,
            chunk_0.m4s
            #EXT-X-ENDLIST
        """.trimIndent()

        val playlist = HlsPlaylistParser.parse(fmp4Content, "https://cdn.example.com/fmp4/video.m3u8")

        assertEquals("https://cdn.example.com/fmp4/init.mp4", playlist.initSegmentUrl)
        assertEquals(1, playlist.segments.size)
        assertEquals("https://cdn.example.com/fmp4/chunk_0.m4s", playlist.segments[0].url)
    }

    @Test
    fun `parse extracts CODECS, FRAME-RATE and estimates size accurately`() {
        val masterWithMetadata = """
            #EXTM3U
            #EXT-X-VERSION:4
            #EXT-X-STREAM-INF:BANDWIDTH=4500000,RESOLUTION=1920x1080,CODECS="avc1.64002a,mp4a.40.2",FRAME-RATE=60.000
            1080p60.m3u8
            #EXT-X-STREAM-INF:BANDWIDTH=1200000,RESOLUTION=854x480,CODECS="avc1.4d401f,mp4a.40.2",FRAME-RATE=30.000
            480p.m3u8
        """.trimIndent()

        val playlist = HlsPlaylistParser.parse(masterWithMetadata, "https://cdn.example.com/stream/master.m3u8")

        assertEquals(2, playlist.variants.size)

        val v1080 = playlist.variants[0]
        assertEquals("1920x1080", v1080.resolution)
        assertEquals(4500000L, v1080.bandwidth)
        assertEquals("avc1.64002a,mp4a.40.2", v1080.codecs)
        assertEquals(60.0, v1080.frameRate, 0.001)

        val v480 = playlist.variants[1]
        assertEquals("854x480", v480.resolution)
        assertEquals(1200000L, v480.bandwidth)
        assertEquals(30.0, v480.frameRate, 0.001)

        // Test estimateSize calculation: 4500000 bps for 60 seconds = (4500000 / 8) * 60 = 33,750,000 bytes
        val estimated1080 = HlsPlaylistParser.estimateSize(v1080.bandwidth, 60.0)
        assertEquals(33750000L, estimated1080)
    }
}
