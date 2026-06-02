package my.cmp.spreadsheeteditor.models

data class Cell(
    val row: Int,
    val column: Int,
    var content: CellContent = CellContent.Empty
) {
    companion object {
        fun Cell.displayValue(): String = when (val c = content) {
            is CellContent.Empty          -> ""
            is CellContent.NumberContent  -> c.value.toString()
            is CellContent.TextContent    -> c.value
            is CellContent.FormulaContent -> c.cachedResult?.toString() ?: c.value
            is CellContent.BooleanContent -> if (c.value) "TRUE" else "FALSE"
            is CellContent.ErrorContent   -> "#ERR: ${c.value}"
        }
    }
}