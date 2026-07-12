package my.cmp.spreadsheeteditor.ui.components

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.cmp.spreadsheeteditor.models.Cell.Companion.displayValue
import my.cmp.spreadsheeteditor.models.CellRepresentation
import my.cmp.spreadsheeteditor.ui.theme.SpreadsheetTheme
import my.cmp.spreadsheeteditor.ui.utils.getTextStyle
import my.cmp.spreadsheeteditor.utils.IGNORE_CHARS
import my.cmp.spreadsheeteditor.utils.NAVIGATION_CHARS
import my.cmp.spreadsheeteditor.utils.columnLabel
import java.awt.Cursor

// ─── Grid constants ───────────────────────────────────────────────────────────
const val ROWS = 50
const val COLS = 50
val COL_HEADER_WIDTH: Dp = 48.dp
val COL_WIDTH: Dp = 120.dp
val ROW_HEIGHT: Dp = 28.dp
val HEADER_HEIGHT: Dp = 26.dp

// ─── Spreadsheet grid ────────────────────────────────────────────────────────
@Composable
fun SpreadsheetGrid(
    cells: Array<Array<CellRepresentation>>,
    rowHeights: List<Dp>,
    colWidths: List<Dp>,
    selectedRow: Int,
    selectedCol: Int,
    selectionAnchorRow: Int = selectedRow,
    selectionAnchorCol: Int = selectedCol,
    onCellSelected: (row: Int, col: Int) -> Unit,
    onSelectionExtend: (row: Int, col: Int) -> Unit = onCellSelected,
    onCellEdited: (row: Int, col: Int, value: String) -> Unit,
    onKeyStartTyping: (Char) -> Unit,
    onRowResized: (Int, Dp) -> Unit,
    onColResized: (Int, Dp) -> Unit,
    onCellRightClick: (row: Int, col: Int, offset: Offset) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
) {
    val colScrollState = rememberScrollState()
    val rowScrollState = rememberScrollState()

    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var isCellContextMenuVisible by remember { mutableStateOf(false) }

    val rangeMinRow = minOf(selectionAnchorRow, selectedRow)
    val rangeMaxRow = maxOf(selectionAnchorRow, selectedRow)
    val rangeMinCol = minOf(selectionAnchorCol, selectedCol)
    val rangeMaxCol = maxOf(selectionAnchorCol, selectedCol)

    fun navigate(key: Key, extend: Boolean) {
        val targetRow = when (key) {
            Key.DirectionUp -> selectedRow - 1
            Key.DirectionDown -> selectedRow + 1
            else -> selectedRow
        }
        val targetCol = when (key) {
            Key.DirectionLeft -> selectedCol - 1
            Key.DirectionRight -> selectedCol + 1
            else -> selectedCol
        }
        if (extend) onSelectionExtend(targetRow, targetCol) else onCellSelected(targetRow, targetCol)
    }

    Column(modifier = modifier.background(SpreadsheetTheme.colors.colBg)) {

        // ── Column headers ──────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(HEADER_HEIGHT)
                .horizontalScroll(colScrollState),
        ) {
            // Corner cell
            var isNearRightBorderCorner by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .width(COL_HEADER_WIDTH)
                    .height(HEADER_HEIGHT)
                    .background(SpreadsheetTheme.colors.colGridHeader)
                    .border(BorderStroke(0.5.dp, SpreadsheetTheme.colors.colGridBorder))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                val position = event.changes.first().position
                                isNearRightBorderCorner = position.x >= size.width - 5
                            }
                        }
                    }
                    .pointerHoverIcon(if (isNearRightBorderCorner) PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)) else PointerIcon.Default),
            )
            repeat(COLS) { col ->
                val isSelected = col in rangeMinCol..rangeMaxCol
                var isNearRightBorder by remember { mutableStateOf(false) }
                Box(
                    modifier = Modifier
                        .width(colWidths[col])
                        .height(HEADER_HEIGHT)
                        .background(if (isSelected) SpreadsheetTheme.colors.colSelected else SpreadsheetTheme.colors.colGridHeader)
                        .border(BorderStroke(0.5.dp, SpreadsheetTheme.colors.colGridBorder))
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val position = event.changes.first().position
                                    isNearRightBorder = position.x >= size.width - 5
                                }
                            }
                        }
                        .pointerInput(col) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newWidth = (colWidths[col] + dragAmount.x.toDp()).coerceAtLeast(20.dp)
                                onColResized(col, newWidth)
                            }
                        }
                        .pointerHoverIcon(if (isNearRightBorder) PointerIcon(Cursor(Cursor.E_RESIZE_CURSOR)) else PointerIcon.Default),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = columnLabel(col),
                        color = if (isSelected) SpreadsheetTheme.colors.colAccent else SpreadsheetTheme.colors.colTextMuted,
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
                        modifier = Modifier.height(rowHeights[rowIdx]),
                    ) {
                        // Row number header
                        val rowSelected = rowIdx in rangeMinRow..rangeMaxRow
                        var isNearBottomBorder by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .width(COL_HEADER_WIDTH)
                                .height(rowHeights[rowIdx])
                                .background(if (rowSelected) SpreadsheetTheme.colors.colSelected else SpreadsheetTheme.colors.colGridHeader)
                                .border(BorderStroke(0.5.dp, SpreadsheetTheme.colors.colGridBorder))
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val position = event.changes.first().position
                                            isNearBottomBorder = position.y >= size.height - 5
                                        }
                                    }
                                }
                                .pointerInput(rowIdx) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val newHeight = (rowHeights[rowIdx] + dragAmount.y.toDp()).coerceAtLeast(10.dp)
                                        onRowResized(rowIdx, newHeight)
                                    }
                                }
                                .pointerHoverIcon(if (isNearBottomBorder) PointerIcon(Cursor(Cursor.S_RESIZE_CURSOR)) else PointerIcon.Default),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = rowIdx.toString(),
                                color = if (rowSelected) SpreadsheetTheme.colors.colAccent else SpreadsheetTheme.colors.colTextMuted,
                                fontSize = 11.sp,
                                fontWeight = if (rowSelected) FontWeight.Bold else FontWeight.Normal,
                            )
                        }

                        // Data cells
                        row.forEachIndexed { colIdx, cellValue ->
                            val isActive = rowIdx == selectedRow && colIdx == selectedCol
                            val isInRange = rowIdx in rangeMinRow..rangeMaxRow && colIdx in rangeMinCol..rangeMaxCol
                            val focusRequester = remember { FocusRequester() }
                            Box(
                                modifier = Modifier
                                    .width(colWidths[colIdx])
                                    .height(rowHeights[rowIdx])
                                    .background(
                                        when {
                                            isActive -> SpreadsheetTheme.colors.colSelected
                                            isInRange -> SpreadsheetTheme.colors.colRangeFill
                                            cellValue.backgroundColor != Color.Transparent -> cellValue.backgroundColor
                                            rowIdx % 2 == 0 -> SpreadsheetTheme.colors.colBg
                                            else -> SpreadsheetTheme.colors.colGrid
                                        }
                                    )
                                    .border(BorderStroke(0.5.dp, SpreadsheetTheme.colors.colGridBorder))
                                    .run {
                                        val accent = SpreadsheetTheme.colors.colAccent
                                        if (isInRange) {
                                            this.drawWithContent {
                                                drawContent()
                                                val stroke = 2.dp.toPx()
                                                val half = stroke / 2f
                                                if (rowIdx == rangeMinRow) drawLine(
                                                    accent,
                                                    Offset(0f, half),
                                                    Offset(size.width, half),
                                                    stroke
                                                )
                                                if (rowIdx == rangeMaxRow) drawLine(
                                                    accent,
                                                    Offset(0f, size.height - half),
                                                    Offset(size.width, size.height - half),
                                                    stroke
                                                )
                                                if (colIdx == rangeMinCol) drawLine(
                                                    accent,
                                                    Offset(half, 0f),
                                                    Offset(half, size.height),
                                                    stroke
                                                )
                                                if (colIdx == rangeMaxCol) drawLine(
                                                    accent,
                                                    Offset(size.width - half, 0f),
                                                    Offset(size.width - half, size.height),
                                                    stroke
                                                )
                                            }
                                        } else this
                                    }
                                    .pointerInput(rowIdx, colIdx) {
                                        awaitPointerEventScope {
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                if (event.type == PointerEventType.Press &&
                                                    event.buttons.isSecondaryPressed
                                                ) {
                                                    val pos = event.changes.first().position
                                                    // Calculate position relative to the grid (excluding headers)
                                                    // This position is relative to the cell Box itself.
                                                    
                                                    // We need to calculate the absolute position within the scrollable content.
                                                    // Since we don't have easy access to LayoutInfo here, let's use a simpler approach.
                                                    // We'll show the menu at the click position relative to the cell,
                                                    // but the anchor Box will be the cell itself.
                                                    
                                                    onCellRightClick(rowIdx, colIdx, pos)
                                                    
                                                    contextMenuOffset = pos 
                                                    isCellContextMenuVisible = true
                                                }
                                            }
                                        }
                                    }
                                    .clickable { onCellSelected(rowIdx, colIdx) }
                                    .onKeyEvent { event ->                          // intercept keystrokes
                                        if (NAVIGATION_CHARS.contains(event.key)) {
                                            navigate(event.key, event.isShiftPressed)
                                        }
                                        if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                            navigate(Key.DirectionDown, extend = false)
                                            return@onKeyEvent true
                                        }
                                        if (isActive &&
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
                                if (isActive) {
                                    BasicTextField(
                                        value = cellValue.cell.displayValue(),
                                        onValueChange = { onCellEdited(rowIdx, colIdx, it) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 6.dp)
                                            .focusRequester(focusRequester),
                                        textStyle = getTextStyle(cellValue),
                                        singleLine = true,
                                        cursorBrush = SolidColor(SpreadsheetTheme.colors.colAccent),
                                    )
                                    LaunchedEffect(true) {
                                        focusRequester.requestFocus()
                                    }
                                } else {
                                    Text(
                                        text = cellValue.cell.displayValue(),
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = getTextStyle(cellValue)
                                    )
                                }

                                // Selection border highlight
                                if (isActive) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .border(BorderStroke(1.5.dp, SpreadsheetTheme.colors.colAccent)),
                                    )
                                }

                                if (isCellContextMenuVisible && rowIdx == selectedRow && colIdx == selectedCol) {
                                    Box(modifier = Modifier.offset {
                                        IntOffset(contextMenuOffset.x.toInt(), contextMenuOffset.y.toInt())
                                    }) {
                                        io.github.composefluent.component.MenuFlyout(
                                            visible = isCellContextMenuVisible,
                                            onDismissRequest = { isCellContextMenuVisible = false },
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .width(150.dp)
                                                    .background(SpreadsheetTheme.colors.colSurface)
                                                    .padding(8.dp)
                                            ) {
                                                Text("Cell Menu", color = SpreadsheetTheme.colors.colText)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Scrollbars ───────────────────────────────────────────────────
            val rowInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            val colInteractionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }

            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(rowScrollState),
                interactionSource = rowInteractionSource
            )
            HorizontalScrollbar(
                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth(),
                adapter = rememberScrollbarAdapter(colScrollState),
                interactionSource = colInteractionSource
            )
        }
    }
}