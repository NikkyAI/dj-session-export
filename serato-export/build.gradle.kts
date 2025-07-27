plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    mingwX64 {
        binaries {
            executable() {
                entryPoint = "main"
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
            implementation("com.fleeksoft.io:io:_")
//            implementation("com.fleeksoft.io:okio:_")
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