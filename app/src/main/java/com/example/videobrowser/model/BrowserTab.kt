package com.example.videobrowser.model

import java.util.UUID

data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New Tab",
    val url: String = "about:blank",
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,
    val detectedMediaList: List<DetectedMedia> = emptyList(),
    val groupedVideos: List<GroupedVideoMedia> = emptyList(),
    val activePlayingMedia: DetectedMedia? = null,
    val errorMessage: String? = null,
    val isDesktopMode: Boolean = false,
    val blockedAdsCount: Int = 0
)
