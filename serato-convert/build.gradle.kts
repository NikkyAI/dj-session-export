plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("multiplatform.binaries")
}

kotlin {
//    mingwX64 {
//        binaries {
//            executable() {
//                entryPoint = "main"
//                if(System.getenv("CI") == null) {
//                    baseName = project.name + "-dev"
//                }
//                runTaskProvider?.get()?.also { runTask ->
//                    val args = providers.gradleProperty("runArgs")
//                    runTask.workingDir = file("run").also { it.mkdirs() }
//                    runTask.argumentProviders.add {
//                        args.orNull?.let { listOf(it) }/*?.split(' ')*/ ?: emptyList()
//                    }
//                }
//            }
//        }
//    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation("app.softwork:kotlinx-serialization-csv:_")
            implementation("app.softwork:kotlinx-serialization-flf:_")
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