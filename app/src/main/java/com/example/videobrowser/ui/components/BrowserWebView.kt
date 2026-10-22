package com.example.videobrowser.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.videobrowser.adblock.AdBlockerEngine
import com.example.videobrowser.detection.MediaBridge
import com.example.videobrowser.detection.WebMediaInjector
import com.example.videobrowser.detection.WebViewCacheManager
import com.example.videobrowser.ui.motion.LocalReducedMotion
import com.example.videobrowser.ui.motion.NavigationTransitionType
import com.example.videobrowser.ui.motion.pageMotionLayer
import com.example.videobrowser.ui.motion.rememberPageMotionState
import com.example.videobrowser.viewmodel.BrowserViewModel
import com.example.videobrowser.viewmodel.WebAction
import kotlinx.coroutines.launch

private const val TAG = "BrowserWebView"

/**
 * Composable that integrates an Android WebView to render web pages, handle navigation commands,
 * and facilitate real-time detection of media elements via network interception and DOM analysis.
 * Includes resilience against GPU/Mesa driver failures via onRenderProcessGone and software layer fallback.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserWebView(
    viewModel: BrowserViewModel,
    targetUrl: String,
    tabId: String = "",
    modifier: Modifier = Modifier,
    onFullscreenStateChange: (Boolean, View?, (() -> Unit)?) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val density = LocalDensity.current
    val isReducedMotion = LocalReducedMotion.current
    val pageMotionState = rememberPageMotionState()
    val coroutineScope = rememberCoroutineScope()
    var lastActionTime by remember { mutableLongStateOf(0L) }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webViewCreationError by remember { mutableStateOf<String?>(null) }
    var rendererCrashCount by remember { mutableIntStateOf(0) }
    val initialUrl = remember { targetUrl }

    // Sync external targetUrl changes with WebView with smooth subtle transform
    LaunchedEffect(targetUrl) {
        if (targetUrl.isNotBlank() && webViewInstance?.url != targetUrl) {
            try {
                lastActionTime = System.currentTimeMillis()
                launch {
                    pageMotionState.animateNavigation(
                        type = NavigationTransitionType.NEW_PAGE,
                        density = density,
                        isReducedMotion = isReducedMotion
                    )
                }
                webViewInstance?.loadUrl(targetUrl)
            } catch (_: Exception) {}
        }
    }

    // Listen to WebAction commands from ViewModel (loadUrl, goBack, goForward, reload, stop)
    LaunchedEffect(viewModel, isReducedMotion, density) {
        viewModel.webActions.collect { action ->
            try {
                when (action) {
                    is WebAction.LoadUrl -> {
                        lastActionTime = System.currentTimeMillis()
                        launch {
                            pageMotionState.animateNavigation(
                                type = NavigationTransitionType.NEW_PAGE,
                                density = density,
                                isReducedMotion = isReducedMotion
                            )
                        }
                        if (webViewInstance?.url != action.url) {
                            webViewInstance?.loadUrl(action.url)
                        }
                    }
                    is WebAction.GoBack -> {
                        lastActionTime = System.currentTimeMillis()
                        launch {
                            pageMotionState.animateNavigation(
                                type = NavigationTransitionType.BACK,
                                density = density,
                                isReducedMotion = isReducedMotion
                            )
                        }
                        if (webViewInstance?.canGoBack() == true) {
                            webViewInstance?.goBack()
                        }
                    }
                    is WebAction.GoForward -> {
                        lastActionTime = System.currentTimeMillis()
                        launch {
                            pageMotionState.animateNavigation(
                                type = NavigationTransitionType.FORWARD,
                                density = density,
                                isReducedMotion = isReducedMotion
                            )
                        }
                        if (webViewInstance?.canGoForward() == true) {
                            webViewInstance?.goForward()
                        }
                    }
                    is WebAction.Reload -> {
                        lastActionTime = System.currentTimeMillis()
                        launch {
                            pageMotionState.animateNavigation(
                                type = NavigationTransitionType.RELOAD,
                                density = density,
                                isReducedMotion = isReducedMotion
                            )
                        }
                        webViewInstance?.reload()
                    }
                    is WebAction.StopLoading -> {
                        webViewInstance?.stopLoading()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    // React safely to lifecycle events: pause timers/DOM when stopped, resume when active
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            try {
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        webViewInstance?.onPause()
                        webViewInstance?.pauseTimers()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        webViewInstance?.onResume()
                        webViewInstance?.resumeTimers()
                    }
                    else -> {}
                }
            } catch (_: Exception) {}
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (webViewCreationError != null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = webViewCreationError ?: "Unable to initialize WebView component",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = {
                            webViewCreationError = null
                            rendererCrashCount = 0
                        }
                    ) {
                        Text("Reload")
                    }
                }
            }
        }
    } else {
        key(rendererCrashCount) {
            AndroidView(
                modifier = modifier.pageMotionLayer(pageMotionState),
                factory = { ctx ->
                    try {
                        createConfiguredWebView(
                            context = ctx,
                            viewModel = viewModel,
                            currentUrl = targetUrl,
                            tabId = tabId,
                            fallbackSoftwareLayer = (rendererCrashCount > 0),
                            onRenderProcessExited = {
                                if (rendererCrashCount < 4) {
                                    rendererCrashCount++
                                } else {
                                    webViewCreationError = "Web rendering engine stopped unexpectedly. Tap below to reload."
                                }
                            },
                            onPageStartedAnimation = {
                                if (System.currentTimeMillis() - lastActionTime > 350L) {
                                    coroutineScope.launch {
                                        pageMotionState.animateNavigation(
                                            type = NavigationTransitionType.NEW_PAGE,
                                            density = density,
                                            isReducedMotion = isReducedMotion
                                        )
                                    }
                                }
                            },
                            onFullscreenStateChange = onFullscreenStateChange
                        ).also { wv ->
                            webViewInstance = wv
                            val urlToLoad = targetUrl.ifBlank { initialUrl }
                            if (urlToLoad.isNotBlank()) {
                                wv.loadUrl(urlToLoad)
                            }
                        }
                    } catch (e: Exception) {
                        webViewCreationError = "WebView initialization failed: ${e.localizedMessage ?: "Unknown error"}"
                        View(ctx)
                    }
                },
                update = { wv ->
                    if (wv is WebView) {
                        val state = viewModel.uiState.value
                        val isLowEndMode = state.lowEndMode
                        val shouldForceSoftware = (rendererCrashCount > 0) || !state.hardwareAccel || isLowEndMode || isHardwareRenderNodeUnavailable()
                        if (shouldForceSoftware && wv.layerType != View.LAYER_TYPE_SOFTWARE) {
                            wv.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                        } else if (!shouldForceSoftware && wv.layerType != View.LAYER_TYPE_HARDWARE) {
                            wv.setLayerType(View.LAYER_TYPE_HARDWARE, null)
                        }
                        wv.settings.offscreenPreRaster = !isLowEndMode
                        wv.settings.mediaPlaybackRequiresUserGesture = isLowEndMode
                        val cookieManager = android.webkit.CookieManager.getInstance()
                        cookieManager.setAcceptThirdPartyCookies(wv, !state.blockCookies)
                        if (state.applyTextZoomToWeb) {
                            val zoomPercent = (state.fontScale * 100).toInt().coerceIn(75, 175)
                            if (wv.settings.textZoom != zoomPercent) {
                                wv.settings.textZoom = zoomPercent
                            }
                        } else if (wv.settings.textZoom != 100) {
                            wv.settings.textZoom = 100
                        }
                    }
                },
                onRelease = { wv ->
                    try {
                        (wv.parent as? ViewGroup)?.removeView(wv)
                        if (wv is WebView) {
                            wv.stopLoading()
                            wv.destroy()
                        }
                    } catch (_: Exception) {}
                }
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun createConfiguredWebView(
    context: Context,
    viewModel: BrowserViewModel,
    currentUrl: String,
    tabId: String,
    fallbackSoftwareLayer: Boolean,
    onRenderProcessExited: () -> Unit,
    onPageStartedAnimation: () -> Unit = {},
    onFullscreenStateChange: (Boolean, View?, (() -> Unit)?) -> Unit
): WebView {
    WebViewCacheManager.configureGraphicsEnvironment()
    val cookieManager = android.webkit.CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    return WebView(context).apply {
        cookieManager.setAcceptThirdPartyCookies(this, !viewModel.uiState.value.blockCookies)
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val isLowEnd = viewModel.uiState.value.lowEndMode
        // Set stable renderer priority bound to hosting process with idle release
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val priority = if (isLowEnd) WebView.RENDERER_PRIORITY_WAIVED else WebView.RENDERER_PRIORITY_BOUND
                setRendererPriorityPolicy(priority, true)
            } catch (_: Exception) {}
        }

        // If the graphics hardware driver (e.g. MESA rendernode) is unavailable in this environment,
        // or performance mode (low-end device mode) is active, gracefully fall back to software rendering layer
        val forceSoftwareLayer = fallbackSoftwareLayer ||
            !viewModel.uiState.value.hardwareAccel ||
            isLowEnd ||
            isHardwareRenderNodeUnavailable()
        if (forceSoftwareLayer) {
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }

        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            builtInZoomControls = true
            displayZoomControls = false
            setSupportZoom(true)
            allowContentAccess = true
            allowFileAccess = false
            // On low-end hardware, avoid aggressive video decoding until user explicitly interacts
            mediaPlaybackRequiresUserGesture = isLowEnd
            // Conserve RAM and GPU memory on low-end devices by avoiding pre-rendering hidden offscreen tiles
            offscreenPreRaster = !isLowEnd
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            cacheMode = WebSettings.LOAD_DEFAULT

            val isDesktop = viewModel.uiState.value.activeTab?.isDesktopMode == true
            if (isDesktop) {
                userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            } else {
                val defaultUa = try {
                    WebSettings.getDefaultUserAgent(context)
                } catch (_: Exception) {
                    userAgentString ?: ""
                }
                userAgentString = "$defaultUa MobileBrowser/1.0"
            }
        }

        // Bridge to receive DOM events from JavaScript
        addJavascriptInterface(MediaBridge(listener = viewModel), WebMediaInjector.JS_BRIDGE_NAME)

        // Handle direct file downloads initiated from links
        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            viewModel.onDownloadRequestedFromWebView(
                url = url,
                userAgent = userAgent ?: "",
                contentDisposition = contentDisposition ?: "",
                mimeType = mimetype ?: "",
                contentLength = contentLength
            )
        }

        webViewClient = object : WebViewClient() {
            private var isViewDestroyed = false

            override fun onRenderProcessGone(
                view: WebView,
                detail: RenderProcessGoneDetail
            ): Boolean {
                val didCrash = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    detail.didCrash()
                } else {
                    true
                }
                Log.w(TAG, "WebView render process exited: didCrash=$didCrash")
                if (!isViewDestroyed) {
                    isViewDestroyed = true
                    try {
                        (view.parent as? ViewGroup)?.removeView(view)
                        view.destroy()
                    } catch (_: Exception) {}
                }
                onRenderProcessExited()
                // Return true to indicate the host application safely handled the termination,
                // preventing Android from killing the entire app process.
                return true
            }

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                try {
                    val reqUrl = request.url?.toString() ?: ""
                    val headers = request.requestHeaders ?: emptyMap()
                    val pageUrl = headers["Referer"] ?: currentUrl

                    // 1. Primary Feature: Always-on Ad & Tracker Blocker across every site
                    if (viewModel.isAdBlockerActive()) {
                        val blockType = AdBlockerEngine.classifyRequest(request.url, isForMainFrame = request.isForMainFrame)
                        if (blockType != AdBlockerEngine.BlockType.NONE) {
                            val host = request.url?.host ?: ""
                            val path = request.url?.path ?: ""
                            val resourceKey = "$host$path"
                            val targetTabId = tabId.ifBlank { viewModel.uiState.value.activeTabId }
                            viewModel.onResourceBlocked(tabId = targetTabId, resourceKey = resourceKey, blockType = blockType)
                            return AdBlockerEngine.createBlockedResponse(reqUrl)
                        }
                    }

                    // 2. Video & Media Network Sniffer
                    if (reqUrl.isNotBlank()) {
                        viewModel.onNetworkResourceIntercepted(reqUrl, headers, pageUrl)
                    }
                } catch (_: Exception) {}
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                onPageStartedAnimation()
                viewModel.onPageStarted(url)
                viewModel.onNavigationStateChanged(view.canGoBack(), view.canGoForward())

                // Inject Ad Blocker cosmetic CSS & DOM element sweeper
                if (viewModel.isAdBlockerActive()) {
                    try {
                        view.evaluateJavascript(AdBlockerEngine.COSMETIC_BLOCK_SCRIPT, null)
                    } catch (_: Exception) {}
                }

                try {
                    view.evaluateJavascript(WebMediaInjector.DETECTION_SCRIPT, null)
                } catch (_: Exception) {}
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                viewModel.onPageFinished(url, view.title)
                viewModel.onNavigationStateChanged(view.canGoBack(), view.canGoForward())
                try {
                    android.webkit.CookieManager.getInstance().flush()
                } catch (_: Exception) {}

                // Sweep remaining ad elements
                if (viewModel.isAdBlockerActive()) {
                    try {
                        view.evaluateJavascript(AdBlockerEngine.COSMETIC_BLOCK_SCRIPT, null)
                    } catch (_: Exception) {}
                }

                // Inject video detection and DOM mutation observer script
                try {
                    view.evaluateJavascript(WebMediaInjector.DETECTION_SCRIPT, null)
                } catch (_: Exception) {}
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    viewModel.onPageError(
                        errorCode = error.errorCode,
                        description = error.description?.toString() ?: "Unknown error",
                        failingUrl = request.url?.toString() ?: ""
                    )
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                // Prevent ad popunder / market schemes
                val isStrict = viewModel.uiState.value.strictPopups
                if (viewModel.isAdBlockerActive() && AdBlockerEngine.shouldBlockNavigation(request.url, isStrictPopups = isStrict)) {
                    val targetTabId = tabId.ifBlank { viewModel.uiState.value.activeTabId }
                    val host = request.url?.host ?: ""
                    viewModel.onResourceBlocked(tabId = targetTabId, resourceKey = host, blockType = AdBlockerEngine.BlockType.AD)
                    return true
                }

                val url = request.url?.toString() ?: return false
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    return false
                }
                // Handle external intents safely (e.g. tel, mailto, maps, intent://)
                try {
                    val intent = if (url.startsWith("intent://")) {
                        Intent.parseUri(url, Intent.URI_INTENT_SCHEME).apply {
                            addCategory(Intent.CATEGORY_BROWSABLE)
                            component = null
                            selector = null
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } else {
                        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                    context.startActivity(intent)
                    return true
                } catch (_: Exception) {
                    return true
                }
            }
        }

        webChromeClient = object : WebChromeClient() {
            private var customView: View? = null
            private var customViewCallback: CustomViewCallback? = null

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                viewModel.onProgressChanged(newProgress)

                // Proactively inject detection script early as DOM nodes become available
                if (newProgress in 20..35 || newProgress in 50..65 || newProgress >= 80) {
                    try {
                        view.evaluateJavascript(WebMediaInjector.DETECTION_SCRIPT, null)
                    } catch (_: Exception) {}
                }
            }

            override fun onReceivedTitle(view: WebView, title: String?) {
                super.onReceivedTitle(view, title)
                viewModel.onPageFinished(view.url ?: "", title)
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView = view
                customViewCallback = callback
                onFullscreenStateChange(true, view) {
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                    customView = null
                    onFullscreenStateChange(false, null, null)
                }
            }

            override fun onHideCustomView() {
                customView = null
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                onFullscreenStateChange(false, null, null)
            }
        }
    }
}

/**
 * Detects whether the current device is a virtualized / containerized emulator
 * that lacks Linux DRM render node devices (/dev/dri/renderD128).
 * In such environments, falling back to a software layer prevents Mesa from failing to open rendernodes.
 */
private fun isHardwareRenderNodeUnavailable(): Boolean {
    val driMissing = try {
        !java.io.File("/dev/dri/renderD128").exists() && !java.io.File("/dev/dri").exists()
    } catch (_: Throwable) {
        true
    }
    return driMissing || isEmulatorEnvironment()
}

private fun isEmulatorEnvironment(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.MODEL.contains("google_sdk", ignoreCase = true) ||
            Build.MODEL.contains("Emulator", ignoreCase = true) ||
            Build.MODEL.contains("Android SDK built for", ignoreCase = true) ||
            Build.HARDWARE.contains("goldfish", ignoreCase = true) ||
            Build.HARDWARE.contains("ranchu", ignoreCase = true) ||
            Build.HARDWARE.contains("cutf", ignoreCase = true) ||
            Build.HARDWARE.contains("qemu", ignoreCase = true) ||
            Build.PRODUCT.contains("sdk", ignoreCase = true) ||
            Build.PRODUCT.contains("google_sdk", ignoreCase = true) ||
            Build.MANUFACTURER.contains("Genymotion", ignoreCase = true) ||
            Build.BRAND.startsWith("generic") ||
            Build.DEVICE.startsWith("generic")
}
