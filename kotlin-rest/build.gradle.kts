plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("onl.ycode.stormify") version "2.6.0"
}

repositories {
    mavenCentral()
}

kotlin {
    val isMac = System.getProperty("os.name").startsWith("Mac")

    linuxX64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
    linuxArm64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
    mingwX64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
    if (isMac) {
        macosArm64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
        macosX64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
    }

    // Create the standard intermediate source sets (nativeMain, …) so a custom one can hang
    // off nativeMain below. Declaring a dependsOn edge otherwise suppresses the default tree.
    applyDefaultHierarchyTemplate()

    compilerOptions {
        // The entities apply @DbField to constructor parameters; opt in to the future
        // param+property target so the annotation stays warning-free (KT-73255).
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.bundles.ktor.server)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        // POSIX and Windows disagree on mkdir's arity, so the filesystem actual splits here:
        // the unix-like targets share one copy, mingw gets its own.
        val nativeMain by getting
        val posixMain by creating { dependsOn(nativeMain) }
        val linuxX64Main by getting { dependsOn(posixMain) }
        val linuxArm64Main by getting { dependsOn(posixMain) }
        if (isMac) {
            val macosArm64Main by getting { dependsOn(posixMain) }
            val macosX64Main by getting { dependsOn(posixMain) }
        }
    }
}
