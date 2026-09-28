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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.videobrowser.model.AppFontFamily
import com.example.videobrowser.model.ColorTheme
import com.example.videobrowser.model.ThemeMode

private fun createCustomColorScheme(colorTheme: ColorTheme, isDark: Boolean): ColorScheme {
    val primary = Color(colorTheme.primaryColor)
    val secondary = Color(colorTheme.secondaryColor)

    // App's default accent is Ocean Blue (0xFF0061A4), with light blue surface (0xFFD1E4FF)
    val lightPrimaryContainer = when (colorTheme) {
        ColorTheme.OCEAN_BLUE -> Color(0xFFD1E4FF) // Crisp light blue surface
        ColorTheme.EMERALD -> Color(0xFFCEEAD6)
        ColorTheme.DEEP_PURPLE -> Color(0xFFEADDFF)
        ColorTheme.SUNSET_AMBER -> Color(0xFFFFE0B2)
        ColorTheme.CRIMSON_RED -> Color(0xFFFFDAD6)
    }
    val lightOnPrimaryContainer = when (colorTheme) {
        ColorTheme.OCEAN_BLUE -> Color(0xFF001D36) // Deep primary blue accent
        ColorTheme.EMERALD -> Color(0xFF002114)
        ColorTheme.DEEP_PURPLE -> Color(0xFF21005D)
        ColorTheme.SUNSET_AMBER -> Color(0xFF2C1600)
        ColorTheme.CRIMSON_RED -> Color(0xFF410002)
    }

    val darkPrimaryContainer = when (colorTheme) {
        ColorTheme.OCEAN_BLUE -> Color(0xFF00497D) // Deep oceanic blue accent surface
        ColorTheme.EMERALD -> Color(0xFF005238)
        ColorTheme.DEEP_PURPLE -> Color(0xFF4F378B)
        ColorTheme.SUNSET_AMBER -> Color(0xFF653800)
        ColorTheme.CRIMSON_RED -> Color(0xFF93000A)
    }
    val darkOnPrimaryContainer = when (colorTheme) {
        ColorTheme.OCEAN_BLUE -> Color(0xFFD1E4FF) // Bright primary light blue accent
        ColorTheme.EMERALD -> Color(0xFF8CF8C2)
        ColorTheme.DEEP_PURPLE -> Color(0xFFEADDFF)
        ColorTheme.SUNSET_AMBER -> Color(0xFFFFDCBE)
        ColorTheme.CRIMSON_RED -> Color(0xFFFFDAD6)
    }

    return if (isDark) {
        darkColorScheme(
            primary = primary,
            secondary = secondary,
            primaryContainer = darkPrimaryContainer,
            onPrimaryContainer = darkOnPrimaryContainer,
            secondaryContainer = Color(0xFF1E293B),
            onSecondaryContainer = Color(0xFFE2E8F0),
            surface = Color(0xFF0F172A),
            background = Color(0xFF020617),
            surfaceVariant = Color(0xFF1E293B),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onSurface = Color(0xFFF8FAFC),
            onBackground = Color(0xFFF8FAFC),
            onSurfaceVariant = Color(0xFF94A3B8),
            outline = Color(0xFF64748B),
            outlineVariant = Color(0xFF334155),
            error = Color(0xFFFFB4AB),
            errorContainer = Color(0xFF93000A),
            onError = Color(0xFF690005),
            onErrorContainer = Color(0xFFFFDAD6)
        )
    } else {
        lightColorScheme(
            primary = primary,
            secondary = secondary,
            primaryContainer = lightPrimaryContainer,
            onPrimaryContainer = lightOnPrimaryContainer,
            secondaryContainer = Color(0xFFE2E8F0),
            onSecondaryContainer = Color(0xFF1E293B),
            surface = Color(0xFFF8FAFC),
            background = Color(0xFFF1F5F9),
            surfaceVariant = Color(0xFFE2E8F0),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onSurface = Color(0xFF0F172A),
            onBackground = Color(0xFF0F172A),
            onSurfaceVariant = Color(0xFF475569),
            outline = Color(0xFF94A3B8),
            outlineVariant = Color(0xFFCBD5E1),
            error = Color(0xFFBA1A1A),
            errorContainer = Color(0xFFFFDAD6),
            onError = Color.White,
            onErrorContainer = Color(0xFF410002)
        )
    }
}

@Composable
fun MyApplicationTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorTheme: ColorTheme = ColorTheme.OCEAN_BLUE,
    fontFamilyChoice: AppFontFamily = AppFontFamily.SYSTEM_DEFAULT,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemDark
        ThemeMode.BRIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> createCustomColorScheme(colorTheme, isDark)
    }

    val typography = createAppTypography(getComposeFontFamily(fontFamilyChoice))

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
