plugins {
    kotlin("multiplatform") version "2.2.21"
    id("onl.ycode.stormify") version "3.0.1"
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

    compilerOptions {
        // The entities apply @DbField to constructor parameters; opt in to the future
        // param+property target so the annotation applies to the property and stays
        // warning-free (KT-73255).
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
}
