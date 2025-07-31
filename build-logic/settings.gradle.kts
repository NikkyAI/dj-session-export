pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    // See https://jmfayard.github.io/refreshVersions
    id("de.fayard.refreshVersions") version "0.60.5"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

refreshVersions {
    versionsPropertiesFile = file("../versions.properties")
}

include("plugins")

