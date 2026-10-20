package com.medsound.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Dim, low-blue night palette — easy on eyes in a dark room.
// Names differ from MedsoundColors members so the object does not shadow these vals.
private val nightDeepColor = Color(0xFF0A0E14)
private val nightMidColor = Color(0xFF121820)
private val nightElevatedColor = Color(0xFF1A222C)
private val mistColor = Color(0xFF6B8F9E)
private val mistSoftColor = Color(0xFF8FA8B3)
private val foamColor = Color(0xFFD4DEE4)
private val foamDimColor = Color(0xFF9AA8B0)
private val seaAccentColor = Color(0xFF3A6574)
private val premiumActiveColor = Color(0xFF2F9E6B)

private val DarkColors = darkColorScheme(
    primary = mistColor,
    onPrimary = nightDeepColor,
    primaryContainer = seaAccentColor,
    onPrimaryContainer = foamColor,
    secondary = mistSoftColor,
    onSecondary = nightDeepColor,
    background = nightDeepColor,
    onBackground = foamColor,
    surface = nightMidColor,
    onSurface = foamColor,
    surfaceVariant = nightElevatedColor,
    onSurfaceVariant = mistSoftColor,
    outline = Color(0xFF2C3844),
)

private val MedsoundTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.2.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        letterSpacing = 0.3.sp,
    ),
)

@Composable
fun MedsoundTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = MedsoundTypography,
        content = content,
    )
}

object MedsoundColors {
    val NightDeep = nightDeepColor
    val NightMid = nightMidColor
    val NightElevated = nightElevatedColor
    val Mist = mistColor
    val MistSoft = mistSoftColor
    val Foam = foamColor
    val FoamDim = foamDimColor
    val SeaAccent = seaAccentColor
    val PremiumActive = premiumActiveColor
}
