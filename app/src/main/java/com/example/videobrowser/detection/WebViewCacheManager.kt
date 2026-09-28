package com.example.videobrowser.detection

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Manages and sanitizes the Chromium WebView cache directory structure.
 *
 * Chromium's Simple Cache Backend expects only specific cache entry files inside
 * `cache/WebView/Default/HTTP Cache/`. If subdirectories (such as a misplaced `Code Cache` or `js`)
 * exist inside `HTTP Cache/`, Simple Cache Backend fails with:
 *   [ERROR:simple_backend_impl.cc(747)] Simple Cache Backend: wrong file structure on disk: 8
 *   [ERROR:simple_version_upgrade.cc(151)] Failed to write a new fake index.
 *   [ERROR:disk_cache.cc(216)] Unable to create cache
 *
 * This utility inspects the cache directory prior to WebView instantiation, detects corrupted
 * or invalid directory structures, and cleans them so Chromium can successfully create and maintain
 * its disk cache without errors.
 */
object WebViewCacheManager {

    private const val TAG = "WebViewCacheManager"

    /**
     * Inspects and sanitizes the WebView cache directory.
     * Deletes corrupted HTTP Cache directories containing misplaced subdirectories
     * like `Code Cache/js` or broken fake index files.
     */
    fun sanitizeWebViewCache(context: Context) {
        try {
            val cacheDir = context.cacheDir ?: return
            val webViewDir = File(cacheDir, "WebView")
            if (!webViewDir.exists()) return

            // Search in Default profile and any other profile directories
            val profileDirs = webViewDir.listFiles { file -> file.isDirectory } ?: arrayOf(File(webViewDir, "Default"))
            for (profileDir in profileDirs) {
                sanitizeProfileCache(profileDir)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during WebView cache sanitization: ${e.message}", e)
        }
    }

    private fun sanitizeProfileCache(profileDir: File) {
        try {
            val httpCacheDir = File(profileDir, "HTTP Cache")
            if (httpCacheDir.exists()) {
                // Check for misplaced subdirectories like "Code Cache" inside "HTTP Cache"
                val subdirs = httpCacheDir.listFiles { file -> file.isDirectory }
                val wrongCodeCache = File(httpCacheDir, "Code Cache")
                val hasMisplacedDirs = !subdirs.isNullOrEmpty() || wrongCodeCache.exists()

                // Check for corrupted fake index (e.g. index file present without valid the-real-index)
                val fakeIndex = File(httpCacheDir, "index")
                val realIndex = File(httpCacheDir, "the-real-index")
                val hasCorruptFakeIndex = fakeIndex.exists() && (!realIndex.exists() || fakeIndex.length() == 0L)

                if (hasMisplacedDirs || hasCorruptFakeIndex) {
                    Log.w(
                        TAG,
                        "Detected corrupted Simple Cache structure in ${httpCacheDir.absolutePath} " +
                                "(subdirs=${subdirs?.size ?: 0}, misplacedCodeCache=${wrongCodeCache.exists()}, corruptIndex=$hasCorruptFakeIndex). Cleaning..."
                    )
                    val deleted = httpCacheDir.deleteRecursively()
                    Log.i(TAG, "Corrupted HTTP Cache cleaned successfully: $deleted")
                }
            }

            // Ensure legitimate Default/Code Cache has proper write permissions
            val legitimateCodeCache = File(profileDir, "Code Cache")
            if (legitimateCodeCache.exists() && !legitimateCodeCache.canWrite()) {
                Log.w(TAG, "Legitimate Code Cache directory is unwritable. Resetting: ${legitimateCodeCache.absolutePath}")
                legitimateCodeCache.deleteRecursively()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sanitizing profile cache in ${profileDir.name}: ${e.message}", e)
        }
    }

    /**
     * In containerized and virtualized emulator environments lacking Linux DRM render nodes (/dev/dri/renderD128),
     * Mesa's OpenGL driver attempts to open the render node and outputs:
     *   "E/MESA : Failed to open rendernode: No such file or directory"
     * By setting LIBGL_ALWAYS_SOFTWARE=1, MESA_LOADER_DRIVER_OVERRIDE=swrast, and EGL_LOG_LEVEL=fatal
     * before EGL loads, Mesa switches to software rasterization directly without probing the nonexistent rendernode.
     */
    fun configureGraphicsEnvironment() {
        try {
            val renderNode = File("/dev/dri/renderD128")
            val hasRenderNode = try {
                renderNode.exists() || (File("/dev/dri").listFiles()?.any { it.name.startsWith("renderD") } == true)
            } catch (_: Throwable) {
                false
            }

            if (!hasRenderNode) {
                android.system.Os.setenv("LIBGL_ALWAYS_SOFTWARE", "1", true)
                android.system.Os.setenv("MESA_LOADER_DRIVER_OVERRIDE", "swrast", true)
                android.system.Os.setenv("EGL_LOG_LEVEL", "fatal", true)
                android.system.Os.setenv("MESA_DEBUG", "0", true)
                android.system.Os.setenv("MESA_NO_ERROR", "1", true)
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Graphics environment configuration skipped: ${e.message}")
        }
    }

    /**
     * Completely purges the WebView HTTP disk cache and code cache.
     * Can be invoked from settings or when a critical rendering/cache error is detected.
     */
    fun clearWebViewDiskCache(context: Context) {
        try {
            val cacheDir = context.cacheDir ?: return
            val webViewDir = File(cacheDir, "WebView")
            if (webViewDir.exists()) {
                val profileDirs = webViewDir.listFiles { file -> file.isDirectory } ?: arrayOf(File(webViewDir, "Default"))
                for (profile in profileDirs) {
                    File(profile, "HTTP Cache").deleteRecursively()
                    File(profile, "Code Cache").deleteRecursively()
                }
                Log.i(TAG, "Purged WebView disk cache")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing WebView disk cache: ${e.message}", e)
        }
    }
}
