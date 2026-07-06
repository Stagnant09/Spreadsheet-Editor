package my.cmp.spreadsheeteditor.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import my.cmp.spreadsheeteditor.models.CellContent
import my.cmp.spreadsheeteditor.models.CellRepresentation
import androidx.compose.runtime.Composable
import my.cmp.spreadsheeteditor.ui.theme.SpreadsheetTheme

@Composable
fun getTextStyle(cellValue: CellRepresentation) = TextStyle(
    color = if (cellValue.cell.content is CellContent.ErrorContent) {
        SpreadsheetTheme.colors.colError
    } else {
        if (cellValue.fontColor == Color.Unspecified) SpreadsheetTheme.colors.colText else cellValue.fontColor
    },
    fontSize = 12.sp,
    fontWeight = if (cellValue.bold) FontWeight.Bold else FontWeight.Normal,
    fontStyle = if (cellValue.italic) FontStyle.Italic else FontStyle.Normal,
    textDecoration = (if (cellValue.underline) TextDecoration.Underline else TextDecoration.None) +
            (if (cellValue.strike) TextDecoration.LineThrough else TextDecoration.None),
    textAlign = cellValue.textAlign,
)