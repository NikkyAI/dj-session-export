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
            executable(entrypoint = "virtualdj.main")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
        }
    }

//    jvmToolchain(21)
//    compilerOptions {
//        optIn.add("kotlin.time.ExperimentalTime")
//    }
}