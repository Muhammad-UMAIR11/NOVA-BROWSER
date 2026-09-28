package com.example.videobrowser.model

/**
 * Supported search engines for the web browser.
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
    )
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
 * Accent color palette options.
 */
enum class ColorTheme(val displayName: String, val primaryColor: Long, val secondaryColor: Long) {
    OCEAN_BLUE("Ocean Blue", 0xFF0061A4, 0xFF535F70),
    EMERALD("Emerald Green", 0xFF006C4C, 0xFF4D6357),
    DEEP_PURPLE("Deep Purple", 0xFF6750A4, 0xFF625B71),
    SUNSET_AMBER("Sunset Amber", 0xFF8B5000, 0xFF715B2E),
    CRIMSON_RED("Crimson Red", 0xFFBA1A1A, 0xFF775656)
}

/**
 * Font typography choices.
 */
enum class AppFontFamily(val displayName: String, val description: String) {
    SYSTEM_DEFAULT("System Default", "Default Android typography"),
    SANS_SERIF("Clean Sans", "Modern geometric sans-serif"),
    SERIF("Editorial Serif", "Classic elegant serif typeface"),
    MONOSPACE("Modern Mono", "Technical monospaced font")
}
