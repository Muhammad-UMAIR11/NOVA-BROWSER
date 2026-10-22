package com.example.videobrowser.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.getComposeFontFamily
import com.example.videobrowser.model.AppFontFamily
import com.example.videobrowser.model.ColorTheme
import com.example.videobrowser.model.CustomSearchEngine
import com.example.videobrowser.model.SearchEngine
import com.example.videobrowser.model.ThemeMode
import com.example.videobrowser.model.theme.AnimationIntensity
import com.example.videobrowser.model.theme.BrowserTheme
import com.example.videobrowser.model.theme.CornerRadiusOption
import com.example.videobrowser.model.theme.UiDensity
import com.example.videobrowser.ui.components.settings.AddSearchEngineDialog
import com.example.videobrowser.ui.components.settings.ColorPickerDialog
import com.example.videobrowser.ui.components.settings.CreateThemeDialog
import com.example.videobrowser.ui.components.settings.ThemeJsonDialog
import com.example.videobrowser.ui.glass.GlassBottomSheet
import com.example.videobrowser.ui.motion.LocalReducedMotion
import com.example.videobrowser.ui.motion.MotionDurations
import com.example.videobrowser.ui.motion.MotionEasing
import com.example.videobrowser.ui.motion.motionSpring
import com.example.videobrowser.ui.motion.motionTween
import com.example.videobrowser.viewmodel.BrowserViewModel
import kotlin.math.roundToInt

private enum class SettingsCategory(val title: String, val icon: ImageVector) {
    THEMES("Themes", Icons.Default.Palette),
    TYPOGRAPHY("Typography", Icons.Default.FontDownload),
    SEARCH("Search", Icons.Default.Search),
    APPEARANCE("Appearance", Icons.Default.Tune),
    PERFORMANCE("Performance", Icons.Default.Speed),
    PRIVACY("Privacy & Disk", Icons.Default.Security)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BrowserSettingsSheet(
    viewModel: BrowserViewModel? = null,
    sheetState: SheetState? = null,
    currentSearchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    currentThemeMode: ThemeMode = ThemeMode.SYSTEM,
    currentColorTheme: ColorTheme = ColorTheme.OCEAN_BLUE,
    currentFontFamily: AppFontFamily = AppFontFamily.SYSTEM_DEFAULT,
    customWallpaperPath: String? = null,
    isAdBlockerActive: Boolean = true,
    onSelectSearchEngine: (SearchEngine) -> Unit = {},
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onSelectColorTheme: (ColorTheme) -> Unit = {},
    onSelectFontFamily: (AppFontFamily) -> Unit = {},
    onUploadWallpaper: (Uri) -> Unit = {},
    onRemoveWallpaper: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onToggleAdBlocker: () -> Unit = {},
    onDismiss: () -> Unit,
    visible: Boolean = true
) {
    val haptic = LocalHapticFeedback.current
    val isReducedMotion = LocalReducedMotion.current

    // Observe rich ViewModel state if connected
    val uiState = viewModel?.uiState?.collectAsStateWithLifecycle()?.value

    var selectedTab by remember { mutableStateOf(SettingsCategory.THEMES) }

    // Live feedback pill
    var appliedNoticeText by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(appliedNoticeText) {
        if (appliedNoticeText != null) {
            kotlinx.coroutines.delay(1800)
            appliedNoticeText = null
        }
    }

    // Dialog controllers
    var showCreateThemeDialog by remember { mutableStateOf(false) }
    var editingSearchEngine by remember { mutableStateOf<CustomSearchEngine?>(null) }
    var showAddSearchEngineDialog by remember { mutableStateOf(false) }
    var showExportThemesDialog by remember { mutableStateOf(false) }
    var showImportThemesDialog by remember { mutableStateOf(false) }
    var activeColorPickerKey by remember { mutableStateOf<String?>(null) }
    var showFactoryResetDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadWallpaper(uri)
            appliedNoticeText = "Custom photo wallpaper applied"
        }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            onUploadWallpaper(uri)
            appliedNoticeText = "Custom wallpaper image set"
        }
    }

    val activeTheme = uiState?.activeBrowserTheme ?: BrowserTheme.MIDNIGHT
    val allThemes = uiState?.allThemes ?: BrowserTheme.BUILT_IN_THEMES
    val customThemes = uiState?.customThemes ?: emptyList()

    val activeEngine = uiState?.activeSearchEngine ?: CustomSearchEngine.BUILT_IN_ENGINES.first()
    val allEngines = uiState?.allSearchEngines ?: CustomSearchEngine.BUILT_IN_ENGINES

    val currentFont = uiState?.appFontFamily ?: currentFontFamily
    val fontScale = uiState?.fontScale ?: 1.0f
    val fontWeightDelta = uiState?.fontWeightDelta ?: 0
    val applyTextZoomToWeb = uiState?.applyTextZoomToWeb ?: true

    val uiDensity = uiState?.uiDensity ?: UiDensity.COMFORTABLE
    val cornerRadiusDp = uiState?.cornerRadiusDp ?: 12
    val panelTransparency = uiState?.panelTransparency ?: 0.88f
    val panelBlur = uiState?.panelBlurIntensity ?: 0.80f
    val animationIntensity = uiState?.animationIntensity ?: AnimationIntensity.BALANCED
    val themeMode = uiState?.themeMode ?: currentThemeMode

    val blockCookies = uiState?.blockCookies ?: true
    val strictPopups = uiState?.strictPopups ?: true
    val hardwareAccel = uiState?.hardwareAccel ?: true
    val lowEndMode = uiState?.lowEndMode ?: false
    val adBlockerActive = uiState?.isAdBlockerEnabled ?: isAdBlockerActive

    GlassBottomSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = "Nova Settings",
        subtitle = "Themes, Typography, Search & Appearance",
        icon = Icons.Default.Palette,
        testTag = "settings_bottom_sheet"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            // Live Floating Notification Banner
            AnimatedVisibility(
                visible = appliedNoticeText != null,
                enter = fadeIn(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)) +
                    slideInVertically(motionTween(MotionDurations.Fast, easing = MotionEasing.EmphasizedDecelerate, isReducedMotion = isReducedMotion)) { -it / 2 },
                exit = fadeOut(motionTween(MotionDurations.Micro, isReducedMotion = isReducedMotion)) +
                    slideOutVertically(motionTween(MotionDurations.Micro, easing = MotionEasing.EmphasizedAccelerate, isReducedMotion = isReducedMotion)) { -it / 2 },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.95f),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Applied",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = appliedNoticeText ?: "Setting saved",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD1FAE5)
                        )
                    }
                }
            }

            // Categories Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                edgePadding = 0.dp,
                divider = {},
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                SettingsCategory.entries.forEach { category ->
                    val isSelected = category == selectedTab
                    Tab(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedTab = category
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = category.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main Scrollable Category Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedTab) {
                    SettingsCategory.THEMES -> {
                        ThemesAndColorsTabContent(
                            activeTheme = activeTheme,
                            allThemes = allThemes,
                            customThemes = customThemes,
                            onSelectTheme = { theme ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel?.setBrowserTheme(theme)
                                appliedNoticeText = "Applied theme: ${theme.name}"
                            },
                            onCreateThemeClick = { showCreateThemeDialog = true },
                            onDeleteCustomTheme = { themeId ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel?.deleteCustomTheme(themeId)
                                appliedNoticeText = "Theme deleted"
                            },
                            onExportThemesClick = { showExportThemesDialog = true },
                            onImportThemesClick = { showImportThemesDialog = true },
                            onOpenColorPicker = { key -> activeColorPickerKey = key },
                            onResetThemeColors = {
                                viewModel?.resetThemeAndColors()
                                appliedNoticeText = "Theme & colors reset to defaults"
                            }
                        )
                    }

                    SettingsCategory.TYPOGRAPHY -> {
                        TypographyTabContent(
                            currentFontFamily = currentFont,
                            fontScale = fontScale,
                            fontWeightDelta = fontWeightDelta,
                            applyTextZoomToWeb = applyTextZoomToWeb,
                            onSelectFont = { font ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectFontFamily(font)
                                viewModel?.setFontFamily(font)
                                appliedNoticeText = "Applied font: ${font.displayName}"
                            },
                            onFontScaleChange = { scale ->
                                viewModel?.setFontScale(scale)
                                appliedNoticeText = "Font scale: ${(scale * 100).roundToInt()}%"
                            },
                            onFontWeightChange = { delta ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel?.setFontWeightDelta(delta)
                                appliedNoticeText = "Font weight adjusted"
                            },
                            onToggleApplyToWeb = { enabled ->
                                viewModel?.setApplyTextZoomToWeb(enabled)
                                appliedNoticeText = if (enabled) "Webpage zoom synchronized" else "Webpage zoom decoupled"
                            },
                            onResetTypography = {
                                viewModel?.resetTypography()
                                appliedNoticeText = "Typography reset to system defaults"
                            }
                        )
                    }

                    SettingsCategory.SEARCH -> {
                        SearchEnginesTabContent(
                            activeEngine = activeEngine,
                            allEngines = allEngines,
                            onSelectEngine = { engine ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel?.setActiveCustomEngine(engine)
                                SearchEngine.entries.find { it.id == engine.id }?.let { onSelectSearchEngine(it) }
                                appliedNoticeText = "Default search engine: ${engine.displayName}"
                            },
                            onAddEngineClick = {
                                editingSearchEngine = null
                                showAddSearchEngineDialog = true
                            },
                            onEditEngineClick = { engine ->
                                editingSearchEngine = engine
                                showAddSearchEngineDialog = true
                            },
                            onDeleteEngineClick = { engineId ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel?.deleteCustomSearchEngine(engineId)
                                appliedNoticeText = "Custom search engine deleted"
                            },
                            onRestoreDefaultEngines = {
                                viewModel?.restoreDefaultSearchEngines()
                                appliedNoticeText = "Search engines restored to defaults"
                            }
                        )
                    }

                    SettingsCategory.APPEARANCE -> {
                        AppearanceTabContent(
                            themeMode = themeMode,
                            uiDensity = uiDensity,
                            cornerRadiusDp = cornerRadiusDp,
                            panelTransparency = panelTransparency,
                            panelBlur = panelBlur,
                            animationIntensity = animationIntensity,
                            customWallpaperPath = customWallpaperPath,
                            onSelectThemeMode = { mode ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectThemeMode(mode)
                                viewModel?.setThemeMode(mode)
                                appliedNoticeText = "Appearance: ${mode.displayName}"
                            },
                            onSelectDensity = { density ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel?.setUiDensity(density)
                                appliedNoticeText = "Density: ${density.displayName}"
                            },
                            onSelectCornerRadius = { radius ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel?.setCornerRadius(radius)
                                appliedNoticeText = "Corner radius: ${radius}dp"
                            },
                            onTransparencyChange = { alpha ->
                                viewModel?.setPanelTransparency(alpha)
                                appliedNoticeText = "Transparency: ${(alpha * 100).roundToInt()}%"
                            },
                            onBlurChange = { blur ->
                                viewModel?.setPanelBlurIntensity(blur)
                                appliedNoticeText = "Blur intensity: ${(blur * 100).roundToInt()}%"
                            },
                            onSelectAnimationIntensity = { intensity ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel?.setAnimationIntensity(intensity)
                                appliedNoticeText = "Motion profile: ${intensity.displayName}"
                            },
                            onPickPhotoWallpaper = {
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            onPickFileWallpaper = {
                                filePickerLauncher.launch("image/*")
                            },
                            onResetWallpaper = {
                                onRemoveWallpaper()
                                appliedNoticeText = "Wallpaper reset"
                            },
                            onResetAdvanced = {
                                viewModel?.resetAdvancedSettings()
                                appliedNoticeText = "Appearance reset to defaults"
                            }
                        )
                    }

                    SettingsCategory.PERFORMANCE -> {
                        PerformanceTabContent(
                            lowEndMode = lowEndMode,
                            hardwareAccel = hardwareAccel,
                            animationIntensity = animationIntensity,
                            onToggleLowEndMode = { v ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel?.setLowEndMode(v)
                                appliedNoticeText = if (v) "⚡ Low-End Mode: Optimizations Active" else "Standard Visuals Restored"
                            },
                            onToggleHardwareAccel = { v ->
                                viewModel?.setHardwareAccel(v)
                                appliedNoticeText = if (v) "GPU Acceleration Enabled" else "Software Rendering Layer"
                            },
                            onSelectAnimationIntensity = { intensity ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel?.setAnimationIntensity(intensity)
                                appliedNoticeText = "Motion profile: ${intensity.displayName}"
                            },
                            onOptimizeMemory = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel?.optimizeMemoryAndPurgeCache()
                                appliedNoticeText = "Memory optimized & temporary cache purged"
                            }
                        )
                    }

                    SettingsCategory.PRIVACY -> {
                        PrivacyAndDiskTabContent(
                            isAdBlockerActive = adBlockerActive,
                            blockCookies = blockCookies,
                            strictPopups = strictPopups,
                            hardwareAccel = hardwareAccel,
                            onToggleAdBlocker = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleAdBlocker()
                                appliedNoticeText = if (!adBlockerActive) "Shield Enabled" else "Shield Paused"
                            },
                            onToggleBlockCookies = { v ->
                                viewModel?.setBlockCookies(v)
                                appliedNoticeText = if (v) "Cookie Guard Enabled" else "Cookie Guard Disabled"
                            },
                            onToggleStrictPopups = { v ->
                                viewModel?.setStrictPopups(v)
                                appliedNoticeText = if (v) "Strict Popups Blocked" else "Popup Rules Relaxed"
                            },
                            onToggleHardwareAccel = { v ->
                                viewModel?.setHardwareAccel(v)
                                appliedNoticeText = if (v) "GPU Acceleration Enabled" else "Software Rendering Layer"
                            },
                            onClearCache = {
                                onClearCache()
                                viewModel?.clearBrowserCache()
                                appliedNoticeText = "Browser disk cache purged"
                            },
                            onFactoryResetClick = { showFactoryResetDialog = true }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // --- Sub-Dialogs ---

    if (showCreateThemeDialog) {
        CreateThemeDialog(
            baseTheme = activeTheme,
            onDismiss = { showCreateThemeDialog = false },
            onSaveTheme = { newTheme ->
                viewModel?.saveCustomTheme(newTheme)
                appliedNoticeText = "Custom theme '${newTheme.name}' saved & applied"
            }
        )
    }

    if (showAddSearchEngineDialog) {
        AddSearchEngineDialog(
            initialEngine = editingSearchEngine,
            onDismiss = {
                showAddSearchEngineDialog = false
                editingSearchEngine = null
            },
            onSaveEngine = { engine ->
                viewModel?.saveCustomSearchEngine(engine)
                appliedNoticeText = "Search engine '${engine.displayName}' saved"
            }
        )
    }

    if (showExportThemesDialog) {
        val exportedJson = viewModel?.exportThemesJson() ?: "{}"
        ThemeJsonDialog(
            isExportMode = true,
            exportJsonString = exportedJson,
            onDismiss = { showExportThemesDialog = false }
        )
    }

    if (showImportThemesDialog) {
        ThemeJsonDialog(
            isExportMode = false,
            onDismiss = { showImportThemesDialog = false },
            onImportJson = { jsonStr ->
                val success = viewModel?.importThemesJson(jsonStr) ?: false
                appliedNoticeText = if (success) "Theme(s) imported successfully" else "Failed to parse theme JSON"
            }
        )
    }

    // Universal Component Color Picker Trigger
    activeColorPickerKey?.let { key ->
        val currentColorLong = when (key) {
            "primary" -> activeTheme.primaryColor
            "secondary" -> activeTheme.secondaryColor
            "background" -> activeTheme.backgroundColor
            "surface" -> activeTheme.surfaceColor
            "toolbar" -> activeTheme.toolbarColor
            "navigationBar" -> activeTheme.navigationBarColor
            "text" -> activeTheme.onSurfaceColor
            "secondaryText" -> activeTheme.secondaryTextColor
            "border" -> activeTheme.borderColor
            else -> activeTheme.primaryColor
        }

        val pickerTitle = when (key) {
            "primary" -> "Primary Accent Color"
            "secondary" -> "Secondary Accent Color"
            "background" -> "Canvas Background Color"
            "surface" -> "Panel / Dialog Surface"
            "toolbar" -> "Toolbar & Address Bar"
            "navigationBar" -> "Bottom Navigation Bar"
            "text" -> "Headings & Primary Text"
            "secondaryText" -> "Secondary Captions Text"
            "border" -> "Border & Divider Color"
            else -> "Component Color"
        }

        ColorPickerDialog(
            title = pickerTitle,
            initialColor = currentColorLong,
            supportsAlpha = true,
            onDismiss = { activeColorPickerKey = null },
            onColorSelected = { newColorLong ->
                val updatedTheme = when (key) {
                    "primary" -> activeTheme.copy(primaryColor = newColorLong, accentColor = newColorLong)
                    "secondary" -> activeTheme.copy(secondaryColor = newColorLong)
                    "background" -> activeTheme.copy(backgroundColor = newColorLong)
                    "surface" -> activeTheme.copy(surfaceColor = newColorLong)
                    "toolbar" -> activeTheme.copy(toolbarColor = newColorLong)
                    "navigationBar" -> activeTheme.copy(navigationBarColor = newColorLong)
                    "text" -> activeTheme.copy(onSurfaceColor = newColorLong)
                    "secondaryText" -> activeTheme.copy(secondaryTextColor = newColorLong)
                    "border" -> activeTheme.copy(borderColor = newColorLong)
                    else -> activeTheme
                }
                viewModel?.saveCustomTheme(updatedTheme.copy(isCustom = true))
                appliedNoticeText = "Updated $pickerTitle"
            }
        )
    }

    // Factory Reset Confirmation Dialog
    if (showFactoryResetDialog) {
        AlertDialog(
            onDismissRequest = { showFactoryResetDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Reset All Browser Settings?") },
            text = {
                Text("This will restore themes, fonts, search engines, and appearance configurations back to factory defaults.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel?.performFullFactoryReset()
                        showFactoryResetDialog = false
                        appliedNoticeText = "Factory settings restored"
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirm Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFactoryResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ==========================================
// TAB 1: THEMES & UNIVERSAL COLOR STUDIO
// ==========================================

@Composable
private fun ThemesAndColorsTabContent(
    activeTheme: BrowserTheme,
    allThemes: List<BrowserTheme>,
    customThemes: List<BrowserTheme>,
    onSelectTheme: (BrowserTheme) -> Unit,
    onCreateThemeClick: () -> Unit,
    onDeleteCustomTheme: (String) -> Unit,
    onExportThemesClick: () -> Unit,
    onImportThemesClick: () -> Unit,
    onOpenColorPicker: (String) -> Unit,
    onResetThemeColors: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Built-in Themes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Crafted palettes with verified contrast and depth",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onCreateThemeClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("create_custom_theme_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Grid of Built-in Themes with Live Previews
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BrowserTheme.BUILT_IN_THEMES.forEach { theme ->
                ThemePreviewCard(
                    theme = theme,
                    isSelected = theme.id == activeTheme.id,
                    onClick = { onSelectTheme(theme) }
                )
            }
        }

        // Custom Themes (if any)
        if (customThemes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Your Custom Themes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(10.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                customThemes.forEach { theme ->
                    ThemePreviewCard(
                        theme = theme,
                        isSelected = theme.id == activeTheme.id,
                        onClick = { onSelectTheme(theme) },
                        onDeleteClick = { onDeleteCustomTheme(theme.id) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Import / Export Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onExportThemesClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Export JSON", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onImportThemesClick,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Import JSON", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // Universal Color Studio (Component Color Controls)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Universal Color Studio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Customize individual browser components",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onResetThemeColors) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset colors",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        ComponentColorSlotRow("Primary Accent & Focus", activeTheme.primaryColor) { onOpenColorPicker("primary") }
        ComponentColorSlotRow("Secondary Highlight", activeTheme.secondaryColor) { onOpenColorPicker("secondary") }
        ComponentColorSlotRow("Canvas Background", activeTheme.backgroundColor) { onOpenColorPicker("background") }
        ComponentColorSlotRow("Panels & Sheets Surface", activeTheme.surfaceColor) { onOpenColorPicker("surface") }
        ComponentColorSlotRow("Toolbar & Address Bar", activeTheme.toolbarColor) { onOpenColorPicker("toolbar") }
        ComponentColorSlotRow("Bottom Navigation Bar", activeTheme.navigationBarColor) { onOpenColorPicker("navigationBar") }
        ComponentColorSlotRow("Headings & Primary Text", activeTheme.onSurfaceColor) { onOpenColorPicker("text") }
        ComponentColorSlotRow("Subtitles & Captions", activeTheme.secondaryTextColor) { onOpenColorPicker("secondaryText") }
        ComponentColorSlotRow("Borders & Dividers", activeTheme.borderColor) { onOpenColorPicker("border") }
    }
}

@Composable
private fun ThemePreviewCard(
    theme: BrowserTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val isReducedMotion = LocalReducedMotion.current
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected && !isReducedMotion) 1.015f else 1.0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "themeScale_${theme.id}"
    )

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(theme.backgroundColor),
        border = BorderStroke(
            if (isSelected) 2.dp else 1.dp,
            if (isSelected) Color(theme.primaryColor) else Color(theme.borderColor).copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("theme_card_${theme.id}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Mock Browser Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(theme.toolbarColor))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color(theme.primaryColor))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "nova://newtab",
                        color = Color(theme.onSurfaceColor).copy(alpha = 0.9f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(Color(theme.secondaryColor).copy(alpha = 0.7f))
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Information Row & Mini Page Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = theme.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(theme.onSurfaceColor)
                        )
                        if (theme.isCustom) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(theme.primaryColor).copy(alpha = 0.25f)
                            ) {
                                Text(
                                    text = "CUSTOM",
                                    color = Color(theme.primaryColor),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = if (theme.isDark) "Dark mode palette" else "Light daylight mode",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(theme.secondaryTextColor),
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = Color(theme.primaryColor),
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color(theme.onPrimaryColor),
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }

                    if (onDeleteClick != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete theme",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ComponentColorSlotRow(
    label: String,
    colorLong: Long,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.ColorLens,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

// ==========================================
// TAB 2: TYPOGRAPHY CUSTOMIZATION
// ==========================================

@Composable
private fun TypographyTabContent(
    currentFontFamily: AppFontFamily,
    fontScale: Float,
    fontWeightDelta: Int,
    applyTextZoomToWeb: Boolean,
    onSelectFont: (AppFontFamily) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onFontWeightChange: (Int) -> Unit,
    onToggleApplyToWeb: (Boolean) -> Unit,
    onResetTypography: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Typography & Fonts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "High-fidelity bundled Google Fonts & UI text scaling",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onResetTypography) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset fonts", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Font Family List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AppFontFamily.entries.forEach { fontOption ->
                val isSelected = fontOption == currentFontFamily
                val composeFamily = getComposeFontFamily(fontOption)

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectFont(fontOption) }
                        .testTag("font_${fontOption.name.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = fontOption.displayName,
                                fontFamily = composeFamily,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = fontOption.description,
                                fontFamily = composeFamily,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // Text Scale Slider
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Interface Text Scale",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "${(fontScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Slider(
            value = fontScale,
            onValueChange = onFontScaleChange,
            valueRange = 0.85f..1.35f,
            steps = 9,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Font Weight Delta
        Text(
            text = "Font Weight Tuning",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(-1 to "Light", 0 to "Regular", 1 to "Medium", 2 to "Bold").forEach { (delta, label) ->
                val isSelected = fontWeightDelta == delta
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onFontWeightChange(delta) }
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Synchronize with Web Pages Toggle
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggleApplyToWeb(!applyTextZoomToWeb) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Synchronize Web Page Zoom",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Applies text scale proportionally to rendered websites in WebView",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = applyTextZoomToWeb,
                    onCheckedChange = onToggleApplyToWeb
                )
            }
        }
    }
}

// ==========================================
// TAB 3: SEARCH ENGINES
// ==========================================

@Composable
private fun SearchEnginesTabContent(
    activeEngine: CustomSearchEngine,
    allEngines: List<CustomSearchEngine>,
    onSelectEngine: (CustomSearchEngine) -> Unit,
    onAddEngineClick: () -> Unit,
    onEditEngineClick: (CustomSearchEngine) -> Unit,
    onDeleteEngineClick: (String) -> Unit,
    onRestoreDefaultEngines: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Search Engine Manager",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Active default provider for omnibox queries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Button(
                onClick = onAddEngineClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.testTag("add_custom_engine_button")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Custom", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            allEngines.forEach { engine ->
                val isSelected = engine.id == activeEngine.id

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        if (isSelected) 1.5.dp else 1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectEngine(engine) }
                        .testTag("search_engine_${engine.id}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = engine.displayName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                if (!engine.isBuiltIn) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = "CUSTOM",
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = engine.hint,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!engine.isBuiltIn) {
                                IconButton(
                                    onClick = { onEditEngineClick(engine) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit engine",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteEngineClick(engine.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete engine",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (isSelected) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedButton(
            onClick = onRestoreDefaultEngines,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Restore Default Engines", fontSize = 12.sp)
        }
    }
}

// ==========================================
// TAB 4: APPEARANCE & MOTION
// ==========================================

@Composable
private fun AppearanceTabContent(
    themeMode: ThemeMode,
    uiDensity: UiDensity,
    cornerRadiusDp: Int,
    panelTransparency: Float,
    panelBlur: Float,
    animationIntensity: AnimationIntensity,
    customWallpaperPath: String?,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onSelectDensity: (UiDensity) -> Unit,
    onSelectCornerRadius: (Int) -> Unit,
    onTransparencyChange: (Float) -> Unit,
    onBlurChange: (Float) -> Unit,
    onSelectAnimationIntensity: (AnimationIntensity) -> Unit,
    onPickPhotoWallpaper: () -> Unit,
    onPickFileWallpaper: () -> Unit,
    onResetWallpaper: () -> Unit,
    onResetAdvanced: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Appearance & Motion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Layout density, geometry, transparency & motion tuning",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onResetAdvanced) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset appearance", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Appearance Mode (System / Bright / Dark)
        Text(
            text = "Light / Dark Appearance Mode",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeMode.entries.forEach { mode ->
                val isSelected = mode == themeMode
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectThemeMode(mode) }
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val icon = when (mode) {
                            ThemeMode.BRIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                        }
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = mode.displayName.split(" ").first(), style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // UI Density
        Text(
            text = "Interface Density",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            UiDensity.entries.forEach { density ->
                val isSelected = density == uiDensity
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectDensity(density) }
                ) {
                    Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = density.displayName, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Corner Radius
        Text(
            text = "Component Corner Radius",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CornerRadiusOption.entries.forEach { opt ->
                val isSelected = cornerRadiusDp == opt.cornerRadiusDp
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelectCornerRadius(opt.cornerRadiusDp) }
                ) {
                    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(text = opt.displayName, style = MaterialTheme.typography.labelSmall, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 10.5.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transparency & Blur Sliders
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Glass Surface Transparency", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "${(panelTransparency * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = panelTransparency,
            onValueChange = onTransparencyChange,
            valueRange = 0.40f..1.0f,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Backdrop Blur Intensity", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "${(panelBlur * 100).roundToInt()}%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = panelBlur,
            onValueChange = onBlurChange,
            valueRange = 0.0f..1.0f,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Animation Intensity
        Text(
            text = "Animation & Transition Feel",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimationIntensity.entries.forEach { intensity ->
                val isSelected = animationIntensity == intensity
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectAnimationIntensity(intensity) }
                ) {
                    Column(modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = intensity.displayName, style = MaterialTheme.typography.labelMedium, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Custom Wallpaper
        Text(
            text = "Custom Wallpaper",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onPickPhotoWallpaper,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Photos", fontSize = 12.sp)
            }

            OutlinedButton(
                onClick = onPickFileWallpaper,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Files", fontSize = 12.sp)
            }
        }

        if (customWallpaperPath != null) {
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onResetWallpaper,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Custom Wallpaper", fontSize = 12.sp)
            }
        }
    }
}

// ==========================================
// TAB 5: PRIVACY & DISK MAINTENANCE
// ==========================================

@Composable
private fun PrivacyAndDiskTabContent(
    isAdBlockerActive: Boolean,
    blockCookies: Boolean,
    strictPopups: Boolean,
    hardwareAccel: Boolean,
    onToggleAdBlocker: () -> Unit,
    onToggleBlockCookies: (Boolean) -> Unit,
    onToggleStrictPopups: (Boolean) -> Unit,
    onToggleHardwareAccel: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    onFactoryResetClick: () -> Unit
) {
    val isReducedMotion = LocalReducedMotion.current

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Privacy & Rendering Rules",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Always-on shield, cookie guards, hardware acceleration",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Ad Blocker Toggle Card
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isAdBlockerActive) Color(0xFF064E3B).copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            border = BorderStroke(1.2.dp, if (isAdBlockerActive) Color(0xFF10B981).copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth().testTag("settings_ad_blocker_card")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (isAdBlockerActive) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = if (isAdBlockerActive) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(8.dp).size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = "Always-On Ad Blocker", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (isAdBlockerActive) Color(0xFF10B981) else Color(0xFF64748B)
                            ) {
                                Text(
                                    text = if (isAdBlockerActive) "PRIMARY" else "OFF",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isAdBlockerActive) Color(0xFF022C22) else Color.White,
                                    fontSize = 9.sp,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(text = "Blocks trackers & banner overlays automatically", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
                    }
                }

                Switch(
                    checked = isAdBlockerActive,
                    onCheckedChange = { onToggleAdBlocker() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF022C22),
                        checkedTrackColor = Color(0xFF10B981)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Checkboxes
        SettingsCheckboxItem(
            checked = blockCookies,
            onCheckedChange = onToggleBlockCookies,
            title = "Block Third-Party Tracking Cookies",
            subtitle = "Protects your browsing identity from advertising trackers"
        )

        Spacer(modifier = Modifier.height(6.dp))

        SettingsCheckboxItem(
            checked = strictPopups,
            onCheckedChange = onToggleStrictPopups,
            title = "Strict Popup & Redirect Prevention",
            subtitle = "Suppresses unsolicited modals, redirection loops, and banner popups"
        )

        Spacer(modifier = Modifier.height(6.dp))

        SettingsCheckboxItem(
            checked = hardwareAccel,
            onCheckedChange = onToggleHardwareAccel,
            title = "GPU Hardware Acceleration",
            subtitle = "Utilizes device GPU hardware for 60fps web rendering"
        )

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        Spacer(modifier = Modifier.height(16.dp))

        // Disk Cache Maintenance
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Browser Disk Cache",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Purge cached web graphics, scripts, and temporary files",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            var isClearing by remember { mutableStateOf(false) }
            LaunchedEffect(isClearing) {
                if (isClearing) {
                    kotlinx.coroutines.delay(650)
                    isClearing = false
                }
            }

            Button(
                onClick = {
                    isClearing = true
                    onClearCache()
                },
                enabled = !isClearing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("clear_cache_button")
            ) {
                if (isClearing) {
                    AsyncOperationProgressIndicator(
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Purging…", fontSize = 12.sp)
                } else {
                    Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Purge Cache", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Factory Reset Section
        OutlinedButton(
            onClick = onFactoryResetClick,
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("factory_reset_button")
        ) {
            Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Full Factory Reset", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsCheckboxItem(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    subtitle: String
) {
    val isReducedMotion = LocalReducedMotion.current
    val checkScale by animateFloatAsState(
        targetValue = 1.0f,
        animationSpec = motionSpring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium, isReducedMotion = isReducedMotion),
        label = "chkScale"
    )

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (checked) 0.35f else 0.18f),
        border = BorderStroke(1.dp, if (checked) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else Color.Transparent),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (checked) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .border(1.5.dp, if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = checked,
                    enter = scaleIn(motionSpring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium, isReducedMotion = isReducedMotion)) + fadeIn(motionTween(MotionDurations.Fast, isReducedMotion = isReducedMotion)),
                    exit = scaleOut(motionTween(MotionDurations.Micro, isReducedMotion = isReducedMotion)) + fadeOut(motionTween(MotionDurations.Micro, isReducedMotion = isReducedMotion))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Checked",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (checked) FontWeight.Bold else FontWeight.Medium,
                    color = if (checked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.5.sp
                )
            }
        }
    }
}

@Composable
private fun PerformanceTabContent(
    lowEndMode: Boolean,
    hardwareAccel: Boolean,
    animationIntensity: AnimationIntensity,
    onToggleLowEndMode: (Boolean) -> Unit,
    onToggleHardwareAccel: (Boolean) -> Unit,
    onSelectAnimationIntensity: (AnimationIntensity) -> Unit,
    onOptimizeMemory: () -> Unit
) {
    val context = LocalContext.current
    val activityManager = remember(context) {
        context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    }
    val isLowRamDevice = remember(activityManager) {
        activityManager?.isLowRamDevice == true
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Hero Performance Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (lowEndMode) {
                Color(0xFF064E3B).copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            },
            border = BorderStroke(
                1.5.dp,
                if (lowEndMode) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("low_end_mode_hero_card")
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (lowEndMode) Color(0xFF10B981) else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = null,
                                    tint = if (lowEndMode) Color(0xFF022C22) else MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Low-End Device Mode",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (lowEndMode) Color(0xFF10B981).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Text(
                                    text = if (lowEndMode) "⚡ Optimizations Active" else "Standard Mode",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (lowEndMode) Color(0xFF34D399) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Switch(
                        checked = lowEndMode,
                        onCheckedChange = onToggleLowEndMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF022C22),
                            checkedTrackColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.testTag("low_end_mode_switch")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Engineered specifically for budget devices, low-RAM hardware, and containerized emulators. Bypasses GPU render node probing to prevent Chromium renderer crashes and reduces memory consumption.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Automated Optimizations When Enabled:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(6.dp))

                val features = listOf(
                    "Software Render Layer Fallback: Eliminates Mesa rendernode probe errors and prevents Chromium renderer crashes",
                    "Offscreen Pre-Raster Disabled: Prevents WebView from pre-rendering hidden tiles, saving memory",
                    "Gesture-Gated Media Playback: Stops unsolicited background videos from consuming CPU",
                    "Opaque UI Fast Path: Replaces real-time Gaussian glass blurs with snappy 98% opaque panels",
                    "Lightweight Shaders: Disables particle physics and background firefly animations",
                    "Reduced Motion Profile: Bypasses bouncy spring interpolators for snappy zero-lag navigation"
                )

                features.forEach { feature ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (lowEndMode) Color(0xFF10B981) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(14.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Manual Tuning Section
        Text(
            text = "Fine-Tuning & Graphics",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))

        SettingsCheckboxItem(
            checked = hardwareAccel,
            onCheckedChange = onToggleHardwareAccel,
            title = "Hardware Acceleration",
            subtitle = if (lowEndMode) "Auto-managed by Low-End Mode (Software layer active for stability)" else "Uses device GPU hardware for web rendering"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Motion Intensity Preference
        Text(
            text = "Motion & Transition Profile",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Select UI transition intensity. 'Reduced' is optimal for battery and low-spec processors.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AnimationIntensity.entries.forEach { intensity ->
                val isSelected = animationIntensity == intensity
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectAnimationIntensity(intensity) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = intensity.displayName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (intensity) {
                                AnimationIntensity.REDUCED -> "Fastest"
                                AnimationIntensity.BALANCED -> "Standard"
                                AnimationIntensity.SMOOTH -> "Fluid"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Device Diagnostics & RAM Optimization Card
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device & Memory Diagnostics",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "System Low-RAM Device:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isLowRamDevice) "Yes (Optimizations Recommended)" else "No (Sufficient RAM)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isLowRamDevice) Color(0xFFF59E0B) else Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Device Model / Hardware:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${Build.MODEL} (${Build.HARDWARE})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                var isOptimizing by remember { mutableStateOf(false) }
                LaunchedEffect(isOptimizing) {
                    if (isOptimizing) {
                        kotlinx.coroutines.delay(650)
                        isOptimizing = false
                    }
                }

                Button(
                    onClick = {
                        isOptimizing = true
                        onOptimizeMemory()
                    },
                    enabled = !isOptimizing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("optimize_ram_button")
                ) {
                    if (isOptimizing) {
                        AsyncOperationProgressIndicator(
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reclaiming Memory…", fontSize = 12.sp)
                    } else {
                        Icon(imageVector = Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Reclaim RAM & Clean Temp Cache", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
