package my.cmp.spreadsheeteditor.utils

import java.time.LocalDate

/**
 * `storeAt` and `symbol` are hoisted to the root so every leaf carries both without
 * re-declaring them. `symbol` is a plain override (not a constructor param), so it
 * doesn't affect data class equals/hashCode/copy — two SineFunctions with different
 * `x` are still unequal, but every SineFunction reports "SIN" regardless of `x`.
 *
 * Because `symbol` is `abstract`, the compiler refuses to build until every leaf
 * overrides it — that's your exhaustiveness check, for free, no `when` needed.
 */
sealed class SpreadsheetFunction {

    abstract val storeAt: Pair<Int, Int>
    abstract val symbol: String
    abstract fun toFormula(): String

    // ============================================================
    // NUMERIC
    // ============================================================
    sealed class NumericFunction : SpreadsheetFunction() {

        /** f(x) -> Double */
        sealed class UnaryFunction(open val x: Double, override val storeAt: Pair<Int, Int>) : NumericFunction() {
            override fun toFormula() = "$symbol($x)"
            data class SineFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "SIN"
            }

            data class CosineFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "COS"
            }

            data class TangentFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "TAN"
            }

            data class LogFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "LOG"
            }

            data class ExpFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "EXP"
            }

            data class SqrtFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "SQRT"
            }

            data class AbsFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "ABS"
            }

            data class CeilingFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "CEILING"
            }

            data class FloorFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "FLOOR"
            }

            data class RoundFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "ROUND"
            }

            data class SignFunction(override val x: Double, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "SIGN"
            }
        }

        /** f(x, y) -> Double */
        sealed class BinaryFunction(open val x: Double, open val y: Double, override val storeAt: Pair<Int, Int>) :
            NumericFunction() {
            override fun toFormula() = "$symbol($x; $y)"
            data class PowerFunction(
                override val x: Double,
                override val y: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                BinaryFunction(x, y, storeAt) {
                override val symbol = "POWER"
            }

            data class ModFunction(
                override val x: Double,
                override val y: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                BinaryFunction(x, y, storeAt) {
                override val symbol = "MOD"
            }

            data class LogBaseFunction(
                override val x: Double,
                override val y: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                BinaryFunction(x, y, storeAt) {
                override val symbol = "LOGBASE"
            }

            data class Atan2Function(
                override val x: Double,
                override val y: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                BinaryFunction(x, y, storeAt) {
                override val symbol = "ATAN2"
            }
        }

        /** f(x, intParam) -> Double, e.g. ROUND(x, 2) */
        sealed class ParameterizedFunction(
            open val x: Double,
            open val digits: Int,
            override val storeAt: Pair<Int, Int>
        ) : NumericFunction() {
            override fun toFormula() = "$symbol($x; $digits)"
            data class RoundToFunction(
                override val x: Double,
                override val digits: Int,
                override val storeAt: Pair<Int, Int>
            ) :
                ParameterizedFunction(x, digits, storeAt) {
                override val symbol = "ROUNDTO"
            }

            data class TruncFunction(
                override val x: Double,
                override val digits: Int,
                override val storeAt: Pair<Int, Int>
            ) :
                ParameterizedFunction(x, digits, storeAt) {
                override val symbol = "TRUNC"
            }
        }

        /** f(range) -> Double */
        sealed class AggregationFunction(open val values: List<Double>, override val storeAt: Pair<Int, Int>) :
            NumericFunction() {
            override fun toFormula() = "$symbol(${values.joinToString("; ")})"
            data class SumFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "SUM"
            }

            data class AverageFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "AVERAGE"
            }

            data class MinFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "MIN"
            }

            data class MaxFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "MAX"
            }

            data class CountFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "COUNT"
            }

            data class ProductFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "PRODUCT"
            }

            data class MedianFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "MEDIAN"
            }

            data class StdDevFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "STDEV"
            }

            data class VarianceFunction(override val values: List<Double>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "VAR"
            }
        }
    }

    // ============================================================
    // LOGICAL
    // ============================================================
    sealed class LogicalFunction : SpreadsheetFunction() {

        /** f(bool) -> Boolean */
        sealed class UnaryFunction(open val x: Boolean, override val storeAt: Pair<Int, Int>) : LogicalFunction() {
            override fun toFormula() = "$symbol(${x.toString().uppercase()})"
            data class NotFunction(override val x: Boolean, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(x, storeAt) {
                override val symbol = "NOT"
            }
        }

        /** f(bool range) -> Boolean */
        sealed class AggregationFunction(open val values: List<Boolean>, override val storeAt: Pair<Int, Int>) :
            LogicalFunction() {
            override fun toFormula() = "$symbol(${values.joinToString("; ") { it.toString().uppercase() }})"
            data class AndFunction(override val values: List<Boolean>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "AND"
            }

            data class OrFunction(override val values: List<Boolean>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "OR"
            }

            data class XorFunction(override val values: List<Boolean>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "XOR"
            }
        }

        /** f(Double, Double) -> Boolean */
        sealed class ComparisonFunction(
            open val left: Double,
            open val right: Double,
            override val storeAt: Pair<Int, Int>
        ) : LogicalFunction() {
            override fun toFormula() = "$symbol($left; $right)"
            data class EqualsFunction(
                override val left: Double,
                override val right: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                ComparisonFunction(left, right, storeAt) {
                override val symbol = "EQ"
            }

            data class NotEqualsFunction(
                override val left: Double,
                override val right: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                ComparisonFunction(left, right, storeAt) {
                override val symbol = "NEQ"
            }

            data class GreaterThanFunction(
                override val left: Double,
                override val right: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                ComparisonFunction(left, right, storeAt) {
                override val symbol = "GT"
            }

            data class GreaterOrEqualFunction(
                override val left: Double,
                override val right: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                ComparisonFunction(left, right, storeAt) {
                override val symbol = "GTE"
            }

            data class LessThanFunction(
                override val left: Double,
                override val right: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                ComparisonFunction(left, right, storeAt) {
                override val symbol = "LT"
            }

            data class LessOrEqualFunction(
                override val left: Double,
                override val right: Double,
                override val storeAt: Pair<Int, Int>
            ) :
                ComparisonFunction(left, right, storeAt) {
                override val symbol = "LTE"
            }
        }
    }

    // ============================================================
    // DATE
    // ============================================================
    sealed class DateFunction : SpreadsheetFunction() {

        /** f() -> LocalDate, no cell inputs */
        sealed class NullaryFunction(override val storeAt: Pair<Int, Int>) : DateFunction() {
            override fun toFormula() = "$symbol()"
            data class TodayFunction(override val storeAt: Pair<Int, Int>) :
                NullaryFunction(storeAt) {
                override val symbol = "TODAY"
            }

            data class NowFunction(override val storeAt: Pair<Int, Int>) :
                NullaryFunction(storeAt) {
                override val symbol = "NOW"
            }
        }

        /** f(date) -> Int */
        sealed class UnaryFunction(open val date: LocalDate, override val storeAt: Pair<Int, Int>) : DateFunction() {
            override fun toFormula() = "$symbol(\"$date\")"
            data class YearFunction(override val date: LocalDate, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(date, storeAt) {
                override val symbol = "YEAR"
            }

            data class MonthFunction(override val date: LocalDate, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(date, storeAt) {
                override val symbol = "MONTH"
            }

            data class DayFunction(override val date: LocalDate, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(date, storeAt) {
                override val symbol = "DAY"
            }

            data class WeekdayFunction(override val date: LocalDate, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(date, storeAt) {
                override val symbol = "WEEKDAY"
            }
        }

        /** f(date, date) -> Int (day span) */
        sealed class BinaryFunction(
            open val start: LocalDate,
            open val end: LocalDate,
            override val storeAt: Pair<Int, Int>
        ) : DateFunction() {
            override fun toFormula() = "$symbol(\"$start\"; \"$end\")"
            data class DateDiffFunction(
                override val start: LocalDate,
                override val end: LocalDate,
                override val storeAt: Pair<Int, Int>
            ) :
                BinaryFunction(start, end, storeAt) {
                override val symbol = "DATEDIF"
            }
        }

        /** f(date, intParam) -> LocalDate */
        sealed class ParameterizedFunction(
            open val date: LocalDate,
            open val days: Int,
            override val storeAt: Pair<Int, Int>
        ) : DateFunction() {
            override fun toFormula() = "$symbol(\"$date\"; $days)"
            data class DateAddFunction(
                override val date: LocalDate,
                override val days: Int,
                override val storeAt: Pair<Int, Int>
            ) :
                ParameterizedFunction(date, days, storeAt) {
                override val symbol = "DATEADD"
            }
        }
    }

    // ============================================================
    // TEXT
    // ============================================================
    sealed class TextFunction : SpreadsheetFunction() {

        /** f(text) -> String or Int */
        sealed class UnaryFunction(open val text: String, override val storeAt: Pair<Int, Int>) : TextFunction() {
            override fun toFormula() = "$symbol(\"$text\")"
            data class UpperFunction(override val text: String, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(text, storeAt) {
                override val symbol = "UPPER"
            }

            data class LowerFunction(override val text: String, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(text, storeAt) {
                override val symbol = "LOWER"
            }

            data class TrimFunction(override val text: String, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(text, storeAt) {
                override val symbol = "TRIM"
            }

            data class LenFunction(override val text: String, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(text, storeAt) {
                override val symbol = "LEN"
            }

            data class ReverseFunction(override val text: String, override val storeAt: Pair<Int, Int>) :
                UnaryFunction(text, storeAt) {
                override val symbol = "REVERSE"
            }
        }

        /** f(text, count) -> String, e.g. LEFT(text, 3) */
        sealed class BinaryFunction(open val text: String, open val count: Int, override val storeAt: Pair<Int, Int>) :
            TextFunction() {
            override fun toFormula() = "$symbol(\"$text\"; $count)"
            data class LeftFunction(
                override val text: String,
                override val count: Int,
                override val storeAt: Pair<Int, Int>
            ) :
                BinaryFunction(text, count, storeAt) {
                override val symbol = "LEFT"
            }

            data class RightFunction(
                override val text: String,
                override val count: Int,
                override val storeAt: Pair<Int, Int>
            ) :
                BinaryFunction(text, count, storeAt) {
                override val symbol = "RIGHT"
            }
        }

        /** f(text, start, length) -> String */
        sealed class TernaryFunction(
            open val text: String,
            open val start: Int,
            open val length: Int,
            override val storeAt: Pair<Int, Int>
        ) : TextFunction() {
            override fun toFormula() = "$symbol(\"$text\"; $start; $length)"
            data class SubstringFunction(
                override val text: String,
                override val start: Int,
                override val length: Int,
                override val storeAt: Pair<Int, Int>
            ) : TernaryFunction(text, start, length, storeAt) {
                override val symbol = "SUBSTRING"
            }
        }

        /** f(text, old, new) -> String */
        sealed class ParameterizedFunction(
            open val text: String,
            open val oldValue: String,
            open val newValue: String,
            override val storeAt: Pair<Int, Int>
        ) : TextFunction() {
            override fun toFormula() = "$symbol(\"$text\"; \"$oldValue\"; \"$newValue\")"
            data class ReplaceFunction(
                override val text: String,
                override val oldValue: String,
                override val newValue: String,
                override val storeAt: Pair<Int, Int>
            ) : ParameterizedFunction(text, oldValue, newValue, storeAt) {
                override val symbol = "REPLACE"
            }
        }

        /** f(text range) -> String */
        sealed class AggregationFunction(open val values: List<String>, override val storeAt: Pair<Int, Int>) :
            TextFunction() {
            override fun toFormula() = "$symbol(${values.joinToString("; ") { "\"$it\"" }})"
            data class ConcatFunction(override val values: List<String>, override val storeAt: Pair<Int, Int>) :
                AggregationFunction(values, storeAt) {
                override val symbol = "CONCAT"
            }
        }
    }
}

/**
 * Symbol -> constructor lookups, grouped by call shape so the parser can pick the
 * right map once it knows the arity/types it parsed out of `"SIN(A1)"`.
 *
 * This — not reflection over `symbol` — is how `getAllFunctions()` gets built. The
 * `symbol` property on each class is for the reverse direction: you already HAVE an
 * instance (built one of these, or loaded one from a saved sheet) and want to
 * display/serialize what it is, without a giant `when` over every leaf type.
 * `KClass.objectInstance` only works for `object` declarations with no state —
 * these are `data class`es that need constructor args (x, storeAt, ...), so there's
 * no instance to reflect on until you already have one. Two different jobs, two
 * different mechanisms.
 */
object FunctionRegistry {

    val numericUnary: Map<String, (Double, Pair<Int, Int>) -> SpreadsheetFunction.NumericFunction.UnaryFunction> =
        mapOf(
            "SIN" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.SineFunction(x, at) },
            "COS" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.CosineFunction(x, at) },
            "TAN" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.TangentFunction(x, at) },
            "LOG" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.LogFunction(x, at) },
            "EXP" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.ExpFunction(x, at) },
            "SQRT" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.SqrtFunction(x, at) },
            "ABS" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.AbsFunction(x, at) },
            "CEILING" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.CeilingFunction(x, at) },
            "FLOOR" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.FloorFunction(x, at) },
            "ROUND" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.RoundFunction(x, at) },
            "SIGN" to { x, at -> SpreadsheetFunction.NumericFunction.UnaryFunction.SignFunction(x, at) },
        )

    val numericBinary: Map<String, (Double, Double, Pair<Int, Int>) -> SpreadsheetFunction.NumericFunction.BinaryFunction> =
        mapOf(
            "POWER" to { x, y, at -> SpreadsheetFunction.NumericFunction.BinaryFunction.PowerFunction(x, y, at) },
            "MOD" to { x, y, at -> SpreadsheetFunction.NumericFunction.BinaryFunction.ModFunction(x, y, at) },
            "LOGBASE" to { x, y, at -> SpreadsheetFunction.NumericFunction.BinaryFunction.LogBaseFunction(x, y, at) },
            "ATAN2" to { x, y, at -> SpreadsheetFunction.NumericFunction.BinaryFunction.Atan2Function(x, y, at) },
        )

    val numericParameterized: Map<String, (Double, Int, Pair<Int, Int>) -> SpreadsheetFunction.NumericFunction.ParameterizedFunction> =
        mapOf(
            "ROUNDTO" to { x, d, at ->
                SpreadsheetFunction.NumericFunction.ParameterizedFunction.RoundToFunction(
                    x,
                    d,
                    at
                )
            },
            "TRUNC" to { x, d, at ->
                SpreadsheetFunction.NumericFunction.ParameterizedFunction.TruncFunction(
                    x,
                    d,
                    at
                )
            },
        )

    val numericAggregation: Map<String, (List<Double>, Pair<Int, Int>) -> SpreadsheetFunction.NumericFunction.AggregationFunction> =
        mapOf(
            "SUM" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.SumFunction(v, at) },
            "AVERAGE" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.AverageFunction(v, at) },
            "MIN" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.MinFunction(v, at) },
            "MAX" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.MaxFunction(v, at) },
            "COUNT" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.CountFunction(v, at) },
            "PRODUCT" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.ProductFunction(v, at) },
            "MEDIAN" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.MedianFunction(v, at) },
            "STDEV" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.StdDevFunction(v, at) },
            "VAR" to { v, at -> SpreadsheetFunction.NumericFunction.AggregationFunction.VarianceFunction(v, at) },
        )

    val logicalUnary: Map<String, (Boolean, Pair<Int, Int>) -> SpreadsheetFunction.LogicalFunction.UnaryFunction> =
        mapOf(
            "NOT" to { x, at -> SpreadsheetFunction.LogicalFunction.UnaryFunction.NotFunction(x, at) },
        )

    val logicalAggregation: Map<String, (List<Boolean>, Pair<Int, Int>) -> SpreadsheetFunction.LogicalFunction.AggregationFunction> =
        mapOf(
            "AND" to { v, at -> SpreadsheetFunction.LogicalFunction.AggregationFunction.AndFunction(v, at) },
            "OR" to { v, at -> SpreadsheetFunction.LogicalFunction.AggregationFunction.OrFunction(v, at) },
            "XOR" to { v, at -> SpreadsheetFunction.LogicalFunction.AggregationFunction.XorFunction(v, at) },
        )

    val logicalComparison: Map<String, (Double, Double, Pair<Int, Int>) -> SpreadsheetFunction.LogicalFunction.ComparisonFunction> =
        mapOf(
            "EQ" to { l, r, at -> SpreadsheetFunction.LogicalFunction.ComparisonFunction.EqualsFunction(l, r, at) },
            "NEQ" to { l, r, at -> SpreadsheetFunction.LogicalFunction.ComparisonFunction.NotEqualsFunction(l, r, at) },
            "GT" to { l, r, at ->
                SpreadsheetFunction.LogicalFunction.ComparisonFunction.GreaterThanFunction(
                    l,
                    r,
                    at
                )
            },
            "GTE" to { l, r, at ->
                SpreadsheetFunction.LogicalFunction.ComparisonFunction.GreaterOrEqualFunction(
                    l,
                    r,
                    at
                )
            },
            "LT" to { l, r, at -> SpreadsheetFunction.LogicalFunction.ComparisonFunction.LessThanFunction(l, r, at) },
            "LTE" to { l, r, at ->
                SpreadsheetFunction.LogicalFunction.ComparisonFunction.LessOrEqualFunction(
                    l,
                    r,
                    at
                )
            },
        )

    val dateNullary: Map<String, (Pair<Int, Int>) -> SpreadsheetFunction.DateFunction.NullaryFunction> = mapOf(
        "TODAY" to { at -> SpreadsheetFunction.DateFunction.NullaryFunction.TodayFunction(at) },
        "NOW" to { at -> SpreadsheetFunction.DateFunction.NullaryFunction.NowFunction(at) },
    )

    val dateUnary: Map<String, (LocalDate, Pair<Int, Int>) -> SpreadsheetFunction.DateFunction.UnaryFunction> = mapOf(
        "YEAR" to { d, at -> SpreadsheetFunction.DateFunction.UnaryFunction.YearFunction(d, at) },
        "MONTH" to { d, at -> SpreadsheetFunction.DateFunction.UnaryFunction.MonthFunction(d, at) },
        "DAY" to { d, at -> SpreadsheetFunction.DateFunction.UnaryFunction.DayFunction(d, at) },
        "WEEKDAY" to { d, at -> SpreadsheetFunction.DateFunction.UnaryFunction.WeekdayFunction(d, at) },
    )

    val dateBinary: Map<String, (LocalDate, LocalDate, Pair<Int, Int>) -> SpreadsheetFunction.DateFunction.BinaryFunction> =
        mapOf(
            "DATEDIF" to { s, e, at -> SpreadsheetFunction.DateFunction.BinaryFunction.DateDiffFunction(s, e, at) },
        )

    val dateParameterized: Map<String, (LocalDate, Int, Pair<Int, Int>) -> SpreadsheetFunction.DateFunction.ParameterizedFunction> =
        mapOf(
            "DATEADD" to { d, n, at ->
                SpreadsheetFunction.DateFunction.ParameterizedFunction.DateAddFunction(
                    d,
                    n,
                    at
                )
            },
        )

    val textUnary: Map<String, (String, Pair<Int, Int>) -> SpreadsheetFunction.TextFunction.UnaryFunction> = mapOf(
        "UPPER" to { s, at -> SpreadsheetFunction.TextFunction.UnaryFunction.UpperFunction(s, at) },
        "LOWER" to { s, at -> SpreadsheetFunction.TextFunction.UnaryFunction.LowerFunction(s, at) },
        "TRIM" to { s, at -> SpreadsheetFunction.TextFunction.UnaryFunction.TrimFunction(s, at) },
        "LEN" to { s, at -> SpreadsheetFunction.TextFunction.UnaryFunction.LenFunction(s, at) },
        "REVERSE" to { s, at -> SpreadsheetFunction.TextFunction.UnaryFunction.ReverseFunction(s, at) },
    )

    val textBinary: Map<String, (String, Int, Pair<Int, Int>) -> SpreadsheetFunction.TextFunction.BinaryFunction> =
        mapOf(
            "LEFT" to { s, n, at -> SpreadsheetFunction.TextFunction.BinaryFunction.LeftFunction(s, n, at) },
            "RIGHT" to { s, n, at -> SpreadsheetFunction.TextFunction.BinaryFunction.RightFunction(s, n, at) },
        )

    val textTernary: Map<String, (String, Int, Int, Pair<Int, Int>) -> SpreadsheetFunction.TextFunction.TernaryFunction> =
        mapOf(
            "SUBSTRING" to { s, start, len, at ->
                SpreadsheetFunction.TextFunction.TernaryFunction.SubstringFunction(
                    s,
                    start,
                    len,
                    at
                )
            },
        )

    val textParameterized: Map<String, (String, String, String, Pair<Int, Int>) -> SpreadsheetFunction.TextFunction.ParameterizedFunction> =
        mapOf(
            "REPLACE" to { s, old, new, at ->
                SpreadsheetFunction.TextFunction.ParameterizedFunction.ReplaceFunction(
                    s,
                    old,
                    new,
                    at
                )
            },
        )

    val textAggregation: Map<String, (List<String>, Pair<Int, Int>) -> SpreadsheetFunction.TextFunction.AggregationFunction> =
        mapOf(
            "CONCAT" to { v, at -> SpreadsheetFunction.TextFunction.AggregationFunction.ConcatFunction(v, at) },
        )

    /** Every registered symbol across every shape group — this is your `getAllFunctions()`. */
    val allSymbols: List<String> by lazy {
        (numericUnary.keys + numericBinary.keys + numericParameterized.keys + numericAggregation.keys +
                logicalUnary.keys + logicalAggregation.keys + logicalComparison.keys +
                dateNullary.keys + dateUnary.keys + dateBinary.keys + dateParameterized.keys +
                textUnary.keys + textBinary.keys + textTernary.keys + textParameterized.keys + textAggregation.keys).toList()
    }
}

fun getAllFunctions(): List<String> = FunctionRegistry.allSymbols