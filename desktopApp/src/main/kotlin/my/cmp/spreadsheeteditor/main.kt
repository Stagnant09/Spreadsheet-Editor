package my.cmp.spreadsheeteditor

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.application
import dev.nucleusframework.window.DecoratedWindow
import dev.nucleusframework.window.DecoratedWindowState
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.TitleBar
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.filled.Add
import io.github.composefluent.icons.filled.Edit
import io.github.composefluent.icons.filled.Save
import io.github.composefluent.icons.regular.*
import my.cmp.spreadsheeteditor.models.Cell
import my.cmp.spreadsheeteditor.models.Cell.Companion.displayValue
import my.cmp.spreadsheeteditor.models.CellContent
import my.cmp.spreadsheeteditor.models.CellContentType
import my.cmp.spreadsheeteditor.models.CellContentType.Companion.toMenuLabel
import my.cmp.spreadsheeteditor.models.CellRepresentation
import my.cmp.spreadsheeteditor.models.CellRepresentation.Companion.cellAddress
import my.cmp.spreadsheeteditor.ui.components.*
import my.cmp.spreadsheeteditor.ui.theme.*
import my.cmp.spreadsheeteditor.utils.IGNORE_CHARS
import my.cmp.spreadsheeteditor.utils.NAVIGATION_CHARS

// ─── Grid constants ───────────────────────────────────────────────────────────
private const val ROWS = 50
private const val COLS = 26
private val COL_HEADER_WIDTH: Dp = 48.dp
private val COL_WIDTH: Dp = 120.dp
private val ROW_HEIGHT: Dp = 28.dp
private val HEADER_HEIGHT: Dp = 26.dp

// ─── Spreadsheet grid ────────────────────────────────────────────────────────
@Composable
fun SpreadsheetGrid(
    cells: Array<Array<CellRepresentation>>,
    selectedRow: Int,
    selectedCol: Int,
    onCellSelected: (row: Int, col: Int) -> Unit,
    onCellEdited: (row: Int, col: Int, value: String) -> Unit,
    onKeyStartTyping: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colScrollState = rememberScrollState()
    val rowScrollState = rememberScrollState()

    fun navigate(key: Key) {
        when (key) {
            Key.DirectionUp -> onCellSelected(selectedRow - 1, selectedCol)
            Key.DirectionDown -> onCellSelected(selectedRow + 1, selectedCol)
            Key.DirectionLeft -> onCellSelected(selectedRow, selectedCol - 1)
            Key.DirectionRight -> onCellSelected(selectedRow, selectedCol + 1)
        }
    }

    Column(modifier = modifier.background(ColBg)) {

        // ── Column headers ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT)
                .horizontalScroll(colScrollState),
        ) {
            // Corner cell
            Box(
                modifier = Modifier
                    .width(COL_HEADER_WIDTH)
                    .fillMaxHeight()
                    .background(ColGridHeader)
                    .border(BorderStroke(0.5.dp, ColGridBorder)),
            )
            repeat(COLS) { col ->
                val isSelected = col == selectedCol
                Box(
                    modifier = Modifier
                        .width(COL_WIDTH)
                        .fillMaxHeight()
                        .background(if (isSelected) ColSelected else ColGridHeader)
                        .border(BorderStroke(0.5.dp, ColGridBorder)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = ('A' + col).toString(),
                        color = if (isSelected) ColAccent else ColTextMuted,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    )
                }
            }
        }

        // ── Rows ─────────────────────────────────────────────────────────────
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(colScrollState),
            ) {
                itemsIndexed(cells) { rowIdx, row ->
                    Row(
                        modifier = Modifier.height(ROW_HEIGHT),
                    ) {
                        // Row number header
                        val rowSelected = rowIdx == selectedRow
                        Box(
                            modifier = Modifier
                                .width(COL_HEADER_WIDTH)
                                .fillMaxHeight()
                                .background(if (rowSelected) ColSelected else ColGridHeader)
                                .border(BorderStroke(0.5.dp, ColGridBorder)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = rowIdx.toString(),
                                color = if (rowSelected) ColAccent else ColTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (rowSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }

                        // Data cells
                        row.forEachIndexed { colIdx, cellValue ->
                            val isSelected = rowIdx == selectedRow && colIdx == selectedCol
                            val focusRequester = remember { FocusRequester() }
                            Box(
                                modifier = Modifier
                                    .width(COL_WIDTH)
                                    .fillMaxHeight()
                                    .background(
                                        when {
                                            isSelected -> ColSelected
                                            rowIdx % 2 == 0 -> ColBg
                                            else -> ColGrid
                                        }
                                    )
                                    .border(BorderStroke(0.5.dp, ColGridBorder))
                                    .clickable { onCellSelected(rowIdx, colIdx) }
                                    .onKeyEvent { event ->                          // intercept keystrokes
                                        if (NAVIGATION_CHARS.contains(event.key)) {
                                            navigate(event.key)
                                        }
                                        if (isSelected &&
                                            event.type == KeyEventType.KeyDown &&
                                            !IGNORE_CHARS.contains(event.key) &&
                                            event.utf16CodePoint > 0x1F             // printable chars only
                                        ) {
                                            val char = if (event.isShiftPressed) {
                                                when (event.key) {
                                                    Key.Zero -> ')'
                                                    Key.One -> '!'
                                                    Key.Two -> '@'
                                                    Key.Three -> '#'
                                                    Key.Four -> '$'
                                                    Key.Five -> '%'
                                                    Key.Six -> '^'
                                                    Key.Seven -> '&'
                                                    Key.Eight -> '*'
                                                    Key.Nine -> '('
                                                    Key.Equals -> '+'
                                                    Key.Minus -> '_'
                                                    Key.Semicolon -> ':'
                                                    Key.Apostrophe -> '"'
                                                    Key.Comma -> '<'
                                                    Key.Period -> '>'
                                                    Key.Slash -> '?'
                                                    Key.Backslash -> '|'
                                                    Key.LeftBracket -> '{'
                                                    Key.RightBracket -> '}'
                                                    Key.Grave -> '~'
                                                    else -> event.utf16CodePoint.toChar().uppercaseChar()
                                                }
                                            } else {
                                                event.utf16CodePoint.toChar()
                                            }
                                            onKeyStartTyping(char)
                                            true                                    // consumed
                                        } else false
                                    },
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                if (isSelected) {
                                    BasicTextField(
                                        value = cellValue.cell.displayValue(),
                                        onValueChange = { onCellEdited(rowIdx, colIdx, it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp)
                                            .focusRequester(focusRequester),
                                        textStyle = TextStyle(
                                            color = ColText,
                                            fontSize = 12.sp,
                                        ),
                                        singleLine = true,
                                        cursorBrush = SolidColor(ColAccent),
                                    )
                                    LaunchedEffect(true) {
                                        focusRequester.requestFocus()
                                    }
                                } else {
                                    Text(
                                        text = cellValue.cell.displayValue(),
                                        color = ColText,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (cellValue.bold) FontWeight.Bold else FontWeight.Normal,
                                        fontStyle = if (cellValue.italic) FontStyle.Italic else FontStyle.Normal,
                                        textDecoration = when {
                                            cellValue.underline -> TextDecoration.Underline
                                            cellValue.strike -> TextDecoration.LineThrough
                                            else -> TextDecoration.None
                                        },
                                    )
                                }

                                // Selection border highlight
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(BorderStroke(1.5.dp, ColAccent)),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Main ─────────────────────────────────────────────────────────────────────

fun main() = application {

    LaunchedEffect(Unit) {
        NativeBridge.init(ROWS, COLS)
    }

    // ── State ──────────────────────────────────────────────────────────────
    val cellReps = remember {
        mutableStateListOf(*Array(ROWS) { row ->
            Array(COLS) { col ->
                CellRepresentation(
                    height = ROW_HEIGHT,
                    width = COL_WIDTH,
                    cell = Cell(row, col)
                )
            }
        })
    }
    var selectedRow by remember { mutableStateOf(0) }
    var selectedCol by remember { mutableStateOf(0) }
    var formulaText by remember { mutableStateOf("") }

    fun currentSelection() : CellRepresentation {
        return cellReps[selectedRow][selectedCol]
    }

    fun syncFormulaBar() {
        formulaText = currentSelection().cell.displayValue()
    }

    // Format toggles
    var bold by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }
    var underline by remember { mutableStateOf(false) }
    var wrapText by remember { mutableStateOf(false) }

    fun syncStyleIndicators() {
        bold = currentSelection().bold
        italic = currentSelection().italic
        underline = currentSelection().underline
        wrapText = currentSelection().wrapText
    }

    fun commitFormula(row: Int, col: Int, formula: String) {
        val colLetter = ('A' + col)
        NativeBridge.processCommand("$colLetter$row = $formula")

        val result = NativeBridge.getCellValue(row, col)
        val newContent = when {
            result.isEmpty()            -> CellContent.Empty
            result.startsWith("#ERR")   -> CellContent.ErrorContent(result)
            result.toDoubleOrNull() != null -> CellContent.NumberContent(result.toDouble())
            else                        -> CellContent.TextContent(result)
        }
        val newCell = cellReps[row][col].copy(
            cell = cellReps[row][col].cell.copy(content = newContent)
        )
        cellReps[row] = cellReps[row].toMutableList()
            .also { it[col] = newCell }.toTypedArray()
        formulaText = result
    }

    NucleusDecoratedWindowTheme(isDark = true) {
        DecoratedWindow(
            onCloseRequest = ::exitApplication,
            title = "Spreadsheet Editor",
        ) {
            Column(modifier = Modifier.fillMaxSize().background(ColBg)) {

                // ── Title bar ──────────────────────────────────────────────
                TitleBar(
                    backgroundContent = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(colorStops = titleBarGradient))
                        )
                    }
                ) { _: DecoratedWindowState ->
                    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
                    Row(
                        modifier = Modifier
                            .fillMaxSize(if (isWindows) 1f else 0.9f)
                            .padding(horizontal = if (isWindows) 78.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Spreadsheet Editor", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── Ribbon ────────────────────────────────────────────────
                Ribbon(
                    modifier = Modifier
                        .background(ColRibbon)
                        .height(120.dp)
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, ColDivider)),
                    contentUnits = listOf(

                        // ── File ──────────────────────────────────────────
                        listOf(
                            {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                ) {
                                    RibbonEntry(
                                        icon = {
                                            Icon(
                                                imageVector = io.github.composefluent.icons.Icons.Filled.Add,
                                                "New",
                                                tint = ColText,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        },
                                        label = "New",
                                        onClick = {},
                                        textColor = Color.White
                                    )
                                }
                            },
                            {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                ) {
                                    RibbonEntry(
                                        icon = {
                                            Icon(
                                                io.github.composefluent.icons.Icons.Filled.Edit,
                                                "Open",
                                                tint = ColText,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        },
                                        label = "Open",
                                        onClick = {},
                                        textColor = Color.White
                                    )
                                }
                            },
                            {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    // Save (large) + Save As (small) stacked
                                    RibbonEntry(
                                        icon = {
                                            Icon(
                                                io.github.composefluent.icons.Icons.Filled.Save,
                                                "Save",
                                                tint = ColText,
                                                modifier = Modifier.size(32.dp)
                                            )
                                        },
                                        label = "Save",
                                        onClick = {},
                                        textColor = Color.White
                                    )
                                }
                            },
                            {
                                Column(
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 2.dp),
                                ) {
                                    SmallRibbonButton(Icons.Default.Share, "Save As", onClick = {})
                                    SmallRibbonButton(Icons.Default.Share, "Export", onClick = {})
                                }
                            },
                        ),

                        // ── Edit ──────────────────────────────────────────
                        listOf(
                            {
                                Column(
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 2.dp),
                                ) {
                                    SmallRibbonButton(
                                        io.github.composefluent.icons.Icons.Default.Copy,
                                        "Copy",
                                        onClick = {}
                                    )
                                    SmallRibbonButton(
                                        io.github.composefluent.icons.Icons.Default.ClipboardPaste,
                                        "Paste",
                                        onClick = {}
                                    )
                                }
                            },
                            {
                                Column(
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 2.dp),
                                ) {
                                    SmallRibbonButton(
                                        io.github.composefluent.icons.Icons.Default.Cut,
                                        "Cut",
                                        onClick = {})
                                    SmallRibbonButton(
                                        io.github.composefluent.icons.Icons.Default.Delete,
                                        "Clear",
                                        onClick = {})
                                }
                            },
                            {
                                Column(
                                    verticalArrangement = Arrangement.SpaceEvenly,
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 2.dp),
                                ) {
                                    SmallRibbonButton(
                                        io.github.composefluent.icons.Icons.Default.ArrowUndo,
                                        "Undo",
                                        onClick = {})
                                    SmallRibbonButton(
                                        io.github.composefluent.icons.Icons.Default.ArrowRedo,
                                        "Redo",
                                        onClick = {})
                                }
                            },
                        ),

                        // ── Format ────────────────────────────────────────
                        listOf(
                            {
                                Column(
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Bold / Italic / Underline toggles
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        ToggleRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.TextBold,
                                            "Bold",
                                            bold,
                                            {
                                                cellReps[selectedRow][selectedCol].bold = !cellReps[selectedRow][selectedCol].bold
                                                syncStyleIndicators()
                                            })
                                        ToggleRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.TextItalic,
                                            "Italic",
                                            italic,
                                            {
                                                cellReps[selectedRow][selectedCol].italic = !cellReps[selectedRow][selectedCol].italic
                                                syncStyleIndicators()
                                            })
                                        ToggleRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.TextUnderline,
                                            "Underline",
                                            underline,
                                            {
                                                cellReps[selectedRow][selectedCol].underline = !cellReps[selectedRow][selectedCol].underline
                                                syncStyleIndicators()
                                            })
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        SmallRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.TextField,
                                            "Text",
                                            onClick = {})
                                        SmallRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.ColorFill,
                                            "Fill",
                                            onClick = {})
                                        SmallRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.BorderAll,
                                            "Border",
                                            onClick = {})
                                    }
                                    RibbonSectionLabel("STYLE")
                                }
                            },
                            {
                                Column(
                                    modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Alignment row
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        SmallRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.AlignLeft,
                                            "Left",
                                            onClick = {})
                                        SmallRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.AlignCenterHorizontal,
                                            "Center",
                                            onClick = {})
                                        SmallRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.AlignRight,
                                            "Right",
                                            onClick = {})
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    ToggleRibbonButton(
                                        io.github.composefluent.icons.Icons.Default.TextWrap,
                                        "Wrap",
                                        wrapText,
                                        { wrapText = it },
                                        modifier = Modifier.width(96.dp)
                                    )
                                    RibbonSectionLabel("FORMAT")
                                }
                            },
                        ),

                        // ── Insert ────────────────────────────────────────
                        listOf(
                            {
                                RibbonEntry(
                                    icon = {
                                        Icon(
                                            io.github.composefluent.icons.Icons.Default.MathFormula,
                                            "Function",
                                            tint = ColText,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    label = "Function",
                                    onClick = {},
                                    textColor = Color.White
                                )
                            },
                            {
                                var flyoutVisible by remember { mutableStateOf(false) }

                                Box(
                                    modifier = Modifier.clip(shape = RoundedCornerShape(4.dp)).background(Color(210,210,210))
                                ) {
                                    SubtleButton(
                                        onClick = { flyoutVisible = !flyoutVisible },
                                        modifier = Modifier.commandBarButtonSize(),
                                        content = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.width(110.dp).clip(shape = FluentTheme.shapes.overlay)
                                            ) {
                                                Text(cellReps[selectedRow][selectedCol].cell.content.type.toMenuLabel(), color = Color.Black)
                                                Text("▾", color = Color.Black)
                                            }
                                        }
                                    )

                                    MenuFlyout(
                                        visible = flyoutVisible,
                                        onDismissRequest = { flyoutVisible = false },
                                        modifier = Modifier.background(
                                            color = Color(0xFF303030),
                                            shape = FluentTheme.shapes.overlay
                                        )
                                    ) {
                                        CellContentType.entries.dropLast(1).forEach { option ->
                                            MenuFlyoutItem(
                                                onClick = {
                                                    flyoutVisible = false
                                                    currentSelection().cell.content = currentSelection().cell.content.convertTo(option)
                                                },
                                                text = { Text(option.toMenuLabel(), color = ColText) },
                                                colors = ListItemDefaults.defaultListItemColors().copy(
                                                    hovered = ListItemDefaults.defaultListItemColors().hovered.copy(
                                                        fillColor = Color(0xFF202020)
                                                    )
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        ),

                        )
                )

                // ── Formula bar ───────────────────────────────────────────
                FormulaBar(
                    cellAddress = cellReps.toTypedArray()[selectedRow][selectedCol].cellAddress(),
                    formula = formulaText,
                    onFormulaChange = { formulaText = it },
                    onCommit = {
                        val row = selectedRow
                        val col = selectedCol
                        val isFormula = formulaText.startsWith("=")

                        when {
                            isFormula -> {
                                // Remove leading `=` before sending to C
                                commitFormula(row, col, formulaText.drop(1))
                            }
                            else -> {
                                val numberValue = formulaText.toDoubleOrNull()
                                val newContent = if (formulaText.isEmpty()) {
                                    CellContent.Empty
                                } else if (numberValue != null) {
                                    val colLetter = ('A' + col)
                                    NativeBridge.processCommand("$colLetter$row = $numberValue")
                                    CellContent.NumberContent(numberValue)
                                } else {
                                    CellContent.TextContent(formulaText)
                                }

                                val newCell = cellReps[row][col].copy(
                                    cell = cellReps[row][col].cell.copy(
                                        content = newContent
                                    )
                                )
                                cellReps[row] = cellReps[row].toMutableList()
                                    .also { it[col] = newCell }.toTypedArray()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                // ── Spreadsheet grid ──────────────────────────────────────
                SpreadsheetGrid(
                    cells = cellReps.toTypedArray(),
                    selectedRow = selectedRow,
                    selectedCol = selectedCol,
                    onCellSelected = { row, col ->
                        if (row < 0 || col < 0) return@SpreadsheetGrid
                        selectedRow = row
                        selectedCol = col
                        syncFormulaBar()
                        syncStyleIndicators()
                    },
                    onCellEdited = { row, col, value ->
                        val isFormula = value.startsWith("=")
                        val newContent = when {
                            isFormula -> CellContent.FormulaContent(value.drop(1))
                            else -> {
                                value.toDoubleOrNull()
                                    ?.let {
                                        val colLetter = ('A' + col)
                                        NativeBridge.processCommand("$colLetter$row = $it")
                                        CellContent.NumberContent(it)
                                    }
                                    ?: CellContent.TextContent(value)
                            }
                        }
                        val newCell = cellReps[row][col].copy(
                            cell = cellReps[row][col].cell.copy(content = newContent)
                        )
                        cellReps[row] = cellReps[row].toMutableList()
                            .also { it[col] = newCell }.toTypedArray()
                        formulaText = value
                    },
                    onKeyStartTyping = { char ->
                        val row = selectedRow;
                        val col = selectedCol
                        val seeded = char.toString()

                        val isFormula = seeded.startsWith("=")
                        val newContent = when {
                            isFormula -> CellContent.FormulaContent(seeded.drop(1))
                            else -> {
                                seeded.toDoubleOrNull()
                                    ?.let {
                                        val colLetter = ('A' + col)
                                        NativeBridge.processCommand("$colLetter$row = $it")
                                        CellContent.NumberContent(it)
                                    }
                                    ?: CellContent.TextContent(seeded)
                            }
                        }

                        val newCell = cellReps[row][col].copy(
                            cell = cellReps[row][col].cell.copy(
                                content = newContent
                            )
                        )
                        cellReps[row] = cellReps[row].toMutableList()
                            .also { it[col] = newCell }.toTypedArray()
                        formulaText = seeded
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}