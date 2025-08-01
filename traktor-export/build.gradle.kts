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
            implementation("io.github.pdvrieze.xmlutil:serialization:_")
        }
    }
}
