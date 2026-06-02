package my.cmp.spreadsheeteditor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.cmp.spreadsheeteditor.ui.theme.ColAccent
import my.cmp.spreadsheeteditor.ui.theme.ColDivider
import my.cmp.spreadsheeteditor.ui.theme.ColFormulaBar
import my.cmp.spreadsheeteditor.ui.theme.ColGridBorder
import my.cmp.spreadsheeteditor.ui.theme.ColSurface
import my.cmp.spreadsheeteditor.ui.theme.ColText

@Composable
fun FormulaBar(
    cellAddress: String,
    formula: String,
    onFormulaChange: (String) -> Unit,
    onCommit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(ColFormulaBar)
            .border(BorderStroke(1.dp, ColDivider))
            .height(32.dp)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Cell address box
        Box(
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
                .background(ColSurface)
                .border(BorderStroke(1.dp, ColGridBorder)),
            contentAlignment = Alignment.Center,
        ) {
            Text(cellAddress, color = ColText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.width(4.dp))

        // fx label
        Text(
            "fx",
            color = ColAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Divider(
            modifier = Modifier.fillMaxHeight().width(1.dp),
            color = ColDivider,
        )

        Spacer(Modifier.width(6.dp))

        // Formula input
        BasicTextField(
            value = formula,
            onValueChange = onFormulaChange,
            modifier = Modifier.weight(1f).onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                    onCommit()
                    true
                } else false
            },
            textStyle = TextStyle(color = ColText, fontSize = 13.sp),
            singleLine = true,
            cursorBrush = SolidColor(ColAccent),
        )
    }
}