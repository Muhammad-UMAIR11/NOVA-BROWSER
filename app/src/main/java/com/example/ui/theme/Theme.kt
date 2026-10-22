package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.videobrowser.model.AppFontFamily
import com.example.videobrowser.model.ColorTheme
import com.example.videobrowser.model.ThemeMode
import com.example.videobrowser.model.theme.AnimationIntensity
import com.example.videobrowser.model.theme.BrowserTheme
import com.example.videobrowser.model.theme.UiDensity
import com.example.videobrowser.ui.motion.LocalMotionScheme
import com.example.videobrowser.ui.motion.LocalReducedMotion
import com.example.videobrowser.ui.motion.MotionScheme
import com.example.videobrowser.ui.motion.rememberReducedMotion

val LocalBrowserCustomTheme = compositionLocalOf { BrowserTheme.MIDNIGHT }
val LocalBrowserUiDensity = compositionLocalOf { UiDensity.COMFORTABLE }
val LocalBrowserCornerRadius = compositionLocalOf { 12.dp }
val LocalBrowserPanelTransparency = compositionLocalOf { 0.88f }

/**
 * Creates Material3 ColorScheme seamlessly mapped from any BrowserTheme.
 */
fun createColorSchemeFromBrowserTheme(theme: BrowserTheme): ColorScheme {
    val primary = Color(theme.primaryColor)
    val onPrimary = Color(theme.onPrimaryColor)
    val secondary = Color(theme.secondaryColor)
    val surface = Color(theme.surfaceColor)
    val onSurface = Color(theme.onSurfaceColor)
    val background = Color(theme.backgroundColor)
    val onBackground = Color(theme.onBackgroundColor)
    val border = Color(theme.borderColor)

    return if (theme.isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.28f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = surface.copy(alpha = 0.7f),
            onSecondaryContainer = onSurface,
            surface = surface,
            onSurface = onSurface,
            background = background,
            onBackground = onBackground,
            surfaceVariant = Color(theme.toolbarColor),
            onSurfaceVariant = Color(theme.secondaryTextColor),
            outline = border,
            outlineVariant = border.copy(alpha = 0.6f),
            error = Color(0xFFFFB4AB),
            onError = Color(0xFF690005)
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primary.copy(alpha = 0.16f),
            onPrimaryContainer = primary,
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = surface.copy(alpha = 0.9f),
            onSecondaryContainer = onSurface,
            surface = surface,
            onSurface = onSurface,
            background = background,
            onBackground = onBackground,
            surfaceVariant = Color(theme.toolbarColor),
            onSurfaceVariant = Color(theme.secondaryTextColor),
            outline = border,
            outlineVariant = border.copy(alpha = 0.6f),
            error = Color(0xFFBA1A1A),
            onError = Color.White
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorTheme: ColorTheme = ColorTheme.OCEAN_BLUE,
    activeBrowserTheme: BrowserTheme = BrowserTheme.MIDNIGHT,
    fontFamilyChoice: AppFontFamily = AppFontFamily.SYSTEM_DEFAULT,
    fontScale: Float = 1.0f,
    fontWeightDelta: Int = 0,
    uiDensity: UiDensity = UiDensity.COMFORTABLE,
    cornerRadiusDp: Int = 12,
    panelTransparency: Float = 0.88f,
    animationIntensity: AnimationIntensity = AnimationIntensity.BALANCED,
    lowEndMode: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.BRIGHT -> false
        ThemeMode.DARK -> true
    }

    // Determine target theme (if user chose Bright/Dark override, align theme lightness)
    val effectiveTheme = if (themeMode == ThemeMode.BRIGHT && activeBrowserTheme.isDark) {
        BrowserTheme.ARCTIC
    } else if (themeMode == ThemeMode.DARK && !activeBrowserTheme.isDark) {
        BrowserTheme.MIDNIGHT
    } else {
        activeBrowserTheme
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> createColorSchemeFromBrowserTheme(effectiveTheme)
    }

    val typography = createAppTypography(
        fontFamily = getComposeFontFamily(fontFamilyChoice),
        scaleFactor = fontScale,
        weightOffset = fontWeightDelta
    )

    val systemReducedMotion = rememberReducedMotion()
    val isReducedMotion = systemReducedMotion || (animationIntensity == AnimationIntensity.REDUCED) || lowEndMode
    val effectiveTransparency = if (lowEndMode) 0.98f else panelTransparency

    CompositionLocalProvider(
        LocalBrowserCustomTheme provides effectiveTheme,
        LocalBrowserUiDensity provides uiDensity,
        LocalBrowserCornerRadius provides cornerRadiusDp.dp,
        LocalBrowserPanelTransparency provides effectiveTransparency,
        LocalReducedMotion provides isReducedMotion,
        LocalMotionScheme provides MotionScheme.Default
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            content = content
        )
    }
}
