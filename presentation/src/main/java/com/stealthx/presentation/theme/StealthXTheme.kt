package com.stealthx.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Palette ──────────────────────────────────────────────────────────────────

val ScBg         = Color(0xFF0D0208)   // dark wine-red black
val ScSurface    = Color(0xFF160509)   // deep crimson surface
val ScSurface2   = Color(0xFF1E0810)   // card surface (wine red tint)
val ScSurface3   = Color(0xFF280C16)   // elevated surface
val ScBorder     = Color(0xFF3D1522)   // subtle wine border
val ScGreen      = Color(0xFF00E676)   // terminal green
val ScGreenDim   = Color(0xFF00A854)   // muted green
val ScCyan       = Color(0xFF00C8FF)   // accent cyan
val ScGold       = Color(0xFFFFB800)   // elite gold
val ScRed        = Color(0xFFFF3D5A)   // error / danger
val ScText       = Color(0xFFEDD8DC)   // primary text (warm white)
val ScTextDim    = Color(0xFF8A6870)   // secondary text (muted rose)
val ScWineShimmer= Color(0xFF3D0A18)   // shimmer accent for bg

// ── Color scheme ─────────────────────────────────────────────────────────────

private val SecureChatColors = darkColorScheme(
    primary            = ScGreen,
    onPrimary          = Color(0xFF002416),
    primaryContainer   = Color(0xFF003A22),
    onPrimaryContainer = ScGreen,
    secondary          = ScCyan,
    onSecondary        = Color(0xFF001F28),
    secondaryContainer = Color(0xFF00344A),
    tertiary           = ScGold,
    onTertiary         = Color(0xFF2A1C00),
    background         = ScBg,
    onBackground       = ScText,
    surface            = ScSurface,
    onSurface          = ScText,
    surfaceVariant     = ScSurface2,
    onSurfaceVariant   = ScTextDim,
    outline            = ScBorder,
    error              = ScRed,
    onError            = Color.White,
    inverseSurface     = ScText,
    inverseOnSurface   = ScBg,
    errorContainer     = Color(0xFF3D0010),
    onErrorContainer   = ScRed
)

// ── Typography ────────────────────────────────────────────────────────────────

private val SecureChatTypography = Typography(
    displayLarge  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Light,   fontSize = 57.sp, letterSpacing = (-0.25).sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Light,   fontSize = 45.sp),
    displaySmall  = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 36.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 32.sp, letterSpacing = (-0.5).sp),
    headlineMedium= TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 28.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 24.sp),
    titleLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold,    fontSize = 20.sp, letterSpacing = 0.15.sp),
    titleMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold,fontSize = 16.sp, letterSpacing = 0.1.sp),
    titleSmall    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 14.sp, letterSpacing = 0.1.sp),
    bodyLarge     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall     = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal,  fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge    = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelMedium   = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium,  fontSize = 12.sp, letterSpacing = 0.5.sp),
    labelSmall    = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium,fontSize = 10.sp, letterSpacing = 1.sp)
)

// ── Theme entry point ─────────────────────────────────────────────────────────

@Composable
fun StealthXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SecureChatColors,
        typography  = SecureChatTypography,
        content     = content
    )
}
