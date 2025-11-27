package com.example.mobile.views.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Tokens.Dark.primary,
    secondary = Tokens.Dark.secondary,
    tertiary = Tokens.Dark.tertiary,
    background = Tokens.Dark.background,
    surface = Tokens.Dark.surface,
    onBackground = Tokens.Dark.onBackground,
    onSurface = Tokens.Dark.onSurface,
    onPrimary = Tokens.Dark.onPrimary,
    onSecondary = Tokens.Dark.onSecondary,
    onTertiary = Tokens.Dark.onTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = Tokens.Light.primary,
    secondary = Tokens.Light.secondary,
    tertiary = Tokens.Light.tertiary,
    background = Tokens.Light.background,
    surface = Tokens.Light.surface,
    onBackground = Tokens.Light.onBackground,
    onSurface = Tokens.Light.onSurface,
    onPrimary = Tokens.Light.onPrimary,
    onSecondary = Tokens.Light.onSecondary,
    onTertiary = Tokens.Light.onTertiary
)

@Composable
fun MobileTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
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