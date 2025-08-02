import de.fayard.refreshVersions.core.versionFor
import org.jetbrains.kotlin.konan.target.HostManager

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("multiplatform.binaries")
}

kotlin {
    mingwX64 {
        binaries {
            executable(
                entrypoint = "mixxx.main"
            )
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("io.github.smyrgeorge:sqlx4k-sqlite:_")
        }
        mingwMain.dependencies {
//            implementation("io.ktor:ktor-client-winhttp:_")
        }
    }

    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}
