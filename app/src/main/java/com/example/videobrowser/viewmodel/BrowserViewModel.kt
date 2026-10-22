package com.example.videobrowser.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import java.io.File
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.videobrowser.detection.MediaDetectionListener
import com.example.videobrowser.detection.PlatformCompatibilityEngine
import com.example.videobrowser.detection.VideoSniffer
import com.example.videobrowser.detection.WebViewCacheManager
import com.example.videobrowser.download.DownloadEngine
import com.example.videobrowser.model.AppFontFamily
import com.example.videobrowser.model.BrowserTab
import com.example.videobrowser.model.ColorTheme
import com.example.videobrowser.model.DetectedMedia
import com.example.videobrowser.model.DetectionSource
import com.example.videobrowser.model.DownloadTask
import com.example.videobrowser.model.GroupedVideoMedia
import com.example.videobrowser.model.MediaCompatibilityCategory
import com.example.videobrowser.model.MediaFilter
import com.example.videobrowser.model.SearchEngine
import com.example.videobrowser.model.ThemeMode
import com.example.videobrowser.model.VideoGroupingEngine
import com.example.videobrowser.model.VideoQualityOption
import com.example.videobrowser.model.CustomSearchEngine
import com.example.videobrowser.model.theme.AnimationIntensity
import com.example.videobrowser.model.theme.BrowserTheme
import com.example.videobrowser.model.theme.UiDensity
import com.example.videobrowser.data.BrowserSettingsManager
import com.example.videobrowser.adblock.AdBlockerEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface WebAction {
    data class LoadUrl(val url: String) : WebAction
    data object GoBack : WebAction
    data object GoForward : WebAction
    data object Reload : WebAction
    data object StopLoading : WebAction
}

data class QuickBookmark(
    val title: String,
    val url: String,
    val subtitle: String,
    val tag: String
)

data class HistoryItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class BookmarkItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class OrbStatus {
    HIDDEN,
    DETECTING,
    SUPPORTED_FOUND,
    MULTIPLE_CANDIDATES,
    UNSUPPORTED_MEDIA,
    ERROR
}

data class BrowserUiState(
    val tabs: List<BrowserTab> = emptyList(),
    val activeTabId: String = "",
    val urlInput: String = "",
    val showDetectionSheet: Boolean = false,
    val showDownloadsSheet: Boolean = false,
    val showTabsSheet: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showToolsSheet: Boolean = false,
    val showAiSheet: Boolean = false,
    val showHistorySheet: Boolean = false,
    val showBookmarksSheet: Boolean = false,
    val showPrivacySheet: Boolean = false,
    val historyEntries: List<HistoryItem> = emptyList(),
    val bookmarkEntries: List<BookmarkItem> = listOf(
        BookmarkItem(url = "https://en.wikipedia.org", title = "Wikipedia"),
        BookmarkItem(url = "https://archive.org", title = "Internet Archive"),
        BookmarkItem(url = "https://github.com", title = "GitHub")
    ),
    val searchEngine: SearchEngine = SearchEngine.DUCKDUCKGO,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val colorTheme: ColorTheme = ColorTheme.OCEAN_BLUE,
    val appFontFamily: AppFontFamily = AppFontFamily.SYSTEM_DEFAULT,
    val activeBrowserTheme: BrowserTheme = BrowserTheme.MIDNIGHT,
    val customThemes: List<BrowserTheme> = emptyList(),
    val allThemes: List<BrowserTheme> = BrowserTheme.BUILT_IN_THEMES,
    val activeSearchEngine: CustomSearchEngine = CustomSearchEngine.BUILT_IN_ENGINES.first(),
    val allSearchEngines: List<CustomSearchEngine> = CustomSearchEngine.BUILT_IN_ENGINES,
    val fontScale: Float = 1.0f,
    val fontWeightDelta: Int = 0,
    val applyTextZoomToWeb: Boolean = true,
    val uiDensity: UiDensity = UiDensity.COMFORTABLE,
    val cornerRadiusDp: Int = 12,
    val panelTransparency: Float = 0.88f,
    val panelBlurIntensity: Float = 0.80f,
    val animationIntensity: AnimationIntensity = AnimationIntensity.BALANCED,
    val blockCookies: Boolean = true,
    val strictPopups: Boolean = true,
    val hardwareAccel: Boolean = true,
    val lowEndMode: Boolean = false,
    val selectedMediaForConfirmation: DetectedMedia? = null,
    val customFileNameInput: String = "",
    val useSystemDownloadManager: Boolean = false,
    val customWallpaperPath: String? = null,
    val statusNotice: String? = null,
    val isAdBlockerEnabled: Boolean = true,
    val totalBlockedAds: Int = 0
) {
    val activeTab: BrowserTab?
        get() = tabs.find { it.id == activeTabId } ?: tabs.firstOrNull()

    val activeTabBlockedAds: Int
        get() = (activeTab?.blockedAdsCount ?: 0) + (activeTab?.blockedTrackersCount ?: 0)

    val detectedVideos: List<DetectedMedia>
        get() = activeTab?.detectedMediaList ?: emptyList()

    val detectedGroupedVideos: List<GroupedVideoMedia>
        get() = activeTab?.groupedVideos ?: emptyList()

    val detectedCount: Int
        get() = if (detectedGroupedVideos.isNotEmpty()) detectedGroupedVideos.size else detectedVideos.size

    val orbStatus: OrbStatus
        get() {
            if (detectedGroupedVideos.isEmpty() && detectedVideos.isEmpty()) {
                return OrbStatus.HIDDEN
            }
            val downloadableCount = if (detectedGroupedVideos.isNotEmpty()) {
                detectedGroupedVideos.count { it.isDownloadable }
            } else {
                detectedVideos.count { it.isDownloadable }
            }
            return when {
                downloadableCount > 1 -> OrbStatus.MULTIPLE_CANDIDATES
                downloadableCount == 1 -> OrbStatus.SUPPORTED_FOUND
                else -> OrbStatus.UNSUPPORTED_MEDIA
            }
        }

    val isOrbVisible: Boolean
        get() = detectedGroupedVideos.isNotEmpty() || detectedVideos.isNotEmpty()

    val activePlayingMedia: DetectedMedia?
        get() = activeTab?.activePlayingMedia
}

class BrowserViewModel(application: Application) : AndroidViewModel(application), MediaDetectionListener {

    companion object {
        const val NOVA_HOME_URL = "nova:home"
        private const val PREFS_NAME = "nova_browser_prefs"
        private const val KEY_CUSTOM_WALLPAPER = "custom_wallpaper_path"
        private const val KEY_AD_BLOCKER_ENABLED = "ad_blocker_enabled"
    }

    private val prefs by lazy { application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    private val sniffer = VideoSniffer()
    val downloadEngine = DownloadEngine(application)

    val downloadTasks: StateFlow<List<DownloadTask>> = downloadEngine.tasks

    val quickBookmarks = listOf(
        QuickBookmark(
            title = "Google",
            url = "https://www.google.com",
            subtitle = "Search & Discovery",
            tag = "Google"
        ),
        QuickBookmark(
            title = "DuckDuckGo",
            url = "https://duckduckgo.com",
            subtitle = "Privacy First Search",
            tag = "Duck"
        ),
        QuickBookmark(
            title = "Wikipedia",
            url = "https://www.wikipedia.org",
            subtitle = "Free Online Encyclopedia",
            tag = "Wiki"
        ),
        QuickBookmark(
            title = "Internet Archive",
            url = "https://archive.org",
            subtitle = "Digital Media & Web Library",
            tag = "Archive"
        ),
        QuickBookmark(
            title = "GitHub",
            url = "https://github.com",
            subtitle = "Developer Platform & Code",
            tag = "GitHub"
        )
    )

    val settingsManager = BrowserSettingsManager(application)

    private val _uiState = MutableStateFlow(
        BrowserUiState(
            tabs = listOf(
                BrowserTab(
                    title = "Nova",
                    url = NOVA_HOME_URL
                )
            ),
            isAdBlockerEnabled = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_AD_BLOCKER_ENABLED, true),
            customWallpaperPath = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CUSTOM_WALLPAPER, null)?.takeIf { File(it).exists() },
            activeBrowserTheme = settingsManager.getActiveTheme(),
            customThemes = settingsManager.getCustomThemes(),
            allThemes = settingsManager.getAllThemes(),
            activeSearchEngine = settingsManager.getActiveEngine(),
            allSearchEngines = settingsManager.getAllEngines(),
            searchEngine = SearchEngine.entries.find { it.id == settingsManager.getActiveEngineId() } ?: SearchEngine.DUCKDUCKGO,
            themeMode = settingsManager.getThemeMode(),
            appFontFamily = settingsManager.getFontFamily(),
            fontScale = settingsManager.getFontScale(),
            fontWeightDelta = settingsManager.getFontWeightDelta(),
            applyTextZoomToWeb = settingsManager.getApplyTextZoomToWeb(),
            uiDensity = settingsManager.getUiDensity(),
            cornerRadiusDp = settingsManager.getCornerRadiusDp(),
            panelTransparency = settingsManager.getPanelTransparency(),
            panelBlurIntensity = settingsManager.getPanelBlurIntensity(),
            animationIntensity = settingsManager.getAnimationIntensity(),
            blockCookies = settingsManager.getBlockCookies(),
            strictPopups = settingsManager.getStrictPopups(),
            hardwareAccel = settingsManager.getHardwareAccel(),
            lowEndMode = settingsManager.getLowEndMode(),
            totalBlockedAds = settingsManager.getTotalBlockedAds()
        ).let { initial ->
            initial.copy(
                activeTabId = initial.tabs.first().id,
                urlInput = ""
            )
        }
    )
    val uiState: StateFlow<BrowserUiState> = _uiState.asStateFlow()

    private val _webActions = MutableSharedFlow<WebAction>(extraBufferCapacity = 16)
    val webActions: SharedFlow<WebAction> = _webActions.asSharedFlow()

    fun onUrlInputChange(newInput: String) {
        _uiState.update { it.copy(urlInput = newInput) }
    }

    /**
     * Navigates to the entered address or performs search using the selected search engine.
     */
    fun submitUrl(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank() || trimmed == NOVA_HOME_URL) {
            loadUrlInActiveTab(NOVA_HOME_URL)
            return
        }

        val targetUrl = when {
            URLUtil.isValidUrl(trimmed) -> trimmed
            trimmed.startsWith("http://") || trimmed.startsWith("https://") -> trimmed
            trimmed.contains(".") && !trimmed.contains(" ") -> "https://$trimmed"
            else -> _uiState.value.activeSearchEngine.buildSearchUrl(trimmed)
        }

        loadUrlInActiveTab(targetUrl)
    }

    fun loadUrlInActiveTab(url: String) {
        val isNovaHome = url == NOVA_HOME_URL
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    tab.copy(
                        url = url,
                        title = if (isNovaHome) "Nova" else tab.title,
                        isLoading = !isNovaHome,
                        progress = if (isNovaHome) 0 else tab.progress,
                        errorMessage = null,
                        detectedMediaList = emptyList(),
                        groupedVideos = emptyList(),
                        activePlayingMedia = null
                    )
                } else tab
            }
            state.copy(
                tabs = updatedTabs,
                urlInput = if (isNovaHome) "" else url,
                showDetectionSheet = false
            )
        }
        if (!isNovaHome) {
            _webActions.tryEmit(WebAction.LoadUrl(url))
        }
    }

    fun navigateBack() {
        _webActions.tryEmit(WebAction.GoBack)
    }

    fun navigateForward() {
        _webActions.tryEmit(WebAction.GoForward)
    }

    fun reloadActiveTab() {
        _webActions.tryEmit(WebAction.Reload)
    }

    fun stopActiveTabLoading() {
        _webActions.tryEmit(WebAction.StopLoading)
    }

    fun goHome() {
        loadUrlInActiveTab(NOVA_HOME_URL)
    }

    // --- Browser Tab Management ---

    fun addNewTab(url: String? = null) {
        val targetUrl = url ?: NOVA_HOME_URL
        val newTab = BrowserTab(
            title = if (targetUrl == NOVA_HOME_URL) "Nova" else "New Tab",
            url = targetUrl
        )
        _uiState.update { state ->
            state.copy(
                tabs = state.tabs + newTab,
                activeTabId = newTab.id,
                urlInput = if (targetUrl == NOVA_HOME_URL) "" else targetUrl,
                showTabsSheet = false
            )
        }
        if (targetUrl != NOVA_HOME_URL) {
            _webActions.tryEmit(WebAction.LoadUrl(targetUrl))
        }
    }

    fun closeTab(tabId: String) {
        val wasActive = _uiState.value.activeTabId == tabId
        _uiState.update { state ->
            val remaining = state.tabs.filterNot { it.id == tabId }
            if (remaining.isEmpty()) {
                val fresh = BrowserTab(title = "Home", url = "https://duckduckgo.com")
                state.copy(tabs = listOf(fresh), activeTabId = fresh.id, urlInput = fresh.url)
            } else {
                val newActiveId = if (state.activeTabId == tabId) remaining.first().id else state.activeTabId
                val newActiveTab = remaining.find { it.id == newActiveId }
                state.copy(
                    tabs = remaining,
                    activeTabId = newActiveId,
                    urlInput = newActiveTab?.url ?: ""
                )
            }
        }
        if (wasActive) {
            val target = _uiState.value.activeTab
            if (target != null) {
                _webActions.tryEmit(WebAction.LoadUrl(target.url))
            }
        }
    }

    fun switchTab(tabId: String) {
        if (_uiState.value.activeTabId == tabId) {
            _uiState.update { it.copy(showTabsSheet = false) }
            return
        }
        val target = _uiState.value.tabs.find { it.id == tabId }
        _uiState.update { state ->
            if (target != null) {
                state.copy(
                    activeTabId = tabId,
                    urlInput = target.url,
                    showTabsSheet = false
                )
            } else state
        }
        if (target != null) {
            _webActions.tryEmit(WebAction.LoadUrl(target.url))
        }
    }

    // --- WebView Progress and Navigation State Updates ---

    fun onPageStarted(url: String) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    val isSamePageNav = tab.url.substringBefore('#') == url.substringBefore('#') && tab.url.isNotBlank() && url.isNotBlank()
                    tab.copy(
                        url = url,
                        isLoading = true,
                        errorMessage = null,
                        detectedMediaList = if (isSamePageNav) tab.detectedMediaList else emptyList(),
                        groupedVideos = if (isSamePageNav) tab.groupedVideos else emptyList(),
                        activePlayingMedia = if (isSamePageNav) tab.activePlayingMedia else null,
                        blockedAdsCount = if (isSamePageNav) tab.blockedAdsCount else 0,
                        blockedTrackersCount = if (isSamePageNav) tab.blockedTrackersCount else 0,
                        blockedResourcesOnPage = if (isSamePageNav) tab.blockedResourcesOnPage else emptySet()
                    )
                } else tab
            }
            state.copy(tabs = updatedTabs, urlInput = url)
        }
    }

    fun onPageFinished(url: String, title: String?) {
        val finalTitle = title?.takeIf { it.isNotBlank() } ?: "Web Page"
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    tab.copy(
                        url = url,
                        title = finalTitle,
                        isLoading = false,
                        progress = 100
                    )
                } else tab
            }
            val updatedHistory = if (url.isNotBlank() && url != NOVA_HOME_URL && !url.startsWith("about:")) {
                listOf(HistoryItem(url = url, title = finalTitle)) + state.historyEntries.filterNot { it.url == url }
            } else {
                state.historyEntries
            }
            state.copy(tabs = updatedTabs, urlInput = url, historyEntries = updatedHistory)
        }
    }

    fun onProgressChanged(newProgress: Int) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    tab.copy(
                        progress = newProgress,
                        isLoading = newProgress < 100
                    )
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    tab.copy(canGoBack = canGoBack, canGoForward = canGoForward)
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    fun onPageError(errorCode: Int, description: String, failingUrl: String) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    tab.copy(
                        isLoading = false,
                        errorMessage = "Failed to load page: $description (Code $errorCode)"
                    )
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    // --- Media Interception from Network ---

    fun onNetworkResourceIntercepted(
        requestUrl: String,
        headers: Map<String, String>,
        pageUrl: String
    ) {
        val activeTab = _uiState.value.activeTab ?: return
        val candidate = sniffer.createCandidateFromNetwork(
            url = requestUrl,
            headers = headers,
            pageUrl = pageUrl,
            pageTitle = activeTab.title
        ) ?: return

        addOrUpdateDetectedMedia(candidate)

        // Asynchronously query HEAD on IO to verify size, exact MIME type, and stream status
        viewModelScope.launch(Dispatchers.IO) {
            val probed = sniffer.probeMediaHeaders(candidate)
            addOrUpdateDetectedMedia(probed)
        }
    }

    /**
     * Handles direct media download requests initiated from WebView download listener.
     */
    fun onDownloadRequestedFromWebView(
        url: String,
        userAgent: String,
        contentDisposition: String,
        mimeType: String,
        contentLength: Long
    ) {
        val activeTab = _uiState.value.activeTab
        val pageUrl = activeTab?.url ?: ""
        val pageTitle = activeTab?.title ?: "Downloaded Media"
        val format = MediaFilter.inferFormat(url, mimeType)
        val cleanTitle = URLUtil.guessFileName(url, contentDisposition, mimeType)
            .substringBeforeLast(".")
            .ifBlank { pageTitle }

        val detected = DetectedMedia(
            id = sniffer.generateMediaId(url),
            mediaUrl = url,
            pageUrl = pageUrl,
            title = cleanTitle,
            mimeType = mimeType.ifBlank { "video/mp4" },
            format = format,
            quality = "Original",
            contentLength = contentLength,
            isStream = false,
            isDownloadable = true,
            source = DetectionSource.NETWORK_INTERCEPTION,
            headers = if (userAgent.isNotBlank()) mapOf("User-Agent" to userAgent) else emptyMap()
        )
        addOrUpdateDetectedMedia(detected)
        selectMediaForConfirmation(detected)
    }

    // --- Media Detection from DOM JavaScript Bridge ---

    override fun onDomVideoFound(
        mediaUrl: String,
        pageUrl: String,
        title: String,
        mimeType: String,
        width: Int,
        height: Int,
        duration: Double,
        isPlaying: Boolean,
        isDrm: Boolean,
        userInteracted: Boolean,
        hasControls: Boolean,
        isLooping: Boolean,
        isMuted: Boolean,
        posterUrl: String?
    ) {
        if (!MediaFilter.isCandidateVideo(mediaUrl, mimeType)) return

        if (MediaFilter.isLikelyGifOrDecorative(
                url = mediaUrl,
                mimeType = mimeType,
                duration = duration,
                width = width,
                height = height,
                isLooping = isLooping,
                isMuted = isMuted,
                hasControls = hasControls
            )
        ) {
            return
        }

        val activeTab = _uiState.value.activeTab ?: return
        val format = MediaFilter.inferFormat(mediaUrl, mimeType)
        val isStream = MediaFilter.isStreamManifest(mediaUrl, mimeType)
        val mediaId = sniffer.generateMediaId(mediaUrl)
        val resolvedPageUrl = pageUrl.ifBlank { activeTab.url }

        val compat = PlatformCompatibilityEngine.evaluate(
            mediaUrl = mediaUrl,
            pageUrl = resolvedPageUrl,
            mimeType = mimeType,
            isStream = isStream,
            isDrmReported = isDrm
        )

        val cleanTitle = MediaFilter.cleanTitle(title.ifBlank { activeTab.title })
        val initialDetected = DetectedMedia(
            id = mediaId,
            mediaUrl = mediaUrl,
            pageUrl = resolvedPageUrl,
            title = cleanTitle,
            mimeType = mimeType.ifBlank { "video/mp4" },
            format = format,
            quality = if (height > 0) "${height}p" else if (isStream) "Adaptive Stream" else "Standard",
            durationSeconds = duration,
            width = width,
            height = height,
            isPlaying = isPlaying,
            isStream = isStream,
            isDownloadable = compat.isDownloadable,
            category = compat.category,
            restrictionReason = compat.restrictionReason,
            isDrmProtected = isDrm,
            source = DetectionSource.DOM_INSPECTION,
            userInteracted = userInteracted,
            hasControls = hasControls,
            isLooping = isLooping,
            isMuted = isMuted,
            isLikelyPreview = isLooping && isMuted,
            thumbnailUrl = posterUrl
        )

        val priorityScore = MediaFilter.calculatePriorityScore(initialDetected)
        val detected = initialDetected.copy(priorityScore = priorityScore)

        addOrUpdateDetectedMedia(detected)

        if (isPlaying || userInteracted) {
            _uiState.update { state ->
                val updatedTabs = state.tabs.map { tab ->
                    if (tab.id == state.activeTabId) {
                        tab.copy(activePlayingMedia = detected)
                    } else tab
                }
                state.copy(tabs = updatedTabs)
            }
        }

        // Probe headers for file size
        viewModelScope.launch(Dispatchers.IO) {
            val probed = sniffer.probeMediaHeaders(detected)
            addOrUpdateDetectedMedia(probed)
        }
    }

    override fun onVideoPlaybackChanged(
        mediaUrl: String,
        isPlaying: Boolean,
        currentTime: Double,
        duration: Double
    ) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    val rawList = tab.detectedMediaList.map { media ->
                        if (media.mediaUrl == mediaUrl) {
                            val newScore = if (isPlaying) media.priorityScore + 100 else media.priorityScore
                            media.copy(
                                isPlaying = isPlaying,
                                durationSeconds = if (duration > 0) duration else media.durationSeconds,
                                priorityScore = newScore
                            )
                        } else {
                            if (isPlaying) media.copy(isPlaying = false) else media
                        }
                    }

                    val sortedList = rawList.sortedWith(
                        compareByDescending<DetectedMedia> { it.isPlaying }
                            .thenByDescending { it.userInteracted }
                            .thenByDescending { it.priorityScore }
                            .thenByDescending { it.height }
                    )

                    val currentPlaying = if (isPlaying) {
                        sortedList.find { it.mediaUrl == mediaUrl }
                    } else {
                        if (tab.activePlayingMedia?.mediaUrl == mediaUrl) {
                            sortedList.find { it.isPlaying }
                        } else {
                            tab.activePlayingMedia
                        }
                    }
                    syncTabMedia(tab, sortedList, currentPlaying)
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    private fun addOrUpdateDetectedMedia(media: DetectedMedia) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    val existing = tab.detectedMediaList.find { it.mediaUrl == media.mediaUrl || it.id == media.id }
                    val rawList = if (existing != null) {
                        tab.detectedMediaList.map {
                            if (it.mediaUrl == media.mediaUrl || it.id == media.id) {
                                // Preserve better metadata and higher priority score
                                it.copy(
                                    contentLength = if (media.contentLength > 0) media.contentLength else it.contentLength,
                                    height = if (media.height > 0) media.height else it.height,
                                    width = if (media.width > 0) media.width else it.width,
                                    mimeType = if (media.mimeType.isNotBlank()) media.mimeType else it.mimeType,
                                    isPlaying = media.isPlaying || it.isPlaying,
                                    userInteracted = media.userInteracted || it.userInteracted,
                                    priorityScore = maxOf(it.priorityScore, media.priorityScore),
                                    isDownloadable = media.isDownloadable,
                                    category = media.category,
                                    restrictionReason = media.restrictionReason,
                                    thumbnailUrl = media.thumbnailUrl ?: it.thumbnailUrl,
                                    parsedHlsVariants = if (media.parsedHlsVariants.isNotEmpty()) media.parsedHlsVariants else it.parsedHlsVariants
                                )
                            } else it
                        }
                    } else {
                        tab.detectedMediaList + media
                    }

                    val sortedList = rawList.sortedWith(
                        compareByDescending<DetectedMedia> { it.isPlaying }
                            .thenByDescending { it.userInteracted }
                            .thenByDescending { it.priorityScore }
                            .thenByDescending { it.height }
                    )

                    val primaryPlaying = sortedList.find { it.isPlaying }
                        ?: sortedList.find { it.userInteracted }
                        ?: tab.activePlayingMedia?.let { active -> sortedList.find { it.mediaUrl == active.mediaUrl } }

                    syncTabMedia(tab, sortedList, primaryPlaying)
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    private fun syncTabMedia(
        tab: BrowserTab,
        sortedList: List<DetectedMedia>,
        primaryPlaying: DetectedMedia?
    ): BrowserTab {
        val grouped = VideoGroupingEngine.groupMedia(sortedList, tab.url, tab.title)
        val updatedGrouped = grouped.map { g ->
            val existing = tab.groupedVideos.find { it.id == g.id }
            if (existing != null && existing.selectedQualityId.isNotBlank() && g.qualities.any { it.id == existing.selectedQualityId }) {
                g.copy(selectedQualityId = existing.selectedQualityId)
            } else {
                g
            }
        }
        return tab.copy(
            detectedMediaList = sortedList,
            groupedVideos = updatedGrouped,
            activePlayingMedia = primaryPlaying
        )
    }

    fun selectQuality(groupId: String, qualityId: String) {
        _uiState.update { state ->
            val updatedTabs = state.tabs.map { tab ->
                if (tab.id == state.activeTabId) {
                    val updatedGrouped = tab.groupedVideos.map { g ->
                        if (g.id == groupId) {
                            g.copy(selectedQualityId = qualityId)
                        } else g
                    }
                    tab.copy(groupedVideos = updatedGrouped)
                } else tab
            }
            state.copy(tabs = updatedTabs)
        }
    }

    fun selectGroupForDownload(group: GroupedVideoMedia) {
        val quality = group.selectedQuality ?: return
        val media = quality.toDetectedMedia(group.title, group.pageUrl)
        selectMediaForConfirmation(media)
    }

    // --- Dialogs and Sheets Controls ---

    fun openDetectionSheet() {
        _uiState.update { it.copy(showDetectionSheet = true) }
    }

    fun closeDetectionSheet() {
        _uiState.update { it.copy(showDetectionSheet = false) }
    }

    fun openDownloadsSheet() {
        _uiState.update { it.copy(showDownloadsSheet = true) }
    }

    fun closeDownloadsSheet() {
        _uiState.update { it.copy(showDownloadsSheet = false) }
    }

    fun openTabsSheet() {
        _uiState.update { it.copy(showTabsSheet = true) }
    }

    fun closeTabsSheet() {
        _uiState.update { it.copy(showTabsSheet = false) }
    }

    fun selectMediaForConfirmation(media: DetectedMedia) {
        _uiState.update {
            it.copy(
                selectedMediaForConfirmation = media,
                customFileNameInput = media.title.take(50).replace(Regex("[^a-zA-Z0-9._-]"), "_"),
                showDetectionSheet = false
            )
        }
    }

    fun onCustomFileNameChange(newName: String) {
        _uiState.update { it.copy(customFileNameInput = newName) }
    }

    fun toggleSystemDownloadManager(enabled: Boolean) {
        _uiState.update { it.copy(useSystemDownloadManager = enabled) }
    }

    fun dismissConfirmationDialog() {
        _uiState.update { it.copy(selectedMediaForConfirmation = null) }
    }

    fun confirmDownload() {
        val state = _uiState.value
        val media = state.selectedMediaForConfirmation ?: return

        downloadEngine.startDownload(
            media = media,
            customFileName = state.customFileNameInput,
            useSystemDownloadManager = state.useSystemDownloadManager
        )

        _uiState.update {
            it.copy(
                selectedMediaForConfirmation = null,
                showDownloadsSheet = true,
                statusNotice = "Download queued: ${media.title}"
            )
        }
    }

    fun clearStatusNotice() {
        _uiState.update { it.copy(statusNotice = null) }
    }

    fun retryDownload(taskId: String) {
        downloadEngine.retryDownload(taskId)
    }

    fun pauseDownload(taskId: String) {
        downloadEngine.pauseDownload(taskId)
    }

    fun resumeDownload(taskId: String) {
        downloadEngine.resumeDownload(taskId)
    }

    fun cancelDownload(taskId: String) {
        downloadEngine.cancelDownload(taskId)
    }

    fun removeDownload(taskId: String) {
        downloadEngine.removeTask(taskId)
    }

    fun pauseAllDownloads() {
        downloadEngine.pauseAll()
        _uiState.update { it.copy(statusNotice = "All downloads paused") }
    }

    fun resumeAllDownloads() {
        downloadEngine.resumeAll()
        _uiState.update { it.copy(statusNotice = "All downloads resumed") }
    }

    fun clearCompletedDownloads() {
        downloadEngine.clearCompleted()
    }

    /**
     * Purges WebView disk cache on demand and sanitizes cache directory structure.
     */
    fun clearBrowserCache() {
        viewModelScope.launch(Dispatchers.IO) {
            WebViewCacheManager.clearWebViewDiskCache(getApplication())
            withContext(Dispatchers.Main) {
                try {
                    val cookieManager = android.webkit.CookieManager.getInstance()
                    cookieManager.removeAllCookies(null)
                    cookieManager.flush()
                    android.webkit.WebStorage.getInstance().deleteAllData()
                } catch (_: Exception) {}
            }
            _uiState.update { it.copy(statusNotice = "Browser cache & cookies cleaned") }
        }
    }

    // --- Settings, Customization & Preferences ---

    fun setSearchEngine(engine: SearchEngine) {
        settingsManager.setActiveEngineId(engine.id)
        val customEngine = _uiState.value.allSearchEngines.find { it.id == engine.id }
            ?: engine.toCustomSearchEngine()
        _uiState.update {
            it.copy(
                searchEngine = engine,
                activeSearchEngine = customEngine,
                statusNotice = "Search engine: ${engine.displayName}"
            )
        }
    }

    fun setActiveCustomEngine(engine: CustomSearchEngine) {
        settingsManager.setActiveEngineId(engine.id)
        val matchingEnum = SearchEngine.entries.find { it.id == engine.id } ?: _uiState.value.searchEngine
        _uiState.update {
            it.copy(
                activeSearchEngine = engine,
                searchEngine = matchingEnum,
                statusNotice = "Search engine: ${engine.displayName}"
            )
        }
    }

    fun saveCustomSearchEngine(engine: CustomSearchEngine) {
        settingsManager.saveCustomEngine(engine)
        val all = settingsManager.getAllEngines()
        _uiState.update {
            it.copy(
                allSearchEngines = all,
                activeSearchEngine = if (it.activeSearchEngine.id == engine.id) engine else it.activeSearchEngine,
                statusNotice = "Saved search engine: ${engine.displayName}"
            )
        }
    }

    fun deleteCustomSearchEngine(engineId: String) {
        settingsManager.deleteCustomEngine(engineId)
        val all = settingsManager.getAllEngines()
        val active = settingsManager.getActiveEngine()
        val matchingEnum = SearchEngine.entries.find { it.id == active.id } ?: SearchEngine.DUCKDUCKGO
        _uiState.update {
            it.copy(
                allSearchEngines = all,
                activeSearchEngine = active,
                searchEngine = matchingEnum,
                statusNotice = "Custom search engine deleted"
            )
        }
    }

    fun restoreDefaultSearchEngines() {
        settingsManager.restoreDefaultEngines()
        val all = settingsManager.getAllEngines()
        val active = settingsManager.getActiveEngine()
        _uiState.update {
            it.copy(
                allSearchEngines = all,
                activeSearchEngine = active,
                searchEngine = SearchEngine.DUCKDUCKGO,
                statusNotice = "Search engines restored to defaults"
            )
        }
    }

    fun setBrowserTheme(theme: BrowserTheme) {
        settingsManager.setActiveThemeId(theme.id)
        _uiState.update {
            it.copy(
                activeBrowserTheme = theme,
                statusNotice = "Theme: ${theme.name}"
            )
        }
    }

    fun saveCustomTheme(theme: BrowserTheme) {
        settingsManager.saveCustomTheme(theme)
        val all = settingsManager.getAllThemes()
        val custom = settingsManager.getCustomThemes()
        _uiState.update {
            it.copy(
                allThemes = all,
                customThemes = custom,
                activeBrowserTheme = theme,
                statusNotice = "Theme saved: ${theme.name}"
            )
        }
    }

    fun deleteCustomTheme(themeId: String) {
        settingsManager.deleteCustomTheme(themeId)
        val all = settingsManager.getAllThemes()
        val custom = settingsManager.getCustomThemes()
        val active = settingsManager.getActiveTheme()
        _uiState.update {
            it.copy(
                allThemes = all,
                customThemes = custom,
                activeBrowserTheme = active,
                statusNotice = "Custom theme removed"
            )
        }
    }

    fun importThemesJson(jsonStr: String): Boolean {
        val success = settingsManager.importThemesJson(jsonStr)
        if (success) {
            val all = settingsManager.getAllThemes()
            val custom = settingsManager.getCustomThemes()
            _uiState.update {
                it.copy(
                    allThemes = all,
                    customThemes = custom,
                    statusNotice = "Theme(s) imported successfully"
                )
            }
        }
        return success
    }

    fun exportThemesJson(): String {
        return settingsManager.exportThemesJson()
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsManager.setThemeMode(mode)
        _uiState.update { it.copy(themeMode = mode, statusNotice = "Appearance: ${mode.displayName}") }
    }

    fun setColorTheme(colorTheme: ColorTheme) {
        // Map to corresponding BrowserTheme for unified experience
        val mappedTheme = when (colorTheme) {
            ColorTheme.OCEAN_BLUE -> BrowserTheme.MIDNIGHT
            ColorTheme.EMERALD -> BrowserTheme.EMERALD
            ColorTheme.DEEP_PURPLE -> BrowserTheme.AURORA
            ColorTheme.SUNSET_AMBER -> BrowserTheme.SOLAR
            ColorTheme.CRIMSON_RED -> BrowserTheme.CYBERPUNK
        }
        settingsManager.setActiveThemeId(mappedTheme.id)
        _uiState.update {
            it.copy(
                colorTheme = colorTheme,
                activeBrowserTheme = mappedTheme,
                statusNotice = "Accent: ${colorTheme.displayName}"
            )
        }
    }

    fun setFontFamily(fontFamily: AppFontFamily) {
        settingsManager.setFontFamily(fontFamily)
        _uiState.update { it.copy(appFontFamily = fontFamily, statusNotice = "Font: ${fontFamily.displayName}") }
    }

    fun setFontScale(scale: Float) {
        settingsManager.setFontScale(scale)
        _uiState.update { it.copy(fontScale = scale) }
    }

    fun setFontWeightDelta(delta: Int) {
        settingsManager.setFontWeightDelta(delta)
        _uiState.update { it.copy(fontWeightDelta = delta) }
    }

    fun setApplyTextZoomToWeb(enabled: Boolean) {
        settingsManager.setApplyTextZoomToWeb(enabled)
        _uiState.update { it.copy(applyTextZoomToWeb = enabled) }
    }

    fun setUiDensity(density: UiDensity) {
        settingsManager.setUiDensity(density)
        _uiState.update { it.copy(uiDensity = density, statusNotice = "Layout density: ${density.displayName}") }
    }

    fun setCornerRadius(radiusDp: Int) {
        settingsManager.setCornerRadiusDp(radiusDp)
        _uiState.update { it.copy(cornerRadiusDp = radiusDp) }
    }

    fun setPanelTransparency(alpha: Float) {
        settingsManager.setPanelTransparency(alpha)
        _uiState.update { it.copy(panelTransparency = alpha) }
    }

    fun setPanelBlurIntensity(blur: Float) {
        settingsManager.setPanelBlurIntensity(blur)
        _uiState.update { it.copy(panelBlurIntensity = blur) }
    }

    fun setAnimationIntensity(intensity: AnimationIntensity) {
        settingsManager.setAnimationIntensity(intensity)
        _uiState.update { it.copy(animationIntensity = intensity, statusNotice = "Motion: ${intensity.displayName}") }
    }

    fun setBlockCookies(enabled: Boolean) {
        settingsManager.setBlockCookies(enabled)
        _uiState.update { it.copy(blockCookies = enabled) }
    }

    fun setStrictPopups(enabled: Boolean) {
        settingsManager.setStrictPopups(enabled)
        _uiState.update { it.copy(strictPopups = enabled) }
    }

    fun setHardwareAccel(enabled: Boolean) {
        settingsManager.setHardwareAccel(enabled)
        _uiState.update { it.copy(hardwareAccel = enabled) }
    }

    fun setLowEndMode(enabled: Boolean) {
        settingsManager.setLowEndMode(enabled)
        if (enabled) {
            settingsManager.setAnimationIntensity(AnimationIntensity.REDUCED)
            settingsManager.setPanelBlurIntensity(0.0f)
            settingsManager.setPanelTransparency(0.98f)
            System.gc()
            _uiState.update {
                it.copy(
                    lowEndMode = true,
                    animationIntensity = AnimationIntensity.REDUCED,
                    panelBlurIntensity = 0.0f,
                    panelTransparency = 0.98f,
                    statusNotice = "Low-End Device Mode: Optimizations Active"
                )
            }
        } else {
            settingsManager.setAnimationIntensity(AnimationIntensity.BALANCED)
            settingsManager.setPanelBlurIntensity(0.80f)
            settingsManager.setPanelTransparency(0.88f)
            _uiState.update {
                it.copy(
                    lowEndMode = false,
                    animationIntensity = AnimationIntensity.BALANCED,
                    panelBlurIntensity = 0.80f,
                    panelTransparency = 0.88f,
                    statusNotice = "Standard Visuals Restored"
                )
            }
        }
    }

    /**
     * Reclaims memory, flushes inactive resources, and cleans temporary WebView disk cache.
     * Especially useful on memory-constrained low-end devices.
     */
    fun optimizeMemoryAndPurgeCache() {
        viewModelScope.launch(Dispatchers.IO) {
            WebViewCacheManager.clearWebViewDiskCache(getApplication())
            withContext(Dispatchers.Main) {
                try {
                    System.gc()
                } catch (_: Exception) {}
            }
            _uiState.update { it.copy(statusNotice = "Memory optimized & temporary cache purged") }
        }
    }

    fun resetThemeAndColors() {
        settingsManager.resetColorsAndThemes()
        _uiState.update {
            it.copy(
                activeBrowserTheme = BrowserTheme.MIDNIGHT,
                customThemes = emptyList(),
                allThemes = BrowserTheme.BUILT_IN_THEMES,
                colorTheme = ColorTheme.OCEAN_BLUE,
                statusNotice = "Theme & colors reset to defaults"
            )
        }
    }

    fun resetTypography() {
        settingsManager.resetTypography()
        _uiState.update {
            it.copy(
                appFontFamily = AppFontFamily.SYSTEM_DEFAULT,
                fontScale = 1.0f,
                fontWeightDelta = 0,
                applyTextZoomToWeb = true,
                statusNotice = "Typography reset to default"
            )
        }
    }

    fun resetAdvancedSettings() {
        settingsManager.resetAdvanced()
        _uiState.update {
            it.copy(
                uiDensity = UiDensity.COMFORTABLE,
                cornerRadiusDp = 12,
                panelTransparency = 0.88f,
                panelBlurIntensity = 0.80f,
                animationIntensity = AnimationIntensity.BALANCED,
                statusNotice = "Advanced appearance reset to default"
            )
        }
    }

    fun performFullFactoryReset() {
        settingsManager.fullFactoryReset()
        _uiState.update {
            it.copy(
                activeBrowserTheme = BrowserTheme.MIDNIGHT,
                customThemes = emptyList(),
                allThemes = BrowserTheme.BUILT_IN_THEMES,
                activeSearchEngine = CustomSearchEngine.BUILT_IN_ENGINES.first(),
                allSearchEngines = CustomSearchEngine.BUILT_IN_ENGINES,
                searchEngine = SearchEngine.DUCKDUCKGO,
                themeMode = ThemeMode.SYSTEM,
                colorTheme = ColorTheme.OCEAN_BLUE,
                appFontFamily = AppFontFamily.SYSTEM_DEFAULT,
                fontScale = 1.0f,
                fontWeightDelta = 0,
                applyTextZoomToWeb = true,
                uiDensity = UiDensity.COMFORTABLE,
                cornerRadiusDp = 12,
                panelTransparency = 0.88f,
                panelBlurIntensity = 0.80f,
                animationIntensity = AnimationIntensity.BALANCED,
                blockCookies = true,
                strictPopups = true,
                hardwareAccel = true,
                lowEndMode = false,
                statusNotice = "All settings reset to factory defaults"
            )
        }
    }

    fun openSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = true) }
    }

    fun closeSettingsSheet() {
        _uiState.update { it.copy(showSettingsSheet = false) }
    }

    fun openToolsSheet() {
        _uiState.update { it.copy(showToolsSheet = true) }
    }

    fun closeToolsSheet() {
        _uiState.update { it.copy(showToolsSheet = false) }
    }

    fun toggleToolsSheet() {
        _uiState.update { it.copy(showToolsSheet = !it.showToolsSheet) }
    }

    fun openAiSheet() {
        _uiState.update { it.copy(showAiSheet = true, showToolsSheet = false) }
    }

    fun closeAiSheet() {
        _uiState.update { it.copy(showAiSheet = false) }
    }

    fun openHistorySheet() {
        _uiState.update { it.copy(showHistorySheet = true, showToolsSheet = false) }
    }

    fun closeHistorySheet() {
        _uiState.update { it.copy(showHistorySheet = false) }
    }

    fun openBookmarksSheet() {
        _uiState.update { it.copy(showBookmarksSheet = true, showToolsSheet = false) }
    }

    fun closeBookmarksSheet() {
        _uiState.update { it.copy(showBookmarksSheet = false) }
    }

    fun openPrivacySheet() {
        _uiState.update { it.copy(showPrivacySheet = true, showToolsSheet = false) }
    }

    fun closePrivacySheet() {
        _uiState.update { it.copy(showPrivacySheet = false) }
    }

    fun addBookmarkForCurrentPage() {
        val tab = _uiState.value.activeTab ?: return
        val url = tab.url
        val title = tab.title.ifBlank { url }
        if (url.isNotBlank() && url != NOVA_HOME_URL) {
            val item = BookmarkItem(url = url, title = title)
            _uiState.update {
                it.copy(
                    bookmarkEntries = listOf(item) + it.bookmarkEntries.filterNot { b -> b.url == url },
                    statusNotice = "Saved to Bookmarks"
                )
            }
        }
    }

    fun removeBookmark(id: String) {
        _uiState.update {
            it.copy(
                bookmarkEntries = it.bookmarkEntries.filterNot { it.id == id },
                statusNotice = "Bookmark removed"
            )
        }
    }

    fun clearHistory() {
        _uiState.update { it.copy(historyEntries = emptyList(), statusNotice = "Browsing history cleared") }
    }

    fun toggleDesktopModeForActiveTab() {
        val tab = uiState.value.activeTab ?: return
        val newMode = !tab.isDesktopMode
        _uiState.update { current ->
            current.copy(
                tabs = current.tabs.map {
                    if (it.id == tab.id) it.copy(isDesktopMode = newMode) else it
                },
                statusNotice = if (newMode) "Desktop mode enabled" else "Mobile mode enabled"
            )
        }
        reloadActiveTab()
    }

    fun setCustomWallpaperFromUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val destFile = File(app.filesDir, "nova_custom_wallpaper.jpg")
                app.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                prefs.edit().putString(KEY_CUSTOM_WALLPAPER, destFile.absolutePath).apply()
                _uiState.update {
                    it.copy(
                        customWallpaperPath = destFile.absolutePath,
                        statusNotice = "Theme wallpaper updated"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusNotice = "Failed to load image: ${e.message}") }
            }
        }
    }

    fun removeCustomWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<Application>()
                val file = File(app.filesDir, "nova_custom_wallpaper.jpg")
                if (file.exists()) {
                    file.delete()
                }
                prefs.edit().remove(KEY_CUSTOM_WALLPAPER).apply()
                _uiState.update {
                    it.copy(
                        customWallpaperPath = null,
                        statusNotice = "Wallpaper reset to default"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(statusNotice = "Failed to reset wallpaper: ${e.message}") }
            }
        }
    }

    fun isAdBlockerActive(): Boolean {
        return _uiState.value.isAdBlockerEnabled
    }

    fun onAdBlocked(host: String) {
        val activeId = _uiState.value.activeTabId
        onResourceBlocked(tabId = activeId, resourceKey = host, blockType = AdBlockerEngine.BlockType.AD)
    }

    fun onResourceBlocked(tabId: String, resourceKey: String, blockType: AdBlockerEngine.BlockType) {
        if (resourceKey.isBlank()) return
        _uiState.update { current ->
            var isNewResourceOnTab = false
            val updatedTabs = current.tabs.map { tab ->
                if (tab.id == tabId) {
                    if (!tab.blockedResourcesOnPage.contains(resourceKey)) {
                        isNewResourceOnTab = true
                        val newSet = tab.blockedResourcesOnPage + resourceKey
                        val newAds = if (blockType == AdBlockerEngine.BlockType.AD) tab.blockedAdsCount + 1 else tab.blockedAdsCount
                        val newTrackers = if (blockType == AdBlockerEngine.BlockType.TRACKER) tab.blockedTrackersCount + 1 else tab.blockedTrackersCount
                        tab.copy(
                            blockedAdsCount = newAds,
                            blockedTrackersCount = newTrackers,
                            blockedResourcesOnPage = newSet
                        )
                    } else tab
                } else tab
            }

            if (isNewResourceOnTab) {
                val newTotal = current.totalBlockedAds + 1
                settingsManager.setTotalBlockedAds(newTotal)
                current.copy(
                    totalBlockedAds = newTotal,
                    tabs = updatedTabs
                )
            } else {
                current.copy(tabs = updatedTabs)
            }
        }
    }

    fun toggleAdBlocker() {
        val newState = !_uiState.value.isAdBlockerEnabled
        prefs.edit().putBoolean(KEY_AD_BLOCKER_ENABLED, newState).apply()
        _uiState.update {
            it.copy(
                isAdBlockerEnabled = newState,
                statusNotice = if (newState) "Ad Blocker active" else "Ad Blocker paused"
            )
        }
        reloadActiveTab()
    }
}
