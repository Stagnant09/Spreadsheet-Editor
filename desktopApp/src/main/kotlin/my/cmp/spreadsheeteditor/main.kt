package my.cmp.spreadsheeteditor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.application
import dev.nucleusframework.window.DecoratedWindow
import dev.nucleusframework.window.DecoratedWindowState
import dev.nucleusframework.window.NucleusDecoratedWindowTheme
import dev.nucleusframework.window.TitleBar
import io.github.composefluent.component.MenuFlyout
import io.github.composefluent.icons.Icons
import io.github.composefluent.icons.filled.Settings
import my.cmp.spreadsheeteditor.models.Cell
import my.cmp.spreadsheeteditor.models.Cell.Companion.displayValue
import my.cmp.spreadsheeteditor.models.CellContent
import my.cmp.spreadsheeteditor.models.CellRepresentation
import my.cmp.spreadsheeteditor.models.CellRepresentation.Companion.cellAddress
import my.cmp.spreadsheeteditor.ui.components.*
import my.cmp.spreadsheeteditor.ui.theme.SpreadsheetTheme
import my.cmp.spreadsheeteditor.utils.FormulaDependencyGraph
import my.cmp.spreadsheeteditor.utils.columnLabel
import my.cmp.spreadsheeteditor.utils.getNewContent
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FilenameFilter
import java.util.*

// ─── Main ─────────────────────────────────────────────────────────────────────

fun main() {
    if (System.getProperty("os.name").lowercase().contains("linux")) {
        System.setProperty("skiko.renderApi", "SOFTWARE")
        System.setProperty("sun.java2d.opengl", "false")
    }
    application {

    var isDark by remember { mutableStateOf(true) }
    var isFlyoutVisible by remember { mutableStateOf(false) }

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
    var selectionAnchorRow by remember { mutableStateOf(0) }
    var selectionAnchorCol by remember { mutableStateOf(0) }
    var formulaText by remember { mutableStateOf("") }

    // Tracks which cells' formulas read which other cells, so editing a
    // cell can auto-recalculate everything downstream of it, and so a
    // formula that would create a reference cycle can be rejected.
    val depGraph = remember { FormulaDependencyGraph() }

    // Currently open file (set on Save/Save As/Open), used so a plain
    // "Save" re-uses the last path instead of always prompting.
    var currentFile by remember { mutableStateOf<File?>(null) }

    val undoStack = remember { mutableStateListOf<List<Array<CellRepresentation>>>() }
    val redoStack = remember { mutableStateListOf<List<Array<CellRepresentation>>>() }

    fun pushUndo() {
        val snapshot = cellReps.map { row ->
            row.map { it.copy(cell = it.cell.copy(content = it.cell.content)) }.toTypedArray()
        }
        undoStack.add(snapshot)
        redoStack.clear()
        if (undoStack.size > 50) undoStack.removeAt(0)
    }

    // Rectangular block clipboard (supports both single-cell and multi-cell copy/paste)
    var clipboardBlock: Array<Array<CellRepresentation>>? by remember { mutableStateOf(null) }

    fun currentSelection(): CellRepresentation {
        return cellReps[selectedRow][selectedCol]
    }

    fun syncFormulaBar() {
        formulaText = currentSelection().cell.displayValue()
    }

    // Runs [action] over every cell in the current selection rectangle
    // (anchor..focus), so formatting/clear operations apply to the whole
    // multi-cell selection instead of just the last-clicked cell.
    fun forEachSelectedCell(action: (row: Int, col: Int) -> Unit) {
        val minRow = minOf(selectionAnchorRow, selectedRow)
        val maxRow = maxOf(selectionAnchorRow, selectedRow)
        val minCol = minOf(selectionAnchorCol, selectedCol)
        val maxCol = maxOf(selectionAnchorCol, selectedCol)
        for (r in minRow..maxRow) for (c in minCol..maxCol) action(r, c)
    }

    // Replaces cellReps[row][col] via an explicit list rebuild + reassignment,
    // which is what makes Compose's snapshot list notice the change — mutating
    // a field on the CellRepresentation object in place would not.
    fun updateCellRep(row: Int, col: Int, transform: (CellRepresentation) -> CellRepresentation) {
        cellReps[row] = cellReps[row].toMutableList()
            .also { it[col] = transform(it[col]) }.toTypedArray()
    }

    // Format toggles
    var bold by remember { mutableStateOf(false) }
    var italic by remember { mutableStateOf(false) }
    var underline by remember { mutableStateOf(false) }
    var strike by remember { mutableStateOf(false) }
    var wrapText by remember { mutableStateOf(false) }
    var textAlign by remember { mutableStateOf(TextAlign.Left) }
    var fontFamily by remember { mutableStateOf<FontFamily>(FontFamily.Default) }
    var fontSize by remember { mutableStateOf(12f) }

    var fontColor by remember { mutableStateOf(Color.Unspecified) }
    var backgroundColor by remember { mutableStateOf(Color.Transparent) }

    fun syncStyleIndicators() {
        bold = currentSelection().bold
        italic = currentSelection().italic
        underline = currentSelection().underline
        strike = currentSelection().strike
        wrapText = currentSelection().wrapText
        textAlign = currentSelection().textAlign
        fontFamily = currentSelection().fontFamily
        fontSize = currentSelection().fontSize
        fontColor = currentSelection().fontColor
        backgroundColor = currentSelection().backgroundColor
    }

    // Converts a raw engine result string into typed cell content. When
    // [formulaSource] is non-null the result came from evaluating that
    // formula, so the content keeps the formula text (for the formula bar
    // and CSV round-trip) alongside the freshly computed value.
    fun classifyEngineResult(result: String, formulaSource: String?): CellContent = when {
        result.isEmpty() -> CellContent.Empty
        result.startsWith("#") -> CellContent.ErrorContent(result)
        result.toDoubleOrNull() != null -> if (formulaSource != null) {
            CellContent.FormulaContent(formulaSource, cachedResult = result.toDouble())
        } else {
            CellContent.NumberContent(result.toDouble())
        }
        else -> CellContent.TextContent(result)
    }

    fun applyContent(row: Int, col: Int, newContent: CellContent) {
        updateCellRep(row, col) { it.copy(cell = it.cell.copy(content = newContent)) }
    }

    // Re-evaluates every cell that (directly or transitively) reads [row]/[col],
    // using a breadth-first walk over the dependency graph so a change to one
    // cell ripples through the whole formula chain, e.g. A1 -> A2 -> A3.
    fun recalcDependents(row: Int, col: Int) {
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.addAll(depGraph.getDirectDependents(row to col))
        val seen = mutableSetOf<Pair<Int, Int>>()
        while (queue.isNotEmpty()) {
            val (r, c) = queue.removeFirst()
            if (!seen.add(r to c)) continue
            val content = cellReps[r][c].cell.content
            if (content is CellContent.FormulaContent) {
                NativeBridge.processCommand("${columnLabel(c)}$r = ${content.value}")
                val result = NativeBridge.getCellValue(r, c)
                applyContent(r, c, classifyEngineResult(result, content.value))
            }
            queue.addAll(depGraph.getDirectDependents(r to c))
        }
    }

    // Rebuilds the native engine's matrix and the dependency graph from the
    // Kotlin-side cellReps, e.g. after undo/redo or loading a file, where
    // cellReps changed wholesale without the engine being told about it.
    fun resyncEngineFromCellReps() {
        NativeBridge.init(ROWS, COLS)
        depGraph.clearAll()
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            when (val content = cellReps[r][c].cell.content) {
                is CellContent.NumberContent ->
                    NativeBridge.processCommand("${columnLabel(c)}$r = ${content.value}")
                is CellContent.FormulaContent ->
                    depGraph.setDependencies(r to c, depGraph.parseReferences(content.value))
                else -> {}
            }
        }
        // Evaluate formulas twice so that forward references (a formula
        // referring to another formula defined later in the sheet) resolve
        // correctly once every base value is in place.
        repeat(2) {
            for (r in 0 until ROWS) for (c in 0 until COLS) {
                val content = cellReps[r][c].cell.content
                if (content is CellContent.FormulaContent) {
                    NativeBridge.processCommand("${columnLabel(c)}$r = ${content.value}")
                }
            }
        }
        for (r in 0 until ROWS) for (c in 0 until COLS) {
            val content = cellReps[r][c].cell.content
            if (content is CellContent.NumberContent || content is CellContent.FormulaContent) {
                val formulaSource = (content as? CellContent.FormulaContent)?.value
                applyContent(r, c, classifyEngineResult(NativeBridge.getCellValue(r, c), formulaSource))
            }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val currentState = cellReps.map { row ->
                row.map { it.copy(cell = it.cell.copy(content = it.cell.content)) }.toTypedArray()
            }
            redoStack.add(currentState)
            val prevState = undoStack.removeAt(undoStack.size - 1)
            cellReps.clear()
            cellReps.addAll(prevState)
            resyncEngineFromCellReps()
            syncStyleIndicators()
            syncFormulaBar()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val currentState = cellReps.map { row ->
                row.map { it.copy(cell = it.cell.copy(content = it.cell.content)) }.toTypedArray()
            }
            undoStack.add(currentState)
            val nextState = redoStack.removeAt(redoStack.size - 1)
            cellReps.clear()
            cellReps.addAll(nextState)
            resyncEngineFromCellReps()
            syncStyleIndicators()
            syncFormulaBar()
        }
    }

    fun copy() {
        val minRow = minOf(selectionAnchorRow, selectedRow)
        val maxRow = maxOf(selectionAnchorRow, selectedRow)
        val minCol = minOf(selectionAnchorCol, selectedCol)
        val maxCol = maxOf(selectionAnchorCol, selectedCol)
        clipboardBlock = Array(maxRow - minRow + 1) { r ->
            Array(maxCol - minCol + 1) { c ->
                val src = cellReps[minRow + r][minCol + c]
                src.copy(cell = src.cell.copy(content = src.cell.content))
            }
        }
    }

    fun paste() {
        val block = clipboardBlock ?: return
        pushUndo()
        val destRow0 = selectedRow
        val destCol0 = selectedCol
        for (r in block.indices) {
            val destRow = destRow0 + r
            if (destRow !in 0 until ROWS) continue
            for (c in block[r].indices) {
                val destCol = destCol0 + c
                if (destCol !in 0 until COLS) continue
                val src = block[r][c]
                updateCellRep(destRow, destCol) {
                    it.copy(
                        cell = it.cell.copy(content = src.cell.content),
                        bold = src.bold,
                        italic = src.italic,
                        underline = src.underline,
                        strike = src.strike,
                        fontColor = src.fontColor,
                        backgroundColor = src.backgroundColor,
                        textAlign = src.textAlign,
                        wrapText = src.wrapText
                    )
                }

                // Mirror the pasted value into the native engine so formulas
                // elsewhere that reference this cell see the new value, and
                // keep the dependency graph in sync. Note: formula text is
                // copied verbatim (no relative-reference shifting).
                val colLetter = columnLabel(destCol)
                when (val content = src.cell.content) {
                    is CellContent.NumberContent -> {
                        NativeBridge.processCommand("$colLetter$destRow = ${content.value}")
                        depGraph.clearDependencies(destRow to destCol)
                    }
                    is CellContent.FormulaContent -> {
                        val deps = depGraph.parseReferences(content.value)
                        if (!depGraph.wouldCreateCycle(destRow to destCol, deps)) {
                            depGraph.setDependencies(destRow to destCol, deps)
                            NativeBridge.processCommand("$colLetter$destRow = ${content.value}")
                        }
                    }
                    else -> depGraph.clearDependencies(destRow to destCol)
                }
            }
        }
        syncStyleIndicators()
        syncFormulaBar()
        // Recalculate anything downstream of the pasted block.
        for (r in block.indices) {
            for (c in block[r].indices) {
                val destRow = destRow0 + r
                val destCol = destCol0 + c
                if (destRow in 0 until ROWS && destCol in 0 until COLS) {
                    recalcDependents(destRow, destCol)
                }
            }
        }
    }

    fun saveToCsv(file: File) {
        val sb = StringBuilder()
        for (row in 0 until ROWS) {
            val rowStrings = mutableListOf<String>()
            for (col in 0 until COLS) {
                val content = cellReps[row][col].cell.content
                val value = when (content) {
                    is CellContent.FormulaContent -> "=${content.value}"
                    else -> cellReps[row][col].cell.displayValue()
                }
                val escaped = if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                    "\"" + value.replace("\"", "\"\"") + "\""
                } else {
                    value
                }
                rowStrings.add(escaped)
            }
            sb.append(rowStrings.joinToString(",")).append("\n")
        }
        file.writeText(sb.toString())
    }

    fun loadFromCsv(file: File) {
        if (!file.exists()) return
        pushUndo()
        val lines = file.readLines()
        for (r in 0 until minOf(ROWS, lines.size)) {
            val cols = lines[r].split(",")
            for (c in 0 until minOf(COLS, cols.size)) {
                val value = cols[c].removeSurrounding("\"").replace("\"\"", "\"")
                val content = getNewContent(r, c, value)
                val cellRep = cellReps[r][c].copy(
                    cell = cellReps[r][c].cell.copy(content = content)
                )
                cellReps[r] = cellReps[r].toMutableList().also { it[c] = cellRep }.toTypedArray()
            }
        }
        resyncEngineFromCellReps()
        syncFormulaBar()
        syncStyleIndicators()
    }

    fun setNewContent(row: Int, col: Int, newContent: CellContent, value: String) {
        pushUndo()
        applyContent(row, col, newContent)
        formulaText = value
        // Plain value edit: this cell is no longer (or wasn't) a formula, so
        // drop any stale dependency bookkeeping, then propagate the new
        // value to anything downstream that references this cell.
        depGraph.clearDependencies(row to col)
        recalcDependents(row, col)
    }

    fun commitFormula(row: Int, col: Int, formula: String) {
        val cellKey = row to col
        val deps = depGraph.parseReferences(formula)
        if (depGraph.wouldCreateCycle(cellKey, deps)) {
            depGraph.clearDependencies(cellKey)
            applyContent(row, col, CellContent.ErrorContent("#CIRCULAR!"))
            formulaText = "=$formula"
            return
        }
        depGraph.setDependencies(cellKey, deps)
        NativeBridge.processCommand("${columnLabel(col)}$row = $formula")
        val result = NativeBridge.getCellValue(row, col)
        applyContent(row, col, classifyEngineResult(result, formula))
        formulaText = result
        recalcDependents(row, col)
    }

    fun commitPendingEditIfFormula(row: Int, col: Int) {
        val content = cellReps[row][col].cell.content
        // A FormulaContent with no cachedResult yet was typed directly into
        // the grid and never sent to the engine (typing happens keystroke by
        // keystroke, so it isn't evaluated until the cell loses focus).
        if (content is CellContent.FormulaContent && content.cachedResult == null) {
            commitFormula(row, col, content.value)
        }
    }

    fun clearSelectionRange() {
        pushUndo()
        forEachSelectedCell { r, c ->
            NativeBridge.processCommand("${columnLabel(c)}$r = 0")
            depGraph.clearDependencies(r to c)
            applyContent(r, c, CellContent.Empty)
        }
        forEachSelectedCell { r, c -> recalcDependents(r, c) }
        syncFormulaBar()
        syncStyleIndicators()
    }

    fun chooseSaveFile(defaultName: String = currentFile?.name ?: "spreadsheet.csv"): File? {
        val dialog = FileDialog(Frame(), "Save Spreadsheet", FileDialog.SAVE)
        dialog.file = defaultName
        dialog.filenameFilter = FilenameFilter { _, name -> name.endsWith(".csv") }
        dialog.isVisible = true
        val name = dialog.file ?: return null
        val fileName = if (name.endsWith(".csv")) name else "$name.csv"
        return File(dialog.directory ?: "", fileName)
    }

    fun chooseOpenFile(): File? {
        val dialog = FileDialog(Frame(), "Open Spreadsheet", FileDialog.LOAD)
        dialog.filenameFilter = FilenameFilter { _, name -> name.endsWith(".csv") }
        dialog.isVisible = true
        val name = dialog.file ?: return null
        return File(dialog.directory ?: "", name)
    }

    fun doSave() {
        val target = currentFile ?: chooseSaveFile() ?: return
        saveToCsv(target)
        currentFile = target
    }

    fun doSaveAs() {
        val target = chooseSaveFile() ?: return
        saveToCsv(target)
        currentFile = target
    }

    fun doOpen() {
        val target = chooseOpenFile() ?: return
        loadFromCsv(target)
        currentFile = target
    }

    NucleusDecoratedWindowTheme(isDark = isDark) {
        SpreadsheetTheme(isDark = isDark) {
            DecoratedWindow(
                onCloseRequest = ::exitApplication,
                title = "Spreadsheet Editor",
                minimumSize = DpSize(1400.dp, 700.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize().background(SpreadsheetTheme.colors.colBg)) {

                    // ── Title bar ──────────────────────────────────────────────
                    TitleBar(
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.horizontalGradient(colorStops = SpreadsheetTheme.colors.titleBarGradient))
                            )
                        }
                    ) { _: DecoratedWindowState ->
                        // NOTE: previously this block wrapped everything in a raw
                        // Row(Modifier.fillMaxSize().padding(start = .., end = ..))
                        // with hand-guessed dp values meant to dodge the native
                        // window control buttons (min/max/close). TitleBar already
                        // reserves that region itself — the guessed padding fought
                        // it and is what was clipping the close button. Using
                        // Modifier.align(...) directly on each child, as below, is
                        // the correct way to lay out content inside TitleBarScope.

                        // Settings gear, start-aligned.
                        Box(modifier = Modifier.align(Alignment.Start)) {
                            IconButton(onClick = { isFlyoutVisible = true }) {
                                Icon(
                                    imageVector = Icons.Filled.Settings,
                                    contentDescription = "Settings",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // MenuFlyout is an anchored popup off this Box — it
                            // handles its own placement. The old code wrapped it in
                            // an extra Box(Modifier.padding(top = 40.dp)), which is
                            // NOT a popup; that Box just overflowed in normal layout
                            // flow from wherever the (broken) parent Row placed the
                            // gear button, landing top-left and overlapping the
                            // ribbon. Removing that wrapper fixes the placement.
                            MenuFlyout(
                                modifier = Modifier.offset(x = 100.dp),
                                visible = isFlyoutVisible,
                                onDismissRequest = { isFlyoutVisible = false }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .width(220.dp)
                                        .background(
                                            color = SpreadsheetTheme.colors.colSurface,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                        )
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {} // Consume clicks to prevent dismissal
                                        )
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Settings",
                                        style = MaterialTheme.typography.subtitle2,
                                        color = SpreadsheetTheme.colors.colText
                                    )
                                    Divider(color = SpreadsheetTheme.colors.colDivider)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { isDark = !isDark }
                                        ) {
                                            Text(
                                                "Dark Mode",
                                                color = SpreadsheetTheme.colors.colText,
                                                style = MaterialTheme.typography.body2
                                            )
                                            Switch(
                                                checked = isDark,
                                                onCheckedChange = { isDark = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = SpreadsheetTheme.colors.colAccent,
                                                    checkedTrackColor = SpreadsheetTheme.colors.colAccent.copy(alpha = 0.5f)
                                                ),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Title, centered within the safe area TitleBar computes
                        // around the control buttons.
                        Text(
                            "Spreadsheet Editor",
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.body1
                        )
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
                            val target = !currentSelection().bold
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(bold = target) } }
                            syncStyleIndicators()
                        },
                        onItalicToggle = {
                            val target = !currentSelection().italic
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(italic = target) } }
                            syncStyleIndicators()
                        },
                        onUnderlineToggle = {
                            val target = !currentSelection().underline
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(underline = target) } }
                            syncStyleIndicators()
                        },
                        onStrikeToggle = {
                            val target = !currentSelection().strike
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(strike = target) } }
                            syncStyleIndicators()
                        },
                        onTextAlignChange = { align ->
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(textAlign = align) } }
                            syncStyleIndicators()
                        },
                        onWrapTextToggle = { value ->
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(wrapText = value) } }
                            wrapText = value
                        },
                        onCellTypeChange = { type ->
                            pushUndo()
                            forEachSelectedCell { r, c ->
                                val newContent = cellReps[r][c].cell.content.convertTo(type)
                                if (newContent !is CellContent.FormulaContent) {
                                    depGraph.clearDependencies(r to c)
                                }
                                applyContent(r, c, newContent)
                            }
                            forEachSelectedCell { r, c -> recalcDependents(r, c) }
                        },
                        onColorSelected = { color ->
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(fontColor = color) } }
                            syncStyleIndicators()
                        },
                        onBackgroundColorSelected = { color ->
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(backgroundColor = color) } }
                            syncStyleIndicators()
                        },
                        onSave = { doSave() },
                        onSaveAs = { doSaveAs() },
                        onLoad = { doOpen() },
                        onUndo = { undo() },
                        onRedo = { redo() },
                        onCopy = { copy() },
                        onPaste = { paste() },
                        onClear = { clearSelectionRange() },
                        fontColor = fontColor,
                        backgroundColor = backgroundColor,
                        fontFamily = fontFamily,
                        fontSize = fontSize,
                        onFontFamilyChange = { fontFamily ->
                            cellReps.toTypedArray()[selectedRow][selectedCol].fontFamily = fontFamily
                            syncStyleIndicators()
                        },
                        onFontSizeChange = { newSize ->
                            pushUndo()
                            forEachSelectedCell { r, c -> updateCellRep(r, c) { it.copy(fontSize = newSize) } }
                            syncStyleIndicators()
                        }
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

                            pushUndo()
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
                                        NativeBridge.processCommand("${columnLabel(col)}$row = $numberValue")
                                        CellContent.NumberContent(numberValue)
                                    } else {
                                        CellContent.TextContent(formulaText)
                                    }
                                    depGraph.clearDependencies(row to col)
                                    applyContent(row, col, newContent)
                                    recalcDependents(row, col)
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
                                    if(cell.fontColor == Color.Unspecified) cell.fontColor = SpreadsheetTheme.colors.colText
                                    if(cell.backgroundColor == Color.Unspecified) cell.backgroundColor = Color.Transparent
                                }
                            }
                        }.toTypedArray(),
                        selectedRow = selectedRow,
                        selectedCol = selectedCol,
                        selectionAnchorRow = selectionAnchorRow,
                        selectionAnchorCol = selectionAnchorCol,
                        onCellSelected = { row, col ->
                            if (row < 0 || col < 0) return@SpreadsheetGrid
                            commitPendingEditIfFormula(selectedRow, selectedCol)
                            selectedRow = row
                            selectedCol = col
                            selectionAnchorRow = row
                            selectionAnchorCol = col
                            syncFormulaBar()
                            syncStyleIndicators()
                        },
                        onSelectionExtend = { row, col ->
                            val clampedRow = row.coerceIn(0, ROWS - 1)
                            val clampedCol = col.coerceIn(0, COLS - 1)
                            commitPendingEditIfFormula(selectedRow, selectedCol)
                            selectedRow = clampedRow
                            selectedCol = clampedCol
                            // anchor stays put; only the focus cell moves, growing the range
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
}
}