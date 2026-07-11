package my.cmp.spreadsheeteditor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import my.cmp.spreadsheeteditor.ui.theme.ColorScheme
import my.cmp.spreadsheeteditor.ui.theme.SpreadsheetTheme
import my.cmp.spreadsheeteditor.utils.*
import java.time.LocalDate

/** Identifies which text field a grid pick should land in — "param_0", "param_1", etc. */
private data class PickTarget(val fieldId: String, val label: String)

@Composable
fun FunctionInsertionDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isDark: Boolean,
    // Live grid selection, threaded in from App so this dialog can turn whatever
    // the user clicks/Shift+Arrows over into a cell or range reference.
    selectedRow: Int,
    selectedCol: Int,
    selectionAnchorRow: Int,
    selectionAnchorCol: Int,
    // Reads a cell's current display value — used to resolve a typed/picked
    // reference into the literal the SpreadsheetFunction constructor needs.
    getCellDisplayValue: (row: Int, col: Int) -> String
) {
    SpreadsheetTheme(isDark = isDark) {
        val colors = SpreadsheetTheme.colors

        var searchText by remember { mutableStateOf("") }
        var selectedCategory by remember { mutableStateOf<FunctionCategory?>(null) }
        var categoryMenuExpanded by remember { mutableStateOf(false) }
        var selectedSpec by remember { mutableStateOf<FunctionSpec?>(null) }
        var paramValues by remember { mutableStateOf(listOf<String>()) }
        var errorText by remember { mutableStateOf<String?>(null) }
        var pickTarget by remember { mutableStateOf<PickTarget?>(null) }

        val filtered = FunctionCatalog.all.filter { spec ->
            (selectedCategory == null || spec.category == selectedCategory) &&
                    (searchText.isBlank() || spec.symbol.contains(searchText.trim(), ignoreCase = true))
        }

        fun selectSpec(spec: FunctionSpec) {
            selectedSpec = spec
            paramValues = List(spec.params.size) { "" }
            errorText = null
        }

        // Recomputed on every grid selection change — this is what feeds the
        // picker bar's live preview while pickTarget != null.
        val liveRefText = rangeRefText(selectionAnchorRow, selectionAnchorCol, selectedRow, selectedCol)

        fun applyPick(target: PickTarget, ref: String) {
            val index = target.fieldId.removePrefix("param_").toIntOrNull() ?: return
            paramValues = paramValues.toMutableList().also {
                while (it.size <= index) it.add("")
                // Append rather than overwrite: clicking several cells for an
                // aggregation param (SUM) builds up "B3, C4, ..." one pick at a time.
                it[index] = if (it[index].isBlank()) ref else "${it[index]}, $ref"
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(shape = RoundedCornerShape(8.dp), color = Color.Transparent)) {
            if (pickTarget == null) {
                // ---------------- Normal modal state ----------------
                // Scrim swallows clicks so the grid can't be edited underneath, but
                // never dismisses on click — an accidental tap shouldn't discard
                // whatever the user was mid-typing.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.32f), shape = RoundedCornerShape(8.dp))
                        .pointerInput(Unit) { detectTapGestures { } }
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .width(900.dp)
                        .height(600.dp)
                        .background(colors.colSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.colGridBorder, RoundedCornerShape(8.dp))
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {

                        // Title bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.colRibbon)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Insert Function", color = colors.colText, fontWeight = FontWeight.Medium)
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close",
                                tint = colors.colTextMuted,
                                modifier = Modifier.size(18.dp).clickable { onDismiss() }
                            )
                        }
                        Divider(color = colors.colDivider, thickness = 1.dp)

                        Row(modifier = Modifier.weight(1f).fillMaxWidth()) {

                            // ---------------- LEFT: function browser ----------------
                            Column(
                                modifier = Modifier
                                    .weight(2f)
                                    .fillMaxHeight()
                                    .padding(16.dp)
                            ) {
                                Text("Search for a function", color = colors.colTextMuted, style = MaterialTheme.typography.caption)
                                Spacer(Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Filled.Search, null, tint = colors.colTextMuted) },
                                    colors = spreadsheetTextFieldColors(colors)
                                )

                                Spacer(Modifier.height(16.dp))
                                Text("Or select a category", color = colors.colTextMuted, style = MaterialTheme.typography.caption)
                                Spacer(Modifier.height(6.dp))
                                Box {
                                    OutlinedButton(
                                        onClick = { categoryMenuExpanded = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            backgroundColor = colors.colSurface,
                                            contentColor = colors.colText
                                        ),
                                        border = BorderStroke(1.dp, colors.colGridBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(selectedCategory?.label ?: "All Categories")
                                            Icon(Icons.Filled.ArrowDropDown, null, tint = colors.colTextMuted)
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = categoryMenuExpanded,
                                        onDismissRequest = { categoryMenuExpanded = false }
                                    ) {
                                        DropdownMenuItem(onClick = {
                                            selectedCategory = null
                                            categoryMenuExpanded = false
                                        }) { Text("All Categories") }
                                        FunctionCategory.entries.forEach { cat ->
                                            DropdownMenuItem(onClick = {
                                                selectedCategory = cat
                                                categoryMenuExpanded = false
                                            }) { Text(cat.label) }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))
                                Text("Select a function", color = colors.colTextMuted, style = MaterialTheme.typography.caption)
                                Spacer(Modifier.height(6.dp))

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .border(1.dp, colors.colGridBorder, RoundedCornerShape(4.dp))
                                ) {
                                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                                        items(filtered) { spec ->
                                            val isSelected = spec.symbol == selectedSpec?.symbol
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(if (isSelected) colors.colSelected else colors.colSurface)
                                                    .clickable { selectSpec(spec) }
                                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                            ) {
                                                Text(spec.symbol, color = if (isSelected) colors.colAccent else colors.colText)
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(12.dp))

                                selectedSpec?.let { spec ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(colors.colGridAlt, RoundedCornerShape(4.dp))
                                            .padding(12.dp)
                                    ) {
                                        Text(spec.signature, color = colors.colText, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(4.dp))
                                        Text(spec.description, color = colors.colTextMuted, style = MaterialTheme.typography.body2)
                                    }
                                }
                            }

                            Divider(color = colors.colDivider, modifier = Modifier.fillMaxHeight().width(1.dp))

                            // ---------------- RIGHT: operand entry ----------------
                            Column(
                                modifier = Modifier
                                    .weight(3f)
                                    .fillMaxHeight()
                                    .padding(16.dp)
                            ) {
                                Text("Options", color = colors.colText, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(12.dp))

                                val spec = selectedSpec
                                if (spec == null) {
                                    Text(
                                        "Select a function on the left to configure its arguments.",
                                        color = colors.colTextMuted,
                                        style = MaterialTheme.typography.body2
                                    )
                                } else if (spec.params.isEmpty()) {
                                    Text("${spec.symbol} takes no arguments.", color = colors.colTextMuted, style = MaterialTheme.typography.body2)
                                } else {
                                    Column(modifier = Modifier.weight(1f)) {
                                        spec.params.forEachIndexed { index, param ->
                                            val fieldId = "param_$index"
                                            Text(
                                                "${param.name}  (${param.kind.hint()})",
                                                color = colors.colTextMuted,
                                                style = MaterialTheme.typography.caption
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            OutlinedTextField(
                                                value = paramValues.getOrElse(index) { "" },
                                                onValueChange = { new ->
                                                    paramValues = paramValues.toMutableList().also {
                                                        while (it.size <= index) it.add("")
                                                        it[index] = new
                                                    }
                                                },
                                                placeholder = { Text(param.kind.placeholder(), color = colors.colTextMuted) },
                                                singleLine = param.kind != ParamKind.NUMBER_LIST &&
                                                        param.kind != ParamKind.BOOLEAN_LIST &&
                                                        param.kind != ParamKind.TEXT_LIST,
                                                trailingIcon = {
                                                    // Grid glyph rather than an Icons.Filled.* entry — no extra
                                                    // icon-pack dependency needed just for this button.
                                                    Text(
                                                        "⊞",
                                                        color = colors.colTextMuted,
                                                        modifier = Modifier
                                                            .padding(end = 8.dp)
                                                            .clickable {
                                                                pickTarget = PickTarget(fieldId, "${spec.symbol} → ${param.name}")
                                                            }
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = spreadsheetTextFieldColors(colors)
                                            )
                                            Spacer(Modifier.height(12.dp))
                                        }
                                    }
                                }

                                errorText?.let {
                                    Text(it, color = colors.colError, style = MaterialTheme.typography.caption)
                                    Spacer(Modifier.height(8.dp))
                                }

                                Spacer(Modifier.weight(1f))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            backgroundColor = colors.colSurface,
                                            contentColor = colors.colText
                                        ),
                                        border = BorderStroke(1.dp, colors.colGridBorder)
                                    ) { Text("Cancel") }

                                    Spacer(Modifier.width(12.dp))

                                    Button(
                                        onClick = {
                                            val currentSpec = selectedSpec
                                            if (currentSpec == null) {
                                                errorText = "Select a function first."
                                                return@Button
                                            }
                                            // storeAt is a required constructor arg but is never read on this
                                            // path — App.kt places the result using the cell that was selected
                                            // when "Insert Function" was opened, not this value.
                                            val result = buildFunction(currentSpec, paramValues, 0 to 0, getCellDisplayValue)
                                            if (result == null) {
                                                errorText = "Check the argument values — one or more couldn't be resolved."
                                            } else {
                                                onConfirm(result.toFormula())
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            backgroundColor = colors.colAccent,
                                            contentColor = colors.colSurface
                                        )
                                    ) { Text("OK") }
                                }
                            }
                        }
                    }
                }
            } else {
                // ---------------- Picking state ----------------
                // Everything except this small bar is un-rendered, so clicks and
                // Shift+Arrow reach the real grid underneath — that's what turns
                // selectedRow/selectedCol into the ref shown below.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 24.dp)
                        .background(colors.colSurface, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.colAccent, RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Click a cell for ${pickTarget!!.label}", color = colors.colTextMuted, style = MaterialTheme.typography.caption)
                        Spacer(Modifier.width(10.dp))
                        Text(liveRefText.ifBlank { "…" }, color = colors.colText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "(Shift+Arrow to extend to a range)",
                            color = colors.colTextMuted,
                            style = MaterialTheme.typography.caption
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "✓",
                            color = colors.colAccent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable {
                                applyPick(pickTarget!!, liveRefText)
                                pickTarget = null
                            }
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "✕",
                            color = colors.colTextMuted,
                            modifier = Modifier.clickable { pickTarget = null }
                        )
                    }
                }
            }
        }
    }
}

private fun ParamKind.hint(): String = when (this) {
    ParamKind.NUMBER -> "number"
    ParamKind.INT -> "integer"
    ParamKind.TEXT -> "text"
    ParamKind.BOOLEAN -> "TRUE / FALSE"
    ParamKind.DATE -> "date"
    ParamKind.NUMBER_LIST -> "numbers, comma-separated"
    ParamKind.BOOLEAN_LIST -> "TRUE/FALSE, comma-separated"
    ParamKind.TEXT_LIST -> "text values, comma-separated"
}

private fun ParamKind.placeholder(): String = when (this) {
    ParamKind.NUMBER -> "e.g. 3.5 or B2"
    ParamKind.INT -> "e.g. 2 or B2"
    ParamKind.TEXT -> "e.g. hello or B2"
    ParamKind.BOOLEAN -> "TRUE or B2"
    ParamKind.DATE -> "YYYY-MM-DD or B2"
    ParamKind.NUMBER_LIST -> "e.g. 1, 2, B3:B8"
    ParamKind.BOOLEAN_LIST -> "e.g. TRUE, FALSE, B3:B8"
    ParamKind.TEXT_LIST -> "e.g. a, b, B3:B8"
}

// ============================================================
// Cell reference parsing / resolution
// ============================================================

private val CELL_REF_REGEX = Regex("^([A-Za-z]+)(\\d+)$")
private val RANGE_REF_REGEX = Regex("^([A-Za-z]+)(\\d+):([A-Za-z]+)(\\d+)$")

/** "AB" -> 27 (0-indexed), matching columnLabel's base-26 letter scheme. */
private fun columnIndexFromLabel(label: String): Int {
    var result = 0
    for (ch in label.uppercase()) result = result * 26 + (ch - 'A' + 1)
    return result - 1
}

/** (row=3, col=1) -> "B3". Row numbers in this app are used unshifted (row headers display 0..ROWS-1 directly, and NativeBridge commands use the raw row index) — so no +1 here. */
private fun cellRef(row: Int, col: Int): String = "${columnLabel(col)}$row"

/** Builds "B3" for a single cell, or "B3:B8" once the anchor and focus differ — this is what the picker bar shows live as Shift+Arrow extends the grid selection. */
private fun rangeRefText(anchorRow: Int, anchorCol: Int, focusRow: Int, focusCol: Int): String {
    val a = cellRef(anchorRow, anchorCol)
    val f = cellRef(focusRow, focusCol)
    return if (anchorRow == focusRow && anchorCol == focusCol) a else "$a:$f"
}

/**
 * Resolves one comma-split token into one or more raw cell strings:
 * - "B3:B8"  -> every cell's display value in that rectangle, row-major
 * - "B3"     -> that cell's display value
 * - anything else -> passed through unchanged as a literal
 */
private fun resolveToken(token: String, getCellValue: (Int, Int) -> String): List<String> {
    val trimmed = token.trim()
    RANGE_REF_REGEX.matchEntire(trimmed)?.let { m ->
        val (c1, r1, c2, r2) = m.destructured
        val col1 = columnIndexFromLabel(c1)
        val col2 = columnIndexFromLabel(c2)
        val row1 = r1.toInt()
        val row2 = r2.toInt()
        val values = mutableListOf<String>()
        for (r in minOf(row1, row2)..maxOf(row1, row2)) {
            for (c in minOf(col1, col2)..maxOf(col1, col2)) {
                values.add(getCellValue(r, c))
            }
        }
        return values
    }
    CELL_REF_REGEX.matchEntire(trimmed)?.let { m ->
        val (c, r) = m.destructured
        return listOf(getCellValue(r.toInt(), columnIndexFromLabel(c)))
    }
    return listOf(trimmed)
}

private fun resolveScalar(token: String, getCellValue: (Int, Int) -> String): String =
    resolveToken(token, getCellValue).firstOrNull() ?: ""

private fun resolveList(token: String, getCellValue: (Int, Int) -> String): List<String> =
    token.split(",").map { it.trim() }.filter { it.isNotEmpty() }.flatMap { resolveToken(it, getCellValue) }

private fun parseNumber(s: String) = s.trim().toDoubleOrNull()
private fun parseInt(s: String) = s.trim().toIntOrNull()
private fun parseBoolean(s: String): Boolean? = when (s.trim().lowercase()) {
    "true", "1" -> true
    "false", "0" -> false
    else -> null
}
private fun parseDate(s: String): LocalDate? = try { LocalDate.parse(s.trim()) } catch (e: Exception) { null }

/**
 * Routes to the matching FunctionRegistry map by shape. Every scalar/list arg is
 * resolved (cell ref / range / literal) via [getCellValue] before type parsing, so
 * a field can hold "B3", "B3:B8", or a plain literal interchangeably. Returns null
 * on any parse failure rather than throwing, so the dialog can show one message
 * instead of crashing.
 */
private fun buildFunction(
    spec: FunctionSpec,
    raw: List<String>,
    storeAt: Pair<Int, Int>,
    getCellValue: (Int, Int) -> String
): SpreadsheetFunction? {
    fun scalar(i: Int) = resolveScalar(raw.getOrElse(i) { "" }, getCellValue)
    fun list(i: Int) = resolveList(raw.getOrElse(i) { "" }, getCellValue)

    return when (spec.shape) {
        FunctionShape.NUMERIC_UNARY -> {
            val x = parseNumber(scalar(0)) ?: return null
            FunctionRegistry.numericUnary[spec.symbol]?.invoke(x, storeAt)
        }
        FunctionShape.NUMERIC_BINARY -> {
            val x = parseNumber(scalar(0)) ?: return null
            val y = parseNumber(scalar(1)) ?: return null
            FunctionRegistry.numericBinary[spec.symbol]?.invoke(x, y, storeAt)
        }
        FunctionShape.NUMERIC_PARAMETERIZED -> {
            val x = parseNumber(scalar(0)) ?: return null
            val d = parseInt(scalar(1)) ?: return null
            FunctionRegistry.numericParameterized[spec.symbol]?.invoke(x, d, storeAt)
        }
        FunctionShape.NUMERIC_AGGREGATION -> {
            val values = list(0).map { parseNumber(it) ?: return null }
            FunctionRegistry.numericAggregation[spec.symbol]?.invoke(values, storeAt)
        }
        FunctionShape.LOGICAL_UNARY -> {
            val x = parseBoolean(scalar(0)) ?: return null
            FunctionRegistry.logicalUnary[spec.symbol]?.invoke(x, storeAt)
        }
        FunctionShape.LOGICAL_AGGREGATION -> {
            val values = list(0).map { parseBoolean(it) ?: return null }
            FunctionRegistry.logicalAggregation[spec.symbol]?.invoke(values, storeAt)
        }
        FunctionShape.LOGICAL_COMPARISON -> {
            val l = parseNumber(scalar(0)) ?: return null
            val r = parseNumber(scalar(1)) ?: return null
            FunctionRegistry.logicalComparison[spec.symbol]?.invoke(l, r, storeAt)
        }
        FunctionShape.DATE_NULLARY -> FunctionRegistry.dateNullary[spec.symbol]?.invoke(storeAt)
        FunctionShape.DATE_UNARY -> {
            val d = parseDate(scalar(0)) ?: return null
            FunctionRegistry.dateUnary[spec.symbol]?.invoke(d, storeAt)
        }
        FunctionShape.DATE_BINARY -> {
            val s = parseDate(scalar(0)) ?: return null
            val e = parseDate(scalar(1)) ?: return null
            FunctionRegistry.dateBinary[spec.symbol]?.invoke(s, e, storeAt)
        }
        FunctionShape.DATE_PARAMETERIZED -> {
            val d = parseDate(scalar(0)) ?: return null
            val n = parseInt(scalar(1)) ?: return null
            FunctionRegistry.dateParameterized[spec.symbol]?.invoke(d, n, storeAt)
        }
        FunctionShape.TEXT_UNARY -> FunctionRegistry.textUnary[spec.symbol]?.invoke(scalar(0), storeAt)
        FunctionShape.TEXT_BINARY -> {
            val n = parseInt(scalar(1)) ?: return null
            FunctionRegistry.textBinary[spec.symbol]?.invoke(scalar(0), n, storeAt)
        }
        FunctionShape.TEXT_TERNARY -> {
            val start = parseInt(scalar(1)) ?: return null
            val length = parseInt(scalar(2)) ?: return null
            FunctionRegistry.textTernary[spec.symbol]?.invoke(scalar(0), start, length, storeAt)
        }
        FunctionShape.TEXT_PARAMETERIZED ->
            FunctionRegistry.textParameterized[spec.symbol]?.invoke(scalar(0), scalar(1), scalar(2), storeAt)
        FunctionShape.TEXT_AGGREGATION -> FunctionRegistry.textAggregation[spec.symbol]?.invoke(list(0), storeAt)
    }
}

@Composable
private fun spreadsheetTextFieldColors(colors: ColorScheme) = TextFieldDefaults.outlinedTextFieldColors(
    textColor = colors.colText,
    backgroundColor = colors.colSurface,
    focusedBorderColor = colors.colAccent,
    unfocusedBorderColor = colors.colGridBorder,
    cursorColor = colors.colAccent,
    placeholderColor = colors.colTextMuted
)