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
    add("kspMacosArm64", "onl.ycode:annproc:$stormifyVersion")
    add("kspMacosX64", "onl.ycode:annproc:$stormifyVersion")
}

// KSP in KMP 2.x does not auto-wire native target output into the source sets;
// each leaf source set must include its generated directory explicitly.
kotlin.sourceSets.named("macosArm64Main") {
    kotlin.srcDir("build/generated/ksp/macosArm64/macosArm64Main/kotlin")
}
kotlin.sourceSets.named("macosX64Main") {
    kotlin.srcDir("build/generated/ksp/macosX64/macosX64Main/kotlin")
}

tasks.named("clean") {
    doLast {
        delete("build/kspCaches")
    }
}
