package my.cmp.spreadsheeteditor.ui.utils

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import java.awt.GraphicsEnvironment

@OptIn(ExperimentalTextApi::class)
fun getSystemFonts(): List<Pair<String, FontFamily>> {
    return GraphicsEnvironment
        .getLocalGraphicsEnvironment()
        .availableFontFamilyNames
        .map { Pair(it, FontFamily(it)) }
        .toList()
}