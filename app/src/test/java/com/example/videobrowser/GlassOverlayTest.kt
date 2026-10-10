package com.example.videobrowser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.videobrowser.model.BrowserTab
import com.example.videobrowser.model.ColorTheme
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DownloadStatus
import com.example.videobrowser.model.DownloadTask
import com.example.videobrowser.model.SearchEngine
import com.example.videobrowser.model.ThemeMode
import com.example.videobrowser.ui.components.BrowserBookmarksSheet
import com.example.videobrowser.ui.components.BrowserHistorySheet
import com.example.videobrowser.ui.components.BrowserPrivacySheet
import com.example.videobrowser.ui.components.BrowserSettingsSheet
import com.example.videobrowser.ui.components.DetectedMediaSheet
import com.example.videobrowser.ui.components.DownloadConfirmationDialog
import com.example.videobrowser.ui.components.DownloadManagerSheet
import com.example.videobrowser.ui.components.NovaAiSheet
import com.example.videobrowser.ui.components.NovaToolsSheet
import com.example.videobrowser.ui.components.TabsSheet
import com.example.videobrowser.ui.glass.GlassBottomSheet
import com.example.videobrowser.ui.glass.GlassCard
import com.example.videobrowser.ui.glass.GlassDialog
import com.example.videobrowser.ui.glass.GlassTokens
import com.example.videobrowser.viewmodel.BookmarkItem
import com.example.videobrowser.viewmodel.HistoryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GlassOverlayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `glass tokens enforce translucent surfaces and high contrast text`() {
        // Translucent alphas (< 1.0f) to guarantee webpage behind remains visible
        assertTrue("Surface must be translucent", GlassTokens.GlassSurface.alpha in 0.70f..0.85f)
        assertTrue("Scrim must be translucent", GlassTokens.GlassScrim.alpha in 0.30f..0.55f)
        assertTrue("Card background must be frosted translucent", GlassTokens.GlassCardBg.alpha in 0.15f..0.35f)

        // Text primary and secondary have high luminance for contrast against dark glass
        assertEquals(androidx.compose.ui.graphics.Color(0xFFF8FAFC), GlassTokens.TextPrimary)
        assertNotNull(GlassTokens.glassBorderBrush())
        assertNotNull(GlassTokens.glassSurfaceBrush())
        assertNotNull(GlassTokens.glassHighlightBrush())
    }

    @Test
    fun `reusable glass bottom sheet renders title, subtitle and dismisses on close button click`() {
        var dismissed = false

        composeTestRule.setContent {
            GlassBottomSheet(
                visible = true,
                onDismiss = { dismissed = true },
                title = "Test Glass Panel",
                subtitle = "Subtle translucent overlay",
                icon = Icons.Default.Palette,
                testTag = "test_glass_panel"
            ) {
                Text("Glass Content Body")
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("test_glass_panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Test Glass Panel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Subtle translucent overlay").assertIsDisplayed()
        composeTestRule.onNodeWithText("Glass Content Body").assertIsDisplayed()

        composeTestRule.onNodeWithTag("test_glass_panel_close_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Dismiss callback must fire", dismissed)
    }

    @Test
    fun `settings panel renders with glass bottom sheet and handles dismiss`() {
        var dismissed = false

        composeTestRule.setContent {
            BrowserSettingsSheet(
                visible = true,
                currentSearchEngine = SearchEngine.DUCKDUCKGO,
                currentThemeMode = ThemeMode.DARK,
                currentColorTheme = ColorTheme.OCEAN_BLUE,
                currentFontFamily = com.example.videobrowser.model.AppFontFamily.SYSTEM_DEFAULT,
                onSelectSearchEngine = {},
                onSelectThemeMode = {},
                onSelectColorTheme = {},
                onSelectFontFamily = {},
                onClearCache = {},
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("settings_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nova Settings").assertIsDisplayed()
        composeTestRule.onNodeWithTag("settings_bottom_sheet_close_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun `tabs sheet renders open tabs and options menu within glass card`() {
        var selectedTab: String? = null
        var dismissed = false

        val tabs = listOf(
            BrowserTab(id = "tab-1", title = "First Tab", url = "https://example.com"),
            BrowserTab(id = "tab-2", title = "Second Tab", url = "https://example.org")
        )

        composeTestRule.setContent {
            TabsSheet(
                visible = true,
                tabs = tabs,
                activeTabId = "tab-1",
                onSelectTab = { selectedTab = it },
                onCloseTab = {},
                onNewTab = {},
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("tabs_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Browser Tabs").assertIsDisplayed()
        composeTestRule.onNodeWithText("First Tab").assertIsDisplayed()
        composeTestRule.onNodeWithText("Second Tab").assertIsDisplayed()

        // Tap second tab card
        composeTestRule.onNodeWithText("Second Tab").performClick()
        composeTestRule.waitForIdle()
        assertEquals("tab-2", selectedTab)
        assertTrue(dismissed)
    }

    @Test
    fun `download manager panel displays active tasks with glass tokens`() {
        var dismissed = false
        val tasks = listOf(
            DownloadTask(
                id = "dl-1",
                title = "Video 1",
                mediaUrl = "https://example.com/video1.mp4",
                pageUrl = "https://example.com",
                fileName = "video1.mp4",
                mimeType = "video/mp4",
                format = "MP4",
                quality = "1080p",
                status = DownloadStatus.DOWNLOADING,
                progress = 0.45f,
                downloadedBytes = 4500000L,
                totalBytes = 10000000L
            )
        )

        composeTestRule.setContent {
            DownloadManagerSheet(
                visible = true,
                tasks = tasks,
                onCancelTask = {},
                onRemoveTask = {},
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("downloads_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Download Manager").assertIsDisplayed()
        composeTestRule.onNodeWithTag("downloads_bottom_sheet_close_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun `detected media panel displays video stream items`() {
        var dismissed = false
        val mediaList = listOf(
            DetectedMedia(
                id = "m-1",
                mediaUrl = "https://example.com/stream.mp4",
                pageUrl = "https://example.com",
                title = "Detected Stream",
                mimeType = "video/mp4",
                format = "MP4",
                quality = "720p",
                contentLength = 5000000L
            )
        )

        composeTestRule.setContent {
            DetectedMediaSheet(
                visible = true,
                pageTitle = "Sample Webpage",
                pageUrl = "https://example.com",
                mediaList = mediaList,
                onDismiss = { dismissed = true },
                onSelectMedia = {}
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("detected_media_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Detected Videos").assertIsDisplayed()
        composeTestRule.onNodeWithTag("detected_media_bottom_sheet_close_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun `nova tools panel displays utilities grid and dispatches action callbacks`() {
        var aiOpened = false
        var historyOpened = false
        var bookmarksOpened = false
        var privacyOpened = false

        composeTestRule.setContent {
            NovaToolsSheet(
                visible = true,
                activeTabTitle = "Current Page",
                activeTabUrl = "https://example.com",
                onDismiss = {},
                onOpenDownloads = {},
                onOpenTabs = {},
                onOpenSettings = {},
                onClearCache = {},
                onSniffUrl = {},
                onOpenAi = { aiOpened = true },
                onOpenHistory = { historyOpened = true },
                onOpenBookmarks = { bookmarksOpened = true },
                onOpenPrivacy = { privacyOpened = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nova_tools_overlay").assertIsDisplayed()
        composeTestRule.onNodeWithTag("tools_sheet_item_ai").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue("AI assistant should open", aiOpened)

        composeTestRule.onNodeWithTag("tools_sheet_item_bookmarks").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue("Bookmarks should open", bookmarksOpened)

        composeTestRule.onNodeWithTag("tools_sheet_item_history").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue("History should open", historyOpened)

        composeTestRule.onNodeWithTag("tools_sheet_item_privacy_shield").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue("Privacy shield should open", privacyOpened)
    }

    @Test
    fun `ai assistant panel renders insight tools in glass bottom sheet`() {
        var dismissed = false

        composeTestRule.setContent {
            NovaAiSheet(
                visible = true,
                pageTitle = "Wikipedia",
                pageUrl = "https://en.wikipedia.org",
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("nova_ai_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nova AI Assistant").assertIsDisplayed()
        composeTestRule.onNodeWithTag("nova_ai_sheet_close_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun `history panel renders and filters visited items`() {
        var dismissed = false
        var selectedUrl: String? = null
        val history = listOf(
            HistoryItem(id = "h-1", title = "Google Search", url = "https://google.com"),
            HistoryItem(id = "h-2", title = "Kotlin Lang", url = "https://kotlinlang.org")
        )

        composeTestRule.setContent {
            BrowserHistorySheet(
                visible = true,
                historyList = history,
                onSelectUrl = { selectedUrl = it },
                onClearHistory = {},
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("history_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Browsing History").assertIsDisplayed()
        composeTestRule.onNodeWithText("Kotlin Lang").assertIsDisplayed()

        composeTestRule.onNodeWithText("Kotlin Lang").performClick()
        composeTestRule.waitForIdle()
        assertEquals("https://kotlinlang.org", selectedUrl)
    }

    @Test
    fun `bookmarks panel renders saved bookmarks`() {
        var dismissed = false
        var selectedUrl: String? = null
        val bookmarks = listOf(
            BookmarkItem(id = "b-1", title = "GitHub", url = "https://github.com"),
            BookmarkItem(id = "b-2", title = "DuckDuckGo", url = "https://duckduckgo.com")
        )

        composeTestRule.setContent {
            BrowserBookmarksSheet(
                visible = true,
                bookmarksList = bookmarks,
                currentPageUrl = "https://example.com",
                currentPageTitle = "Example",
                onSelectBookmark = { selectedUrl = it },
                onAddCurrentPage = {},
                onDeleteBookmark = {},
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("bookmarks_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bookmarks").assertIsDisplayed()
        composeTestRule.onNodeWithText("GitHub").assertIsDisplayed()

        composeTestRule.onNodeWithText("GitHub").performClick()
        composeTestRule.waitForIdle()
        assertEquals("https://github.com", selectedUrl)
    }

    @Test
    fun `privacy sheet displays connection security and shield controls`() {
        var dismissed = false
        var adBlockToggled = false

        composeTestRule.setContent {
            BrowserPrivacySheet(
                visible = true,
                pageUrl = "https://secure-site.com",
                isAdBlockerActive = true,
                blockedAdsOnPage = 4,
                totalBlockedAds = 142,
                onToggleAdBlocker = { adBlockToggled = true },
                onClearCache = {},
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("privacy_bottom_sheet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Privacy & Shield").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connection is Encrypted").assertIsDisplayed()
        composeTestRule.onNodeWithTag("privacy_bottom_sheet_close_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(dismissed)
    }

    @Test
    fun `reusable glass card supports custom background and click ripple`() {
        var clicked = false

        composeTestRule.setContent {
            GlassCard(
                onClick = { clicked = true }
            ) {
                Text("Card Inner Text")
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Card Inner Text").assertIsDisplayed()
        composeTestRule.onNodeWithText("Card Inner Text").performClick()
        composeTestRule.waitForIdle()
        assertTrue("Card click must fire", clicked)
    }

    @Test
    fun `download confirmation dialog renders with glass tokens and handles confirm`() {
        var confirmed = false
        var dismissed = false

        val testMedia = DetectedMedia(
            id = "media-confirm-test",
            mediaUrl = "https://example.com/test.mp4",
            pageUrl = "https://example.com",
            title = "Test Video",
            mimeType = "video/mp4",
            format = "MP4",
            quality = "1080p",
            contentLength = 12000000L
        )

        composeTestRule.setContent {
            DownloadConfirmationDialog(
                media = testMedia,
                fileNameInput = "my_video.mp4",
                useSystemDownloadManager = false,
                onFileNameChange = {},
                onToggleSystemDownloadManager = {},
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("download_confirmation_dialog_surface").assertIsDisplayed()
        composeTestRule.onNodeWithText("Confirm Video Download").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_confirm_download_button").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(confirmed)
    }

    @Test
    fun `glass dropdown panel renders floating translucent menu`() {
        var itemClicked = false

        composeTestRule.setContent {
            com.example.videobrowser.ui.glass.GlassDropdownPanel(
                expanded = true,
                onDismissRequest = {}
            ) {
                com.example.videobrowser.ui.motion.AnimatedDropdownMenuItem(
                    text = "Glass Menu Action",
                    onClick = { itemClicked = true }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Glass Menu Action").assertIsDisplayed()
        composeTestRule.onNodeWithText("Glass Menu Action").performClick()
        composeTestRule.waitForIdle()
        assertTrue(itemClicked)
    }
}
