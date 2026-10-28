plugins {
    kotlin("multiplatform")
    id("onl.ycode.stormify")
}

repositories {
    mavenCentral()
}

kotlin {
    listOf(
        iosArm64(),           // device
        iosSimulatorArm64(),  // simulator on Apple Silicon
        iosX64(),             // simulator on Intel
    ).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
}
