import de.fayard.refreshVersions.core.versionFor
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.konan.target.HostManager

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
            mainClass.set("virtualdj.MainKt")
        }
    }
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
tasks {
    shadowJar {
        archiveVersion = ""
        archiveClassifier = ""
    }
}