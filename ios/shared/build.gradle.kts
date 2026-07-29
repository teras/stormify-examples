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

    sourceSets {
        // One `iosMain` shared by device and simulator, rather than grafting one
        // target's source directory onto the other: both targets are peers here, and
        // an intermediate source set is what the Kotlin toolchain expects to see.
        val iosMain by creating { dependsOn(commonMain.get()) }
        iosArm64Main.get().dependsOn(iosMain)
        iosSimulatorArm64Main.get().dependsOn(iosMain)
        iosX64Main.get().dependsOn(iosMain)
    }
}
