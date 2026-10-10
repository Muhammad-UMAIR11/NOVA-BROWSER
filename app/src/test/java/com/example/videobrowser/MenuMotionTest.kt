package com.example.videobrowser

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.ui.components.DownloadConfirmationDialog
import com.example.videobrowser.ui.motion.AnimatedDropdownMenuItem
import com.example.videobrowser.ui.motion.AnimatedDropdownPanel
import com.example.videobrowser.ui.motion.MenuMotionSpecs
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MenuMotionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `menu motion specs provide valid transitions`() {
        val menuEnter = MenuMotionSpecs.menuEnter(isReducedMotion = false)
        val menuExit = MenuMotionSpecs.menuExit(isReducedMotion = false)
        assertNotNull(menuEnter)
        assertNotNull(menuExit)

        val dropdownEnter = MenuMotionSpecs.dropdownEnter(isReducedMotion = false)
        val dropdownExit = MenuMotionSpecs.dropdownExit(isReducedMotion = false)
        assertNotNull(dropdownEnter)
        assertNotNull(dropdownExit)

        val dialogEnter = MenuMotionSpecs.dialogEnter(isReducedMotion = false)
        val dialogExit = MenuMotionSpecs.dialogExit(isReducedMotion = false)
        assertNotNull(dialogEnter)
        assertNotNull(dialogExit)

        // Reduced motion
        val rmEnter = MenuMotionSpecs.dropdownEnter(isReducedMotion = true)
        val rmExit = MenuMotionSpecs.dropdownExit(isReducedMotion = true)
        assertNotNull(rmEnter)
        assertNotNull(rmExit)
    }

    @Test
    fun `animated dropdown panel renders and fires item actions`() {
        var isExpanded by mutableStateOf(true)
        var actionFired by mutableStateOf(false)

        composeTestRule.setContent {
            AnimatedDropdownPanel(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false }
            ) {
                AnimatedDropdownMenuItem(
                    text = "Close Tab",
                    icon = Icons.Default.Close,
                    onClick = { actionFired = true }
                )
                AnimatedDropdownMenuItem(
                    text = "Copy Address",
                    icon = Icons.Default.ContentCopy,
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Close Tab").assertIsDisplayed()
        composeTestRule.onNodeWithText("Copy Address").assertIsDisplayed()

        composeTestRule.onNodeWithText("Close Tab").performClick()
        composeTestRule.waitForIdle()
        assertTrue(actionFired)
    }

    @Test
    fun `download confirmation dialog renders with animated surface and handles buttons`() {
        var confirmed by mutableStateOf(false)
        var dismissed by mutableStateOf(false)

        val testMedia = DetectedMedia(
            id = "test-1",
            mediaUrl = "https://example.com/video.mp4",
            pageUrl = "https://example.com",
            title = "Sample Video",
            format = "MP4",
            quality = "1080p",
            contentLength = 1024L * 1024L * 12L,
            mimeType = "video/mp4"
        )

        composeTestRule.setContent {
            DownloadConfirmationDialog(
                media = testMedia,
                fileNameInput = "sample_video",
                useSystemDownloadManager = false,
                onFileNameChange = {},
                onToggleSystemDownloadManager = {},
                onConfirm = { confirmed = true },
                onDismiss = { dismissed = true }
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("download_confirmation_dialog_surface").assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_confirm_download_button").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("dialog_cancel_button").performScrollTo().assertIsDisplayed()

        composeTestRule.onNodeWithTag("dialog_confirm_download_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue(confirmed)
    }
}
