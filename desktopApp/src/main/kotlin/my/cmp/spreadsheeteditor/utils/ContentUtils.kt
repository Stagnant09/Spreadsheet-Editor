package my.cmp.spreadsheeteditor.utils

import my.cmp.spreadsheeteditor.NativeBridge
import my.cmp.spreadsheeteditor.models.CellContent

fun getNewContent(row: Int, col: Int, value: String): CellContent {
    val isFormula = value.startsWith("=")
    return when {
        isFormula -> CellContent.FormulaContent(value.drop(1))
        else -> {
            value.toDoubleOrNull()
                ?.let {
                    NativeBridge.processCommand("${columnLabel(col)}$row = $it")
                    CellContent.NumberContent(it)
                }
                ?: CellContent.TextContent(value)
        }
    }
}
