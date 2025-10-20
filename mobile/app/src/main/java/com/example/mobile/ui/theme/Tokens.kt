package com.example.mobile.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Design Tokens – Couleurs
 *
 * Collez ici les valeurs issues de vos variables CSS (ex: depuis :root { --color-primary: #0066CC; ... })
 * et mappez-les vers les couleurs Material 3 utilisées par l’app.
 *
 * Guide de mapping (exemples):
 *   --color-primary           -> Light.primary / Dark.primary
 *   --on-primary              -> Light.onPrimary / Dark.onPrimary
 *   --color-secondary         -> Light.secondary / Dark.secondary
 *   --color-tertiary          -> Light.tertiary / Dark.tertiary
 *   --background              -> Light.background / Dark.background
 *   --surface                 -> Light.surface / Dark.surface
 *   --on-background           -> Light.onBackground / Dark.onBackground
 *   --on-surface              -> Light.onSurface / Dark.onSurface
 *   (ajustez si votre système de design diffère)
 */
object Tokens {
    object Light {
        // Placeholders: remplacez par les valeurs de vos CSS variables (format #RRGGBB)
        val primary = Color(0xFF6650A4)       // ex: --color-primary
        val onPrimary = Color(0xFFFFFFFF)     // ex: --on-primary
        val secondary = Color(0xFF625B71)     // ex: --color-secondary
        val onSecondary = Color(0xFFFFFFFF)   // ex: --on-secondary
        val tertiary = Color(0xFF7D5260)      // ex: --color-tertiary
        val onTertiary = Color(0xFFFFFFFF)    // ex: --on-tertiary

        val background = Color(0xFFFFFBFE)    // ex: --background
        val onBackground = Color(0xFF1C1B1F)  // ex: --on-background
        val surface = Color(0xFFFFFBFE)       // ex: --surface
        val onSurface = Color(0xFF1C1B1F)     // ex: --on-surface
    }

    object Dark {
        // Placeholders: remplacez par les valeurs de vos CSS variables (format #RRGGBB)
        val primary = Color(0xFFD0BCFF)
        val onPrimary = Color(0xFF381E72)
        val secondary = Color(0xFFCCC2DC)
        val onSecondary = Color(0xFF332D41)
        val tertiary = Color(0xFFEFB8C8)
        val onTertiary = Color(0xFF492532)

        val background = Color(0xFF1C1B1F)
        val onBackground = Color(0xFFE6E1E5)
        val surface = Color(0xFF1C1B1F)
        val onSurface = Color(0xFFE6E1E5)
    }
}

