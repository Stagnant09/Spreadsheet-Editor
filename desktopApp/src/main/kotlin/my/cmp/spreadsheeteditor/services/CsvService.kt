package my.cmp.spreadsheeteditor.services

import my.cmp.spreadsheeteditor.models.Cell.Companion.displayValue
import my.cmp.spreadsheeteditor.models.CellContent
import my.cmp.spreadsheeteditor.models.CellRepresentation
import java.io.File

object CsvService {
    fun readCsv(file: File): List<List<String>> {
        return file.readLines().map { line ->
            // Simple split by comma, but handle quoted values
            val result = mutableListOf<String>()
            var current = StringBuilder()
            var inQuotes = false
            var i = 0
            while (i < line.length) {
                val c = line[i]
                if (c == '\"') {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '\"') {
                        current.append('\"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                } else if (c == ',' && !inQuotes) {
                    result.add(current.toString())
                    current = StringBuilder()
                } else {
                    current.append(c)
                }
                i++
            }
            result.add(current.toString())
            result
        }
    }

    fun writeCsv(file: File, data: List<List<String>>) {
        file.writeText(data.joinToString("\n", transform = { row ->
            row.joinToString(",") { value ->
                if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
                    "\"" + value.replace("\"", "\"\"") + "\""
                } else {
                    value
                }
            }
        }))
    }

    fun saveToCsv(file: File, cellReps: List<Array<CellRepresentation>>, rows: Int, cols: Int) {
        val data = (0 until rows).map { r ->
            (0 until cols).map { c ->
                val cellRep = cellReps[r][c]
                val content = cellRep.cell.content
                when (content) {
                    is CellContent.FormulaContent -> "=${content.value}"
                    else -> cellRep.cell.displayValue()
                }
            }
        }
        writeCsv(file, data)
    }

    fun loadFromCsv(file: File): List<List<String>>? {
        if (!file.exists()) return null
        return readCsv(file)
    }
}