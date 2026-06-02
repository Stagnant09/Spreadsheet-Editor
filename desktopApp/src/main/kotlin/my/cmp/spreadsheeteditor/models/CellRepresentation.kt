package my.cmp.spreadsheeteditor.models

import androidx.compose.ui.unit.Dp

data class CellRepresentation(
    val cell: Cell,
    val height: Int,
    val width: Int,
    val isSelected: Boolean
) {
    val row = cell.row
    val column = cell.column
    val content = cell.content

    constructor(
        cell: Cell,
        height: Dp,
        width: Dp,
    ) : this(
        cell = cell,
        height = height.value.toInt(),
        width = width.value.toInt(),
        isSelected = false
    )

    companion object {
        fun CellRepresentation.cellAddress() = "${'A' + column}$row"
    }
}