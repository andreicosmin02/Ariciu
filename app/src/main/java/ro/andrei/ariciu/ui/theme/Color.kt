package ro.andrei.ariciu.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Graphite, warm paper, and slate blue. Identity stays independent of wallpaper colors.
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFB4C9EC), onPrimary = Color(0xFF18283F),
    primaryContainer = Color(0xFF28364A), onPrimaryContainer = Color(0xFFDCE6F7),
    secondary = Color(0xFFC5C7CE), onSecondary = Color(0xFF24262B),
    secondaryContainer = Color(0xFF2B2E34), onSecondaryContainer = Color(0xFFE7E8EB),
    tertiary = Color(0xFFE3C191), onTertiary = Color(0xFF392913),
    tertiaryContainer = Color(0xFF3B3226), onTertiaryContainer = Color(0xFFF4D9B3),
    background = Color(0xFF111214), onBackground = Color(0xFFF2F0EC),
    surface = Color(0xFF191B1E), onSurface = Color(0xFFF2F0EC),
    surfaceVariant = Color(0xFF25282D), onSurfaceVariant = Color(0xFFB9BDC6),
    surfaceContainerLowest = Color(0xFF0E0F11), surfaceContainerLow = Color(0xFF17191C),
    surfaceContainer = Color(0xFF1D1F23), surfaceContainerHigh = Color(0xFF25282D),
    surfaceContainerHighest = Color(0xFF30343B), surfaceBright = Color(0xFF363A42), surfaceDim = Color(0xFF111214),
    outline = Color(0xFF757B87), outlineVariant = Color(0xFF353A43),
    error = Color(0xFFF0B4AE), onError = Color(0xFF4B1C19),
    errorContainer = Color(0xFF412725), onErrorContainer = Color(0xFFF5D4D0),
    inverseSurface = Color(0xFFF2F0EC), inverseOnSurface = Color(0xFF25272C),
    inversePrimary = Color(0xFF355B8D), surfaceTint = Color.Transparent,
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF355B8D), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EBF5), onPrimaryContainer = Color(0xFF213C60),
    secondary = Color(0xFF555D6B), onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8E9EC), onSecondaryContainer = Color(0xFF30353E),
    tertiary = Color(0xFF755422), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E9D9), onTertiaryContainer = Color(0xFF59421E),
    background = Color(0xFFF5F4F0), onBackground = Color(0xFF202329),
    surface = Color(0xFFFCFBF8), onSurface = Color(0xFF202329),
    surfaceVariant = Color(0xFFECEEF1), onSurfaceVariant = Color(0xFF5B626E),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF9F8F5),
    surfaceContainer = Color(0xFFF0F0ED), surfaceContainerHigh = Color(0xFFE8E9E8),
    surfaceContainerHighest = Color(0xFFE1E3E4), surfaceBright = Color(0xFFFCFBF8), surfaceDim = Color(0xFFE1E3E4),
    outline = Color(0xFF777F8C), outlineVariant = Color(0xFFD5D8DF),
    error = Color(0xFF9A3E37), onError = Color.White,
    errorContainer = Color(0xFFF7E5E2), onErrorContainer = Color(0xFF71332D),
    inverseSurface = Color(0xFF23262C), inverseOnSurface = Color(0xFFF2F0EC),
    inversePrimary = Color(0xFFB4C9EC), surfaceTint = Color.Transparent,
)
