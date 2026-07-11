package my.cmp.spreadsheeteditor.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import io.github.composefluent.FluentTheme
import io.github.composefluent.component.*
import io.github.composefluent.icons.filled.Add
import io.github.composefluent.icons.filled.Edit
import io.github.composefluent.icons.filled.Save
import io.github.composefluent.icons.regular.*
import my.cmp.spreadsheeteditor.models.CellContentType
import my.cmp.spreadsheeteditor.models.CellContentType.Companion.toMenuLabel
import my.cmp.spreadsheeteditor.models.CellRepresentation
import my.cmp.spreadsheeteditor.ui.theme.SpreadsheetTheme
import my.cmp.spreadsheeteditor.ui.utils.getSystemFonts

@OptIn(ExperimentalComposeUiApi::class)
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
    fontFamily: FontFamily,
    onFontFamilyChange: (FontFamily) -> Unit,
    fontSize: Float,
    onFontSizeChange: (Float) -> Unit,
    onTextAlignChange: (TextAlign) -> Unit,
    onWrapTextToggle: (Boolean) -> Unit,
    onCellTypeChange: (CellContentType) -> Unit,
    onColorSelected: (Color) -> Unit,
    onBackgroundColorSelected: (Color) -> Unit,
    onSave: () -> Unit,
    onSaveAs: () -> Unit,
    onLoad: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onCopy: () -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit,
    onFunctionClick: () -> Unit,
    fontColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    val currentSelection = cellReps[selectedRow][selectedCol]
    val ribbonFontColor = SpreadsheetTheme.colors.colTextMuted

    Ribbon(
        modifier = modifier
            .background(SpreadsheetTheme.colors.colRibbon)
            .height(120.dp)
            .fillMaxWidth()
            .border(BorderStroke(1.dp, SpreadsheetTheme.colors.colDivider)),
        contentUnits = listOf(
            // ── File ──────────────────────────────────────────
            listOf(
                {
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            ) {
                                RibbonEntry(
                                    icon = {
                                        Icon(
                                            imageVector = io.github.composefluent.icons.Icons.Filled.Add,
                                            "New",
                                            tint = SpreadsheetTheme.colors.colText,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    label = "New",
                                    onClick = {},
                                    textColor = ribbonFontColor
                                )
                            }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            ) {
                                RibbonEntry(
                                    icon = {
                                        Icon(
                                            io.github.composefluent.icons.Icons.Filled.Edit,
                                            "Open",
                                            tint = SpreadsheetTheme.colors.colText,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    label = "Open",
                                    onClick = onLoad,
                                    textColor = ribbonFontColor
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                RibbonEntry(
                                    icon = {
                                        Icon(
                                            io.github.composefluent.icons.Icons.Filled.Save,
                                            "Save",
                                            tint = SpreadsheetTheme.colors.colText,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    },
                                    label = "Save",
                                    onClick = onSave,
                                    textColor = ribbonFontColor
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 2.dp),
                            ) {
                                SmallRibbonButton(Icons.Default.Share, "Save As", onClick = onSaveAs)
                                SmallRibbonButton(
                                    io.github.composefluent.icons.Icons.Default.ArrowExportLtr,
                                    "Export",
                                    onClick = {})
                            }
                        }
                        RibbonSectionLabel("FILE")
                    }
                },
            ),

            // ── Edit ──────────────────────────────────────────
            listOf(
                {
                    Column(
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                verticalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 2.dp),
                            ) {
                                SmallRibbonButton(
                                    io.github.composefluent.icons.Icons.Default.Copy,
                                    "Copy",
                                    onClick = onCopy
                                )
                                SmallRibbonButton(
                                    io.github.composefluent.icons.Icons.Default.ClipboardPaste,
                                    "Paste",
                                    onClick = onPaste
                                )
                            }
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
                                    onClick = onClear
                                )
                            }
                            Column(
                                verticalArrangement = Arrangement.SpaceEvenly,
                                modifier = Modifier.fillMaxHeight().padding(horizontal = 2.dp),
                            ) {
                                SmallRibbonButton(
                                    io.github.composefluent.icons.Icons.Default.ArrowUndo,
                                    "Undo",
                                    onClick = onUndo
                                )
                                SmallRibbonButton(
                                    io.github.composefluent.icons.Icons.Default.ArrowRedo,
                                    "Redo",
                                    onClick = onRedo
                                )
                            }
                        }
                        RibbonSectionLabel("EDIT")
                    }
                },
            ),

            // ── Format ────────────────────────────────────────
            listOf(
                {
                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(

                            ) {
                                Row(
                                    modifier = Modifier.padding(bottom = 4.dp)
                                ) {
                                    ToggleRibbonButton(
                                        icon = io.github.composefluent.icons.Icons.Default.FontIncrease,
                                        toggled = false,
                                        onToggle = {
                                            onFontSizeChange(fontSize + 1f)
                                        },
                                        label = "Increase Font Size"
                                    )
                                    ToggleRibbonButton(
                                        icon = io.github.composefluent.icons.Icons.Default.FontDecrease,
                                        toggled = false,
                                        onToggle = {
                                            if (fontSize > 1f) onFontSizeChange(fontSize - 1f)
                                        },
                                        label = "Decrease Font Size"
                                    )
                                    ToggleRibbonButton(
                                        icon = io.github.composefluent.icons.Icons.Default.TextCaseUppercase,
                                        toggled = false,
                                        onToggle = {

                                        },
                                        label = "Uppercase"
                                    )
                                    ToggleRibbonButton(
                                        icon = io.github.composefluent.icons.Icons.Default.TextSuperscript,
                                        toggled = false,
                                        onToggle = {

                                        },
                                        label = "Superscript"
                                    )
                                    ToggleRibbonButton(
                                        icon = io.github.composefluent.icons.Icons.Default.TextSubscript,
                                        toggled = false,
                                        onToggle = {

                                        },
                                        label = "Subscript"
                                    )
                                }
                                Row(modifier = Modifier.width(150.dp), horizontalArrangement = Arrangement.Center) {
                                    Box(
                                        modifier = Modifier.width(width = 140.dp).clip(shape = RoundedCornerShape(4.dp))
                                            .background(SpreadsheetTheme.colors.colRibbonHover),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        var fontFlyoutMenuVisible by remember { mutableStateOf(false) }

                                        SubtleButton(
                                            onClick = {
                                                fontFlyoutMenuVisible = !fontFlyoutMenuVisible
                                            },
                                            modifier = Modifier.commandBarButtonSize(),
                                            content = {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    modifier = Modifier.width(110.dp)
                                                        .clip(shape = FluentTheme.shapes.overlay)
                                                ) {
                                                    val defaultFontName = if (System.getProperty("os.name").lowercase().contains("win")) "Segoe UI" else "Noto Sans"
                                                    Text(
                                                        text = getSystemFonts().find { fontPair -> fontPair.second.toString() == fontFamily.toString() }?.first ?: defaultFontName,
                                                        fontFamily = fontFamily,
                                                        color = SpreadsheetTheme.colors.colText
                                                    )
                                                    Text("▾", color = SpreadsheetTheme.colors.colText)
                                                }
                                            }
                                        )

                                        MenuFlyout(
                                            visible = fontFlyoutMenuVisible,
                                            onDismissRequest = { fontFlyoutMenuVisible = false }
                                        ) {
                                            var fontList by remember { mutableStateOf(getSystemFonts()) }
                                            val listState = rememberLazyListState()
                                            Box(
                                                modifier = Modifier
                                                    .width(210.dp)
                                                    .height(400.dp)
                                                    .background(SpreadsheetTheme.colors.colSurface)
                                            ) {
                                                Row {
                                                    LazyColumn(
                                                        state = listState,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        items(fontList) { fontFamily ->
                                                            var isHovered by remember { mutableStateOf(false) }
                                                            Row(
                                                                modifier = Modifier
                                                                    .height(36.dp)
                                                                    .fillMaxWidth()
                                                                    .onPointerEvent(PointerEventType.Enter) {
                                                                        isHovered = true
                                                                    }
                                                                    .onPointerEvent(PointerEventType.Exit) {
                                                                        isHovered = false
                                                                    }
                                                                    .background(
                                                                        if (isHovered) SpreadsheetTheme.colors.colRibbonHover
                                                                        else SpreadsheetTheme.colors.colSurface
                                                                    )
                                                                    .clickable {
                                                                        fontFlyoutMenuVisible = false
                                                                        onFontFamilyChange(fontFamily.second)
                                                                    }
                                                                    .padding(horizontal = 12.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                            ) {
                                                                Text(
                                                                    text = fontFamily.first,
                                                                    fontFamily = fontFamily.second,
                                                                    color = SpreadsheetTheme.colors.colText
                                                                )
                                                            }
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxHeight()
                                                            .pointerInput(Unit) {
                                                                awaitPointerEventScope {
                                                                    while (true) {
                                                                        awaitPointerEvent(PointerEventPass.Initial)
                                                                        // just intercepting here is enough to claim priority;
                                                                        // don't call change.consume() or the scrollbar itself won't get the event
                                                                    }
                                                                }
                                                            }
                                                    ) {
                                                        VerticalScrollbar(
                                                            modifier = Modifier.fillMaxHeight().zIndex(99f),
                                                            adapter = rememberScrollbarAdapter(scrollState = listState),
                                                            style = androidx.compose.foundation.defaultScrollbarStyle().copy(
                                                                unhoverColor = SpreadsheetTheme.colors.colAccent.copy(alpha = 0.5f),
                                                                hoverColor = SpreadsheetTheme.colors.colAccent
                                                            ),
                                                            interactionSource = remember { MutableInteractionSource() }
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
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

                                    ) {
                                        var colorPickerVisible by remember { mutableStateOf(false) }
                                        var isExpanded by remember { mutableStateOf(false) }

                                        SmallRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.TextField,
                                            "Text",
                                            tint = ribbonFontColor,
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
                                                modifier = Modifier.height(if (isExpanded) 300.dp else 100.dp)
                                                    .width(200.dp)
                                            ) {
                                                ColorPicker(
                                                    isExpanded = isExpanded,
                                                    expandTrigger = { isExpanded = !isExpanded },
                                                    suggestedColors = listOf(
                                                        Color.Black,
                                                        Color.White,
                                                        Color.Red,
                                                        Color.Green,
                                                        Color.Blue,
                                                        Color.Yellow,
                                                        Color.Magenta,
                                                        Color.Cyan
                                                    ),
                                                    onColorSelected = {
                                                        onColorSelected(it)
                                                        colorPickerVisible = false
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    Box(

                                    ) {
                                        var colorPickerVisible by remember { mutableStateOf(false) }
                                        var isExpanded by remember { mutableStateOf(false) }

                                        SmallRibbonButton(
                                            io.github.composefluent.icons.Icons.Default.ColorFill,
                                            "Fill",
                                            tint = if (backgroundColor == Color.Transparent) SpreadsheetTheme.colors.colText else backgroundColor,
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
                                                modifier = Modifier.height(if (isExpanded) 300.dp else 100.dp)
                                                    .width(200.dp)
                                            ) {
                                                ColorPicker(
                                                    isExpanded = isExpanded,
                                                    expandTrigger = { isExpanded = !isExpanded },
                                                    suggestedColors = listOf(
                                                        Color.Black,
                                                        Color.White,
                                                        Color.Red,
                                                        Color.Green,
                                                        Color.Blue,
                                                        Color.Yellow,
                                                        Color.Magenta,
                                                        Color.Cyan
                                                    ),
                                                    onColorSelected = {
                                                        onBackgroundColorSelected(it)
                                                        colorPickerVisible = false
                                                    },
                                                )
                                            }
                                        }
                                    }
                                    SmallRibbonButton(
                                        io.github.composefluent.icons.Icons.Default.BorderAll,
                                        "Border",
                                        modifier = Modifier.width(44.dp),
                                        onClick = {})
                                }
                            }
                        }
                        RibbonSectionLabel("STYLE")
                    }
                },
                {
                    Column(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp),
                        verticalArrangement = Arrangement.Bottom,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
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
                        }
                        RibbonSectionLabel("FORMAT")
                    }
                },
            ),

            // ── Insert ────────────────────────────────────────
            listOf(
                {
                    var flyoutVisible by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier.clip(shape = RoundedCornerShape(4.dp))
                            .background(SpreadsheetTheme.colors.colRibbonHover)
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
                                        color = SpreadsheetTheme.colors.colText
                                    )
                                    Text("▾", color = SpreadsheetTheme.colors.colText)
                                }
                            }
                        )

                        MenuFlyout(
                            visible = flyoutVisible,
                            onDismissRequest = { flyoutVisible = false },
                            modifier = Modifier.background(
                                color = SpreadsheetTheme.colors.colSurface,
                                shape = FluentTheme.shapes.overlay
                            )
                        ) {
                            CellContentType.entries.dropLast(1).forEach { option ->
                                MenuFlyoutItem(
                                    onClick = {
                                        flyoutVisible = false
                                        onCellTypeChange(option)
                                    },
                                    text = { Text(option.toMenuLabel(), color = SpreadsheetTheme.colors.colText) },
                                    colors = ListItemDefaults.defaultListItemColors().copy(
                                        hovered = ListItemDefaults.defaultListItemColors().hovered.copy(
                                            fillColor = SpreadsheetTheme.colors.colAccentSoft.copy(alpha = 0.2f)
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            ),

            listOf(
                {
                    RibbonEntry(
                        icon = {
                            Icon(
                                io.github.composefluent.icons.Icons.Default.MathFormula,
                                "Function",
                                tint = SpreadsheetTheme.colors.colText,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = "Function",
                        onClick = { onFunctionClick() },
                        textColor = ribbonFontColor
                    )
                },
                {
                    RibbonEntry(
                        icon = {
                            Icon(
                                io.github.composefluent.icons.Icons.Default.ArrowSort,
                                "Sort",
                                tint = SpreadsheetTheme.colors.colText,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = "Sort",
                        onClick = {},
                        textColor = ribbonFontColor
                    )
                },
                {
                    RibbonEntry(
                        icon = {
                            Icon(
                                io.github.composefluent.icons.Icons.Default.Filter,
                                "Filter",
                                tint = SpreadsheetTheme.colors.colText,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        label = "Filter",
                        onClick = {},
                        textColor = ribbonFontColor
                    )
                }
            )
        )
    )
}
