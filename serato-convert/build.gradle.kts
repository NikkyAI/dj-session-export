plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    mingwX64 {
        binaries {
            executable() {
                entryPoint = "main"
//                baseName = "traktor-history-converter"
                runTaskProvider?.get()?.also { runTask ->
                    val args = providers.gradleProperty("runArgs")
                    runTask.workingDir = file("run").also { it.mkdirs() }
                    runTask.argumentProviders.add {
                        args.orNull?.let { listOf(it) }/*?.split(' ')*/ ?: emptyList()
                    }
                }
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("app.softwork:kotlinx-serialization-csv:_")
            implementation("app.softwork:kotlinx-serialization-flf:_")
//            implementation("org.jetbrains.kotlinx:kotlinx-datetime:_")
//            implementation("com.fleeksoft.ksoup:ksoup:_")
//            implementation("com.squareup.okio:okio:_")
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