import de.fayard.refreshVersions.core.versionFor
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    id("multiplatform.binaries")
}


repositories {
    mavenCentral()
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("rekordbox.MainKt")
        }
    }
    mingwX64 {
        binaries {
            executable(entrypoint = "rekordbox.main")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("io.github.smyrgeorge:sqlx4k-sqlite:_")
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

tasks {
    shadowJar {
        archiveVersion = ""
        archiveClassifier = ""
    }
}