plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("multiplatform.binaries")
}

kotlin {
    mingwX64 {
        binaries {
            executable(
                entrypoint = "main"
            )
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared"))
            implementation(project(":mixxx-export"))
            implementation(project(":rekordbox-export"))
            implementation(project(":traktor-export"))
            implementation(project(":serato-export"))
            implementation(project(":virtualdj-export"))
        }
        mingwMain.dependencies {
            implementation("io.ktor:ktor-client-winhttp:_")
        }
//        all {
//            languageSettings.enableLanguageFeature("WhenGuards")
//        }
    }

    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}


