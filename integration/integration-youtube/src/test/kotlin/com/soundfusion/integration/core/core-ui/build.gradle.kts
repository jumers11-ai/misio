plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.soundfusion.core.ui"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    buildFeatures { compose = true }
    kotlin { jvmToolchain(17) }
}

dependencies {
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    api(libs.compose.material3)
    api(libs.compose.ui)
    api(libs.compose.ui.tooling.preview)
    api(libs.compose.animation)
    api(libs.coil.compose)
    debugImplementation(libs.compose.ui.tooling)
}
