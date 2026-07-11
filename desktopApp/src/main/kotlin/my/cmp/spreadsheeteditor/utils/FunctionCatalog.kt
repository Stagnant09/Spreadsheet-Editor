package my.cmp.spreadsheeteditor.utils

/** What kind of value a param expects — drives which input widget + parser the dialog uses. */
enum class ParamKind { NUMBER, INT, TEXT, BOOLEAN, DATE, NUMBER_LIST, BOOLEAN_LIST, TEXT_LIST }

data class ParamSpec(val name: String, val kind: ParamKind)

/** Mirrors the shape groups in FunctionRegistry 1:1 — this is what tells the dialog which registry map + constructor arity to use once the user hits OK. */
enum class FunctionShape {
    NUMERIC_UNARY, NUMERIC_BINARY, NUMERIC_PARAMETERIZED, NUMERIC_AGGREGATION,
    LOGICAL_UNARY, LOGICAL_AGGREGATION, LOGICAL_COMPARISON,
    DATE_NULLARY, DATE_UNARY, DATE_BINARY, DATE_PARAMETERIZED,
    TEXT_UNARY, TEXT_BINARY, TEXT_TERNARY, TEXT_PARAMETERIZED, TEXT_AGGREGATION
}

enum class FunctionCategory(val label: String) {
    NUMERIC("Numeric"), LOGICAL("Logical"), DATE("Date"), TEXT("Text")
}

data class FunctionSpec(
    val symbol: String,
    val category: FunctionCategory,
    val shape: FunctionShape,
    val signature: String,
    val description: String,
    val params: List<ParamSpec>
)

/**
 * One entry per FunctionRegistry key. Kept as flat data (not derived by reflecting
 * over the registry maps) so the dialog can show a signature and description before
 * any values are typed in — that text has no equivalent in the registry's
 * constructor lambdas.
 */
object FunctionCatalog {

    val all: List<FunctionSpec> = listOf(
        // Numeric — unary
        FunctionSpec("SIN", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "SIN(x)", "Returns the sine of x, in radians.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("COS", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "COS(x)", "Returns the cosine of x, in radians.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("TAN", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "TAN(x)", "Returns the tangent of x, in radians.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("LOG", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "LOG(x)", "Returns the natural logarithm of x.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("EXP", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "EXP(x)", "Returns e raised to the power of x.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("SQRT", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "SQRT(x)", "Returns the square root of x.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("ABS", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "ABS(x)", "Returns the absolute value of x.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("CEILING", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "CEILING(x)", "Rounds x up to the nearest integer.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("FLOOR", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "FLOOR(x)", "Rounds x down to the nearest integer.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("ROUND", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "ROUND(x)", "Rounds x to the nearest integer.", listOf(ParamSpec("x", ParamKind.NUMBER))),
        FunctionSpec("SIGN", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_UNARY, "SIGN(x)", "Returns -1, 0, or 1 depending on the sign of x.", listOf(ParamSpec("x", ParamKind.NUMBER))),

        // Numeric — binary
        FunctionSpec("POWER", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_BINARY, "POWER(x; y)", "Returns x raised to the power of y.", listOf(ParamSpec("x", ParamKind.NUMBER), ParamSpec("y", ParamKind.NUMBER))),
        FunctionSpec("MOD", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_BINARY, "MOD(x; y)", "Returns the remainder after x is divided by y.", listOf(ParamSpec("x", ParamKind.NUMBER), ParamSpec("y", ParamKind.NUMBER))),
        FunctionSpec("LOGBASE", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_BINARY, "LOGBASE(x; base)", "Returns the logarithm of x in the given base.", listOf(ParamSpec("x", ParamKind.NUMBER), ParamSpec("base", ParamKind.NUMBER))),
        FunctionSpec("ATAN2", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_BINARY, "ATAN2(x; y)", "Returns the angle between the x-axis and the point (x, y).", listOf(ParamSpec("x", ParamKind.NUMBER), ParamSpec("y", ParamKind.NUMBER))),

        // Numeric — parameterized
        FunctionSpec("ROUNDTO", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_PARAMETERIZED, "ROUNDTO(x; digits)", "Rounds x to the given number of decimal digits.", listOf(ParamSpec("x", ParamKind.NUMBER), ParamSpec("digits", ParamKind.INT))),
        FunctionSpec("TRUNC", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_PARAMETERIZED, "TRUNC(x; digits)", "Truncates x to the given number of decimal digits.", listOf(ParamSpec("x", ParamKind.NUMBER), ParamSpec("digits", ParamKind.INT))),

        // Numeric — aggregation
        FunctionSpec("SUM", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "SUM(number1; number2; ...)", "Adds all the numbers in a range of cells.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),
        FunctionSpec("AVERAGE", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "AVERAGE(number1; number2; ...)", "Returns the average of a set of numbers.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),
        FunctionSpec("MIN", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "MIN(number1; number2; ...)", "Returns the smallest value in a set of numbers.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),
        FunctionSpec("MAX", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "MAX(number1; number2; ...)", "Returns the largest value in a set of numbers.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),
        FunctionSpec("COUNT", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "COUNT(number1; number2; ...)", "Counts the numbers in a set of values.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),
        FunctionSpec("PRODUCT", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "PRODUCT(number1; number2; ...)", "Multiplies a set of numbers together.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),
        FunctionSpec("MEDIAN", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "MEDIAN(number1; number2; ...)", "Returns the median of a set of numbers.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),
        FunctionSpec("STDEV", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "STDEV(number1; number2; ...)", "Returns the standard deviation of a set of numbers.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),
        FunctionSpec("VAR", FunctionCategory.NUMERIC, FunctionShape.NUMERIC_AGGREGATION, "VAR(number1; number2; ...)", "Returns the variance of a set of numbers.", listOf(ParamSpec("values", ParamKind.NUMBER_LIST))),

        // Logical
        FunctionSpec("NOT", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_UNARY, "NOT(x)", "Returns the opposite of a logical value.", listOf(ParamSpec("x", ParamKind.BOOLEAN))),
        FunctionSpec("AND", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_AGGREGATION, "AND(logical1; logical2; ...)", "Returns TRUE if all arguments are TRUE.", listOf(ParamSpec("values", ParamKind.BOOLEAN_LIST))),
        FunctionSpec("OR", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_AGGREGATION, "OR(logical1; logical2; ...)", "Returns TRUE if any argument is TRUE.", listOf(ParamSpec("values", ParamKind.BOOLEAN_LIST))),
        FunctionSpec("XOR", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_AGGREGATION, "XOR(logical1; logical2; ...)", "Returns TRUE if an odd number of arguments are TRUE.", listOf(ParamSpec("values", ParamKind.BOOLEAN_LIST))),
        FunctionSpec("EQ", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_COMPARISON, "EQ(left; right)", "Returns TRUE if left equals right.", listOf(ParamSpec("left", ParamKind.NUMBER), ParamSpec("right", ParamKind.NUMBER))),
        FunctionSpec("NEQ", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_COMPARISON, "NEQ(left; right)", "Returns TRUE if left does not equal right.", listOf(ParamSpec("left", ParamKind.NUMBER), ParamSpec("right", ParamKind.NUMBER))),
        FunctionSpec("GT", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_COMPARISON, "GT(left; right)", "Returns TRUE if left is greater than right.", listOf(ParamSpec("left", ParamKind.NUMBER), ParamSpec("right", ParamKind.NUMBER))),
        FunctionSpec("GTE", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_COMPARISON, "GTE(left; right)", "Returns TRUE if left is greater than or equal to right.", listOf(ParamSpec("left", ParamKind.NUMBER), ParamSpec("right", ParamKind.NUMBER))),
        FunctionSpec("LT", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_COMPARISON, "LT(left; right)", "Returns TRUE if left is less than right.", listOf(ParamSpec("left", ParamKind.NUMBER), ParamSpec("right", ParamKind.NUMBER))),
        FunctionSpec("LTE", FunctionCategory.LOGICAL, FunctionShape.LOGICAL_COMPARISON, "LTE(left; right)", "Returns TRUE if left is less than or equal to right.", listOf(ParamSpec("left", ParamKind.NUMBER), ParamSpec("right", ParamKind.NUMBER))),

        // Date
        FunctionSpec("TODAY", FunctionCategory.DATE, FunctionShape.DATE_NULLARY, "TODAY()", "Returns the current date.", emptyList()),
        FunctionSpec("NOW", FunctionCategory.DATE, FunctionShape.DATE_NULLARY, "NOW()", "Returns the current date and time.", emptyList()),
        FunctionSpec("YEAR", FunctionCategory.DATE, FunctionShape.DATE_UNARY, "YEAR(date)", "Returns the year of a date.", listOf(ParamSpec("date", ParamKind.DATE))),
        FunctionSpec("MONTH", FunctionCategory.DATE, FunctionShape.DATE_UNARY, "MONTH(date)", "Returns the month of a date.", listOf(ParamSpec("date", ParamKind.DATE))),
        FunctionSpec("DAY", FunctionCategory.DATE, FunctionShape.DATE_UNARY, "DAY(date)", "Returns the day of a date.", listOf(ParamSpec("date", ParamKind.DATE))),
        FunctionSpec("WEEKDAY", FunctionCategory.DATE, FunctionShape.DATE_UNARY, "WEEKDAY(date)", "Returns the day of the week of a date.", listOf(ParamSpec("date", ParamKind.DATE))),
        FunctionSpec("DATEDIF", FunctionCategory.DATE, FunctionShape.DATE_BINARY, "DATEDIF(start; end)", "Returns the number of days between two dates.", listOf(ParamSpec("start", ParamKind.DATE), ParamSpec("end", ParamKind.DATE))),
        FunctionSpec("DATEADD", FunctionCategory.DATE, FunctionShape.DATE_PARAMETERIZED, "DATEADD(date; days)", "Adds a number of days to a date.", listOf(ParamSpec("date", ParamKind.DATE), ParamSpec("days", ParamKind.INT))),

        // Text
        FunctionSpec("UPPER", FunctionCategory.TEXT, FunctionShape.TEXT_UNARY, "UPPER(text)", "Converts text to upper case.", listOf(ParamSpec("text", ParamKind.TEXT))),
        FunctionSpec("LOWER", FunctionCategory.TEXT, FunctionShape.TEXT_UNARY, "LOWER(text)", "Converts text to lower case.", listOf(ParamSpec("text", ParamKind.TEXT))),
        FunctionSpec("TRIM", FunctionCategory.TEXT, FunctionShape.TEXT_UNARY, "TRIM(text)", "Removes leading and trailing whitespace from text.", listOf(ParamSpec("text", ParamKind.TEXT))),
        FunctionSpec("LEN", FunctionCategory.TEXT, FunctionShape.TEXT_UNARY, "LEN(text)", "Returns the length of text.", listOf(ParamSpec("text", ParamKind.TEXT))),
        FunctionSpec("REVERSE", FunctionCategory.TEXT, FunctionShape.TEXT_UNARY, "REVERSE(text)", "Reverses the characters in text.", listOf(ParamSpec("text", ParamKind.TEXT))),
        FunctionSpec("LEFT", FunctionCategory.TEXT, FunctionShape.TEXT_BINARY, "LEFT(text; count)", "Returns the leftmost characters from text.", listOf(ParamSpec("text", ParamKind.TEXT), ParamSpec("count", ParamKind.INT))),
        FunctionSpec("RIGHT", FunctionCategory.TEXT, FunctionShape.TEXT_BINARY, "RIGHT(text; count)", "Returns the rightmost characters from text.", listOf(ParamSpec("text", ParamKind.TEXT), ParamSpec("count", ParamKind.INT))),
        FunctionSpec("SUBSTRING", FunctionCategory.TEXT, FunctionShape.TEXT_TERNARY, "SUBSTRING(text; start; length)", "Returns a substring of text starting at a given position.", listOf(ParamSpec("text", ParamKind.TEXT), ParamSpec("start", ParamKind.INT), ParamSpec("length", ParamKind.INT))),
        FunctionSpec("REPLACE", FunctionCategory.TEXT, FunctionShape.TEXT_PARAMETERIZED, "REPLACE(text; old; new)", "Replaces occurrences of old with new in text.", listOf(ParamSpec("text", ParamKind.TEXT), ParamSpec("oldValue", ParamKind.TEXT), ParamSpec("newValue", ParamKind.TEXT))),
        FunctionSpec("CONCAT", FunctionCategory.TEXT, FunctionShape.TEXT_AGGREGATION, "CONCAT(text1; text2; ...)", "Joins together a set of text values.", listOf(ParamSpec("values", ParamKind.TEXT_LIST))),
    )

    val bySymbol: Map<String, FunctionSpec> = all.associateBy { it.symbol }
}