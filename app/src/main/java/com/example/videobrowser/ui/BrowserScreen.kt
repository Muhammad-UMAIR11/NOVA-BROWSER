package com.example.videobrowser.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.videobrowser.ui.components.BrowserBottomBar
import com.example.videobrowser.ui.components.BrowserSettingsSheet
import com.example.videobrowser.ui.components.BrowserTopBar
import com.example.videobrowser.ui.components.BrowserWebView
import com.example.videobrowser.ui.components.DetectedMediaSheet
import com.example.videobrowser.ui.components.DownloadConfirmationDialog
import com.example.videobrowser.ui.components.DownloadManagerSheet
import com.example.videobrowser.ui.components.DownloadOrb
import com.example.videobrowser.ui.components.NovaFrontPage
import com.example.videobrowser.ui.components.NovaToolsSheet
import com.example.videobrowser.ui.components.TabsSheet
import com.example.videobrowser.viewmodel.BrowserViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val downloadTasks by viewModel.downloadTasks.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val activeTab = uiState.activeTab
    val detectedMediaList = uiState.detectedVideos
    val detectedGroupedVideos = uiState.detectedGroupedVideos
    val detectedCount = uiState.detectedCount
    val isPlaying = activeTab?.activePlayingMedia != null

    val detectionSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val downloadsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tabsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }

    var fullscreenCustomView by remember { mutableStateOf<View?>(null) }
    var isFullscreen by remember { mutableStateOf(false) }
    var exitFullscreenCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* Handled gracefully */ }

        LaunchedEffect(Unit) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Display status notices
    LaunchedEffect(uiState.statusNotice) {
        uiState.statusNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice, duration = SnackbarDuration.Short)
            viewModel.clearStatusNotice()
        }
    }

    val isFrontPage = activeTab == null || activeTab.url == BrowserViewModel.NOVA_HOME_URL

    // System Back Press handling - only enabled when there is navigation or active modal to dismiss
    val isBackHandlerEnabled = isFullscreen ||
        uiState.showDetectionSheet ||
        uiState.showDownloadsSheet ||
        uiState.showTabsSheet ||
        uiState.showSettingsSheet ||
        uiState.showToolsSheet ||
        (uiState.selectedMediaForConfirmation != null) ||
        (activeTab?.canGoBack == true) ||
        !isFrontPage

    BackHandler(enabled = isBackHandlerEnabled) {
        when {
            isFullscreen -> {
                exitFullscreenCallback?.invoke()
                isFullscreen = false
                fullscreenCustomView = null
                exitFullscreenCallback = null
            }
            uiState.showToolsSheet -> viewModel.closeToolsSheet()
            uiState.showSettingsSheet -> viewModel.closeSettingsSheet()
            uiState.showDetectionSheet -> viewModel.closeDetectionSheet()
            uiState.showDownloadsSheet -> viewModel.closeDownloadsSheet()
            uiState.showTabsSheet -> viewModel.closeTabsSheet()
            uiState.selectedMediaForConfirmation != null -> viewModel.dismissConfirmationDialog()
            activeTab != null && activeTab.canGoBack -> {
                viewModel.navigateBack()
            }
            !isFrontPage -> {
                viewModel.goHome()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (!isFullscreen && !isFrontPage) {
                BrowserTopBar(
                    urlInput = uiState.urlInput,
                    isLoading = activeTab?.isLoading ?: false,
                    progress = activeTab?.progress ?: 0,
                    activeDownloadCount = downloadTasks.count { it.status == com.example.videobrowser.model.DownloadStatus.DOWNLOADING },
                    tabCount = uiState.tabs.size,
                    searchEngineName = uiState.searchEngine.displayName,
                    blockedAdsCount = uiState.activeTabBlockedAds,
                    isAdBlockerActive = uiState.isAdBlockerEnabled,
                    onUrlChange = viewModel::onUrlInputChange,
                    onSubmitUrl = viewModel::submitUrl,
                    onRefreshOrStop = {
                        if (activeTab?.isLoading == true) {
                            viewModel.stopActiveTabLoading()
                        } else {
                            viewModel.reloadActiveTab()
                        }
                    },
                    onOpenDownloads = viewModel::openDownloadsSheet,
                    onOpenTabs = viewModel::openTabsSheet,
                    onOpenSettings = viewModel::openSettingsSheet,
                    onShieldClick = viewModel::openToolsSheet,
                    modifier = Modifier.statusBarsPadding()
                )
            }
        },
        bottomBar = {
            if (!isFullscreen) {
                BrowserBottomBar(
                    canGoBack = activeTab?.canGoBack ?: false,
                    canGoForward = activeTab?.canGoForward ?: false,
                    detectedMediaCount = detectedCount,
                    isToolsOpen = uiState.showToolsSheet,
                    onBack = viewModel::navigateBack,
                    onForward = viewModel::navigateForward,
                    onHome = viewModel::goHome,
                    onToggleTools = viewModel::toggleToolsSheet,
                    onNewTab = { viewModel.addNewTab() },
                    onOpenMediaSheet = viewModel::openDetectionSheet
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isFrontPage) {
                NovaFrontPage(
                    customWallpaperPath = uiState.customWallpaperPath,
                    onSearch = viewModel::submitUrl,
                    onOpenSettings = viewModel::openSettingsSheet,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Error Banner if webpage failed
                    val errorMessage = activeTab?.errorMessage
                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
                        exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
                    ) {
                        if (errorMessage != null) {
                            ErrorBanner(
                                message = errorMessage,
                                onRetry = { viewModel.loadUrlInActiveTab(activeTab.url) },
                                onDismiss = { /* dismiss */ }
                            )
                        }
                    }

                    // WebView Container
                    Box(modifier = Modifier.weight(1f)) {
                        activeTab?.let { tab ->
                            androidx.compose.runtime.key(tab.id) {
                                BrowserWebView(
                                    viewModel = viewModel,
                                    targetUrl = tab.url,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("browser_webview"),
                                    onFullscreenStateChange = { inFullscreen, view, exitCallback ->
                                        isFullscreen = inFullscreen
                                        fullscreenCustomView = view
                                        exitFullscreenCallback = exitCallback
                                    }
                                )
                            }
                        }
                    }
                }

                // Floating Download Orb
                DownloadOrb(
                    visible = uiState.isOrbVisible,
                    detectedCount = if (detectedGroupedVideos.isNotEmpty()) detectedGroupedVideos.size else detectedMediaList.size,
                    isPlaying = isPlaying,
                    orbStatus = uiState.orbStatus,
                    onClick = viewModel::openDetectionSheet,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 16.dp)
                )
            }

            // Fullscreen View Overlay (when HTML5 video requests fullscreen)
            if (isFullscreen && fullscreenCustomView != null) {
                AndroidView(
                    factory = { ctx ->
                        FrameLayout(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            fullscreenCustomView?.let { customView ->
                                (customView.parent as? ViewGroup)?.removeView(customView)
                                addView(customView)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                )
            }
        }
    }

    // Modal Sheets and Dialogs

    if (uiState.showDetectionSheet) {
        DetectedMediaSheet(
            sheetState = detectionSheetState,
            pageTitle = activeTab?.title ?: "Webpage",
            pageUrl = activeTab?.url ?: "",
            groupedVideos = detectedGroupedVideos,
            mediaList = detectedMediaList,
            onDismiss = viewModel::closeDetectionSheet,
            onSelectMedia = viewModel::selectMediaForConfirmation,
            onSelectQuality = viewModel::selectQuality
        )
    }

    if (uiState.selectedMediaForConfirmation != null) {
        DownloadConfirmationDialog(
            media = uiState.selectedMediaForConfirmation!!,
            fileNameInput = uiState.customFileNameInput,
            useSystemDownloadManager = uiState.useSystemDownloadManager,
            onFileNameChange = viewModel::onCustomFileNameChange,
            onToggleSystemDownloadManager = viewModel::toggleSystemDownloadManager,
            onConfirm = viewModel::confirmDownload,
            onDismiss = viewModel::dismissConfirmationDialog
        )
    }

    if (uiState.showDownloadsSheet) {
        DownloadManagerSheet(
            sheetState = downloadsSheetState,
            tasks = downloadTasks,
            onCancelTask = viewModel::cancelDownload,
            onRemoveTask = viewModel::removeDownload,
            onRetryTask = viewModel::retryDownload,
            onPauseTask = viewModel::pauseDownload,
            onResumeTask = viewModel::resumeDownload,
            onPauseAll = viewModel::pauseAllDownloads,
            onResumeAll = viewModel::resumeAllDownloads,
            onClearCompleted = viewModel::clearCompletedDownloads,
            onDismiss = viewModel::closeDownloadsSheet
        )
    }

    if (uiState.showTabsSheet) {
        TabsSheet(
            sheetState = tabsSheetState,
            tabs = uiState.tabs,
            activeTabId = uiState.activeTabId,
            onSelectTab = viewModel::switchTab,
            onCloseTab = viewModel::closeTab,
            onNewTab = { viewModel.addNewTab() },
            onDismiss = viewModel::closeTabsSheet
        )
    }

    if (uiState.showSettingsSheet) {
        BrowserSettingsSheet(
            sheetState = settingsSheetState,
            currentSearchEngine = uiState.searchEngine,
            currentThemeMode = uiState.themeMode,
            currentColorTheme = uiState.colorTheme,
            currentFontFamily = uiState.appFontFamily,
            customWallpaperPath = uiState.customWallpaperPath,
            onSelectSearchEngine = viewModel::setSearchEngine,
            onSelectThemeMode = viewModel::setThemeMode,
            onSelectColorTheme = viewModel::setColorTheme,
            onSelectFontFamily = viewModel::setFontFamily,
            onUploadWallpaper = viewModel::setCustomWallpaperFromUri,
            onRemoveWallpaper = viewModel::removeCustomWallpaper,
            onClearCache = viewModel::clearBrowserCache,
            isAdBlockerActive = uiState.isAdBlockerEnabled,
            onToggleAdBlocker = viewModel::toggleAdBlocker,
            onDismiss = viewModel::closeSettingsSheet
        )
    }

    NovaToolsSheet(
        visible = uiState.showToolsSheet,
        activeTabTitle = activeTab?.title,
        activeTabUrl = activeTab?.url,
        detectedMediaCount = detectedCount,
        activeDownloadsCount = downloadTasks.count { it.status == com.example.videobrowser.model.DownloadStatus.DOWNLOADING },
        openTabsCount = uiState.tabs.size,
        isDesktopMode = activeTab?.isDesktopMode ?: false,
        isAdBlockerActive = uiState.isAdBlockerEnabled,
        blockedAdsOnPage = uiState.activeTabBlockedAds,
        totalBlockedAds = uiState.totalBlockedAds,
        onDismiss = viewModel::closeToolsSheet,
        onOpenDownloads = viewModel::openDownloadsSheet,
        onOpenTabs = viewModel::openTabsSheet,
        onOpenSettings = viewModel::openSettingsSheet,
        onClearCache = viewModel::clearBrowserCache,
        onSniffUrl = viewModel::submitUrl,
        onReloadPage = viewModel::reloadActiveTab,
        onToggleDesktopMode = viewModel::toggleDesktopModeForActiveTab,
        onOpenDetectedMedia = viewModel::openDetectionSheet
    )
}

@Composable
private fun ErrorBanner(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Retry", fontSize = 12.sp)
            }
        }
    }
}
