package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.videobrowser.model.AppFontFamily

// Bundled Google Fonts downloaded locally to res/font/
val InterFontFamily = FontFamily(Font(R.font.inter))
val PlusJakartaSansFontFamily = FontFamily(Font(R.font.plus_jakarta_sans))
val OutfitFontFamily = FontFamily(Font(R.font.outfit))
val JetBrainsMonoFontFamily = FontFamily(Font(R.font.jetbrains_mono))
val PlayfairDisplayFontFamily = FontFamily(Font(R.font.playfair_display))

fun getComposeFontFamily(font: AppFontFamily): FontFamily {
    return when (font) {
        AppFontFamily.SYSTEM_DEFAULT -> FontFamily.Default
        AppFontFamily.PLUS_JAKARTA_SANS -> PlusJakartaSansFontFamily
        AppFontFamily.INTER -> InterFontFamily
        AppFontFamily.OUTFIT -> OutfitFontFamily
        AppFontFamily.JETBRAINS_MONO -> JetBrainsMonoFontFamily
        AppFontFamily.PLAYFAIR_DISPLAY -> PlayfairDisplayFontFamily
        AppFontFamily.SERIF -> FontFamily.Serif
        AppFontFamily.SANS_SERIF -> FontFamily.SansSerif
        AppFontFamily.MONOSPACE -> FontFamily.Monospace
    }
}

fun createAppTypography(
    fontFamily: FontFamily = FontFamily.Default,
    scaleFactor: Float = 1.0f,
    weightOffset: Int = 0
): Typography {
    val clampedScale = scaleFactor.coerceIn(0.80f, 1.40f)

    // Helper to calculate scaled font sizes without text clipping
    fun sp(size: Float) = (size * clampedScale).sp

    // Adjust font weight based on user preference (-1 light, 0 normal, 1 medium, 2 semibold)
    fun adjustWeight(base: FontWeight): FontWeight {
        return when (weightOffset) {
            -1 -> when (base) {
                FontWeight.Bold, FontWeight.SemiBold -> FontWeight.Medium
                FontWeight.Medium -> FontWeight.Normal
                else -> FontWeight.Light
            }
            1 -> when (base) {
                FontWeight.Normal -> FontWeight.Medium
                FontWeight.Light -> FontWeight.Normal
                else -> FontWeight.SemiBold
            }
            2 -> when (base) {
                FontWeight.Normal -> FontWeight.SemiBold
                FontWeight.Medium -> FontWeight.Bold
                else -> FontWeight.ExtraBold
            }
            else -> base
        }
    }

    return Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Normal),
            fontSize = sp(57f),
            lineHeight = sp(64f)
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Normal),
            fontSize = sp(45f),
            lineHeight = sp(52f)
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.SemiBold),
            fontSize = sp(32f),
            lineHeight = sp(40f)
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.SemiBold),
            fontSize = sp(28f),
            lineHeight = sp(36f)
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.SemiBold),
            fontSize = sp(24f),
            lineHeight = sp(32f)
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Medium),
            fontSize = sp(22f),
            lineHeight = sp(28f)
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Medium),
            fontSize = sp(16f),
            lineHeight = sp(24f),
            letterSpacing = 0.15.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Medium),
            fontSize = sp(14f),
            lineHeight = sp(20f),
            letterSpacing = 0.1.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Normal),
            fontSize = sp(16f),
            lineHeight = sp(24f),
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Normal),
            fontSize = sp(14f),
            lineHeight = sp(20f),
            letterSpacing = 0.25.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Normal),
            fontSize = sp(12f),
            lineHeight = sp(16f),
            letterSpacing = 0.4.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Medium),
            fontSize = sp(14f),
            lineHeight = sp(20f),
            letterSpacing = 0.1.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Medium),
            fontSize = sp(12f),
            lineHeight = sp(16f),
            letterSpacing = 0.5.sp
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = adjustWeight(FontWeight.Medium),
            fontSize = sp(11f),
            lineHeight = sp(16f),
            letterSpacing = 0.5.sp
        )
    )
}

val Typography = createAppTypography()
