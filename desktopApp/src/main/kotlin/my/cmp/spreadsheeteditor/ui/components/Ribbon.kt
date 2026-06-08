package my.cmp.spreadsheeteditor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import my.cmp.spreadsheeteditor.ui.theme.ColAccent
import my.cmp.spreadsheeteditor.ui.theme.ColText
import my.cmp.spreadsheeteditor.ui.theme.ColTextMuted

/** A ribbon entry is a basic clickable column that contains an icon and a label,
 * centered horizontally and vertically. It resembles the ordinary available large
 * buttons in the ribbon of Microsoft Excel.
 */
@Composable
fun RibbonEntry(
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit,
    spaceBetweenIconAndLabel: Int = 4,
    label: String,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = { onClick() }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ){
        icon()
        Spacer(modifier = Modifier.width(spaceBetweenIconAndLabel.dp))
        Text(text = label, color = textColor)
    }
}

/** A small ribbon button is a basic clickable column that contains an icon and a label,
 * centered horizontally and vertically. It resembles the ordinary available small
 * buttons in the ribbon of Microsoft Excel.
 */
@Composable
fun SmallRibbonButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ColText,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        io.github.composefluent.component.Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.height(2.dp))
        Text(label, color = ColTextMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ToggleRibbonButton(
    icon: ImageVector,
    label: String,
    toggled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (toggled) ColAccent.copy(alpha = 0.25f) else Color.Transparent
    val tint = if (toggled) ColAccent else ColText
    Box(
        modifier = modifier
            .size(30.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(bg)
            .clickable { onToggle(!toggled) },
        contentAlignment = Alignment.Center,
    ) {
        io.github.composefluent.component.Icon(
            icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun RibbonSectionLabel(text: String) {
    Text(
        text = text,
        color = ColTextMuted,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .padding(top = 2.dp),
        textAlign = TextAlign.Center,
        letterSpacing = 0.8.sp,
    )
}

@Composable
fun Ribbon(
    modifier: Modifier = Modifier,
    isMaximized: Boolean = true,
    onMaxMinToggle: (Boolean) -> Unit = {},
    contentUnits: List<List<@Composable ()->Unit>> = emptyList()
) {
    Row(modifier = modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        contentUnits.forEach { unit ->
            unit.forEach { entry ->
                entry()
                Spacer(modifier = Modifier.width(8.dp))
            }
            VerticalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Preview
@Composable
fun RibbonPreview() {
    Ribbon(
        modifier = Modifier.background(Color.White).height(78.dp),
        contentUnits = listOf(
            listOf(
                {
                    RibbonEntry(
                        icon = {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", modifier = Modifier.size(36.dp))
                        },
                        label = "Settings",
                        onClick = {}
                    )
                },
                {
                    RibbonEntry(
                        icon = {
                            Icon(Icons.Default.Settings, contentDescription = "Settings2", modifier = Modifier.size(36.dp))
                        },
                        label = "Settings2",
                        onClick = {}
                    )
                }
            ),
            listOf(
                {
                    RibbonEntry(
                        icon = {
                            Icon(Icons.Default.Create, contentDescription = "Create", modifier = Modifier.size(36.dp))
                        },
                        label = "Create",
                        onClick = {}
                    )
                }
            )
        )
    )
}

