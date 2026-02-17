import de.fayard.refreshVersions.core.versionFor
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("multiplatform.binaries")
}

kotlin {
    jvm {}
    mingwX64 {
        binaries {
            executable(
                entrypoint = "djay.main"
            )
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("io.github.smyrgeorge:sqlx4k-sqlite:_")
        }
    }

    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}
