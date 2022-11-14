enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "floodgate-whitelist"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("net.kyori.indra.git") version "2.2.0"
        id("com.github.johnrengelman.shadow") version "7.1.2" // shadowing dependencies
    }
}



