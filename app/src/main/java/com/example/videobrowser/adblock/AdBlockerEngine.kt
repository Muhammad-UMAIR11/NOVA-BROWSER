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
 * 2. Deep cosmetic DOM element filtering & continuous MutationObserver ad sweeping.
 * 3. Aggressive popup, pop-under, and deceptive redirect prevention.
 * 4. Tracking beacon & telemetry blocking to protect user privacy and accelerate page load speeds.
 */
object AdBlockerEngine {

    // Comprehensive curated set of known ad network, tracker, and telemetric domains
    private val AD_DOMAINS: Set<String> = hashSetOf(
        // Google Ad & Tracking ecosystem
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "pagead2.googlesyndication.com",
        "tpc.googlesyndication.com",
        "admob.com",
        "google-analytics.com",
        "googletagservices.com",
        "googletagmanager.com",

        // Major programmatic ad networks & DSPs
        "adnxs.com",
        "appnexus.com",
        "criteo.com",
        "criteo.net",
        "rubiconproject.com",
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

        // Content recommendation & native ad widgets (Taboola, Outbrain, MGID, Revcontent)
        "taboola.com",
        "outbrain.com",
        "revcontent.com",
        "mgid.com",
        "ligatus.com",
        "plista.com",

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

        // Mobile app & in-stream ad networks
        "applovin.com",
        "unityads.unity3d.com",
        "ironsrc.com",
        "adcolony.com",
        "vungle.com",
        "chartboost.com",
        "fyber.com",
        "mintegral.com",
        "pangle.io",

        // Trackers, audience profiling & data brokers
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
        "branch.io",
        "appsflyer.com",
        "adjust.com"
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
        "connect.facebook.net/en_US/fbevents.js",
        "analytics.tiktok.com/i18n/pixel/",
        "ads.twitter.com/uwt.js"
    )

    /**
     * Inspects a requested URI and determines if it belongs to an ad network or tracking service.
     */
    fun isAdOrTracker(uri: Uri?): Boolean {
        if (uri == null) return false
        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        val urlString = uri.toString().lowercase(Locale.ROOT)

        // 1. Direct or parent domain match
        if (isDomainBlocked(host)) {
            return true
        }

        // 2. Subdomain check for common ad prefixes
        if (host.startsWith("ads.") ||
            host.startsWith("ad.") ||
            host.startsWith("adserver.") ||
            host.startsWith("adservice.") ||
            host.startsWith("banner.") ||
            host.startsWith("telemetry.") ||
            host.startsWith("tracking.")
        ) {
            return true
        }

        // 3. Known ad script URL pattern match
        for (pattern in AD_URL_PATTERNS) {
            if (urlString.contains(pattern)) {
                return true
            }
        }

        return false
    }

    /**
     * Fast hierarchical domain matching: checks host and any parent domain suffixes
     * e.g. "pagead2.googlesyndication.com" matches "googlesyndication.com".
     */
    private fun isDomainBlocked(host: String): Boolean {
        if (AD_DOMAINS.contains(host)) return true

        val parts = host.split(".")
        if (parts.size >= 2) {
            // Check last two parts: e.g. "doubleclick.net"
            val rootDomain = "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
            if (AD_DOMAINS.contains(rootDomain)) return true

            // Check last three parts: e.g. "adservice.google.com" or "unityads.unity3d.com"
            if (parts.size >= 3) {
                val subRootDomain = "${parts[parts.size - 3]}.${parts[parts.size - 2]}.${parts[parts.size - 1]}"
                if (AD_DOMAINS.contains(subRootDomain)) return true
            }
        }
        return false
    }

    /**
     * Checks if a navigation URL should be blocked (e.g. deceptive ad pop-ups and intent hijackings).
     */
    fun shouldBlockNavigation(uri: Uri?): Boolean {
        if (uri == null) return false
        val scheme = uri.scheme?.lowercase(Locale.ROOT) ?: ""
        val host = uri.host?.lowercase(Locale.ROOT) ?: ""

        // Block suspicious schemes originating from ad redirects (e.g. market, intent, itms-appss)
        if (scheme == "market" || scheme == "intent" || scheme == "itms-appss" || scheme == "itms-apps") {
            return true
        }

        // Block navigation directly into known pop-up / redirection ad networks
        if (isDomainBlocked(host)) {
            return true
        }

        return false
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
     * video pre-roll ad containers, sticky overlays, and block unprompted popup windows.
     */
    val COSMETIC_BLOCK_SCRIPT: String = """
        (function() {
            if (window.__nova_adblock_initialized) return;
            window.__nova_adblock_initialized = true;

            // 1. Inject Comprehensive Anti-Ad Stylesheet
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
            styleEl.type = 'text/css';
            styleEl.appendChild(document.createTextNode(css));
            (document.head || document.documentElement).appendChild(styleEl);

            // 2. Active DOM node sweeper
            function sweepAdElements() {
                try {
                    var selectors = [
                        'ins.adsbygoogle',
                        '[class*="adsbygoogle"]',
                        '.taboola',
                        '.outbrain',
                        '.mgid',
                        'iframe[src*="doubleclick"]',
                        'iframe[src*="adservice"]',
                        '.ad-container',
                        '.ad_box',
                        '.advertisement'
                    ];
                    selectors.forEach(function(sel) {
                        var nodes = document.querySelectorAll(sel);
                        for (var i = 0; i < nodes.length; i++) {
                            nodes[i].remove();
                        }
                    });
                } catch(e) {}
            }

            sweepAdElements();
            if (document.readyState === 'loading') {
                document.addEventListener('DOMContentLoaded', sweepAdElements);
            }
            window.addEventListener('load', sweepAdElements);

            // 3. Continuous MutationObserver for dynamic single-page applications
            try {
                var observer = new MutationObserver(function(mutations) {
                    sweepAdElements();
                });
                observer.observe(document.documentElement || document.body, {
                    childList: true,
                    subtree: true
                });
            } catch(e) {}

            // 4. Block Deceptive Popups and New Window Hijackers
            try {
                var origOpen = window.open;
                window.open = function(url, target, features) {
                    if (!url || url === 'about:blank' || url.indexOf('javascript:') === 0) {
                        return null;
                    }
                    return origOpen.apply(window, arguments);
                };
            } catch(e) {}
        })();
    """.trimIndent()
}
