import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    id("multiplatform.binaries")
}

kotlin {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        mainRun {
            mainClass.set("serato.MainKt")
        }
    }
    mingwX64 {
        binaries {
            executable(entrypoint = "serato.main")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("com.fleeksoft.io:io:_")
        }
        mingwMain.dependencies {
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