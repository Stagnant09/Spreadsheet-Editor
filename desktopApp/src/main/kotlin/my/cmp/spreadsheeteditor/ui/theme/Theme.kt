package my.cmp.spreadsheeteditor.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

data class ColorScheme(
    val colBg : Color,
    val colSurface: Color,
    val colRibbon: Color,
    val colRibbonHover: Color,
    val colAccent: Color,
    val colAccentSoft: Color,
    val colAccentGlow: Color,
    val colGrid: Color,
    val colGridAlt: Color,
    val colGridHeader: Color,
    val colGridBorder: Color,
    val colSelected: Color,
    val colRangeFill: Color,
    val colText: Color,
    val colTextMuted: Color,
    val colError: Color,
    val colFormulaBar: Color,
    val colDivider: Color,
    val titleBarGradient: Array<Pair<Float, Color>>
)

fun lightColorScheme() = ColorScheme(
    colBg = Color(0xFFF8F9FA),
    colSurface = Color(0xFFFFFFFF),
    colRibbon = Color(0xFFF1F3F4),
    colRibbonHover = Color(0xFFE8EAED),
    colAccent = Color(0xFF5E5CE6),
    colAccentSoft = Color(0xFF4845D2),
    colAccentGlow = Color(0x205E5CE6),
    colGrid = Color(0xFFFFFFFF),
    colGridAlt = Color(0xFFF1F3F4),
    colGridHeader = Color(0xFFE8EAED),
    colGridBorder = Color(0xFFDADCE0),
    colSelected = Color(0xFFE8EAF6),
    colRangeFill = Color(0x155E5CE6),
    colText = Color(0xFF202124),
    colTextMuted = Color(0xFF5F6368),
    colError = Color(0xFFD93025),
    colFormulaBar = Color(0xFFFFFFFF),
    colDivider = Color(0xFFDADCE0),
    titleBarGradient = arrayOf(
        0.0f to Color(0xFF5E5CE6),
        0.5f to Color(0xFF8B7CFF),
        1.0f to Color(0xFF4845D2),
    )
)

fun darkColorScheme() = ColorScheme(
    colBg = Color(0xFF15151F),
    colSurface = Color(0xFF1F1F2E),
    colRibbon = Color(0xFF191926),
    colRibbonHover = Color(0xFF2C2C42),
    colAccent = Color(0xFF8B7CFF),
    colAccentSoft = Color(0xFF6656E0),
    colAccentGlow = Color(0x408B7CFF),
    colGrid = Color(0xFF1C1C29),
    colGridAlt = Color(0xFF20202F),
    colGridHeader = Color(0xFF232336),
    colGridBorder = Color(0xFF32324A),
    colSelected = Color(0xFF3C3670),
    colRangeFill = Color(0x298B7CFF),
    colText = Color(0xFFEDEDF7),
    colTextMuted = Color(0xFF9494B8),
    colError = Color(0xFFFF6B6B),
    colFormulaBar = Color(0xFF1B1B29),
    colDivider = Color(0xFF37374F),
    titleBarGradient = arrayOf(
        0.0f to Color(0xFF4230A8),
        0.5f to Color(0xFF8B7CFF),
        1.0f to Color(0xFF33228F),
    )
)

val LocalColorScheme = staticCompositionLocalOf { lightColorScheme() }

object SpreadsheetTheme {
    val colors: ColorScheme
        @Composable
        @ReadOnlyComposable
        get() = LocalColorScheme.current
}

@Composable
fun SpreadsheetTheme(
    isDark: Boolean = true,
    content: @Composable () -> Unit
) {
    val colors = if (isDark) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(
        LocalColorScheme provides colors,
        content = content
    )
}
