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
            mainClass.set("traktor.MainKt")
        }
    }
    mingwX64 {
        binaries {
            executable(entrypoint = "traktor.main")
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("io.github.pdvrieze.xmlutil:serialization:_")
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
