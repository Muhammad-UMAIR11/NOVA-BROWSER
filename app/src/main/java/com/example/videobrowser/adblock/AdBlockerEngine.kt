package com.example.videobrowser.adblock

import android.net.Uri
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream
import java.util.Locale

/**
 * High-performance, zero-configuration Ad & Tracker Blocker Engine for Nova Browser.
 * Starts active by default across all websites with zero setup needed.
 *
 * Features:
 * 1. Sub-millisecond network request interception (DoubleClick, AdSense, Taboola, Outbrain, Criteo, PopAds, etc.).
 * 2. Deep cosmetic DOM element filtering & debounced MutationObserver ad sweeping.
 * 3. Aggressive popup, pop-under, and deceptive redirect prevention.
 * 4. Tracking beacon & telemetry blocking to protect user privacy and accelerate page load speeds.
 * 5. Accurate request classification (AD, TRACKER, or NONE) and false-positive prevention.
 */
object AdBlockerEngine {

    enum class BlockType {
        NONE,
        AD,
        TRACKER
    }

    // Comprehensive curated set of known ad network, programmatic DSP, and popup domains
    private val AD_DOMAINS: Set<String> = hashSetOf(
        // Google Ad ecosystem
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "admob.com",

        // Major programmatic ad networks & DSPs
        "adnxs.com",
        "appnexus.com",
        "criteo.com",
        "criteo.net",
        "rubiconproject.com",
        "magnite.com",
        "pubmatic.com",
        "openx.net",
        "casalemedia.com",
        "inmobi.com",
        "smaato.net",
        "smartadserver.com",
        "adsrvr.org",
        "bidswitch.net",
        "contextweb.com",
        "sharethrough.com",
        "adform.net",
        "sovrn.com",
        "zemanta.com",
        "yieldmo.com",
        "advertising.com",
        "exponential.com",
        "media.net",
        "amazon-adsystem.com",
        "carbonads.net",
        "buysellads.com",
        "nativeads.com",
        "zergnet.com",
        "content.ad",
        "triplelift.com",
        "undertone.com",
        "gumgum.com",
        "indexexchange.com",
        "smartclip.net",
        "teads.tv",
        "adtech.de",
        "adtechus.com",
        "serving-sys.com",
        "flashtalking.com",
        "conversantmedia.com",
        "dotomi.com",
        "matomy.com",

        // Content recommendation & native ad widgets (Taboola, Outbrain, MGID, Revcontent)
        "taboola.com",
        "outbrain.com",
        "revcontent.com",
        "mgid.com",
        "ligatus.com",
        "plista.com",
        "engageya.com",
        "adblade.com",

        // Aggressive popups, pop-unders & deceptive redirects
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "adcash.com",
        "exoclick.com",
        "juicyads.com",
        "trafficjunky.com",
        "clickadu.com",
        "hilltopads.com",
        "adsterra.com",
        "monetag.com",
        "richpush.com",
        "zeroredirect.com",
        "adsupply.com",
        "ero-advertising.com",
        "popunder.net",
        "trafficstars.com",

        // Mobile in-app / SDK ad networks
        "applovin.com",
        "unityads.unity3d.com",
        "ironsrc.com",
        "adcolony.com",
        "vungle.com",
        "chartboost.com",
        "fyber.com",
        "mintegral.com",
        "pangle.io",
        "leadbolt.com",
        "startapp.com",

        // Web publisher ad management networks
        "mediavine.com",
        "adthrive.com",
        "raptive.com",
        "ezoic.com",
        "ezoic.net",
        "monetizemore.com",
        "infolinks.com",
        "bidvertiser.com",
        "sovrn.co",
        "lijit.com"
    )

    // Curated set of known tracking, analytics, and telemetric domains
    private val TRACKER_DOMAINS: Set<String> = hashSetOf(
        // Web analytics, audience profiling & data brokers
        "google-analytics.com",
        "googletagservices.com",
        "googletagmanager.com",
        "scorecardresearch.com",
        "quantserve.com",
        "moatads.com",
        "chartbeat.com",
        "hotjar.com",
        "mouseflow.com",
        "crazyegg.com",
        "fullstory.com",
        "tealiumiq.com",
        "segment.com",
        "segment.io",
        "mixpanel.com",
        "amplitude.com",
        "heap.io",
        "clarity.ms",
        "newrelic.com",
        "nr-data.net",
        "mc.yandex.ru",
        "statcounter.com",
        "clicky.com",
        "omtrdc.net",
        "demdex.net",
        "everesttech.net",
        "app-measurement.com",
        "braze.com",
        "optimizely.com",

        // Social tracking & ad conversion pixels
        "analytics.tiktok.com",
        "static.ads-twitter.com",
        "ct.pinterest.com",
        "snap.licdn.com",

        // Attribution & device fingerprinting
        "branch.io",
        "appsflyer.com",
        "adjust.com",
        "singular.net",
        "kochava.com",
        "attributionapp.com"
    )

    // Common ad script path keywords
    private val AD_URL_PATTERNS = listOf(
        "/ads.js",
        "/prebid.js",
        "/adsbygoogle.js",
        "/show_ads.js",
        "/ad_status.js",
        "/pagead/js/",
        "/gpt/pubads_",
        "/ad-banner/",
        "/ad_banner/",
        "/banner_ad.js"
    )

    // Known tracking script path keywords & pixel endpoints
    private val TRACKER_URL_PATTERNS = listOf(
        "connect.facebook.net/en_us/fbevents.js",
        "connect.facebook.net/signals/config/",
        "facebook.com/tr/",
        "facebook.com/tr?",
        "analytics.tiktok.com/i18n/pixel/",
        "ads.twitter.com/uwt.js",
        "static.ads-twitter.com/uwt.js",
        "snap.licdn.com/li.lms-analytics/",
        "sc-static.net/scevent.min.js",
        "ct.pinterest.com/v3/",
        "google-analytics.com/analytics.js",
        "google-analytics.com/g/collect",
        "google-analytics.com/collect",
        "googletagmanager.com/gtag/js",
        "googletagmanager.com/gtm.js",
        "hotjar.com/c/hotjar-",
        "clarity.ms/tag/"
    )

    // Safety whitelist for root domains where specific services reside
    private val LEGITIMATE_ROOTS_WHITELIST: Set<String> = hashSetOf(
        "wikipedia.org",
        "github.com",
        "google.com",
        "apple.com",
        "microsoft.com",
        "mozilla.org",
        "amazon.com",
        "walmart.com",
        "target.com",
        "ebay.com",
        "fedex.com",
        "ups.com",
        "dhl.com",
        "usps.com"
    )

    /**
     * Inspects a requested URI and determines whether it is an ad or tracker.
     */
    fun isAdOrTracker(uri: Uri?): Boolean {
        return classifyRequest(uri) != BlockType.NONE
    }

    /**
     * Classifies a network request into AD, TRACKER, or NONE.
     * Prevents false positives by ensuring legitimate sites (e.g. ad.nl, tracking.fedex.com, bannerhealth.com)
     * are never misidentified as ads or trackers.
     */
    fun classifyRequest(uri: Uri?, isForMainFrame: Boolean = false): BlockType {
        if (uri == null) return BlockType.NONE
        // Never block main frame document loads through shouldInterceptRequest
        if (isForMainFrame) return BlockType.NONE

        val host = uri.host?.lowercase(Locale.ROOT) ?: return BlockType.NONE
        val urlString = uri.toString().lowercase(Locale.ROOT)

        // 1. Direct or parent domain match for dedicated ad networks
        if (isHostInSet(host, AD_DOMAINS)) {
            return BlockType.AD
        }

        // 2. Direct or parent domain match for dedicated tracker/analytics domains
        if (isHostInSet(host, TRACKER_DOMAINS)) {
            return BlockType.TRACKER
        }

        // 3. Subdomain check for dedicated ad servers (requires >= 3 domain parts to avoid false positives like ad.nl)
        val hostParts = host.split(".")
        if (hostParts.size >= 3) {
            val subPrefix = hostParts[0]
            if (subPrefix == "adserver" || subPrefix == "adservice" || subPrefix == "ads" || subPrefix == "ad") {
                val root = "${hostParts[hostParts.size - 2]}.${hostParts[hostParts.size - 1]}"
                if (!LEGITIMATE_ROOTS_WHITELIST.contains(root)) {
                    return BlockType.AD
                }
            }
        }

        // 4. Known ad script URL pattern match
        for (pattern in AD_URL_PATTERNS) {
            if (urlString.contains(pattern)) {
                return BlockType.AD
            }
        }

        // 5. Known tracking script URL pattern match
        for (pattern in TRACKER_URL_PATTERNS) {
            if (urlString.contains(pattern)) {
                return BlockType.TRACKER
            }
        }

        return BlockType.NONE
    }

    /**
     * Efficient hierarchical domain matching against a hash set.
     * Checks exact host and parent domain segments in O(parts) lookups.
     * e.g., "sub.adservice.google.com" checks "sub.adservice.google.com", "adservice.google.com", "google.com".
     */
    fun isHostInSet(host: String, domainSet: Set<String>): Boolean {
        if (domainSet.contains(host)) return true
        var dotIndex = host.indexOf('.')
        while (dotIndex != -1 && dotIndex < host.length - 1) {
            val sub = host.substring(dotIndex + 1)
            if (domainSet.contains(sub)) return true
            dotIndex = host.indexOf('.', dotIndex + 1)
        }
        return false
    }

    /**
     * Checks if a navigation URL should be blocked (e.g. deceptive ad pop-ups and intent hijackings).
     */
    fun shouldBlockNavigation(uri: Uri?, isStrictPopups: Boolean = true): Boolean {
        if (uri == null) return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""

        // Block suspicious schemes originating from ad redirects (e.g. market, intent, itms-appss)
        if (scheme == "market" || scheme == "intent" || scheme == "itms-appss" || scheme == "itms-apps") {
            return true
        }

        // Block navigation directly into known pop-up / redirection ad networks
        if (isHostInSet(host, AD_DOMAINS)) {
            return true
        }

        return false
    }

    // 1x1 transparent GIF byte array
    private val TRANSPARENT_1X1_GIF = byteArrayOf(
        0x47, 0x49, 0x46, 0x38, 0x39, 0x61, 0x01, 0x00,
        0x01, 0x00, 0x80.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00,
        0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0x21, 0xF9.toByte(), 0x04,
        0x01, 0x00, 0x00, 0x00, 0x00, 0x2C, 0x00, 0x00,
        0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x02,
        0x02, 0x44, 0x01, 0x00, 0x3B
    )

    private val SAFE_STUB_JS = """
        (function() {
            window.adsbygoogle = window.adsbygoogle || [];
            window.ga = window.ga || function(){};
            window.gtag = window.gtag || function(){};
            window.fbq = window.fbq || function(){};
            window._fbq = window._fbq || window.fbq;
        })();
    """.trimIndent().toByteArray(Charsets.UTF_8)

    /**
     * Creates an optimized WebResourceResponse tailored to the blocked resource type.
     * JavaScript scripts receive benign no-op SDK stubs so host page code doesn't throw
     * unhandled TypeErrors. Tracking beacons and pixels receive a 1x1 transparent image
     * so layout images don't display broken icon artifacts.
     */
    fun createBlockedResponse(url: String): WebResourceResponse {
        val lower = url.lowercase(Locale.ROOT)
        val isScript = lower.endsWith(".js") ||
                lower.contains(".js?") ||
                lower.contains("/js/") ||
                lower.contains("adsbygoogle") ||
                lower.contains("analytics") ||
                lower.contains("fbevents") ||
                lower.contains("gtag") ||
                lower.contains("gtm") ||
                lower.contains("prebid")

        if (isScript) {
            return WebResourceResponse(
                "application/javascript",
                "utf-8",
                200,
                "OK",
                mapOf(
                    "Cache-Control" to "no-store, no-cache",
                    "Access-Control-Allow-Origin" to "*"
                ),
                ByteArrayInputStream(SAFE_STUB_JS)
            )
        }

        val isImage = lower.endsWith(".gif") ||
                lower.endsWith(".png") ||
                lower.endsWith(".jpg") ||
                lower.endsWith(".jpeg") ||
                lower.endsWith(".webp") ||
                lower.contains("/tr?") ||
                lower.contains("/tr/") ||
                lower.contains("/collect?") ||
                lower.contains("/collect/") ||
                lower.contains("beacon")

        if (isImage) {
            return WebResourceResponse(
                "image/gif",
                "binary",
                200,
                "OK",
                mapOf(
                    "Cache-Control" to "no-store, no-cache",
                    "Access-Control-Allow-Origin" to "*"
                ),
                ByteArrayInputStream(TRANSPARENT_1X1_GIF)
            )
        }

        return createEmptyResponse()
    }

    /**
     * Returns an empty WebResourceResponse to immediately terminate the ad request with zero overhead.
     */
    fun createEmptyResponse(): WebResourceResponse {
        return WebResourceResponse(
            "text/plain",
            "utf-8",
            200,
            "OK",
            mapOf("Cache-Control" to "no-store, no-cache"),
            ByteArrayInputStream(ByteArray(0))
        )
    }

    /**
     * Injected CSS & DOM JavaScript to cleanly hide all remaining ad placeholders,
     * video pre-roll ad containers, and sticky overlays without breaking page layout.
     * Debounced to eliminate CPU churn and prevent observer recursion.
     */
    val COSMETIC_BLOCK_SCRIPT: String = """
        (function() {
            var STYLE_ID = 'nova-adblock-styles';
            if (!document.getElementById(STYLE_ID)) {
                var css = `
                    ins.adsbygoogle, [class*="adsbygoogle"], [id*="google_ads"],
                    [id*="banner-ad"], [class*="ad-banner"], [class*="banner-ad"],
                    [class*="sponsored-"], [id*="sponsored-"],
                    .taboola, .outbrain, .mgid, .revcontent,
                    iframe[src*="doubleclick"], iframe[src*="adservice"],
                    iframe[src*="adsystem"], iframe[src*="adnxs"],
                    div[data-ad-unit], div[data-ad-client], div[data-ad-slot],
                    .ad-container, .advertisement-container, #ad-wrapper, .ad_box,
                    .banner_ad, .ad-slot, .ad_unit, [id^="dfp-ad-"],
                    .native-ad, .sponsored-content, .sticky-ad, .floating-ad,
                    .video-ad-overlay, .ad-showing .video-ads,
                    [class*="popup-ad"], [id*="popup-ad"] {
                        display: none !important;
                        visibility: hidden !important;
                        height: 0 !important;
                        min-height: 0 !important;
                        width: 0 !important;
                        opacity: 0 !important;
                        pointer-events: none !important;
                        margin: 0 !important;
                        padding: 0 !important;
                    }
                `;
                var styleEl = document.createElement('style');
                styleEl.id = STYLE_ID;
                styleEl.type = 'text/css';
                styleEl.textContent = css;

                function attachStyle() {
                    if (document.getElementById(STYLE_ID)) return;
                    var target = document.head || document.documentElement || document.body;
                    if (target) {
                        target.appendChild(styleEl);
                    } else if (document.addEventListener) {
                        document.addEventListener('DOMContentLoaded', attachStyle, { once: true });
                    }
                }
                attachStyle();
            }

            if (window.__nova_adblock_sweeper_active) return;
            window.__nova_adblock_sweeper_active = true;

            var sweepTimeout = null;
            function debouncedSweep() {
                if (sweepTimeout) return;
                sweepTimeout = setTimeout(function() {
                    sweepTimeout = null;
                    try {
                        var selectors = [
                            'ins.adsbygoogle',
                            '.taboola',
                            '.outbrain',
                            '.mgid',
                            'iframe[src*="doubleclick"]',
                            'iframe[src*="adservice"]',
                            '.ad-container'
                        ];
                        for (var s = 0; s < selectors.length; s++) {
                            var nodes = document.querySelectorAll(selectors[s]);
                            for (var i = 0; i < nodes.length; i++) {
                                var n = nodes[i];
                                if (n && n.parentNode) {
                                    n.style.display = 'none';
                                }
                            }
                        }
                    } catch(e) {}
                }, 200);
            }

            debouncedSweep();
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', debouncedSweep);
            }

            // Safe, throttled MutationObserver that hides rather than removes to prevent observer feedback loops
            try {
                var obs = new MutationObserver(function() {
                    debouncedSweep();
                });
                obs.observe(document.documentElement || document.body, {
                    childList: true,
                    subtree: true
                });
            } catch(e) {}
        })();
    """.trimIndent()
}
