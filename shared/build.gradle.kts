import org.gradle.kotlin.dsl.commonMain
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.implementation

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    mingwX64()
    sourceSets {
        commonMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:_")

            implementation("org.jetbrains.kotlinx:kotlinx-datetime:_")

            implementation("com.squareup.okio:okio:_")

            implementation("org.jetbrains.kotlinx:kotlinx-io-core:_")
            implementation("org.jetbrains.kotlinx:kotlinx-io-okio:_")

            implementation("com.saveourtool.okio-extras:okio-extras:_")

            implementation("app.softwork:kotlinx-serialization-csv:_")

            implementation("com.kgit2:kommand:_")
        }
    }
}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}