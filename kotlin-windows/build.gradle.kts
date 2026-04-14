plugins {
    kotlin("multiplatform") version "2.2.20"
    id("com.google.devtools.ksp") version "2.2.20-2.0.2"
    id("org.jetbrains.kotlinx.atomicfu") version "0.30.0-beta"
}

val stormifyVersion: String = providers.gradleProperty("stormifyVersion").getOrElse("2.1.1")

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

kotlin {
    mingwX64 {
        binaries {
            executable {
                entryPoint = "demo.main"
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("onl.ycode:stormify:$stormifyVersion")
            }
        }
    }
}

// Annotation processor generates entity metadata (required on native)
dependencies {
    add("kspMingwX64", "onl.ycode:annproc:$stormifyVersion")
}

tasks.named("clean") {
    doLast {
        delete("build/kspCaches")
    }
}
