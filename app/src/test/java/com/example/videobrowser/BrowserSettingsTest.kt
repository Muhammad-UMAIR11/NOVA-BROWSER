package com.example.videobrowser

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.videobrowser.model.AppFontFamily
import com.example.videobrowser.model.ColorTheme
import com.example.videobrowser.model.SearchEngine
import com.example.videobrowser.model.ThemeMode
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
class BrowserSettingsTest {

    private lateinit var app: Application
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        viewModel = BrowserViewModel(app)
    }

    @Test
    fun `default settings are properly initialized`() {
        val state = viewModel.uiState.value
        assertEquals(SearchEngine.DUCKDUCKGO, state.searchEngine)
        assertEquals(ThemeMode.SYSTEM, state.themeMode)
        assertEquals(ColorTheme.OCEAN_BLUE, state.colorTheme)
        assertEquals(AppFontFamily.SYSTEM_DEFAULT, state.appFontFamily)
        assertFalse(state.showSettingsSheet)
    }

    @Test
    fun `switching search engine updates search URL generation`() {
        // Switch to Google
        viewModel.setSearchEngine(SearchEngine.GOOGLE)
        assertEquals(SearchEngine.GOOGLE, viewModel.uiState.value.searchEngine)

        viewModel.submitUrl("android jetpack compose")
        var activeTab = viewModel.uiState.value.activeTab
        assertTrue(activeTab?.url?.startsWith("https://www.google.com/search?q=") == true)

        // Switch to Bing
        viewModel.setSearchEngine(SearchEngine.BING)
        viewModel.submitUrl("kotlin flows")
        activeTab = viewModel.uiState.value.activeTab
        assertTrue(activeTab?.url?.startsWith("https://www.bing.com/search?q=") == true)

        // Switch to Brave
        viewModel.setSearchEngine(SearchEngine.BRAVE)
        viewModel.submitUrl("material 3 components")
        activeTab = viewModel.uiState.value.activeTab
        assertTrue(activeTab?.url?.startsWith("https://search.brave.com/search?q=") == true)
    }

    @Test
    fun `toggling theme mode updates state from dark to bright`() {
        // Toggle to bright
        viewModel.setThemeMode(ThemeMode.BRIGHT)
        assertEquals(ThemeMode.BRIGHT, viewModel.uiState.value.themeMode)

        // Toggle to dark
        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)

        // Toggle to system
        viewModel.setThemeMode(ThemeMode.SYSTEM)
        assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `color theme and font selection update state`() {
        viewModel.setColorTheme(ColorTheme.EMERALD)
        assertEquals(ColorTheme.EMERALD, viewModel.uiState.value.colorTheme)

        viewModel.setFontFamily(AppFontFamily.SERIF)
        assertEquals(AppFontFamily.SERIF, viewModel.uiState.value.appFontFamily)
    }

    @Test
    fun `settings sheet visibility toggles cleanly`() {
        viewModel.openSettingsSheet()
        assertTrue(viewModel.uiState.value.showSettingsSheet)

        viewModel.closeSettingsSheet()
        assertFalse(viewModel.uiState.value.showSettingsSheet)
    }

    @Test
    fun `quick bookmarks do not contain test stream titles`() {
        val bookmarks = viewModel.quickBookmarks
        assertTrue(bookmarks.isNotEmpty())
        for (b in bookmarks) {
            assertFalse(b.title.contains("Demo", ignoreCase = true))
            assertFalse(b.title.contains("Test", ignoreCase = true))
            assertFalse(b.subtitle.contains("test", ignoreCase = true))
        }
    }

    @Test
    fun `default initial tab is Nova front page`() {
        val activeTab = viewModel.uiState.value.activeTab
        assertEquals(BrowserViewModel.NOVA_HOME_URL, activeTab?.url)
        assertEquals("Nova", activeTab?.title)
    }

    @Test
    fun `goHome and addNewTab navigate to Nova front page`() {
        // First navigate to a webpage
        viewModel.submitUrl("https://example.com")
        assertEquals("https://example.com", viewModel.uiState.value.activeTab?.url)

        // Then go home
        viewModel.goHome()
        assertEquals(BrowserViewModel.NOVA_HOME_URL, viewModel.uiState.value.activeTab?.url)

        // Add new tab
        viewModel.addNewTab()
        val newTab = viewModel.uiState.value.activeTab
        assertEquals(BrowserViewModel.NOVA_HOME_URL, newTab?.url)
        assertEquals("Nova", newTab?.title)
    }

    @Test
    fun `removeCustomWallpaper sets state to null cleanly`() {
        viewModel.removeCustomWallpaper()
        assertEquals(null, viewModel.uiState.value.customWallpaperPath)
    }

    @Test
    fun `toggleToolsSheet opens and closes smoothly`() {
        assertFalse(viewModel.uiState.value.showToolsSheet)

        viewModel.openToolsSheet()
        assertTrue(viewModel.uiState.value.showToolsSheet)

        viewModel.closeToolsSheet()
        assertFalse(viewModel.uiState.value.showToolsSheet)

        viewModel.toggleToolsSheet()
        assertTrue(viewModel.uiState.value.showToolsSheet)

        viewModel.toggleToolsSheet()
        assertFalse(viewModel.uiState.value.showToolsSheet)
    }

    @Test
    fun `toggleDesktopMode switches active tab desktop mode and updates notice`() {
        val initialDesktop = viewModel.uiState.value.activeTab?.isDesktopMode ?: false
        assertFalse(initialDesktop)

        viewModel.toggleDesktopModeForActiveTab()
        assertTrue(viewModel.uiState.value.activeTab?.isDesktopMode == true)
        assertEquals("Desktop mode enabled", viewModel.uiState.value.statusNotice)

        viewModel.toggleDesktopModeForActiveTab()
        assertFalse(viewModel.uiState.value.activeTab?.isDesktopMode == true)
        assertEquals("Mobile mode enabled", viewModel.uiState.value.statusNotice)
    }

    @Test
    fun `ad blocker is enabled by default on app launch with zero activation needed`() {
        assertTrue(viewModel.isAdBlockerActive())
        assertTrue(viewModel.uiState.value.isAdBlockerEnabled)
        assertEquals(0, viewModel.uiState.value.totalBlockedAds)
    }

    @Test
    fun `ad blocker increments blocked ads count per tab and lifetime`() {
        assertEquals(0, viewModel.uiState.value.activeTabBlockedAds)
        assertEquals(0, viewModel.uiState.value.totalBlockedAds)

        viewModel.onAdBlocked("doubleclick.net")
        assertEquals(1, viewModel.uiState.value.activeTabBlockedAds)
        assertEquals(1, viewModel.uiState.value.totalBlockedAds)

        viewModel.onAdBlocked("googleadservices.com")
        assertEquals(2, viewModel.uiState.value.activeTabBlockedAds)
        assertEquals(2, viewModel.uiState.value.totalBlockedAds)
    }

    @Test
    fun `AdBlockerEngine accurately identifies ad networks and allows normal web content`() {
        // Known ad domains
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://pagead2.googlesyndication.com/pagead/js/adsbygoogle.js")))
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://ad.doubleclick.net/ddm/trackclk/N12345")))
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://cdn.taboola.com/libtrc/unip/123/tfa.js")))
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://c.outbrain.com/widget")))
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://static.criteo.net/js/ld/ld.js")))
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://served-by.popads.net/banner.js")))

        // Common ad paths on third-party CDNs
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://some-news-site.com/assets/prebid.js")))
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://some-blog.com/scripts/ads.js")))

        // Legitimate non-ad websites must NOT be blocked
        assertFalse(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://en.wikipedia.org/wiki/Kotlin")))
        assertFalse(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://github.com/torvalds/linux")))
        assertFalse(com.example.videobrowser.adblock.AdBlockerEngine.isAdOrTracker(android.net.Uri.parse("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")))
    }

    @Test
    fun `AdBlockerEngine blocks malicious popunder and intent redirects`() {
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.shouldBlockNavigation(android.net.Uri.parse("market://details?id=com.fake.adware")))
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.shouldBlockNavigation(android.net.Uri.parse("intent://scan/#Intent;scheme=zxing;package=com.adware;end")))
        assertTrue(com.example.videobrowser.adblock.AdBlockerEngine.shouldBlockNavigation(android.net.Uri.parse("https://popads.net/click/redirect")))
        assertFalse(com.example.videobrowser.adblock.AdBlockerEngine.shouldBlockNavigation(android.net.Uri.parse("https://www.google.com/search?q=nova")))
    }
}
