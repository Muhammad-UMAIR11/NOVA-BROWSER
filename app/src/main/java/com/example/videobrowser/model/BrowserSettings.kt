package com.example.videobrowser.model

import android.net.Uri
import org.json.JSONObject

/**
 * Model representing a search engine, whether built-in or user-customized.
 */
data class CustomSearchEngine(
    val id: String,
    val displayName: String,
    val searchUrlTemplate: String,
    val homeUrl: String = "",
    val hint: String = "Web search engine",
    val isBuiltIn: Boolean = false
) {
    /**
     * Correctly generates search URL by validating template and safely encoding query.
     */
    fun buildSearchUrl(query: String): String {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return homeUrl.ifEmpty { "https://duckduckgo.com" }

        val encodedQuery = Uri.encode(trimmed)
        return when {
            searchUrlTemplate.contains("{searchTerms}") -> {
                searchUrlTemplate.replace("{searchTerms}", encodedQuery)
            }
            searchUrlTemplate.contains("%s") -> {
                searchUrlTemplate.replace("%s", encodedQuery)
            }
            searchUrlTemplate.endsWith("=") || searchUrlTemplate.endsWith("/") -> {
                searchUrlTemplate + encodedQuery
            }
            searchUrlTemplate.contains("?") -> {
                "$searchUrlTemplate&q=$encodedQuery"
            }
            else -> {
                "$searchUrlTemplate?q=$encodedQuery"
            }
        }
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("displayName", displayName)
            put("searchUrlTemplate", searchUrlTemplate)
            put("homeUrl", homeUrl)
            put("hint", hint)
            put("isBuiltIn", isBuiltIn)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CustomSearchEngine? {
            return try {
                CustomSearchEngine(
                    id = json.getString("id"),
                    displayName = json.getString("displayName"),
                    searchUrlTemplate = json.getString("searchUrlTemplate"),
                    homeUrl = json.optString("homeUrl", ""),
                    hint = json.optString("hint", ""),
                    isBuiltIn = json.optBoolean("isBuiltIn", false)
                )
            } catch (e: Exception) {
                null
            }
        }

        val BUILT_IN_ENGINES = listOf(
            CustomSearchEngine(
                id = "duckduckgo",
                displayName = "DuckDuckGo",
                searchUrlTemplate = "https://duckduckgo.com/?q=%s",
                homeUrl = "https://duckduckgo.com",
                hint = "Privacy-focused tracker-free search",
                isBuiltIn = true
            ),
            CustomSearchEngine(
                id = "google",
                displayName = "Google",
                searchUrlTemplate = "https://www.google.com/search?q=%s",
                homeUrl = "https://www.google.com",
                hint = "Fast comprehensive web search",
                isBuiltIn = true
            ),
            CustomSearchEngine(
                id = "bing",
                displayName = "Bing",
                searchUrlTemplate = "https://www.bing.com/search?q=%s",
                homeUrl = "https://www.bing.com",
                hint = "Microsoft intelligent search engine",
                isBuiltIn = true
            ),
            CustomSearchEngine(
                id = "brave",
                displayName = "Brave Search",
                searchUrlTemplate = "https://search.brave.com/search?q=%s",
                homeUrl = "https://search.brave.com",
                hint = "Independent and private web index",
                isBuiltIn = true
            ),
            CustomSearchEngine(
                id = "ecosia",
                displayName = "Ecosia",
                searchUrlTemplate = "https://www.ecosia.org/search?q=%s",
                homeUrl = "https://www.ecosia.org",
                hint = "Eco-friendly search that plants trees",
                isBuiltIn = true
            ),
            CustomSearchEngine(
                id = "yahoo",
                displayName = "Yahoo",
                searchUrlTemplate = "https://search.yahoo.com/search?p=%s",
                homeUrl = "https://search.yahoo.com",
                hint = "Global search portal and trending news",
                isBuiltIn = true
            )
        )
    }
}

/**
 * Standard SearchEngine enum maintained for backward compatibility.
 */
enum class SearchEngine(
    val id: String,
    val displayName: String,
    val searchUrlPrefix: String,
    val homeUrl: String,
    val hint: String
) {
    DUCKDUCKGO(
        id = "duckduckgo",
        displayName = "DuckDuckGo",
        searchUrlPrefix = "https://duckduckgo.com/?q=",
        homeUrl = "https://duckduckgo.com",
        hint = "Privacy-focused search engine"
    ),
    GOOGLE(
        id = "google",
        displayName = "Google",
        searchUrlPrefix = "https://www.google.com/search?q=",
        homeUrl = "https://www.google.com",
        hint = "Fast comprehensive web search"
    ),
    BING(
        id = "bing",
        displayName = "Bing",
        searchUrlPrefix = "https://www.bing.com/search?q=",
        homeUrl = "https://www.bing.com",
        hint = "Microsoft intelligent search"
    ),
    BRAVE(
        id = "brave",
        displayName = "Brave Search",
        searchUrlPrefix = "https://search.brave.com/search?q=",
        homeUrl = "https://search.brave.com",
        hint = "Independent and private search"
    ),
    YAHOO(
        id = "yahoo",
        displayName = "Yahoo",
        searchUrlPrefix = "https://search.yahoo.com/search?p=",
        homeUrl = "https://search.yahoo.com",
        hint = "Search and web portal"
    ),
    ECOSIA(
        id = "ecosia",
        displayName = "Ecosia",
        searchUrlPrefix = "https://www.ecosia.org/search?q=",
        homeUrl = "https://www.ecosia.org",
        hint = "Search that plants trees"
    );

    fun toCustomSearchEngine(): CustomSearchEngine {
        return CustomSearchEngine(
            id = id,
            displayName = displayName,
            searchUrlTemplate = "${searchUrlPrefix}%s",
            homeUrl = homeUrl,
            hint = hint,
            isBuiltIn = true
        )
    }

    companion object {
        fun fromId(id: String): SearchEngine {
            return entries.find { it.id == id } ?: DUCKDUCKGO
        }
    }
}

/**
 * Display theme options for light/bright, dark, and system follow.
 */
enum class ThemeMode(val displayName: String, val description: String) {
    SYSTEM("System Default", "Follows device appearance"),
    BRIGHT("Bright (Light)", "Clean bright background and dark text"),
    DARK("Dark", "Deep dark background easy on eyes")
}

/**
 * Accent color palette options (legacy compatibility).
 */
enum class ColorTheme(val displayName: String, val primaryColor: Long, val secondaryColor: Long) {
    OCEAN_BLUE("Ocean Blue", 0xFF0061A4, 0xFF535F70),
    EMERALD("Emerald Green", 0xFF006C4C, 0xFF4D6357),
    DEEP_PURPLE("Deep Purple", 0xFF6750A4, 0xFF625B71),
    SUNSET_AMBER("Sunset Amber", 0xFF8B5000, 0xFF715B2E),
    CRIMSON_RED("Crimson Red", 0xFFBA1A1A, 0xFF775656)
}

/**
 * Enhanced typography choices including real bundled Google Fonts.
 */
enum class AppFontFamily(
    val displayName: String,
    val description: String,
    val category: String = "Display"
) {
    SYSTEM_DEFAULT("System Default", "Default Android system typeface", "System"),
    PLUS_JAKARTA_SANS("Plus Jakarta Sans", "Geometric modern display typeface", "Sans-Serif"),
    INTER("Inter", "Ultra-clean UI readability typeface", "Sans-Serif"),
    OUTFIT("Outfit", "Futuristic rounded geometric typeface", "Modern"),
    JETBRAINS_MONO("JetBrains Mono", "Technical monospaced developer typeface", "Monospace"),
    PLAYFAIR_DISPLAY("Playfair Display", "Luxurious high-contrast editorial serif", "Serif"),
    SERIF("Editorial Serif", "Classic elegant serif typeface", "Serif"),
    SANS_SERIF("Clean Sans", "Modern geometric sans-serif", "Sans-Serif"),
    MONOSPACE("Modern Mono", "Technical monospaced font", "Monospace")
}
