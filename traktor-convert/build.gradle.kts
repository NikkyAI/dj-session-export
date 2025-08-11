plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("multiplatform.binaries")
}

kotlin {
    mingwX64 {
        binaries {
            executable(entrypoint = "traktor.main")
        }
    }
    sourceSets {
        mingwMain.dependencies {
            implementation(project(":shared"))
            implementation("com.fleeksoft.ksoup:ksoup:_")
        }
    }
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}