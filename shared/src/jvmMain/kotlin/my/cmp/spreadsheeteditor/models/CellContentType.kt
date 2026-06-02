package my.cmp.spreadsheeteditor.models

enum class CellContentType {
    EMPTY,
    NUMBER,
    TEXT,
    FORMULA,
    BOOLEAN,
    ERROR;

    companion object {
        fun CellContentType.toMenuLabel() = when (this) {
            CellContentType.EMPTY -> "Default"
            CellContentType.NUMBER -> "Number"
            CellContentType.TEXT -> "Text"
            CellContentType.FORMULA -> "Formula"
            CellContentType.BOOLEAN -> "Boolean"
            CellContentType.ERROR -> "Error"
        }
    }
}