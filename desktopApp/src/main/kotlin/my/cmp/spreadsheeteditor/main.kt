package my.cmp.spreadsheeteditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import dev.nucleusframework.window.DecoratedWindow
import dev.nucleusframework.window.DecoratedWindowState
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.TitleBar
import my.cmp.spreadsheeteditor.models.Cell
import my.cmp.spreadsheeteditor.models.Cell.Companion.displayValue
import my.cmp.spreadsheeteditor.models.CellContent
import my.cmp.spreadsheeteditor.models.CellRepresentation
import my.cmp.spreadsheeteditor.models.CellRepresentation.Companion.cellAddress
import my.cmp.spreadsheeteditor.ui.components.*
import my.cmp.spreadsheeteditor.ui.theme.ColBg
import my.cmp.spreadsheeteditor.ui.theme.ColText
import my.cmp.spreadsheeteditor.ui.theme.titleBarGradient
import my.cmp.spreadsheeteditor.utils.getNewContent

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

    fun currentSelection(): CellRepresentation {
        return cellReps[selectedRow][selectedCol]
    }

    fun syncFormulaBar() {
        formulaText = currentSelection().cell.displayValue()
    }

    // Format toggles
    var bold by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }
    var underline by remember { mutableStateOf(false) }
    var strike by remember { mutableStateOf(false) }
    var wrapText by remember { mutableStateOf(false) }
    var textAlign by remember { mutableStateOf(TextAlign.Left) }

    var fontColor by remember { mutableStateOf(ColText) }
    var backgroundColor by remember { mutableStateOf(Color.Transparent) }

    fun syncStyleIndicators() {
        bold = currentSelection().bold
        italic = currentSelection().italic
        underline = currentSelection().underline
        strike = currentSelection().strike
        wrapText = currentSelection().wrapText
        textAlign = currentSelection().textAlign
        fontColor = currentSelection().fontColor
        backgroundColor = currentSelection().backgroundColor
    }

    fun setNewContent(row: Int, col: Int, newContent: CellContent, value: String) {
        val newCell = cellReps[row][col].copy(
            cell = cellReps[row][col].cell.copy(content = newContent)
        )
        cellReps[row] = cellReps[row].toMutableList()
            .also { it[col] = newCell }.toTypedArray()
        formulaText = value
    }

    fun commitFormula(row: Int, col: Int, formula: String) {
        val colLetter = ('A' + col)
        NativeBridge.processCommand("$colLetter$row = $formula")

        val result = NativeBridge.getCellValue(row, col)
        val newContent = when {
            result.isEmpty() -> CellContent.Empty
            result.startsWith("#ERR") -> CellContent.ErrorContent(result)
            result.toDoubleOrNull() != null -> CellContent.NumberContent(result.toDouble())
            else -> CellContent.TextContent(result)
        }
        setNewContent(row, col, newContent, result)
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
                            .fillMaxSize(0.9f)
                            .padding(start = if (isWindows) 80.dp else 0.dp, end = if (isWindows) 200.dp else 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Spreadsheet Editor", color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }

                // ── Ribbon ────────────────────────────────────────────────
                SpreadsheetRibbon(
                    cellReps = cellReps,
                    selectedRow = selectedRow,
                    selectedCol = selectedCol,
                    bold = bold,
                    italic = italic,
                    underline = underline,
                    strike = strike,
                    wrapText = wrapText,
                    onBoldToggle = {
                        cellReps[selectedRow][selectedCol].bold = !cellReps[selectedRow][selectedCol].bold
                        syncStyleIndicators()
                    },
                    onItalicToggle = {
                        cellReps[selectedRow][selectedCol].italic = !cellReps[selectedRow][selectedCol].italic
                        syncStyleIndicators()
                    },
                    onUnderlineToggle = {
                        cellReps[selectedRow][selectedCol].underline = !cellReps[selectedRow][selectedCol].underline
                        syncStyleIndicators()
                    },
                    onStrikeToggle = {
                        cellReps[selectedRow][selectedCol].strike = !cellReps[selectedRow][selectedCol].strike
                        syncStyleIndicators()
                    },
                    onTextAlignChange = {
                        cellReps[selectedRow][selectedCol].textAlign = it
                        syncStyleIndicators()
                    },
                    onWrapTextToggle = { wrapText = it },
                    onCellTypeChange = {
                        currentSelection().cell.content = currentSelection().cell.content.convertTo(it)
                    },
                    onColorSelected = {
                        cellReps[selectedRow][selectedCol].fontColor = it
                        syncStyleIndicators()
                    },
                    onBackgroundColorSelected = {
                        cellReps[selectedRow][selectedCol].backgroundColor = it
                        syncStyleIndicators()
                    },
                    fontColor = fontColor,
                    backgroundColor = backgroundColor
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
                    cells = cellReps.apply {
                        this.forEach { array ->
                            array.forEach { cell ->
                                if(cell.fontColor == Color.Unspecified) cell.fontColor = ColText
                                if(cell.backgroundColor == Color.Unspecified) cell.backgroundColor = Color.Transparent
                            }
                        }
                    }.toTypedArray(),
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
                        val newContent = getNewContent(row, col, value)
                        setNewContent(row, col, newContent, value)
                    },
                    onKeyStartTyping = { char ->
                        val row = selectedRow
                        val col = selectedCol
                        val seeded = char.toString()
                        val newContent = getNewContent(row, col, seeded)
                        setNewContent(row, col, newContent, value = seeded)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
