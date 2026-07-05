package my.cmp.spreadsheeteditor.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import my.cmp.spreadsheeteditor.models.CellRepresentation

// Base surfaces — a slightly cooler, deeper slate so accent colors pop more
val ColBg          = Color(0xFF15151F)
val ColSurface     = Color(0xFF1F1F2E)
val ColRibbon      = Color(0xFF191926)
val ColRibbonHover = Color(0xFF2C2C42)

// Accent — a crisper, more saturated violet/indigo
val ColAccent      = Color(0xFF8B7CFF)
val ColAccentSoft  = Color(0xFF6656E0)
val ColAccentGlow  = Color(0x408B7CFF) // translucent accent, for range fills/glows

// Grid
val ColGrid        = Color(0xFF1C1C29)
val ColGridAlt     = Color(0xFF20202F)   // subtler zebra striping than before
val ColGridHeader  = Color(0xFF232336)
val ColGridBorder  = Color(0xFF32324A)
val ColSelected    = Color(0xFF3C3670)
val ColRangeFill   = Color(0x298B7CFF)   // translucent accent fill for range selection

// Text
val ColText        = Color(0xFFEDEDF7)
val ColTextMuted   = Color(0xFF9494B8)
val ColError       = Color(0xFFFF6B6B)

// Bars
val ColFormulaBar  = Color(0xFF1B1B29)
val ColDivider     = Color(0xFF37374F)

val titleBarGradient = arrayOf(
    0.0f to Color(0xFF4230A8),
    0.5f to Color(0xFF8B7CFF),
    1.0f to Color(0xFF33228F),
)
