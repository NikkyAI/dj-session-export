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
            mainClass.set("MainKt")
        }
    }
    mingwX64 {
        binaries {
            executable(
                entrypoint = "main"
            )
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":mixxx-export"))
//            implementation(project(":djay-export"))
            implementation(project(":rekordbox-export"))
            implementation(project(":traktor-export"))
            implementation(project(":serato-export"))
            implementation(project(":virtualdj-export"))
        }
        mingwMain.dependencies {
            implementation("io.ktor:ktor-client-winhttp:_")
        }
//        all {
//            languageSettings.enableLanguageFeature("WhenGuards")
//        }
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

