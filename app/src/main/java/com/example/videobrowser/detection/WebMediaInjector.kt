package com.example.videobrowser.detection

import android.webkit.JavascriptInterface
import org.json.JSONObject

/**
 * Interface that receives callbacks from injected JavaScript inside the WebView.
 */
interface MediaDetectionListener {
    fun onDomVideoFound(
        mediaUrl: String,
        pageUrl: String,
        title: String,
        mimeType: String,
        width: Int,
        height: Int,
        duration: Double,
        isPlaying: Boolean,
        isDrm: Boolean = false,
        userInteracted: Boolean = false,
        hasControls: Boolean = true,
        isLooping: Boolean = false,
        isMuted: Boolean = false,
        posterUrl: String? = null
    )
    fun onVideoPlaybackChanged(
        mediaUrl: String,
        isPlaying: Boolean,
        currentTime: Double,
        duration: Double
    )
}

/**
 * Android Javascript Interface exposed to the webpage DOM as window.AndroidMediaBridge.
 */
class MediaBridge(private val listener: MediaDetectionListener) {

    @JavascriptInterface
    fun onVideoDetected(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val mediaUrl = json.optString("src", "")
            if (mediaUrl.isBlank() || mediaUrl.startsWith("blob:")) {
                // Ignore empty or unresolvable internal blob URLs without stream info
                if (!json.has("currentSrc") || json.optString("currentSrc").startsWith("blob:")) {
                    return
                }
            }
            val resolvedUrl = json.optString("currentSrc", mediaUrl)
            val pageUrl = json.optString("pageUrl", "")
            val title = json.optString("title", "")
            val mimeType = json.optString("type", "video/mp4")
            val width = json.optInt("videoWidth", 0)
            val height = json.optInt("videoHeight", 0)
            val duration = json.optDouble("duration", 0.0)
            val isPlaying = json.optBoolean("isPlaying", false)
            val isDrm = json.optBoolean("isDrm", false)
            val userInteracted = json.optBoolean("userInteracted", false)
            val hasControls = json.optBoolean("hasControls", true)
            val isLooping = json.optBoolean("isLooping", false)
            val isMuted = json.optBoolean("isMuted", false)
            val posterUrl = json.optString("poster", "").takeIf { it.isNotBlank() }

            listener.onDomVideoFound(
                mediaUrl = resolvedUrl,
                pageUrl = pageUrl,
                title = title,
                mimeType = mimeType,
                width = width,
                height = height,
                duration = duration,
                isPlaying = isPlaying,
                isDrm = isDrm,
                userInteracted = userInteracted,
                hasControls = hasControls,
                isLooping = isLooping,
                isMuted = isMuted,
                posterUrl = posterUrl
            )
        } catch (_: Exception) {
            // Silently protect against invalid script callbacks
        }
    }

    @JavascriptInterface
    fun onPlaybackState(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            val mediaUrl = json.optString("src", "")
            val isPlaying = json.optBoolean("isPlaying", false)
            val currentTime = json.optDouble("currentTime", 0.0)
            val duration = json.optDouble("duration", 0.0)

            listener.onVideoPlaybackChanged(
                mediaUrl = mediaUrl,
                isPlaying = isPlaying,
                currentTime = currentTime,
                duration = duration
            )
        } catch (_: Exception) {
            // Silently ignore
        }
    }

    @JavascriptInterface
    fun onStreamUrlDetected(url: String, type: String, pageTitle: String) {
        try {
            if (url.isNotBlank() && !url.startsWith("blob:") && !url.startsWith("data:")) {
                listener.onDomVideoFound(
                    mediaUrl = url,
                    pageUrl = "",
                    title = pageTitle,
                    mimeType = if (type.isNotBlank()) type else if (url.contains(".m3u8", ignoreCase = true)) "application/x-mpegURL" else "video/mp4",
                    width = 0,
                    height = 0,
                    duration = 0.0,
                    isPlaying = false,
                    isDrm = false
                )
            }
        } catch (_: Exception) {}
    }
}

/**
 * Helper to generate and inject JavaScript into the loaded webpage to detect and hook video elements.
 */
object WebMediaInjector {

    const val JS_BRIDGE_NAME = "AndroidMediaBridge"

    val DETECTION_SCRIPT = """
        (function() {
            if (window.__videoSnifferInjected) return;
            window.__videoSnifferInjected = true;

            // Hook EME DRM key system detection
            try {
                if (navigator.requestMediaKeySystemAccess && !window.__origReqMediaKey) {
                    window.__origReqMediaKey = navigator.requestMediaKeySystemAccess;
                    navigator.requestMediaKeySystemAccess = function(keySystem, supportedConfigurations) {
                        window.__emeDetected = true;
                        return window.__origReqMediaKey.apply(this, arguments);
                    };
                }
            } catch(e) {}

            function reportVideo(video, isPlaying) {
                try {
                    var src = video.currentSrc || video.src;
                    if (!src) {
                        var sourceEl = video.querySelector('source');
                        if (sourceEl) src = sourceEl.src;
                    }
                    if (!src || (src.indexOf('blob:') === 0 && !video.currentSrc)) {
                        return;
                    }

                    var width = video.videoWidth || 0;
                    var height = video.videoHeight || 0;
                    var dur = isNaN(video.duration) ? 0 : video.duration;

                    // Filter out tiny UI stickers, animated badges, or avatars (< 100x100 and < 1.0s)
                    if (width > 0 && width < 100 && height > 0 && height < 100 && dur > 0 && dur < 1.0) {
                        return;
                    }

                    var pageTitle = document.title || '';
                    try {
                        var ogTitle = document.querySelector('meta[property="og:title"]');
                        if (ogTitle && ogTitle.content) {
                            pageTitle = ogTitle.content;
                        }
                    } catch(e) {}

                    var isDrm = !!(video.mediaKeys || window.__emeDetected);
                    var hasControls = !!(video.controls || video.getAttribute('controls'));
                    var isLooping = !!video.loop;
                    var isMuted = !!video.muted;
                    var userInteracted = !!video.__userInteracted;

                    // Filter out decorative micro animations and avatars (< 120x120 and < 3.0s)
                    if (width > 0 && width < 120 && height > 0 && height < 120 && dur > 0 && dur < 3.0) {
                        return;
                    }
                    // Filter out tiny muted looping background snippets without controls
                    if (isLooping && isMuted && !hasControls && dur > 0 && dur < 3.5 && width < 240) {
                        return;
                    }

                    var poster = video.poster || video.getAttribute('poster') || '';
                    if (!poster) {
                        try {
                            var ogImg = document.querySelector('meta[property="og:image"]');
                            if (ogImg && ogImg.content) poster = ogImg.content;
                        } catch(e) {}
                    }

                    var payload = {
                        src: src,
                        currentSrc: video.currentSrc || src,
                        pageUrl: window.location.href,
                        title: pageTitle,
                        poster: poster,
                        type: video.type || (video.querySelector('source') ? video.querySelector('source').type : 'video/mp4'),
                        videoWidth: width,
                        videoHeight: height,
                        duration: dur,
                        isPlaying: !!isPlaying,
                        isDrm: isDrm,
                        userInteracted: userInteracted,
                        hasControls: hasControls,
                        isLooping: isLooping,
                        isMuted: isMuted
                    };

                    if (window.AndroidMediaBridge && window.AndroidMediaBridge.onVideoDetected) {
                        window.AndroidMediaBridge.onVideoDetected(JSON.stringify(payload));
                    }
                } catch(e) {}
            }

            // Track user interaction to prioritize the video the user actually clicked/tapped
            function onUserTouchOrClick(e) {
                try {
                    var target = e.target;
                    var videoEl = (target && target.tagName === 'VIDEO') ? target : (target && target.closest ? target.closest('video') : null);
                    if (!videoEl && target && target.closest) {
                        var player = target.closest('[class*="player"], [id*="player"], .video-container');
                        if (player) videoEl = player.querySelector('video');
                    }
                    if (videoEl) {
                        videoEl.__userInteracted = true;
                        reportVideo(videoEl, !videoEl.paused);
                    }
                } catch(err) {}
            }
            document.addEventListener('click', onUserTouchOrClick, true);
            document.addEventListener('touchstart', onUserTouchOrClick, true);

            function hookVideo(video) {
                if (video.__snifferHooked) return;
                video.__snifferHooked = true;

                reportVideo(video, !video.paused && !video.ended);

                video.addEventListener('play', function() {
                    reportVideo(video, true);
                    if (window.AndroidMediaBridge && window.AndroidMediaBridge.onPlaybackState) {
                        window.AndroidMediaBridge.onPlaybackState(JSON.stringify({
                            src: video.currentSrc || video.src,
                            isPlaying: true,
                            currentTime: video.currentTime,
                            duration: video.duration
                        }));
                    }
                });

                video.addEventListener('playing', function() {
                    reportVideo(video, true);
                });

                video.addEventListener('loadedmetadata', function() {
                    reportVideo(video, !video.paused);
                });

                video.addEventListener('pause', function() {
                    if (window.AndroidMediaBridge && window.AndroidMediaBridge.onPlaybackState) {
                        window.AndroidMediaBridge.onPlaybackState(JSON.stringify({
                            src: video.currentSrc || video.src,
                            isPlaying: false,
                            currentTime: video.currentTime,
                            duration: video.duration
                        }));
                    }
                });
            }

            // Sniff XHR and fetch for media stream URLs (.m3u8, .mpd, .mp4, .webm, manifests, etc.)
            var streamRegex = /\.(m3u8|mpd|mp4|webm|m4v|mov|mkv)(\?|$)|(\/video\/|videoplayback|playlist\.m3u8|master\.m3u8|\/manifest|\.ts(\?|$)|mime=video)/i;

            function checkAndReportStreamUrl(url) {
                if (!url || typeof url !== 'string') return;
                if (url.indexOf('blob:') === 0 || url.indexOf('data:') === 0) return;
                if (streamRegex.test(url)) {
                    if (window.AndroidMediaBridge && window.AndroidMediaBridge.onStreamUrlDetected) {
                        window.AndroidMediaBridge.onStreamUrlDetected(url, '', document.title || '');
                    }
                }
            }

            try {
                if (window.XMLHttpRequest && !window.__origXhrOpen) {
                    window.__origXhrOpen = XMLHttpRequest.prototype.open;
                    XMLHttpRequest.prototype.open = function(method, url) {
                        try {
                            checkAndReportStreamUrl(url);
                        } catch(e) {}
                        return window.__origXhrOpen.apply(this, arguments);
                    };
                }

                if (window.fetch && !window.__origFetch) {
                    window.__origFetch = window.fetch;
                    window.fetch = function(input, init) {
                        try {
                            var fetchUrl = (typeof input === 'string') ? input : (input && input.url ? input.url : '');
                            checkAndReportStreamUrl(fetchUrl);
                        } catch(e) {}
                        return window.__origFetch.apply(this, arguments);
                    };
                }
            } catch(e) {}

            // Hook HTMLMediaElement prototype play and src setter for instantaneous discovery
            try {
                if (window.HTMLMediaElement && !window.__origMediaPlay) {
                    window.__origMediaPlay = HTMLMediaElement.prototype.play;
                    HTMLMediaElement.prototype.play = function() {
                        try {
                            hookVideo(this);
                            reportVideo(this, true);
                        } catch(e) {}
                        return window.__origMediaPlay.apply(this, arguments);
                    };

                    var origSrcDesc = Object.getOwnPropertyDescriptor(HTMLMediaElement.prototype, 'src');
                    if (origSrcDesc && origSrcDesc.set) {
                        Object.defineProperty(HTMLMediaElement.prototype, 'src', {
                            set: function(val) {
                                origSrcDesc.set.call(this, val);
                                try {
                                    hookVideo(this);
                                    reportVideo(this, !this.paused);
                                } catch(e) {}
                            },
                            get: origSrcDesc.get,
                            configurable: true
                        });
                    }
                }
            } catch(e) {}

            function collectVideosFromNode(node) {
                var list = [];
                if (!node) return list;
                if (node.querySelectorAll) {
                    var vids = node.querySelectorAll('video');
                    for (var i = 0; i < vids.length; i++) list.push(vids[i]);
                }
                if (node.shadowRoot) {
                    list = list.concat(collectVideosFromNode(node.shadowRoot));
                }
                var children = node.children || [];
                for (var j = 0; j < children.length; j++) {
                    if (children[j].shadowRoot) {
                        list = list.concat(collectVideosFromNode(children[j].shadowRoot));
                    }
                }
                return list;
            }

            function scanAllVideos() {
                var videos = collectVideosFromNode(document);
                for (var i = 0; i < videos.length; i++) {
                    hookVideo(videos[i]);
                }
            }

            // Rapid debounced scanner using requestAnimationFrame (16ms latency)
            var scanScheduled = false;
            function triggerFastScan() {
                if (scanScheduled) return;
                scanScheduled = true;
                if (window.requestAnimationFrame) {
                    window.requestAnimationFrame(function() {
                        scanScheduled = false;
                        scanAllVideos();
                    });
                } else {
                    setTimeout(function() {
                        scanScheduled = false;
                        scanAllVideos();
                    }, 20);
                }
            }

            // Initial scan immediately
            scanAllVideos();

            // Observe newly added video elements via MutationObserver with immediate microtask trigger
            try {
                var observer = new MutationObserver(function(mutations) {
                    triggerFastScan();
                });
                observer.observe(document.documentElement || document.body, {
                    childList: true,
                    subtree: true
                });
            } catch(e) {}

            // SPA history hooks for instant navigation detection on dynamic sites
            try {
                if (window.history) {
                    var origPush = window.history.pushState;
                    if (origPush) {
                        window.history.pushState = function() {
                            origPush.apply(this, arguments);
                            setTimeout(scanAllVideos, 50);
                        };
                    }
                    var origReplace = window.history.replaceState;
                    if (origReplace) {
                        window.history.replaceState = function() {
                            origReplace.apply(this, arguments);
                            setTimeout(scanAllVideos, 50);
                        };
                    }
                }
                window.addEventListener('popstate', scanAllVideos);
                window.addEventListener('hashchange', scanAllVideos);
            } catch(e) {}

            // Periodic sanity scan for late loaded video embeds
            setInterval(scanAllVideos, 1500);
        })();
    """.trimIndent()
}
