plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.gradleup.shadow")
    id("multiplatform.binaries")
}

kotlin {
    mingwX64 {
        binaries {
            executable(entrypoint = "main")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("app.softwork:kotlinx-serialization-csv:_")
            implementation("app.softwork:kotlinx-serialization-flf:_")
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