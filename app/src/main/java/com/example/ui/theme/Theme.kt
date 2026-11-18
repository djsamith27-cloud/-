package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val ForestEmerald = Color(0xFF10B981)
val ForestTeal = Color(0xFF0D9488)
val AmberGold = Color(0xFFFBBF24)
val AmberOrange = Color(0xFFF59E0B)
val FlameOrange = Color(0xFFEA580C)
val RoseCrimson = Color(0xFFF43F5E)
val SkyWater = Color(0xFF38BDF8)
val DeepForestBg = Color(0xFF050A08)
val GlassHudBg = Color(0xCC0A120F)
val WoodBookDark = Color(0xFF1C130D)
val WoodBookLight = Color(0xFF291D14)
val WoodBookBorder = Color(0xFF78553A)

val SinhalaSansFamily = FontFamily(
    Font(R.font.noto_sans_sinhala, FontWeight.Normal),
    Font(R.font.noto_sans_sinhala, FontWeight.SemiBold),
    Font(R.font.noto_sans_sinhala, FontWeight.Bold),
    Font(R.font.noto_sans_sinhala, FontWeight.ExtraBold)
)

val DisplaySansFamily = FontFamily(
    Font(R.font.plus_jakarta_sans, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans, FontWeight.Bold),
    Font(R.font.noto_sans_sinhala, FontWeight.Normal)
)

private val SurvivalColorScheme = darkColorScheme(
    primary = ForestEmerald,
    onPrimary = Color.White,
    secondary = AmberOrange,
    onSecondary = Color(0xFF1C130D),
    tertiary = SkyWater,
    background = DeepForestBg,
    onBackground = Color(0xFFF5F5F4),
    surface = Color(0xFF0F1A15),
    onSurface = Color(0xFFF5F5F4),
    surfaceVariant = Color(0xFF1C2B24),
    onSurfaceVariant = Color(0xFFD6D3D1),
    error = RoseCrimson,
    onError = Color.White
)

val SurvivalTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SinhalaSansFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SinhalaSansFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = SinhalaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SinhalaSansFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = SinhalaSansFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SinhalaSansFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp,
        lineHeight = 12.sp
    )
)

@Composable
fun WildSurvivalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SurvivalColorScheme,
        typography = SurvivalTypography,
        content = content
    )
}
