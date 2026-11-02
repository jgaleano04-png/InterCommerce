package com.intercommerce.core.presentation.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF14325C)
private val NavyLight = Color(0xFFA9C7FF)
private val Orange = Color(0xFFE8620C)
private val OrangeLight = Color(0xFFFFB68F)

private val LightColors = lightColorScheme(
    primary = Navy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    inversePrimary = NavyLight,
    secondary = Orange,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCB),
    onSecondaryContainer = Color(0xFF341000),
    background = Color(0xFFF7F8FB),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE9EDF4),
    surfaceContainerLow = Color(0xFFF1F3F8),
    onSurfaceVariant = Color(0xFF44474E),
    outlineVariant = Color(0xFFD5DAE3),
    error = Color(0xFFBA1A1A),
)

private val DarkColors = darkColorScheme(
    primary = NavyLight,
    onPrimary = Color(0xFF002F65),
    primaryContainer = Color(0xFF1D3F70),
    onPrimaryContainer = Color(0xFFD6E3FF),
    inversePrimary = Navy,
    secondary = OrangeLight,
    onSecondary = Color(0xFF552000),
    secondaryContainer = Color(0xFF793000),
    onSecondaryContainer = Color(0xFFFFDBCB),
    background = Color(0xFF111318),
    surface = Color(0xFF1A1C21),
    surfaceVariant = Color(0xFF2A2D34),
    surfaceContainerLow = Color(0xFF1E2026),
    onSurfaceVariant = Color(0xFFC4C6CF),
    outlineVariant = Color(0xFF3A3E46),
)

private val AppTypography = Typography().run {
    copy(
        titleLarge = titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontWeight = FontWeight.Bold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

/** Price emphasis used by every price tag, kept here so the whole app scales together. */
val PriceTextStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, letterSpacing = 0.sp)

private val AppShapes = Shapes(
    small = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(14.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
)

@Composable
fun InterCommerceTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
