plugins {
    kotlin("multiplatform")
    id("onl.ycode.stormify")
}

repositories {
    mavenCentral()
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    sourceSets {
        // iosArm64 reuses iosSimulatorArm64 sources
        val iosArm64Main by getting {
            kotlin.srcDir("src/iosSimulatorArm64Main/kotlin")
        }
    }
}
