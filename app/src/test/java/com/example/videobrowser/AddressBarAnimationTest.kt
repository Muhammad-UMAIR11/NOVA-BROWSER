package com.example.videobrowser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.videobrowser.ui.components.BrowserTopBar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AddressBarAnimationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `address bar renders and handles input and clear smoothly`() {
        var currentUrl by mutableStateOf("https://example.com")
        var isRefreshed by mutableStateOf(false)

        composeTestRule.setContent {
            BrowserTopBar(
                urlInput = currentUrl,
                isLoading = false,
                progress = 0,
                activeDownloadCount = 0,
                tabCount = 1,
                onUrlChange = { currentUrl = it },
                onSubmitUrl = {},
                onRefreshOrStop = { isRefreshed = true },
                onOpenDownloads = {},
                onOpenTabs = {}
            )
        }

        // Verify Lock icon is present for HTTPS
        composeTestRule.onNodeWithContentDescription("Secure Connection").assertIsDisplayed()

        // Focus the URL input field and type
        val urlField = composeTestRule.onNodeWithTag("url_input_field")
        urlField.assertIsDisplayed()
        urlField.performClick()
        composeTestRule.waitForIdle()

        // Change URL to non-HTTPS search
        currentUrl = "kotlin compose"
        composeTestRule.waitForIdle()

        // Verify Search icon morphs in
        composeTestRule.onNodeWithContentDescription("Search Status").assertIsDisplayed()

        // Clear button should be visible when input is not empty and focused
        val clearButton = composeTestRule.onNodeWithContentDescription("Clear address")
        clearButton.assertIsDisplayed()
        clearButton.performClick()
        composeTestRule.waitForIdle()

        // Verify URL cleared
        assertEquals("", currentUrl)
    }

    @Test
    fun `loading state renders animated progress bar and refresh stop morph`() {
        var isLoading by mutableStateOf(true)

        composeTestRule.setContent {
            BrowserTopBar(
                urlInput = "https://example.com",
                isLoading = isLoading,
                progress = 45,
                activeDownloadCount = 0,
                tabCount = 1,
                onUrlChange = {},
                onSubmitUrl = {},
                onRefreshOrStop = { isLoading = false },
                onOpenDownloads = {},
                onOpenTabs = {}
            )
        }

        // When loading, Stop loading icon is shown
        composeTestRule.onNodeWithContentDescription("Stop loading").assertIsDisplayed()

        // Toggle loading
        isLoading = false
        composeTestRule.waitForIdle()

        // When not loading, Refresh page icon is shown
        composeTestRule.onNodeWithContentDescription("Refresh page").assertIsDisplayed()
    }
}
