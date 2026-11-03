package com.julen.clases.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.julen.clases.R
import com.julen.clases.data.RiskLevel
import com.julen.clases.data.ThemeMode

/** Plus Jakarta Sans (SIL Open Font License, ver licenses/PlusJakartaSans-OFL.txt). */
val Jakarta = FontFamily(
    Font(R.font.jakarta_regular, FontWeight.Normal),
    Font(R.font.jakarta_medium, FontWeight.Medium),
    Font(R.font.jakarta_semibold, FontWeight.SemiBold),
    Font(R.font.jakarta_bold, FontWeight.Bold),
    Font(R.font.jakarta_extrabold, FontWeight.ExtraBold),
    Font(R.font.jakarta_extrabold, FontWeight.Black),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF5046E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE6E4FF),
    onPrimaryContainer = Color(0xFF1C1663),
    secondary = Color(0xFF7C3AED),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE7FE),
    onSecondaryContainer = Color(0xFF3B1782),
    tertiary = Color(0xFF0E9F8E),
    background = Color(0xFFF4F5FA),
    onBackground = Color(0xFF0E1222),
    surface = Color(0xFFF4F5FA),
    onSurface = Color(0xFF0E1222),
    surfaceVariant = Color(0xFFEDEFF6),
    onSurfaceVariant = Color(0xFF636B82),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color.White,
    surfaceContainerHigh = Color(0xFFF0F1F7),
    surfaceContainerHighest = Color(0xFFE4E6F0),
    outline = Color(0xFFCDD1DE),
    outlineVariant = Color(0xFFE7E9F1),
    error = Color(0xFFDC2626),
    onError = Color.White,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9B95FF),
    onPrimary = Color(0xFF17124F),
    primaryContainer = Color(0xFF2E2A7A),
    onPrimaryContainer = Color(0xFFE6E4FF),
    secondary = Color(0xFFC4A6FF),
    onSecondary = Color(0xFF2A0F66),
    secondaryContainer = Color(0xFF3A2378),
    onSecondaryContainer = Color(0xFFEDE7FE),
    tertiary = Color(0xFF3FD9C4),
    background = Color(0xFF080B14),
    onBackground = Color(0xFFEFF1F8),
    surface = Color(0xFF080B14),
    onSurface = Color(0xFFEFF1F8),
    surfaceVariant = Color(0xFF1A2032),
    onSurfaceVariant = Color(0xFF959DB4),
    surfaceContainerLowest = Color(0xFF0C1020),
    surfaceContainerLow = Color(0xFF111628),
    surfaceContainer = Color(0xFF151B2F),
    surfaceContainerHigh = Color(0xFF1C2338),
    surfaceContainerHighest = Color(0xFF283048),
    outline = Color(0xFF3A4360),
    outlineVariant = Color(0xFF1D2438),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
)

private fun TextStyle.jakarta(weight: FontWeight? = null, spacing: Float? = null): TextStyle = copy(
    fontFamily = Jakarta,
    fontWeight = weight ?: fontWeight,
    letterSpacing = spacing?.sp ?: letterSpacing,
)

private val Base = Typography()

private val AppTypography = Typography(
    displayLarge = Base.displayLarge.jakarta(FontWeight.ExtraBold, -1.5f),
    displayMedium = Base.displayMedium.jakarta(FontWeight.ExtraBold, -1.2f),
    displaySmall = Base.displaySmall.jakarta(FontWeight.ExtraBold, -1f),
    headlineLarge = Base.headlineLarge.jakarta(FontWeight.ExtraBold, -0.8f),
    headlineMedium = Base.headlineMedium.jakarta(FontWeight.ExtraBold, -0.6f),
    headlineSmall = Base.headlineSmall.jakarta(FontWeight.Bold, -0.4f),
    titleLarge = Base.titleLarge.jakarta(FontWeight.Bold, -0.2f),
    titleMedium = Base.titleMedium.jakarta(FontWeight.Bold),
    titleSmall = Base.titleSmall.jakarta(FontWeight.SemiBold),
    bodyLarge = Base.bodyLarge.jakarta(FontWeight.Medium),
    bodyMedium = Base.bodyMedium.jakarta(FontWeight.Medium),
    bodySmall = Base.bodySmall.jakarta(FontWeight.Medium),
    labelLarge = Base.labelLarge.jakarta(FontWeight.Bold),
    labelMedium = Base.labelMedium.jakarta(FontWeight.SemiBold),
    labelSmall = Base.labelSmall.jakarta(FontWeight.ExtraBold, 1f),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
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
        shapes = AppShapes,
        content = content,
    )
}

object AppColors {
    val Safe = Color(0xFF10B981)
    val Warning = Color(0xFFF59E0B)
    val Danger = Color(0xFFEF4444)

    /** Degradado principal de la marca (tarjetas destacadas). */
    val HeroGradient = listOf(Color(0xFF4338CA), Color(0xFF6D28D9), Color(0xFFBE185D))
    val CalmGradient = listOf(Color(0xFF1E3A8A), Color(0xFF4338CA))
    val SuccessGradient = listOf(Color(0xFF047857), Color(0xFF0E7490))
}

/** true si el tema activo es oscuro (según el fondo). */
@Composable
@ReadOnlyComposable
fun isDark(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

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

/** Color según la nota (sobre 10). */
fun gradeColor(score: Float?): Color = when {
    score == null -> Color(0xFF94A3B8)
    score < 5f -> AppColors.Danger
    score < 6.5f -> AppColors.Warning
    score < 8.5f -> Color(0xFF3B82F6)
    else -> AppColors.Safe
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
    val pair = SubjectPalette[code] ?: return MaterialTheme.colorScheme.onSurfaceVariant
    return if (isDark()) pair.second else pair.first
}

/** Color base (tono claro) de la asignatura, para degradados. */
fun subjectBaseColor(code: String?): Color = SubjectPalette[code]?.first ?: Color(0xFF64748B)
