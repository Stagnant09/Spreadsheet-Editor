package my.cmp.spreadsheeteditor.services

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import my.cmp.spreadsheeteditor.models.Cell
import my.cmp.spreadsheeteditor.models.CellContent
import my.cmp.spreadsheeteditor.models.CellRepresentation
import my.cmp.spreadsheeteditor.ui.utils.getSystemFonts
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFCellStyle
import org.apache.poi.xssf.usermodel.XSSFColor
import org.apache.poi.xssf.usermodel.XSSFFont
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * XLSX read/write via Apache POI, mapped directly onto CellContent/CellRepresentation.
 *
 * Scope, deliberately: single sheet (sheetIndex, default 0) — matches the app's current
 * single-grid model; no merged-cell support yet, since CellRepresentation has no merge
 * concept to read into (that's roadmap item #1 — cell merging — this should be revisited
 * once that lands). Formulas round-trip as text (POI's cellFormula), re-evaluated on write
 * so the file opens with correct cached values in Excel/LibreOffice without a recalc step.
 */
object XlsxService {

    fun readXlsx(filePath: String, sheetIndex: Int = 0): List<CellRepresentation> {
        val cellReps = mutableListOf<CellRepresentation>()
        FileInputStream(filePath).use { input ->
            val workbook = WorkbookFactory.create(input)
            val sheet = workbook.getSheetAt(sheetIndex)
            val evaluator = workbook.creationHelper.createFormulaEvaluator()

            for (row in sheet) {
                for (poiCell in row) {
                    val content = readCellContent(poiCell, evaluator)
                    if (content is CellContent.Empty) continue // don't materialize blank cells

                    cellReps.add(
                        CellRepresentation(
                            cell = Cell(row = poiCell.rowIndex, column = poiCell.columnIndex, content = content),
                            bold = poiCell.font()?.bold ?: false,
                            italic = poiCell.font()?.italic ?: false,
                            underline = (poiCell.font()?.underline ?: false) != false,
                            strike = poiCell.font()?.strikeout ?: false,
                            fontSize = poiCell.font()?.fontHeightInPoints?.toFloat() ?: 12f,
                            fontFamily = poiCell.font()?.fontName?.let(::fontFamilyFromName) ?: FontFamily.Default,
                            fontColor = poiCell.font()?.let(::colorFromXssfFont) ?: Color.Unspecified,
                            backgroundColor = colorFromCellFill(poiCell.cellStyle as? XSSFCellStyle)
                                ?: Color.Transparent,
                            textAlign = alignmentFrom(poiCell.cellStyle?.alignment),
                            wrapText = poiCell.cellStyle?.wrapText ?: false,
                            isSelected = false
                        )
                    )
                }
            }
            workbook.close()
        }
        return cellReps
    }

    fun writeXlsx(filePath: String, cellReps: List<CellRepresentation>) {
        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet()
        val styleCache = mutableMapOf<StyleKey, XSSFCellStyle>()

        for (rep in cellReps) {
            val content = rep.cell.content
            if (content is CellContent.Empty) continue

            val row = sheet.getRow(rep.cell.row) ?: sheet.createRow(rep.cell.row)
            val poiCell = row.getCell(rep.cell.column) ?: row.createCell(rep.cell.column)

            when (content) {
                is CellContent.NumberContent -> poiCell.setCellValue(content.value)
                is CellContent.TextContent -> poiCell.setCellValue(content.value)
                is CellContent.FormulaContent -> {
                    // POI wants the formula without a leading '=', matching how it's
                    // already stored internally (App.kt strips it the same way on commit).
                    poiCell.cellFormula = content.value.removePrefix("=")
                }
                // No native Excel concept maps cleanly onto your internal error strings
                // (#CIRCULAR! etc. aren't Excel error codes) — write them as visible text
                // rather than silently dropping the cell.
                is CellContent.ErrorContent -> poiCell.setCellValue(content.value)
                CellContent.Empty -> {}
                else -> {}
            }

            poiCell.cellStyle = styleCache.getOrPut(StyleKey.from(rep)) { buildStyle(workbook, rep) }
        }

        // Recalculate every formula so the cached values POI writes into the file are
        // correct even before the person who opens it forces a recalc.
        workbook.creationHelper.createFormulaEvaluator().evaluateAll()

        FileOutputStream(filePath).use { workbook.write(it) }
        workbook.close()
    }

    // ============================================================
    // Reading
    // ============================================================

    private fun readCellContent(
        poiCell: org.apache.poi.ss.usermodel.Cell,
        evaluator: org.apache.poi.ss.usermodel.FormulaEvaluator
    ): CellContent = when (poiCell.cellType) {
        CellType.BLANK -> CellContent.Empty
        CellType.STRING -> CellContent.TextContent(poiCell.stringCellValue)
        CellType.NUMERIC -> CellContent.NumberContent(poiCell.numericCellValue)
        // No Boolean variant in CellContent — surfaced as text so the value is at least
        // visible and round-trips as a string; flag if you want a dedicated variant instead.
        CellType.BOOLEAN -> CellContent.TextContent(poiCell.booleanCellValue.toString().uppercase())
        CellType.ERROR -> CellContent.ErrorContent("#${org.apache.poi.ss.usermodel.FormulaError.forInt(poiCell.errorCellValue).string}")
        CellType.FORMULA -> {
            val cached = try {
                when (evaluator.evaluateFormulaCell(poiCell)) {
                    CellType.NUMERIC -> poiCell.numericCellValue
                    else -> null
                }
            } catch (e: Exception) {
                null // unsupported/unevaluable formula — keep the formula text, just no cached result
            }
            CellContent.FormulaContent(poiCell.cellFormula, cachedResult = cached)
        }

        else -> CellContent.Empty
    }

    private fun org.apache.poi.ss.usermodel.Cell.font(): XSSFFont? {
        val style = cellStyle as? XSSFCellStyle ?: return null
        return sheet.workbook.let { (it as? XSSFWorkbook)?.getFontAt(style.fontIndex) }
    }

    private fun colorFromXssfFont(font: XSSFFont): Color {
        val rgb = font.xssfColor?.rgb ?: return Color.Unspecified
        return Color(red = rgb[0].toUByte().toInt(), green = rgb[1].toUByte().toInt(), blue = rgb[2].toUByte().toInt())
    }

    private fun colorFromCellFill(style: XSSFCellStyle?): Color? {
        if (style == null || style.fillPattern != FillPatternType.SOLID_FOREGROUND) return null
        val rgb = (style.fillForegroundColorColor as? XSSFColor)?.rgb ?: return null
        return Color(red = rgb[0].toUByte().toInt(), green = rgb[1].toUByte().toInt(), blue = rgb[2].toUByte().toInt())
    }

    private fun alignmentFrom(alignment: HorizontalAlignment?): TextAlign = when (alignment) {
        HorizontalAlignment.CENTER -> TextAlign.Center
        HorizontalAlignment.RIGHT -> TextAlign.Right
        else -> TextAlign.Left
    }

    private fun fontFamilyFromName(name: String): FontFamily =
        getSystemFonts().firstOrNull { it.first == name }?.second ?: FontFamily.Default

    // ============================================================
    // Writing
    // ============================================================

    private data class StyleKey(
        val bold: Boolean,
        val italic: Boolean,
        val underline: Boolean,
        val strike: Boolean,
        val fontSize: Int,
        val fontColorArgb: Int,
        val backgroundColorArgb: Int,
        val align: TextAlign,
        val wrapText: Boolean
    ) {
        companion object {
            fun from(rep: CellRepresentation) = StyleKey(
                bold = rep.bold,
                italic = rep.italic,
                underline = rep.underline,
                strike = rep.strike,
                fontSize = rep.fontSize.toInt(),
                fontColorArgb = if (rep.fontColor.isSpecified) rep.fontColor.toArgb() else 0,
                backgroundColorArgb = if (rep.backgroundColor.isSpecified) rep.backgroundColor.toArgb() else 0,
                align = rep.textAlign,
                wrapText = rep.wrapText
            )
        }
    }

    private val Color.isSpecified: Boolean get() = this != Color.Unspecified

    private fun buildStyle(workbook: XSSFWorkbook, rep: CellRepresentation): XSSFCellStyle {
        val style = workbook.createCellStyle()
        val font = workbook.createFont()

        font.bold = rep.bold
        font.italic = rep.italic
        font.strikeout = rep.strike
        if (rep.underline) font.setUnderline(XSSFFont.U_SINGLE)
        font.fontHeightInPoints = rep.fontSize.toInt().toShort()
        if (rep.fontColor.isSpecified) {
            font.setColor(
                XSSFColor(
                    byteArrayOf(
                        (rep.fontColor.red * 255).toInt().toByte(),
                        (rep.fontColor.green * 255).toInt().toByte(),
                        (rep.fontColor.blue * 255).toInt().toByte()
                    ), null
                )
            )
        }
        style.setFont(font)

        if (rep.backgroundColor.isSpecified) {
            style.setFillForegroundColor(
                XSSFColor(
                    byteArrayOf(
                        (rep.backgroundColor.red * 255).toInt().toByte(),
                        (rep.backgroundColor.green * 255).toInt().toByte(),
                        (rep.backgroundColor.blue * 255).toInt().toByte()
                    ), null
                )
            )
            style.fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        style.alignment = when (rep.textAlign) {
            TextAlign.Center -> HorizontalAlignment.CENTER
            TextAlign.Right -> HorizontalAlignment.RIGHT
            else -> HorizontalAlignment.LEFT
        }
        style.wrapText = rep.wrapText

        return style
    }
}