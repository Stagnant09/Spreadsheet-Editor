package my.cmp.spreadsheeteditor.models

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import my.cmp.spreadsheeteditor.utils.columnLabel

data class CellRepresentation(
    val cell: Cell,
    val isSelected: Boolean,
    var bold: Boolean = false,
    var italic: Boolean = false,
    var underline: Boolean = false,
    var strike: Boolean = false,
    var fontSize: Float = 12f,
    var fontFamily: FontFamily = FontFamily.Default,
    var fontColor: Color = Color.Unspecified,
    var backgroundColor: Color = Color.Unspecified,
    var textAlign: TextAlign = TextAlign.Left,
    var wrapText: Boolean = false,
) {
    val row = cell.row
    val column = cell.column
    val content = cell.content

    constructor(
        cell: Cell,
    ) : this(
        cell = cell,
        isSelected = false
    )

    companion object {
        fun CellRepresentation.cellAddress() = "${columnLabel(column)}$row"
    }
}