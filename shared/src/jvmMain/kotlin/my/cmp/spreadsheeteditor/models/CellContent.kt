package my.cmp.spreadsheeteditor.models

sealed class CellContent(val type: CellContentType, open val value: Any? = null) {
    data object Empty                                                              : CellContent(CellContentType.EMPTY,    null)
    data class NumberContent(override val value: Double)                          : CellContent(CellContentType.NUMBER,   value)
    data class TextContent(override val value: String)                            : CellContent(CellContentType.TEXT,     value)
    data class FormulaContent(override val value: String, val cachedResult: Double? = null) : CellContent(CellContentType.FORMULA, value)
    data class BooleanContent(override val value: Boolean)                        : CellContent(CellContentType.BOOLEAN,  value)
    data class ErrorContent(override val value: String)                           : CellContent(CellContentType.ERROR,    value)

    /** Convert this content to a different type, returning ErrorContent if the
     *  conversion is not meaningful (e.g. "hello" → NUMBER). */
    fun convertTo(target: CellContentType): CellContent = when (target) {

        CellContentType.EMPTY -> Empty

        CellContentType.NUMBER -> {
            val raw = when (this) {
                is NumberContent  -> return this                          // already correct
                is TextContent    -> value
                is FormulaContent -> cachedResult?.toString() ?: value
                is BooleanContent -> if (value) "1" else "0"
                is ErrorContent   -> return ErrorContent("Cannot convert error to number")
                is Empty          -> return ErrorContent("Cannot convert empty to number")
            }
            raw.toDoubleOrNull()
                ?.let { NumberContent(it) }
                ?: ErrorContent("\"$raw\" is not a valid number")
        }

        CellContentType.TEXT -> when (this) {
            is TextContent    -> this
            is NumberContent  -> TextContent(value.toBigDecimal().stripTrailingZeros().toPlainString())
            is BooleanContent -> TextContent(if (value) "TRUE" else "FALSE")
            is FormulaContent -> TextContent(cachedResult?.toString() ?: value)
            is ErrorContent   -> TextContent(value)
            is Empty          -> TextContent("")
        }

        CellContentType.BOOLEAN -> when (this) {
            is BooleanContent -> this
            is NumberContent  -> BooleanContent(value != 0.0)
            is TextContent    -> when (value.trim().uppercase()) {
                "TRUE",  "1", "YES" -> BooleanContent(true)
                "FALSE", "0", "NO"  -> BooleanContent(false)
                else -> ErrorContent("\"$value\" cannot be interpreted as a boolean")
            }
            is FormulaContent -> BooleanContent((cachedResult ?: 0.0) != 0.0)
            is ErrorContent   -> ErrorContent("Cannot convert error to boolean")
            is Empty          -> ErrorContent("Cannot convert empty to boolean")
        }

        CellContentType.FORMULA -> when (this) {
            is FormulaContent -> this
            is TextContent    -> FormulaContent(value)          // treat the text as the expression
            is NumberContent  -> FormulaContent(value.toBigDecimal().stripTrailingZeros().toPlainString())
            else              -> FormulaContent("")
        }

        // ERROR is never a target the user picks — dropLast(1) already excludes it,
        // but the when must be exhaustive
        CellContentType.ERROR -> ErrorContent("Manual error")
    }
}