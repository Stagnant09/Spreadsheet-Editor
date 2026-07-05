package my.cmp.spreadsheeteditor.utils

/**
 * Converts a 0-indexed column number into its spreadsheet letter label,
 * using the same bijective base-26 scheme as the native C engine's
 * `col_letters_to_index` (A=0 ... Z=25, AA=26, AB=27, ... AZ=51, BA=52, ...).
 *
 * This lets the grid, formula bar, and CSV round-trip support sheets wider
 * than 26 columns, matching what the parser already accepts.
 */
fun columnLabel(index: Int): String {
    var n = index + 1 // shift into 1-indexed bijective base-26
    val sb = StringBuilder()
    while (n > 0) {
        val rem = (n - 1) % 26
        sb.append('A' + rem)
        n = (n - 1) / 26
    }
    return sb.reverse().toString()
}

/**
 * Inverse of [columnLabel]: converts a column letter label (e.g. "A", "Z",
 * "AA") back into its 0-indexed column number. Returns -1 if the string
 * contains anything other than letters.
 */
fun columnIndex(label: String): Int {
    if (label.isEmpty()) return -1
    var idx = 0L
    for (c in label) {
        if (!c.isLetter()) return -1
        idx = idx * 26 + (c.uppercaseChar() - 'A' + 1)
    }
    return (idx - 1).toInt()
}
