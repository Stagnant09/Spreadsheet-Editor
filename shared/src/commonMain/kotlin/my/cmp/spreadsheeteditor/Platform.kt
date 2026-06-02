package my.cmp.spreadsheeteditor

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform