pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolution {
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "SoundFusion"

include(":app")

include(":core:core-ui")
include(":core:core-network")
include(":core:core-audio")
include(":core:core-storage")
include(":core:core-auth")
include(":core:core-database")

include(":feature:feature-home")
include(":feature:feature-search")
include(":feature:feature-library")
include(":feature:feature-playlists")
include(":feature:feature-player")
include(":feature:feature-settings")
include(":feature:feature-downloads")
include(":feature:feature-recommendations")

include(":integration:integration-youtube")
include(":integration:integration-spotify")
include(":integration:integration-lastfm")
include(":integration:integration-podcast")
include(":integration:integration-localmedia")
