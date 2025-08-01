import org.gradle.kotlin.dsl.commonMain
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.implementation

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val generatedSrc = file(layout.buildDirectory).resolve("generated-src")
generatedSrc.mkdirs()
val generatedFile = generatedSrc.resolve("generated.kt")
generatedFile.writeText(
    """
        import kotlin.time.Instant

        object Generated {
            val buildTime: Instant = Instant.fromEpochMilliseconds(${System.currentTimeMillis()})
            val buildEnv: String? = ${System.getenv("CI")?.let { "\"$it\"" }}
        }
    """.trimIndent()
)

kotlin {
    mingwX64()
    sourceSets {
        commonMain.dependencies {
            api("org.jetbrains.kotlinx:kotlinx-coroutines-core:_")
            api("org.jetbrains.kotlinx:kotlinx-serialization-json:_")

            api("org.jetbrains.kotlinx:kotlinx-datetime:_")

            api("com.squareup.okio:okio:_")

            api("org.jetbrains.kotlinx:kotlinx-io-core:_")
            api("org.jetbrains.kotlinx:kotlinx-io-okio:_")

            api("com.saveourtool.okio-extras:okio-extras:_")

            api("app.softwork:kotlinx-serialization-csv:_")

            api("com.kgit2:kommand:_")
//            implementation("com.soywiz:korlibs-template:_")

            api("com.github.ajalt.clikt:clikt:_")
//            api("com.github.ajalt.clikt:clikt-markdown:_")

            implementation("org.kotlincrypto.hash:sha2:_")

            api("io.github.oshai:kotlin-logging:_")
//            implementation("io.klogging:klogging:_")
        }
        mingwMain.dependencies {
            implementation("io.ktor:ktor-client-winhttp:_")
        }
        commonMain {
            kotlin.srcDir(generatedSrc)
        }
    }
}
kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}