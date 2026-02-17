import org.gradle.kotlin.dsl.commonMain
import org.gradle.kotlin.dsl.dependencies

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

val generatedSrc = file(layout.buildDirectory).resolve("generated-src")
val generatedFile = generatedSrc.resolve("generated.kt")
fun generate() {
    generatedSrc.mkdirs()
    generatedFile.writeText(
        """
        import kotlin.time.Instant

        object Generated {
            val buildTime: Instant = Instant.fromEpochMilliseconds(${System.currentTimeMillis()})
            val buildEnv: String? = ${System.getenv("CI")?.let { "\"$it\"" }}
        }
    """.trimIndent()
    )
}
//generate()

//tasks.compileKotlinJvm {
//    doFirst {
//        generate()
//    }
//}
kotlin {
    jvm {}
    mingwX64 {
        compilerOptions {

        }
//        binaries {
//            staticLib {
//                this.linkerOpts += listOf(
//                    "-static-libgcc",
//                    "-static-libstdc++",
//                    "-Wl,--allow-multiple-definition",
//                )
//            }
//        }
    }

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
            api("com.github.ajalt.clikt:clikt-markdown:_")

            implementation("org.kotlincrypto.hash:sha2:_")

            api("io.github.oshai:kotlin-logging:_")
//            implementation("io.klogging:klogging:_")
            api("io.ktor:ktor-client-core:_")
            api("io.ktor:ktor-client-cio:_")
        }
        mingwMain.dependencies {
            implementation("io.ktor:ktor-client-winhttp:_")
        }
        jvmMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:_")
//            implementation("ch.qos.logback:logback-classic:_")
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

abstract class GenerateCodeTask : DefaultTask() {
    @get:Input
    abstract val generatedFileProp: Property<File>

    @TaskAction
    fun generateCode() {
        val generatedFile = generatedFileProp.get()
        generatedFile.parentFile.mkdirs()
        generatedFile.writeText(
        """
            import kotlin.time.Instant
    
            object Generated {
                val buildTime: Instant = Instant.fromEpochMilliseconds(${System.currentTimeMillis()})
                val buildEnv: String? = ${System.getenv("CI")?.let { "\"$it\"" }}
            }
        """.trimIndent()
        )
    }
}


val generateCode = tasks.register("generateCode", GenerateCodeTask::class) {
    generatedFileProp = project.provider {
        generatedFile
    }
}

val compileKotlinJvm by tasks.getting {
    dependsOn(generateCode)
}
val compileKotlinMingwX64 by tasks.getting {
    dependsOn(generateCode)
}