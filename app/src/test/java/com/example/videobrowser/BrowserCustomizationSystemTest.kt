package com.example.videobrowser

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ui.theme.createColorSchemeFromBrowserTheme
import com.example.ui.theme.getComposeFontFamily
import com.example.videobrowser.data.BrowserSettingsManager
import com.example.videobrowser.model.AppFontFamily
import com.example.videobrowser.model.ColorTheme
import com.example.videobrowser.model.CustomSearchEngine
import com.example.videobrowser.model.SearchEngine
import com.example.videobrowser.model.ThemeMode
import com.example.videobrowser.model.theme.AnimationIntensity
import com.example.videobrowser.model.theme.BrowserTheme
import com.example.videobrowser.model.theme.UiDensity
import com.example.videobrowser.viewmodel.BrowserViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BrowserCustomizationSystemTest {

    private lateinit var app: Application
    private lateinit var settingsManager: BrowserSettingsManager
    private lateinit var viewModel: BrowserViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        settingsManager = BrowserSettingsManager(app)
        settingsManager.fullFactoryReset()
        viewModel = BrowserViewModel(app)
    }

    @Test
    fun `built in themes are present and valid`() {
        val themes = BrowserTheme.BUILT_IN_THEMES
        assertEquals(7, themes.size)

        val names = themes.map { it.name }
        assertTrue(names.contains("Midnight"))
        assertTrue(names.contains("Aurora"))
        assertTrue(names.contains("Obsidian"))
        assertTrue(names.contains("Cyberpunk"))
        assertTrue(names.contains("Arctic"))
        assertTrue(names.contains("Emerald"))
        assertTrue(names.contains("Solar"))

        // Verify ColorScheme creation for every theme
        themes.forEach { theme ->
            val colorScheme = createColorSchemeFromBrowserTheme(theme)
            assertNotNull(colorScheme.primary)
            assertNotNull(colorScheme.surface)
            assertNotNull(colorScheme.background)
        }
    }

    @Test
    fun `theme switching updates and persists across instances`() {
        // Switch to Aurora
        viewModel.setBrowserTheme(BrowserTheme.AURORA)
        assertEquals(BrowserTheme.AURORA.id, viewModel.uiState.value.activeBrowserTheme.id)
        assertEquals("Aurora", viewModel.uiState.value.activeBrowserTheme.name)

        // Verify saved in settingsManager
        val freshManager = BrowserSettingsManager(app)
        assertEquals(BrowserTheme.AURORA.id, freshManager.getActiveThemeId())

        // Recreate ViewModel and verify restored from persistence
        val freshViewModel = BrowserViewModel(app)
        assertEquals(BrowserTheme.AURORA.id, freshViewModel.uiState.value.activeBrowserTheme.id)
    }

    @Test
    fun `custom theme lifecycle - creation, duplicate, export, import, delete`() {
        val customTheme = BrowserTheme(
            id = "custom_neon_1",
            name = "Neon Synth",
            isCustom = true,
            isDark = true,
            primaryColor = 0xFFFF0055,
            onPrimaryColor = 0xFFFFFFFF,
            secondaryColor = 0xFF00FFCC,
            surfaceColor = 0xFF10081E,
            onSurfaceColor = 0xFFFFFFFF,
            backgroundColor = 0xFF050010,
            onBackgroundColor = 0xFFFFFFFF,
            toolbarColor = 0xFF180A2A,
            navigationBarColor = 0xFF180A2A,
            borderColor = 0xFF3D1863,
            secondaryTextColor = 0xFFB388FF,
            accentColor = 0xFFFF0055
        )

        // 1. Save
        viewModel.saveCustomTheme(customTheme)
        assertTrue(viewModel.uiState.value.customThemes.any { it.id == "custom_neon_1" })
        assertEquals("custom_neon_1", viewModel.uiState.value.activeBrowserTheme.id)

        // 2. Export JSON
        val exportedJson = viewModel.exportThemesJson()
        assertTrue(exportedJson.contains("Neon Synth"))

        // 3. Delete
        viewModel.deleteCustomTheme("custom_neon_1")
        assertFalse(viewModel.uiState.value.customThemes.any { it.id == "custom_neon_1" })

        // 4. Import JSON back
        val importSuccess = viewModel.importThemesJson(exportedJson)
        assertTrue(importSuccess)
        assertTrue(viewModel.uiState.value.customThemes.any { it.name == "Neon Synth" })
    }

    @Test
    fun `search engine manager - built-ins and custom engine template formatting`() {
        val engines = CustomSearchEngine.BUILT_IN_ENGINES
        assertEquals(6, engines.size)

        // Test template with %s
        val custom1 = CustomSearchEngine(
            id = "startpage",
            displayName = "Startpage",
            searchUrlTemplate = "https://www.startpage.com/sp/search?query=%s",
            homeUrl = "https://www.startpage.com"
        )
        val formatted1 = custom1.buildSearchUrl("android jetpack")
        assertEquals("https://www.startpage.com/sp/search?query=android%20jetpack", formatted1)

        // Test template with {searchTerms}
        val custom2 = CustomSearchEngine(
            id = "kagi",
            displayName = "Kagi",
            searchUrlTemplate = "https://kagi.com/search?q={searchTerms}",
            homeUrl = "https://kagi.com"
        )
        val formatted2 = custom2.buildSearchUrl("kotlin flows")
        assertEquals("https://kagi.com/search?q=kotlin%20flows", formatted2)

        // Test add to ViewModel and submitUrl
        viewModel.saveCustomSearchEngine(custom1)
        viewModel.setActiveCustomEngine(custom1)
        assertEquals(custom1.id, viewModel.uiState.value.activeSearchEngine.id)

        viewModel.submitUrl("compose testing")
        val activeTab = viewModel.uiState.value.activeTab
        assertEquals("https://www.startpage.com/sp/search?query=compose%20testing", activeTab?.url)
    }

    @Test
    fun `typography customization and font family mappings`() {
        // Test all font options resolve valid FontFamily
        AppFontFamily.entries.forEach { font ->
            val family = getComposeFontFamily(font)
            assertNotNull(family)
        }

        // Test font scale update
        viewModel.setFontScale(1.25f)
        assertEquals(1.25f, viewModel.uiState.value.fontScale, 0.001f)

        // Test font weight adjustment
        viewModel.setFontWeightDelta(1)
        assertEquals(1, viewModel.uiState.value.fontWeightDelta)

        // Test font selection and persistence
        viewModel.setFontFamily(AppFontFamily.INTER)
        assertEquals(AppFontFamily.INTER, viewModel.uiState.value.appFontFamily)

        val freshViewModel = BrowserViewModel(app)
        assertEquals(AppFontFamily.INTER, freshViewModel.uiState.value.appFontFamily)
        assertEquals(1.25f, freshViewModel.uiState.value.fontScale, 0.001f)
    }

    @Test
    fun `appearance options - density, corner radius, and motion settings`() {
        viewModel.setUiDensity(UiDensity.SPACIOUS)
        assertEquals(UiDensity.SPACIOUS, viewModel.uiState.value.uiDensity)

        viewModel.setCornerRadius(18)
        assertEquals(18, viewModel.uiState.value.cornerRadiusDp)

        viewModel.setPanelTransparency(0.75f)
        assertEquals(0.75f, viewModel.uiState.value.panelTransparency, 0.01f)

        viewModel.setAnimationIntensity(AnimationIntensity.SMOOTH)
        assertEquals(AnimationIntensity.SMOOTH, viewModel.uiState.value.animationIntensity)

        viewModel.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `category reset and full factory reset restore clean state`() {
        // Mutate multiple settings
        viewModel.setBrowserTheme(BrowserTheme.CYBERPUNK)
        viewModel.setFontScale(1.30f)
        viewModel.setUiDensity(UiDensity.COMPACT)

        // Reset theme and colors only
        viewModel.resetThemeAndColors()
        assertEquals(BrowserTheme.MIDNIGHT.id, viewModel.uiState.value.activeBrowserTheme.id)
        // Typography should NOT be wiped by theme reset
        assertEquals(1.30f, viewModel.uiState.value.fontScale, 0.001f)

        // Full Factory Reset
        viewModel.performFullFactoryReset()
        assertEquals(BrowserTheme.MIDNIGHT.id, viewModel.uiState.value.activeBrowserTheme.id)
        assertEquals(1.0f, viewModel.uiState.value.fontScale, 0.001f)
        assertEquals(UiDensity.COMFORTABLE, viewModel.uiState.value.uiDensity)
    }

    @Test
    fun `corrupted JSON recovers gracefully`() {
        val corruptedJson = "{ not a valid json [[[ "
        val success = viewModel.importThemesJson(corruptedJson)
        assertFalse(success)
        // Ensure state is uncorrupted
        assertNotNull(viewModel.uiState.value.activeBrowserTheme)
    }
}
