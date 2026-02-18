import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.toHex
import kotlin.plus
import kotlin.random.Random
import kotlin.random.nextUBytes

@Suppress("unused")
class MultiplatformBinariesConventions : Plugin<Project> {
    override fun apply(project: Project) {
        val targets = Utils.targetsOf(project)
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.extensions.configure<KotlinMultiplatformExtension> {
            val availableTargets = mapOf(
//                Pair("iosArm64") { iosArm64 { binaries { executable() } } },
//                Pair("androidNativeArm64") { androidNativeArm64 { binaries { executable() } } },
//                Pair("androidNativeX64") { androidNativeX64 { binaries { executable() } } },
//                Pair("macosArm64") { macosArm64 { binaries { executable() } } },
//                Pair("macosX64") { macosX64 { binaries { executable() } } },
//                Pair("linuxArm64") { linuxArm64 { binaries { executable() } } },
//                Pair("linuxX64") { linuxX64 { binaries { executable() } } },
                Pair("mingwX64") {
                    mingwX64 {
                        binaries {
//                            executable() {
//                                entryPoint = "main"
//                                if (System.getenv("CI") == null) {
//                                    baseName = project.name + "-dev"
//                                }
//                                runTaskProvider?.configure {
//                                    val args = project.providers.gradleProperty("runArgs")
//                                    workingDir = project.file("run").also { it.mkdirs() }
//                                    argumentProviders.add {
//                                        args.orNull?.let { listOf(it) } ?: emptyList()
//                                    }
//                                }
//                                linkerOpts += listOf("-Wl,--allow-multiple-definition")
//                            }
                        }
                    }
                },
            )

//            logger.info { "Enabling target jvm" }
            println("Enabling target jvm")
            jvm {
                @OptIn(ExperimentalKotlinGradlePluginApi::class)
                mainRun {
                    mainClass.set("MainKt")
                }
            }

            targets.forEach {
                println("Enabling target $it")
                availableTargets[it]?.invoke()
            }

            jvmToolchain(21)
            compilerOptions {
                optIn.add("kotlin.time.ExperimentalTime")
            }

            applyDefaultHierarchyTemplate()
        }
    }
}

@OptIn(ExperimentalUnsignedTypes::class)
fun KotlinNativeBinaryContainer.executable(
    entrypoint: String = "main",
    linkerOptions: List<String> = listOf(
        "-Wl,--allow-multiple-definition",
    )
) {
    executable() {
        entryPoint = entrypoint
        baseName = if (System.getenv("CI") == null) {
    //            baseName = project.name + "-" + Random.nextUBytes(4).joinToString("") { it.toString(16) }
            project.name + "-dev"
        } else {
            project.name
        }
        println("${buildType.name} baseName: $baseName")
        runTaskProvider?.configure {
            val args = project.providers.gradleProperty("runArgs")
            workingDir = project.file("run").also { it.mkdirs() }
            argumentProviders.add {
                args.orNull?.let { listOf(it) } ?: emptyList()
            }
        }
        linkerOpts += linkerOptions
//        freeCompilerArgs += "-Xdisable-phases=EscapeAnalysis"
    }
}