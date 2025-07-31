plugins {
    `kotlin-dsl`
}

gradlePlugin {
    plugins {
        create("multiplatform.binaries") {
            id = "multiplatform.binaries"
            implementationClass = "MultiplatformBinariesConventions"
        }
    }
}

dependencies {
    compileOnly(kotlin("gradle-plugin", "_"))
}