plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.soundfusion.core.storage"
    compileSdk = 35
    defaultConfig { minSdk = 29 }
    kotlin { jvmToolchain(17) }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.kotlinx.coroutines.core)
}
