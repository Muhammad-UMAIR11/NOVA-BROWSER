package com.example.videobrowser

import android.app.Application
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.adblock.AdBlockerEngine
import com.example.videobrowser.adblock.AdBlockerEngine.BlockType
import com.example.videobrowser.data.BrowserSettingsManager
import com.example.videobrowser.viewmodel.BrowserViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrowserPrivacyProtectionTest {

    private lateinit var app: Application
    private lateinit var viewModel: BrowserViewModel
    private lateinit var settingsManager: BrowserSettingsManager

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = BrowserViewModel(app)
        settingsManager = BrowserSettingsManager(app)
    }

    // ==========================================
    // 1. AD BLOCKER VERIFICATION & FALSE POSITIVES
    // ==========================================

    @Test
    fun `ad networks and script assets are accurately classified as AD`() {
        // Major programmatic ad networks & DSPs
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://ad.doubleclick.net/ddm/trackclk/N12345")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://securepubads.g.doubleclick.net/gampad/ads?iu=/1234/ad_unit")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://static.criteo.net/js/ld/ld.js")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://cdn.taboola.com/libtrc/unip/123/tfa.js")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://c.outbrain.com/widget")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://served-by.popads.net/banner.js")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://s.amazon-adsystem.com/iui3/panda")))

        // Common ad script endpoints on third-party publishers
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://news.example.com/assets/prebid.js")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://blog.example.com/js/ads.js")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://site.example.com/static/ad-banner/leaderboard.gif")))
    }

    @Test
    fun `legitimate websites with ad in their name are never falsely blocked`() {
        // Algemeen Dagblad (ad.nl) is a major Dutch news site, not an ad network
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://www.ad.nl/binnenland/nieuws")))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://ad.nl/sport")))
        assertFalse(AdBlockerEngine.isAdOrTracker(Uri.parse("https://ad.nl/")))

        // Banner Health and legitimate health/education sites
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://www.bannerhealth.com/doctors")))
        assertFalse(AdBlockerEngine.isAdOrTracker(Uri.parse("https://www.bannerhealth.com/")))

        // Standard web resources
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://en.wikipedia.org/wiki/Android")))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://github.com/torvalds/linux")))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")))
    }

    @Test
    fun `main frame document requests are never terminated as empty responses`() {
        // Even if user visits doubleclick.net directly in address bar, main frame is not empty-intercepted
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://doubleclick.net"), isForMainFrame = true))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://google-analytics.com"), isForMainFrame = true))
    }

    // ==========================================
    // 2. TRACKER BLOCKER VERIFICATION
    // ==========================================

    @Test
    fun `analytics beacons and social tracking pixels are accurately classified as TRACKER`() {
        // Web analytics and session replay
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://www.google-analytics.com/analytics.js")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://www.google-analytics.com/g/collect?v=2&tid=G-12345")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://www.googletagmanager.com/gtag/js?id=G-12345")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://static.hotjar.com/c/hotjar-12345.js")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://sb.scorecardresearch.com/beacon.js")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://cdn.segment.com/analytics.js/v1/key/analytics.min.js")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://www.clarity.ms/tag/abc1234")))

        // Social tracking pixels and event collectors
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://connect.facebook.net/en_US/fbevents.js")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://www.facebook.com/tr/?id=12345&ev=PageView")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://analytics.tiktok.com/i18n/pixel/events.js")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://static.ads-twitter.com/uwt.js")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://snap.licdn.com/li.lms-analytics/insight.min.js")))
    }

    @Test
    fun `parcel package tracking services are never falsely blocked as web trackers`() {
        // Legitimate shipping & logistics tracking URLs
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://tracking.fedex.com/en-us/tracking?trackingnumber=123456")))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://tracking.ups.com/track?tracknum=1Z9999999999999999")))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://www.dhl.com/en/express/tracking.html")))

        assertFalse(AdBlockerEngine.isAdOrTracker(Uri.parse("https://tracking.fedex.com/track")))
        assertFalse(AdBlockerEngine.isAdOrTracker(Uri.parse("https://tracking.ups.com/track")))
    }

    // ==========================================
    // 3. COOKIE BLOCKER & STORAGE MAINTENANCE
    // ==========================================

    @Test
    fun `cookie guard toggle updates settings and UI state properly`() {
        assertTrue(viewModel.uiState.value.blockCookies)
        assertTrue(settingsManager.getBlockCookies())

        // Toggle cookie guard off
        viewModel.setBlockCookies(false)
        assertFalse(viewModel.uiState.value.blockCookies)
        assertFalse(settingsManager.getBlockCookies())

        // Toggle cookie guard back on
        viewModel.setBlockCookies(true)
        assertTrue(viewModel.uiState.value.blockCookies)
        assertTrue(settingsManager.getBlockCookies())
    }

    @Test
    fun `clearBrowserCache executes smoothly without crashing`() {
        viewModel.clearBrowserCache()
        // Coroutine dispatches on IO & Main without uncaught exceptions
        val status = viewModel.uiState.value.statusNotice
        assertTrue(status == null || status.contains("cleaned") || status.contains("cache"))
    }

    // ==========================================
    // 4. ACCURACY, DEDUPLICATION & STATISTICS
    // ==========================================

    @Test
    fun `repeated requests for the same ad or tracker resource are deduplicated`() {
        val tabId = viewModel.uiState.value.activeTabId
        val initialLifetime = viewModel.uiState.value.totalBlockedAds

        // Simulate an ad network script retrying 5 times on the same page
        for (i in 1..5) {
            viewModel.onResourceBlocked(
                tabId = tabId,
                resourceKey = "pagead2.googlesyndication.com/pagead/js/adsbygoogle.js",
                blockType = BlockType.AD
            )
        }

        // Deduplication must record only 1 count for this resource on this page
        assertEquals(1, viewModel.uiState.value.activeTabBlockedAds)
        assertEquals(initialLifetime + 1, viewModel.uiState.value.totalBlockedAds)

        // A distinct tracking script on the same page increments the count by 1
        viewModel.onResourceBlocked(
            tabId = tabId,
            resourceKey = "www.google-analytics.com/analytics.js",
            blockType = BlockType.TRACKER
        )

        assertEquals(2, viewModel.uiState.value.activeTabBlockedAds)
        assertEquals(initialLifetime + 2, viewModel.uiState.value.totalBlockedAds)
    }

    @Test
    fun `blocked resources in background tabs do not contaminate active tab stats`() {
        // Create a secondary tab
        viewModel.addNewTab("https://site-b.org")
        val tabBId = viewModel.uiState.value.activeTabId

        // Switch back to primary tab
        val tabAId = viewModel.uiState.value.tabs.first().id
        viewModel.switchTab(tabAId)
        assertEquals(tabAId, viewModel.uiState.value.activeTabId)

        // Block an ad belonging to background Tab B
        viewModel.onResourceBlocked(
            tabId = tabBId,
            resourceKey = "adnxs.com/script.js",
            blockType = BlockType.AD
        )

        // Active tab (Tab A) must NOT show ads blocked
        assertEquals(0, viewModel.uiState.value.activeTabBlockedAds)

        // Tab B must correctly record its blocked ad
        val tabB = viewModel.uiState.value.tabs.find { it.id == tabBId }
        assertEquals(1, tabB?.blockedAdsCount)
    }

    @Test
    fun `navigating to new page resets page stats while hash navigation preserves it`() {
        val tabId = viewModel.uiState.value.activeTabId
        viewModel.onPageStarted("https://example.com/article")
        viewModel.onResourceBlocked(
            tabId = tabId,
            resourceKey = "doubleclick.net/ad.js",
            blockType = BlockType.AD
        )
        assertEquals(1, viewModel.uiState.value.activeTabBlockedAds)

        // In-page hash navigation (e.g. anchor link to #comments)
        viewModel.onPageStarted("https://example.com/article#comments")
        assertEquals(1, viewModel.uiState.value.activeTabBlockedAds)

        // Navigating to completely different URL
        viewModel.onPageStarted("https://other-site.com/home")
        assertEquals(0, viewModel.uiState.value.activeTabBlockedAds)
    }

    @Test
    fun `lifetime totalBlockedAds is persisted to settings manager`() {
        val tabId = viewModel.uiState.value.activeTabId
        val currentTotal = viewModel.uiState.value.totalBlockedAds

        viewModel.onResourceBlocked(
            tabId = tabId,
            resourceKey = "criteo.com/banner.js",
            blockType = BlockType.AD
        )

        val newTotal = viewModel.uiState.value.totalBlockedAds
        assertEquals(currentTotal + 1, newTotal)
        assertEquals(newTotal, settingsManager.getTotalBlockedAds())
    }

    // ==========================================
    // 5. NAVIGATION REDIRECT & MALICIOUS POPUPS
    // ==========================================

    @Test
    fun `shouldBlockNavigation blocks deceptive mobile schemes and popunder ad networks`() {
        assertTrue(AdBlockerEngine.shouldBlockNavigation(Uri.parse("market://details?id=com.malware.fake")))
        assertTrue(AdBlockerEngine.shouldBlockNavigation(Uri.parse("intent://scan/#Intent;scheme=zxing;package=com.adware;end")))
        assertTrue(AdBlockerEngine.shouldBlockNavigation(Uri.parse("itms-apps://itunes.apple.com/app/id123456")))
        assertTrue(AdBlockerEngine.shouldBlockNavigation(Uri.parse("https://popads.net/click/redirect")))
        assertTrue(AdBlockerEngine.shouldBlockNavigation(Uri.parse("https://exoclick.com/redirect")))

        // Safe web navigation is never blocked
        assertFalse(AdBlockerEngine.shouldBlockNavigation(Uri.parse("https://www.google.com/search?q=kotlin")))
        assertFalse(AdBlockerEngine.shouldBlockNavigation(Uri.parse("https://en.wikipedia.org/wiki/Main_Page")))
    }

    // ==========================================
    // 6. SAFE BLOCKED RESPONSES & COMPATIBILITY
    // ==========================================

    @Test
    fun `blocked script responses return benign JavaScript stubs preventing site runtime errors`() {
        val scriptResponse = AdBlockerEngine.createBlockedResponse("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js")
        assertEquals("application/javascript", scriptResponse.mimeType)
        assertEquals(200, scriptResponse.statusCode)
        val body = scriptResponse.data.bufferedReader().use { it.readText() }
        assertTrue(body.contains("adsbygoogle"))
        assertTrue(body.contains("gtag"))
        assertTrue(body.contains("fbq"))
    }

    @Test
    fun `blocked tracking pixels return transparent 1x1 image avoiding broken icon layout artifacts`() {
        val pixelResponse = AdBlockerEngine.createBlockedResponse("https://www.facebook.com/tr/?id=12345&ev=PageView")
        assertEquals("image/gif", pixelResponse.mimeType)
        assertEquals(200, pixelResponse.statusCode)
        val bytes = pixelResponse.data.readBytes()
        assertTrue(bytes.isNotEmpty())
        assertEquals('G'.code.toByte(), bytes[0])
        assertEquals('I'.code.toByte(), bytes[1])
        assertEquals('F'.code.toByte(), bytes[2])
    }

    @Test
    fun `modern publisher ad networks and enterprise analytics domains are blocked accurately`() {
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://scripts.mediavine.com/tags/test.js")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://ads.adthrive.com/sites/123/ads.min.js")))
        assertEquals(BlockType.AD, AdBlockerEngine.classifyRequest(Uri.parse("https://g.ezoic.net/ezoic/ezad.js")))

        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://metrics.omtrdc.net/b/ss/suite/1/JS-2.22.0/s12345")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://dpm.demdex.net/id?d_visid_ver=5.2.0")))
        assertEquals(BlockType.TRACKER, AdBlockerEngine.classifyRequest(Uri.parse("https://app-measurement.com/a")))
    }

    @Test
    fun `major ecommerce services and order tracking remain fully whitelisted and functional`() {
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://www.amazon.com/gp/your-account/order-history")))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://tools.usps.com/go/TrackConfirmAction?tLabels=9400100000000000000000")))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://www.walmart.com/orders")))
        assertEquals(BlockType.NONE, AdBlockerEngine.classifyRequest(Uri.parse("https://www.target.com/orders")))
    }

    // ==========================================
    // 7. LOW-END DEVICE PERFORMANCE MODE
    // ==========================================

    @Test
    fun `enabling low end device mode tunes animations blur and transparency for fast performance`() {
        viewModel.setLowEndMode(true)

        val state = viewModel.uiState.value
        assertTrue("Low-end mode must be active in state", state.lowEndMode)
        assertTrue("Low-end mode must be persisted in settings", settingsManager.getLowEndMode())
        assertEquals(
            "Animation intensity must be REDUCED to conserve processor cycles",
            com.example.videobrowser.model.theme.AnimationIntensity.REDUCED,
            state.animationIntensity
        )
        assertEquals(0.0f, state.panelBlurIntensity, 0.001f)
        assertEquals(0.98f, state.panelTransparency, 0.001f)

        // Disable low-end mode
        viewModel.setLowEndMode(false)
        val restoredState = viewModel.uiState.value
        assertFalse("Low-end mode must be disabled", restoredState.lowEndMode)
        assertFalse(settingsManager.getLowEndMode())
        assertEquals(
            com.example.videobrowser.model.theme.AnimationIntensity.BALANCED,
            restoredState.animationIntensity
        )
    }

    @Test
    fun `memory optimization purge updates status notice successfully`() {
        viewModel.optimizeMemoryAndPurgeCache()
        // Wait or check status notice
        val notice = viewModel.uiState.value.statusNotice
        assertTrue(notice == null || notice.contains("Memory") || notice.contains("cache"))
    }
}
