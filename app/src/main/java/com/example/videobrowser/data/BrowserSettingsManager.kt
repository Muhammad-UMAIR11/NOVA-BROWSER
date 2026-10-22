package com.example.videobrowser.data

import android.content.Context
import android.content.SharedPreferences
import com.example.videobrowser.model.AppFontFamily
import com.example.videobrowser.model.ColorTheme
import com.example.videobrowser.model.CustomSearchEngine
import com.example.videobrowser.model.SearchEngine
import com.example.videobrowser.model.ThemeMode
import com.example.videobrowser.model.theme.AnimationIntensity
import com.example.videobrowser.model.theme.BrowserTheme
import com.example.videobrowser.model.theme.UiDensity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Robust settings repository with persistence, validation, fallback, and import/export.
 */
class BrowserSettingsManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "nova_browser_settings_v2"

        private const val KEY_ACTIVE_THEME_ID = "active_theme_id"
        private const val KEY_CUSTOM_THEMES_JSON = "custom_themes_json"
        private const val KEY_ACTIVE_ENGINE_ID = "active_engine_id"
        private const val KEY_CUSTOM_ENGINES_JSON = "custom_engines_json"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_FONT_FAMILY = "font_family"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_FONT_WEIGHT_DELTA = "font_weight_delta"
        private const val KEY_APPLY_TEXT_ZOOM_WEB = "apply_text_zoom_web"
        private const val KEY_UI_DENSITY = "ui_density"
        private const val KEY_CORNER_RADIUS = "corner_radius_dp"
        private const val KEY_PANEL_TRANSPARENCY = "panel_transparency"
        private const val KEY_PANEL_BLUR = "panel_blur"
        private const val KEY_ANIMATION_INTENSITY = "animation_intensity"
        private const val KEY_BLOCK_COOKIES = "block_cookies"
        private const val KEY_STRICT_POPUPS = "strict_popups"
        private const val KEY_HARDWARE_ACCEL = "hardware_accel"
        private const val KEY_LOW_END_MODE = "low_end_mode"
        private const val KEY_TOTAL_BLOCKED_ADS = "total_blocked_ads"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // --- Search Engines ---

    fun getActiveEngineId(): String {
        return prefs.getString(KEY_ACTIVE_ENGINE_ID, "duckduckgo") ?: "duckduckgo"
    }

    fun setActiveEngineId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_ENGINE_ID, id).apply()
    }

    fun getCustomEngines(): List<CustomSearchEngine> {
        val jsonStr = prefs.getString(KEY_CUSTOM_ENGINES_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<CustomSearchEngine>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                CustomSearchEngine.fromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllEngines(): List<CustomSearchEngine> {
        return CustomSearchEngine.BUILT_IN_ENGINES + getCustomEngines()
    }

    fun getActiveEngine(): CustomSearchEngine {
        val id = getActiveEngineId()
        return getAllEngines().find { it.id == id } ?: CustomSearchEngine.BUILT_IN_ENGINES.first()
    }

    fun saveCustomEngine(engine: CustomSearchEngine) {
        val current = getCustomEngines().toMutableList()
        val index = current.indexOfFirst { it.id == engine.id }
        if (index >= 0) {
            current[index] = engine
        } else {
            current.add(engine)
        }
        val array = JSONArray()
        current.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_CUSTOM_ENGINES_JSON, array.toString()).apply()
    }

    fun deleteCustomEngine(engineId: String) {
        val current = getCustomEngines().filterNot { it.id == engineId }
        val array = JSONArray()
        current.forEach { array.put(it.toJson()) }
        val editor = prefs.edit().putString(KEY_CUSTOM_ENGINES_JSON, array.toString())
        if (getActiveEngineId() == engineId) {
            editor.putString(KEY_ACTIVE_ENGINE_ID, "duckduckgo")
        }
        editor.apply()
    }

    fun restoreDefaultEngines() {
        prefs.edit()
            .remove(KEY_CUSTOM_ENGINES_JSON)
            .putString(KEY_ACTIVE_ENGINE_ID, "duckduckgo")
            .apply()
    }

    // --- Themes ---

    fun getActiveThemeId(): String {
        return prefs.getString(KEY_ACTIVE_THEME_ID, BrowserTheme.MIDNIGHT.id) ?: BrowserTheme.MIDNIGHT.id
    }

    fun setActiveThemeId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_THEME_ID, id).apply()
    }

    fun getCustomThemes(): List<BrowserTheme> {
        val jsonStr = prefs.getString(KEY_CUSTOM_THEMES_JSON, null) ?: return emptyList()
        return try {
            val array = JSONArray(jsonStr)
            val list = mutableListOf<BrowserTheme>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                BrowserTheme.fromJson(obj)?.let { list.add(it) }
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getAllThemes(): List<BrowserTheme> {
        return BrowserTheme.BUILT_IN_THEMES + getCustomThemes()
    }

    fun getActiveTheme(): BrowserTheme {
        val id = getActiveThemeId()
        return getAllThemes().find { it.id == id } ?: BrowserTheme.MIDNIGHT
    }

    fun saveCustomTheme(theme: BrowserTheme) {
        val current = getCustomThemes().toMutableList()
        val index = current.indexOfFirst { it.id == theme.id }
        if (index >= 0) {
            current[index] = theme
        } else {
            current.add(theme)
        }
        val array = JSONArray()
        current.forEach { array.put(it.toJson()) }
        prefs.edit()
            .putString(KEY_CUSTOM_THEMES_JSON, array.toString())
            .putString(KEY_ACTIVE_THEME_ID, theme.id)
            .apply()
    }

    fun deleteCustomTheme(themeId: String) {
        val current = getCustomThemes().filterNot { it.id == themeId }
        val array = JSONArray()
        current.forEach { array.put(it.toJson()) }
        val editor = prefs.edit().putString(KEY_CUSTOM_THEMES_JSON, array.toString())
        if (getActiveThemeId() == themeId) {
            editor.putString(KEY_ACTIVE_THEME_ID, BrowserTheme.MIDNIGHT.id)
        }
        editor.apply()
    }

    fun exportThemesJson(): String {
        val all = getAllThemes()
        val array = JSONArray()
        all.forEach { array.put(it.toJson()) }
        return array.toString(2)
    }

    fun importThemesJson(jsonString: String): Boolean {
        return try {
            val trimmed = jsonString.trim()
            val importedList = mutableListOf<BrowserTheme>()
            if (trimmed.startsWith("[")) {
                val array = JSONArray(trimmed)
                for (i in 0 until array.length()) {
                    BrowserTheme.fromJson(array.getJSONObject(i))?.let { importedList.add(it) }
                }
            } else if (trimmed.startsWith("{")) {
                BrowserTheme.fromJson(JSONObject(trimmed))?.let { importedList.add(it) }
            }
            if (importedList.isNotEmpty()) {
                importedList.forEach { saveCustomTheme(it.copy(isCustom = true)) }
                true
            } else false
        } catch (e: Exception) {
            false
        }
    }

    // --- Appearance & Typography Settings ---

    fun getThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try { ThemeMode.valueOf(name) } catch (e: Exception) { ThemeMode.SYSTEM }
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
    }

    fun getFontFamily(): AppFontFamily {
        val name = prefs.getString(KEY_FONT_FAMILY, AppFontFamily.SYSTEM_DEFAULT.name)
            ?: AppFontFamily.SYSTEM_DEFAULT.name
        return try { AppFontFamily.valueOf(name) } catch (e: Exception) { AppFontFamily.SYSTEM_DEFAULT }
    }

    fun setFontFamily(font: AppFontFamily) {
        prefs.edit().putString(KEY_FONT_FAMILY, font.name).apply()
    }

    fun getFontScale(): Float {
        return prefs.getFloat(KEY_FONT_SCALE, 1.0f)
    }

    fun setFontScale(scale: Float) {
        prefs.edit().putFloat(KEY_FONT_SCALE, scale.coerceIn(0.80f, 1.40f)).apply()
    }

    fun getFontWeightDelta(): Int {
        return prefs.getInt(KEY_FONT_WEIGHT_DELTA, 0)
    }

    fun setFontWeightDelta(delta: Int) {
        prefs.edit().putInt(KEY_FONT_WEIGHT_DELTA, delta.coerceIn(-1, 2)).apply()
    }

    fun getApplyTextZoomToWeb(): Boolean {
        return prefs.getBoolean(KEY_APPLY_TEXT_ZOOM_WEB, true)
    }

    fun setApplyTextZoomToWeb(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_APPLY_TEXT_ZOOM_WEB, enabled).apply()
    }

    fun getUiDensity(): UiDensity {
        val name = prefs.getString(KEY_UI_DENSITY, UiDensity.COMFORTABLE.name) ?: UiDensity.COMFORTABLE.name
        return try { UiDensity.valueOf(name) } catch (e: Exception) { UiDensity.COMFORTABLE }
    }

    fun setUiDensity(density: UiDensity) {
        prefs.edit().putString(KEY_UI_DENSITY, density.name).apply()
    }

    fun getCornerRadiusDp(): Int {
        return prefs.getInt(KEY_CORNER_RADIUS, 12)
    }

    fun setCornerRadiusDp(radius: Int) {
        prefs.edit().putInt(KEY_CORNER_RADIUS, radius.coerceIn(4, 24)).apply()
    }

    fun getPanelTransparency(): Float {
        return prefs.getFloat(KEY_PANEL_TRANSPARENCY, 0.88f)
    }

    fun setPanelTransparency(alpha: Float) {
        prefs.edit().putFloat(KEY_PANEL_TRANSPARENCY, alpha.coerceIn(0.40f, 1.0f)).apply()
    }

    fun getPanelBlurIntensity(): Float {
        return prefs.getFloat(KEY_PANEL_BLUR, 0.80f)
    }

    fun setPanelBlurIntensity(blur: Float) {
        prefs.edit().putFloat(KEY_PANEL_BLUR, blur.coerceIn(0.0f, 1.0f)).apply()
    }

    fun getAnimationIntensity(): AnimationIntensity {
        val name = prefs.getString(KEY_ANIMATION_INTENSITY, AnimationIntensity.BALANCED.name)
            ?: AnimationIntensity.BALANCED.name
        return try { AnimationIntensity.valueOf(name) } catch (e: Exception) { AnimationIntensity.BALANCED }
    }

    fun setAnimationIntensity(intensity: AnimationIntensity) {
        prefs.edit().putString(KEY_ANIMATION_INTENSITY, intensity.name).apply()
    }

    // --- Privacy & Rendering Toggles ---

    fun getBlockCookies(): Boolean = prefs.getBoolean(KEY_BLOCK_COOKIES, true)
    fun setBlockCookies(v: Boolean) = prefs.edit().putBoolean(KEY_BLOCK_COOKIES, v).apply()

    fun getStrictPopups(): Boolean = prefs.getBoolean(KEY_STRICT_POPUPS, true)
    fun setStrictPopups(v: Boolean) = prefs.edit().putBoolean(KEY_STRICT_POPUPS, v).apply()

    fun getHardwareAccel(): Boolean = prefs.getBoolean(KEY_HARDWARE_ACCEL, true)
    fun setHardwareAccel(v: Boolean) = prefs.edit().putBoolean(KEY_HARDWARE_ACCEL, v).apply()

    fun getLowEndMode(): Boolean = prefs.getBoolean(KEY_LOW_END_MODE, false)
    fun setLowEndMode(v: Boolean) = prefs.edit().putBoolean(KEY_LOW_END_MODE, v).apply()

    fun getTotalBlockedAds(): Int = prefs.getInt(KEY_TOTAL_BLOCKED_ADS, 0)
    fun setTotalBlockedAds(v: Int) = prefs.edit().putInt(KEY_TOTAL_BLOCKED_ADS, v).apply()

    // --- Reset Actions ---

    fun resetColorsAndThemes() {
        prefs.edit()
            .remove(KEY_ACTIVE_THEME_ID)
            .remove(KEY_CUSTOM_THEMES_JSON)
            .putString(KEY_ACTIVE_THEME_ID, BrowserTheme.MIDNIGHT.id)
            .apply()
    }

    fun resetTypography() {
        prefs.edit()
            .remove(KEY_FONT_FAMILY)
            .remove(KEY_FONT_SCALE)
            .remove(KEY_FONT_WEIGHT_DELTA)
            .remove(KEY_APPLY_TEXT_ZOOM_WEB)
            .apply()
    }

    fun resetAdvanced() {
        prefs.edit()
            .remove(KEY_UI_DENSITY)
            .remove(KEY_CORNER_RADIUS)
            .remove(KEY_PANEL_TRANSPARENCY)
            .remove(KEY_PANEL_BLUR)
            .remove(KEY_ANIMATION_INTENSITY)
            .apply()
    }

    fun fullFactoryReset() {
        prefs.edit().clear().apply()
    }
}
