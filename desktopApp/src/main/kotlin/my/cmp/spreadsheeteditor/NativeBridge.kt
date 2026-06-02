package my.cmp.spreadsheeteditor

object NativeBridge {
    init {
        val projectRoot = System.getProperty("user.dir")
            .removeSuffix("\\desktopApp")
            .removeSuffix("/desktopApp")
        val dllPath = "$projectRoot/build/native/libspreadsheet_native.dll"
        println("Loading native library from: $dllPath")
        System.load(dllPath)
    }

    external fun init(rows: Int, cols: Int)
    external fun processCommand(command: String)
    external fun getCellValue(row: Int, col: Int): String
    external fun getRows(): Int
    external fun getCols(): Int
}