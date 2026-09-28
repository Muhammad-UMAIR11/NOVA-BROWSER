package com.example

import android.app.Application
import android.system.Os
import android.util.Log
import com.example.videobrowser.detection.WebViewCacheManager
import java.io.File

/**
 * Custom Application class for VideoBrowser.
 * Initializes and sanitizes application-level resources:
 * 1. Configures Mesa driver environment variables prior to EGL loading in emulator/container
 *    environments that lack Linux DRM render nodes (/dev/dri/renderD128), preventing Mesa from
 *    failing to open nonexistent render nodes.
 * 2. Cleans up any corrupted Chromium WebView disk cache directories on startup before WebViews are created.
 */
class VideoBrowserApp : Application() {

    companion object {
        init {
            WebViewCacheManager.configureGraphicsEnvironment()
        }
    }

    override fun onCreate() {
        super.onCreate()
        WebViewCacheManager.configureGraphicsEnvironment()
        Log.i("VideoBrowserApp", "Application starting. Sanitizing WebView disk cache...")
        WebViewCacheManager.sanitizeWebViewCache(this)
    }
}

