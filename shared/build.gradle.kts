plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("io.github.kdroidfilter.nucleus") version "1.15.7"
}

kotlin {
    jvm()


    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("dev.nucleusframework:nucleus.decorated-window-jbr:2.0.0-alpha-202605291533")
            implementation("dev.nucleusframework:nucleus.decorated-window-material2:2.0.0-alpha-202605291533")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}