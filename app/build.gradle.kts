plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.soundfusion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.soundfusion"
        minSdk = 29
        targetSdk = 35
        versionCode = 5
        versionName = "2.1.0"
        testInstrumentationRunner = "com.soundfusion.testing.HiltTestRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { compose = true; buildConfig = true }
    kotlin { jvmToolchain(17) }
}

dependencies {
    // Core modules
    implementation(project(":core:core-ui"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-audio"))
    implementation(project(":core:core-storage"))
    implementation(project(":core:core-auth"))
    implementation(project(":core:core-database"))

    // Feature modules
    implementation(project(":feature:feature-home"))
    implementation(project(":feature:feature-search"))
    implementation(project(":feature:feature-library"))
    implementation(project(":feature:feature-playlists"))
    implementation(project(":feature:feature-player"))
    implementation(project(":feature:feature-settings"))
    implementation(project(":feature:feature-downloads"))
    implementation(project(":feature:feature-recommendations"))

    // Integration modules
    implementation(project(":integration:integration-youtube"))
    implementation(project(":integration:integration-spotify"))
    implementation(project(":integration:integration-lastfm"))
    implementation(project(":integration:integration-podcast"))
    implementation(project(":integration:integration-localmedia"))

    // Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.animation)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Media3
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.ui)

    // Glance (widgets)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Guava (for Android Auto ListenableFuture)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.9.0")

    // Wearable Data API
    implementation("com.google.android.gms:play-services-wearable:18.2.0")

    // Image
    implementation(libs.coil.compose)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
