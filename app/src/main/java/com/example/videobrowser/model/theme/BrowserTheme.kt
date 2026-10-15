package com.example.videobrowser.model.theme

import androidx.compose.ui.graphics.Color
import org.json.JSONObject

/**
 * Universal browser theme model with comprehensive component color tokens.
 */
data class BrowserTheme(
    val id: String,
    val name: String,
    val isCustom: Boolean = false,
    val isDark: Boolean = true,
    // Core semantic tokens
    val primaryColor: Long,         // Primary accent, focused states, pills
    val onPrimaryColor: Long,       // Text/icon on primary accent
    val secondaryColor: Long,       // Secondary accent / highlights
    val surfaceColor: Long,         // Panels, bottom sheets, menus, dialogs
    val onSurfaceColor: Long,       // Main text, headings
    val backgroundColor: Long,      // App canvas and page background
    val onBackgroundColor: Long,    // Text on page background
    val toolbarColor: Long,         // Top bar and address bar container
    val navigationBarColor: Long,   // Bottom bar surface
    val borderColor: Long,          // Cards, dividers, subtle borders
    val secondaryTextColor: Long,   // Hints, captions, timestamps
    val accentColor: Long           // Badges, highlights, active toggles
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("isCustom", isCustom)
            put("isDark", isDark)
            put("primaryColor", primaryColor)
            put("onPrimaryColor", onPrimaryColor)
            put("secondaryColor", secondaryColor)
            put("surfaceColor", surfaceColor)
            put("onSurfaceColor", onSurfaceColor)
            put("backgroundColor", backgroundColor)
            put("onBackgroundColor", onBackgroundColor)
            put("toolbarColor", toolbarColor)
            put("navigationBarColor", navigationBarColor)
            put("borderColor", borderColor)
            put("secondaryTextColor", secondaryTextColor)
            put("accentColor", accentColor)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BrowserTheme? {
            return try {
                BrowserTheme(
                    id = json.getString("id"),
                    name = json.getString("name"),
                    isCustom = json.optBoolean("isCustom", true),
                    isDark = json.optBoolean("isDark", true),
                    primaryColor = json.getLong("primaryColor"),
                    onPrimaryColor = json.optLong("onPrimaryColor", 0xFFFFFFFF),
                    secondaryColor = json.optLong("secondaryColor", 0xFF64748B),
                    surfaceColor = json.getLong("surfaceColor"),
                    onSurfaceColor = json.optLong("onSurfaceColor", 0xFFF8FAFC),
                    backgroundColor = json.getLong("backgroundColor"),
                    onBackgroundColor = json.optLong("onBackgroundColor", 0xFFF8FAFC),
                    toolbarColor = json.optLong("toolbarColor", json.getLong("surfaceColor")),
                    navigationBarColor = json.optLong("navigationBarColor", json.getLong("surfaceColor")),
                    borderColor = json.optLong("borderColor", 0xFF334155),
                    secondaryTextColor = json.optLong("secondaryTextColor", 0xFF94A3B8),
                    accentColor = json.optLong("accentColor", json.getLong("primaryColor"))
                )
            } catch (e: Exception) {
                null
            }
        }

        // 1. Midnight — deep black surfaces with restrained, elegant contrast
        val MIDNIGHT = BrowserTheme(
            id = "midnight",
            name = "Midnight",
            isCustom = false,
            isDark = true,
            primaryColor = 0xFF38BDF8,         // Electric sky blue
            onPrimaryColor = 0xFF02131F,
            secondaryColor = 0xFF818CF8,       // Indigo
            surfaceColor = 0xFF0F172A,         // Deep navy surface
            onSurfaceColor = 0xFFF8FAFC,       // Pure crisp white
            backgroundColor = 0xFF020617,      // Abyssal black
            onBackgroundColor = 0xFFF8FAFC,
            toolbarColor = 0xFF0B132B,
            navigationBarColor = 0xFF0B132B,
            borderColor = 0xFF1E293B,
            secondaryTextColor = 0xFF94A3B8,
            accentColor = 0xFF38BDF8
        )

        // 2. Aurora — dark blue and violet cosmic accents
        val AURORA = BrowserTheme(
            id = "aurora",
            name = "Aurora",
            isCustom = false,
            isDark = true,
            primaryColor = 0xFFA855F7,         // Vivid violet
            onPrimaryColor = 0xFFFFFFFF,
            secondaryColor = 0xFF06B6D4,       // Glowing cyan
            surfaceColor = 0xFF131127,         // Dark galactic indigo
            onSurfaceColor = 0xFFF8FAFC,
            backgroundColor = 0xFF090714,      // Deep cosmic void
            onBackgroundColor = 0xFFF8FAFC,
            toolbarColor = 0xFF171333,
            navigationBarColor = 0xFF171333,
            borderColor = 0xFF2A2254,
            secondaryTextColor = 0xFFA5B4FC,
            accentColor = 0xFF06B6D4
        )

        // 3. Obsidian — minimal monochrome styling
        val OBSIDIAN = BrowserTheme(
            id = "obsidian",
            name = "Obsidian",
            isCustom = false,
            isDark = true,
            primaryColor = 0xFFE2E8F0,         // Clean silver/titanium
            onPrimaryColor = 0xFF0F172A,
            secondaryColor = 0xFF94A3B8,
            surfaceColor = 0xFF121214,         // Monolithic graphite
            onSurfaceColor = 0xFFFFFFFF,
            backgroundColor = 0xFF09090B,      // Obsidian black
            onBackgroundColor = 0xFFFFFFFF,
            toolbarColor = 0xFF18181B,
            navigationBarColor = 0xFF18181B,
            borderColor = 0xFF27272A,
            secondaryTextColor = 0xFFA1A1AA,
            accentColor = 0xFFFFFFFF
        )

        // 4. Cyberpunk — futuristic neon accents with controlled intensity
        val CYBERPUNK = BrowserTheme(
            id = "cyberpunk",
            name = "Cyberpunk",
            isCustom = false,
            isDark = true,
            primaryColor = 0xFFFACC15,         // Neon electric yellow
            onPrimaryColor = 0xFF1C1917,
            secondaryColor = 0xFFEC4899,       // Neon hot pink
            surfaceColor = 0xFF120E1F,         // Deep synthwave violet
            onSurfaceColor = 0xFFFDFEFE,
            backgroundColor = 0xFF0A0714,      // Neon void
            onBackgroundColor = 0xFFFDFEFE,
            toolbarColor = 0xFF1A132E,
            navigationBarColor = 0xFF1A132E,
            borderColor = 0xFF3B2766,
            secondaryTextColor = 0xFFD8B4FE,
            accentColor = 0xFF22D3EE          // Cyber cyan
        )

        // 5. Arctic — cool, clean light surfaces
        val ARCTIC = BrowserTheme(
            id = "arctic",
            name = "Arctic",
            isCustom = false,
            isDark = false,
            primaryColor = 0xFF0284C7,         // Crisp glacier blue
            onPrimaryColor = 0xFFFFFFFF,
            secondaryColor = 0xFF0EA5E9,
            surfaceColor = 0xFFFFFFFF,         // Pure arctic white
            onSurfaceColor = 0xFF0F172A,       // Slate navy text
            backgroundColor = 0xFFF0F9FF,      // Pale frosted blue
            onBackgroundColor = 0xFF0F172A,
            toolbarColor = 0xFFE0F2FE,
            navigationBarColor = 0xFFE0F2FE,
            borderColor = 0xFFBAE6FD,
            secondaryTextColor = 0xFF64748B,
            accentColor = 0xFF0284C7
        )

        // 6. Emerald — dark surfaces with refined green accents
        val EMERALD = BrowserTheme(
            id = "emerald",
            name = "Emerald",
            isCustom = false,
            isDark = true,
            primaryColor = 0xFF10B981,         // Jewel emerald
            onPrimaryColor = 0xFF022C22,
            secondaryColor = 0xFF34D399,       // Mint glow
            surfaceColor = 0xFF062018,         // Deep forest surface
            onSurfaceColor = 0xFFECFDF5,
            backgroundColor = 0xFF02130E,      // Abyssal pine
            onBackgroundColor = 0xFFECFDF5,
            toolbarColor = 0xFF0B2E23,
            navigationBarColor = 0xFF0B2E23,
            borderColor = 0xFF134E3E,
            secondaryTextColor = 0xFF6EE7B7,
            accentColor = 0xFF10B981
        )

        // 7. Solar — warm, comfortable light styling
        val SOLAR = BrowserTheme(
            id = "solar",
            name = "Solar",
            isCustom = false,
            isDark = false,
            primaryColor = 0xFFD97706,         // Warm amber sun
            onPrimaryColor = 0xFFFFFFFF,
            secondaryColor = 0xFFB45309,       // Terracotta
            surfaceColor = 0xFFFFFDF5,         // Warm ivory
            onSurfaceColor = 0xFF291E10,       // Deep espresso text
            backgroundColor = 0xFFFEF3C7,      // Golden parchment
            onBackgroundColor = 0xFF291E10,
            toolbarColor = 0xFFFDE68A,
            navigationBarColor = 0xFFFDE68A,
            borderColor = 0xFFFCD34D,
            secondaryTextColor = 0xFF785E3A,
            accentColor = 0xFFD97706
        )

        val BUILT_IN_THEMES = listOf(
            MIDNIGHT,
            AURORA,
            OBSIDIAN,
            CYBERPUNK,
            ARCTIC,
            EMERALD,
            SOLAR
        )

        fun findById(id: String): BrowserTheme {
            return BUILT_IN_THEMES.find { it.id == id } ?: MIDNIGHT
        }
    }
}

/**
 * UI Layout Density options.
 */
enum class UiDensity(val displayName: String, val description: String, val scale: Float) {
    COMPACT("Compact", "Tight spacing for maximum screen content", 0.85f),
    COMFORTABLE("Comfortable", "Balanced spacing and standard touch targets", 1.0f),
    SPACIOUS("Spacious", "Generous margins and relaxed layout", 1.18f)
}

/**
 * Animation speed / smoothness preferences.
 */
enum class AnimationIntensity(val displayName: String, val description: String) {
    REDUCED("Reduced", "Minimal motion for maximum battery & accessibility"),
    BALANCED("Balanced", "Snappy standard transitions (120-180ms)"),
    SMOOTH("Smooth", "Spring physics and fluid easing curves")
}

/**
 * Component Corner Radius style.
 */
enum class CornerRadiusOption(val displayName: String, val cornerRadiusDp: Int) {
    SHARP("Sharp (4dp)", 4),
    MODERN("Modern (10dp)", 10),
    SMOOTH("Smooth (16dp)", 16),
    PILL("Pill (24dp)", 24)
}
