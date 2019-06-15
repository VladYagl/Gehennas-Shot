rootProject.name = "GehennasShot"

pluginManagement({
    repositories {
        gradlePluginPortal()
        maven("https://kotlin.bintray.com/kotlinx")
    }
    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "kotlinx-serialization" -> {
                    useModule("org.jetbrains.kotlin:kotlin-serialization:${requested.version}")
                }
            }
        }
    }
})