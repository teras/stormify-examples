plugins {
    kotlin("multiplatform") version "2.2.21"
    id("onl.ycode.stormify") version "2.5.0"
}

repositories {
    mavenCentral()
}

kotlin {
    macosArm64 {
        binaries {
            executable {
                entryPoint = "demo.main"
            }
        }
    }
    macosX64 {
        binaries {
            executable {
                entryPoint = "demo.main"
            }
        }
    }
}
