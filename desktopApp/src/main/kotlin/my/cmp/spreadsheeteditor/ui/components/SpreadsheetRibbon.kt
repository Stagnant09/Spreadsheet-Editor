package my.cmp.spreadsheeteditor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.filled.Add
import io.github.composefluent.icons.filled.Edit
import io.github.composefluent.icons.filled.Save
import io.github.composefluent.icons.regular.*
import my.cmp.spreadsheeteditor.models.CellContent
import my.cmp.spreadsheeteditor.models.CellContentType
import my.cmp.spreadsheeteditor.models.CellContentType.Companion.toMenuLabel
import my.cmp.spreadsheeteditor.models.CellRepresentation
import my.cmp.spreadsheeteditor.ui.theme.*

@Composable
fun SpreadsheetRibbon(
    cellReps: List<Array<CellRepresentation>>,
    selectedRow: Int,
    selectedCol: Int,
    bold: Boolean,
    italic: Boolean,
    underline: Boolean,
    strike: Boolean,
    wrapText: Boolean,
    onBoldToggle: () -> Unit,
    onItalicToggle: () -> Unit,
    onUnderlineToggle: () -> Unit,
    onStrikeToggle: () -> Unit,
    onTextAlignChange: (TextAlign) -> Unit,
    onWrapTextToggle: (Boolean) -> Unit,
    onCellTypeChange: (CellContentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentSelection = cellReps[selectedRow][selectedCol]

    Ribbon(
        modifier = modifier
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
                        SmallRibbonButton(io.github.composefluent.icons.Icons.Default.ArrowExportLtr, "Export", onClick = {})
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                4.dp,
                                Alignment.CenterHorizontally
                            ), modifier = Modifier.width(140.dp)
                        ) {
                            ToggleRibbonButton(
                                io.github.composefluent.icons.Icons.Default.TextBold,
                                "Bold",
                                bold,
                                { onBoldToggle() })
                            ToggleRibbonButton(
                                io.github.composefluent.icons.Icons.Default.TextItalic,
                                "Italic",
                                italic,
                                { onItalicToggle() })
                            ToggleRibbonButton(
                                io.github.composefluent.icons.Icons.Default.TextUnderline,
                                "Underline",
                                underline,
                                { onUnderlineToggle() })
                            ToggleRibbonButton(
                                io.github.composefluent.icons.Icons.Default.TextStrikethrough,
                                "Strike",
                                strike,
                                { onStrikeToggle() }
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                4.dp,
                                Alignment.CenterHorizontally
                            ), modifier = Modifier.width(140.dp)
                        ) {
                            Box(

                            ){
                                var colorPickerVisible by remember { mutableStateOf(false) }
                                var isExpanded by remember { mutableStateOf(false) }

                                SmallRibbonButton(
                                    io.github.composefluent.icons.Icons.Default.TextField,
                                    "Text",
                                    modifier = Modifier.width(44.dp),
                                    onClick = {
                                        colorPickerVisible = true
                                    })

                                MenuFlyout(
                                    visible = colorPickerVisible,
                                    onDismissRequest = {
                                        colorPickerVisible = false
                                        isExpanded = false
                                    }
                                ) {
                                    Box(
                                        modifier = Modifier.height(if (isExpanded) 200.dp else 100.dp).width(200.dp)
                                    ) {
                                        ColorPicker(
                                            isExpanded = isExpanded,
                                            expandTrigger = { isExpanded = !isExpanded },
                                            suggestedColors = listOf(Color.Black, Color.White, Color.Red, Color.Green, Color.Blue),
                                            onColorSelected = { colorPickerVisible = false },
                                        )
                                    }
                                }
                            }
                            SmallRibbonButton(
                                io.github.composefluent.icons.Icons.Default.ColorFill,
                                "Fill",
                                modifier = Modifier.width(44.dp),
                                onClick = {})
                            SmallRibbonButton(
                                io.github.composefluent.icons.Icons.Default.BorderAll,
                                "Border",
                                modifier = Modifier.width(44.dp),
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
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                4.dp,
                                Alignment.CenterHorizontally
                            )
                        ) {
                            SmallRibbonButton(
                                io.github.composefluent.icons.Icons.Default.AlignLeft,
                                "Left",
                                onClick = { onTextAlignChange(TextAlign.Left) })
                            SmallRibbonButton(
                                io.github.composefluent.icons.Icons.Default.AlignCenterHorizontal,
                                "Center",
                                onClick = { onTextAlignChange(TextAlign.Center) })
                            SmallRibbonButton(
                                io.github.composefluent.icons.Icons.Default.AlignRight,
                                "Right",
                                onClick = { onTextAlignChange(TextAlign.Right) })
                        }
                        Spacer(Modifier.height(4.dp))
                        ToggleRibbonButton(
                            io.github.composefluent.icons.Icons.Default.TextWrap,
                            "Wrap",
                            wrapText,
                            { onWrapTextToggle(it) },
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
                        modifier = Modifier.clip(shape = RoundedCornerShape(4.dp))
                            .background(Color(210, 210, 210))
                    ) {
                        SubtleButton(
                            onClick = { flyoutVisible = !flyoutVisible },
                            modifier = Modifier.commandBarButtonSize(),
                            content = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.width(110.dp)
                                        .clip(shape = FluentTheme.shapes.overlay)
                                ) {
                                    Text(
                                        currentSelection.cell.content.type.toMenuLabel(),
                                        color = Color.Black
                                    )
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
                                        onCellTypeChange(option)
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
            )
        )
    )
}
