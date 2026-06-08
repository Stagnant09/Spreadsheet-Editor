package my.cmp.spreadsheeteditor

object NativeBridge {
    init {
        val osName = System.getProperty("os.name").lowercase()
        val isWindows = osName.contains("windows")
        val libName = if (isWindows) "libspreadsheet_native" else "spreadsheet_native"
        
        try {
            // Try loading from java.library.path first (set in build.gradle.kts)
            System.loadLibrary(libName)
        } catch (e: UnsatisfiedLinkError) {
            // Fallback to absolute path if java.library.path fails
            println("catch entered")
            val projectRoot = System.getProperty("user.dir")
                .removeSuffix("\\desktopApp")
                .removeSuffix("/desktopApp")
            val extension = if (isWindows) ".dll" else ".so"
            val prefix = if (isWindows) "" else "lib"
            val libFileName = "${prefix}${libName}${extension}"
            val libPath = "$projectRoot/build/native/$libFileName"
            println("Failed to load from library path, trying absolute path: $libPath")
            System.load(libPath)
        }
    }

    external fun init(rows: Int, cols: Int)
    external fun processCommand(command: String)
    external fun getCellValue(row: Int, col: Int): String
    external fun getRows(): Int
    external fun getCols(): Int
}