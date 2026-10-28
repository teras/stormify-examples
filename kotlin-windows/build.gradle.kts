plugins {
    kotlin("multiplatform") version "2.2.21"
    id("onl.ycode.stormify") version "3.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    mingwX64 {
        binaries {
            executable {
                entryPoint = "demo.main"
            }
        }
    }
}
