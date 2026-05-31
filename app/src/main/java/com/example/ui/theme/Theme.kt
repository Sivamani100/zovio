package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FinancePurple,
    secondary = FinanceYellow,
    tertiary = FinancePurpleLight,
    background = FinanceBlack,
    surface = Color(0xFF1E2330),
    onPrimary = Color.White,
    onSecondary = FinanceBlack,
    onTertiary = FinanceBlack,
    onBackground = FinanceBg,
    onSurface = FinanceBg,
    primaryContainer = Color(0xFF42281A),
    onPrimaryContainer = FinanceYellowLight
)

private val LightColorScheme = lightColorScheme(
    primary = FinancePurple, // Warm terracotta replaced with mockup active Purple
    secondary = FinanceYellow, // Elegant yellow accent
    tertiary = FinanceGrey,
    background = FinanceBg,
    surface = FinanceWhite,
    onPrimary = Color.White,
    onSecondary = FinanceBlack,
    onTertiary = Color.White,
    onBackground = FinanceBlack,
    onSurface = FinanceBlack,
    primaryContainer = FinancePurpleLight,
    onPrimaryContainer = FinancePurple,
    secondaryContainer = FinanceYellowLight,
    onSecondaryContainer = FinanceBlack,
    surfaceVariant = FinanceBg,
    onSurfaceVariant = FinanceBlack
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ but we want our Bento theme to be uniform and distinct
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

