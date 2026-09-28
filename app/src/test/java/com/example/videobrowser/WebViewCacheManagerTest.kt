package com.example.videobrowser

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.detection.WebViewCacheManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WebViewCacheManagerTest {

    private lateinit var application: Application

    @Before
    fun setup() {
        application = ApplicationProvider.getApplicationContext()
        // Clean any existing test cache
        File(application.cacheDir, "WebView").deleteRecursively()
    }

    @Test
    fun `sanitizeWebViewCache cleans corrupted Code Cache inside HTTP Cache directory`() {
        // Setup corrupted directory structure as reported:
        // cache/WebView/Default/HTTP Cache/Code Cache/js
        val webViewDir = File(application.cacheDir, "WebView")
        val defaultDir = File(webViewDir, "Default")
        val httpCacheDir = File(defaultDir, "HTTP Cache")
        val corruptedCodeCache = File(httpCacheDir, "Code Cache/js")
        corruptedCodeCache.mkdirs()

        // Place a dummy file in the corrupted path
        File(corruptedCodeCache, "dummy.bin").writeText("corrupted")
        assertTrue("Corrupted directory must exist before test", corruptedCodeCache.exists())

        // Run sanitizer
        WebViewCacheManager.sanitizeWebViewCache(application)

        // The corrupted HTTP Cache directory structure must be purged
        assertFalse(
            "Corrupted HTTP Cache structure should be removed to prevent Simple Cache Backend error code 8",
            corruptedCodeCache.exists()
        )
    }

    @Test
    fun `sanitizeWebViewCache cleans corrupted fake index without real index`() {
        val webViewDir = File(application.cacheDir, "WebView")
        val defaultDir = File(webViewDir, "Default")
        val httpCacheDir = File(defaultDir, "HTTP Cache")
        httpCacheDir.mkdirs()

        // Write a 0-byte fake index with no the-real-index
        val fakeIndex = File(httpCacheDir, "index")
        fakeIndex.writeBytes(ByteArray(0))

        assertTrue(fakeIndex.exists())

        WebViewCacheManager.sanitizeWebViewCache(application)

        assertFalse("Broken fake index should be cleared", fakeIndex.exists())
    }

    @Test
    fun `clearWebViewDiskCache removes HTTP Cache and Code Cache cleanly`() {
        val webViewDir = File(application.cacheDir, "WebView")
        val defaultDir = File(webViewDir, "Default")
        val httpCacheDir = File(defaultDir, "HTTP Cache")
        val codeCacheDir = File(defaultDir, "Code Cache")
        httpCacheDir.mkdirs()
        codeCacheDir.mkdirs()

        File(httpCacheDir, "cache_entry_0").writeText("cache data")
        File(codeCacheDir, "v8_cache.bin").writeText("bytecode")

        assertTrue(httpCacheDir.exists())
        assertTrue(codeCacheDir.exists())

        WebViewCacheManager.clearWebViewDiskCache(application)

        assertFalse(httpCacheDir.exists())
        assertFalse(codeCacheDir.exists())
    }

    @Test
    fun `configureGraphicsEnvironment executes without exception`() {
        // Must succeed without throwing any exceptions
        WebViewCacheManager.configureGraphicsEnvironment()
    }
}
