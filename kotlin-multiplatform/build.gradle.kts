plugins {
    kotlin("multiplatform") version "2.2.21"
    id("onl.ycode.stormify") version "3.0.0"
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("demo.MainKt")
        }
    }

    val isMac = System.getProperty("os.name").startsWith("Mac")
    if (!isMac) {
        linuxX64 { binaries { executable { entryPoint = "demo.main" } } }
        linuxArm64 { binaries { executable { entryPoint = "demo.main" } } }
        mingwX64 { binaries { executable { entryPoint = "demo.main" } } }
    } else {
        macosArm64 { binaries { executable { entryPoint = "demo.main" } } }
        macosX64 { binaries { executable { entryPoint = "demo.main" } } }
    }

    jvmToolchain(8)
    compilerOptions {
        // The entities apply @DbField to constructor parameters; opt in to the future
        // param+property target so the annotation applies to the property and stays
        // warning-free (KT-73255).
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation("org.xerial:sqlite-jdbc:3.47.2.0")
            }
        }
    }
}
