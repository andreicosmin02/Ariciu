package ro.andrei.arichu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun type(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = height.sp,
    fontWeight = weight, letterSpacing = tracking.sp,
)

val Typography = Typography(
    displaySmall = type(36, 42, FontWeight.Medium, -1f),
    headlineLarge = type(32, 39, FontWeight.Medium, -0.8f),
    headlineMedium = type(28, 35, FontWeight.Medium, -0.6f),
    headlineSmall = type(22, 28, FontWeight.Medium, -0.3f),
    titleLarge = type(20, 26, FontWeight.Medium, -0.2f),
    titleMedium = type(16, 22, FontWeight.Medium),
    titleSmall = type(14, 20, FontWeight.Medium),
    bodyLarge = type(16, 24), bodyMedium = type(14, 21), bodySmall = type(13, 19),
    labelLarge = type(15, 21, FontWeight.Medium),
    labelMedium = type(13, 18, FontWeight.Medium, 0.2f),
    labelSmall = type(12, 17, FontWeight.Medium, 0.4f),
)
