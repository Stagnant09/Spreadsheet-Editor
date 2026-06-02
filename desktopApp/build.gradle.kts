import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
    implementation(libs.nucleus.decoratedWindow)
    implementation(libs.nucleus.decoratedWindow.material2)
    implementation("io.github.compose-fluent:fluent:v0.1.0")
    implementation("io.github.compose-fluent:fluent-icons-extended:v0.1.0")
    implementation(libs.androidx.material3.desktop)
    
}

val cmakeConfigure = tasks.register<Exec>("cmakeConfigure") {
    workingDir = rootDir
    commandLine(
        "cmake", "-S", ".", "-B", "build/native",
        "-G", "MinGW Makefiles",
        "-DCMAKE_BUILD_TYPE=Release",
        "-DCMAKE_C_COMPILER=C:/Qt/Tools/mingw1310_64/bin/gcc.exe"
    )
}

val cmakeBuild = tasks.register<Exec>("cmakeBuild") {
    workingDir = rootDir
    commandLine(
        "cmake", "--build", "build/native", "--config", "Release"
    )
    dependsOn(cmakeConfigure)
}

tasks.named("build") {
    dependsOn(cmakeBuild)
}

compose.desktop {
    application {
        mainClass = "my.cmp.spreadsheeteditor.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "my.cmp.spreadsheeteditor"
            packageVersion = "1.0.0"
            appResourcesRootDir.set(project.layout.projectDirectory.dir("build/native"))
        }
        jvmArgs += "-Djava.library.path=${rootDir}/build/native"
    }
}