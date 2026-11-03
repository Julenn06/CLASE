package com.julen.clases.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.julen.clases.data.RiskLevel
import com.julen.clases.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondary = Color(0xFF0F172A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary = Color(0xFF0D9488),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF8FAFC),
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF1F5F9),
    surfaceContainerHighest = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    outlineVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF818CF8),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFFE2E8F0),
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFE0E7FF),
    tertiary = Color(0xFF2DD4BF),
    background = Color(0xFF0B1120),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0B1120),
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerLowest = Color(0xFF0F172A),
    surfaceContainerLow = Color(0xFF151F32),
    surfaceContainer = Color(0xFF111A2C),
    surfaceContainerHigh = Color(0xFF1E293B),
    surfaceContainerHighest = Color(0xFF334155),
    outline = Color(0xFF475569),
    outlineVariant = Color(0xFF1E293B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)

private val BaseTypography = Typography()

private val AppTypography = Typography(
    displaySmall = BaseTypography.displaySmall.copy(fontWeight = FontWeight.Black, letterSpacing = (-1).sp),
    headlineLarge = BaseTypography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.5).sp),
    headlineMedium = BaseTypography.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = BaseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = BaseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = BaseTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    titleSmall = BaseTypography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = BaseTypography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelSmall = BaseTypography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp),
)

@Composable
fun isAppInDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun ClasesTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

object AppColors {
    val Safe = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)

    /** Degradado de la tarjeta principal del inicio. */
    val HeroGradient = listOf(Color(0xFF4338CA), Color(0xFF1E1B4B))
}

fun RiskLevel.color(): Color = when (this) {
    RiskLevel.SAFE -> AppColors.Safe
    RiskLevel.WARNING -> AppColors.Warning
    RiskLevel.DANGER -> AppColors.Danger
}

fun RiskLevel.label(): String = when (this) {
    RiskLevel.SAFE -> "Bien"
    RiskLevel.WARNING -> "Cuidado"
    RiskLevel.DANGER -> "Límite superado"
}

private val SubjectPalette = mapOf(
    "SI" to (Color(0xFFE11D48) to Color(0xFFFB7185)),
    "SGE" to (Color(0xFF0D9488) to Color(0xFF2DD4BF)),
    "DI" to (Color(0xFF16A34A) to Color(0xFF4ADE80)),
    "AD" to (Color(0xFFEA580C) to Color(0xFFFB923C)),
    "PSP" to (Color(0xFF9333EA) to Color(0xFFC084FC)),
    "PMDM" to (Color(0xFF2563EB) to Color(0xFF60A5FA)),
    "IPEII" to (Color(0xFFCA8A04) to Color(0xFFFACC15)),
    "OP1" to (Color(0xFF0891B2) to Color(0xFF22D3EE)),
    "OP2" to (Color(0xFFDB2777) to Color(0xFFF472B6)),
)

/** Color identificativo de cada asignatura, ajustado al tema claro u oscuro. */
@Composable
@ReadOnlyComposable
fun subjectColor(code: String?): Color {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val pair = SubjectPalette[code] ?: return MaterialTheme.colorScheme.onSurfaceVariant
    return if (dark) pair.second else pair.first
}
