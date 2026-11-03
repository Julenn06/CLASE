package com.julen.clases

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Paleta de Colores Ultra-Moderna, Premium y Minimalista (Cyber-Aesthetic)
object AppTheme {
    val Primary = Color(0xFF0F172A)       // Slate 900 (Fondo Premium muy oscuro / Elementos principales)
    val Accent = Color(0xFF6366F1)        // Indigo 500 (Acento eléctrico vibrante)
    val Background = Color(0xFFF8FAFC)    // Slate 50 (Fondo ultra limpio y suave)

    // Gradientes Premium
    val PrimaryGradient = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
    val AccentGradient = listOf(Color(0xFF818CF8), Color(0xFF6366F1))

    // Colores semánticos de Estado suaves pero nítidos
    val StatusSafe = Color(0xFF10B981)
    val StatusWarning = Color(0xFFF59E0B)
    val StatusDanger = Color(0xFFEF4444)

    // Tipografía limpia y estilizada
    val Typography = Typography(
        headlineLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 32.sp,
            letterSpacing = (-0.5).sp,
            color = Primary
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            letterSpacing = (-0.5).sp,
            color = Primary
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            letterSpacing = 0.sp,
            color = Primary
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            color = Color(0xFF334155) // Slate 700
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color(0xFF64748B) // Slate 500
        )
    )
}
