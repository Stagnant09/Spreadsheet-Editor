package my.cmp.spreadsheeteditor.utils

import androidx.compose.ui.input.key.Key

// The chars below must not be converted to their string representation,
// and, instead, be ignored
val IGNORE_CHARS = listOf(
    Key.ShiftLeft,
    Key.ShiftRight,
    Key.CapsLock,
    Key.CtrlLeft,
    Key.CtrlRight,
    Key.AltLeft,
    Key.AltRight,
    Key.Window,
    Key.Escape,
    Key.Enter,
    Key.Tab
)