plugins {
    kotlin("multiplatform") version "2.2.20"
    kotlin("plugin.serialization") version "2.2.20"
    id("com.google.devtools.ksp") version "2.2.20-2.0.4"
    id("org.jetbrains.kotlinx.atomicfu") version "0.30.0-beta"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

val ktorVersion = "3.3.3"
val stormifyVersion: String = providers.gradleProperty("stormifyVersion").getOrElse("2.1.1")
val datetimeVersion = "0.7.1"
val serializationVersion = "1.9.0"

kotlin {
    val isMac = System.getProperty("os.name").startsWith("Mac")

    linuxX64 {
        binaries {
            executable {
                entryPoint = "com.example.kotlinrest.main"
            }
        }
    }

    linuxArm64 {
        binaries {
            executable {
                entryPoint = "com.example.kotlinrest.main"
            }
        }
    }

    mingwX64 {
        binaries {
            executable {
                entryPoint = "com.example.kotlinrest.main"
            }
        }
    }

    if (isMac) {
        macosArm64 {
            binaries {
                executable {
                    entryPoint = "com.example.kotlinrest.main"
                }
            }
        }

        macosX64 {
            binaries {
                executable {
                    entryPoint = "com.example.kotlinrest.main"
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("onl.ycode:stormify:$stormifyVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:$datetimeVersion")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
                implementation("io.ktor:ktor-server-core:$ktorVersion")
                implementation("io.ktor:ktor-server-cio:$ktorVersion")
                implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
                implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
                implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
                implementation("io.ktor:ktor-server-default-headers:$ktorVersion")
                implementation("io.ktor:ktor-server-cors:$ktorVersion")
            }
        }

    }
}

// KSP runs per target; kspCommonMainMetadata is not reliably supported in
// KSP 2.x for native KMP yet. Each target source set picks up its own generated
// code; commonMain references the registrar through an expect/actual shim.
dependencies {
    add("kspLinuxX64", "onl.ycode:annproc:$stormifyVersion")
    add("kspLinuxArm64", "onl.ycode:annproc:$stormifyVersion")
    add("kspMingwX64", "onl.ycode:annproc:$stormifyVersion")
    if (System.getProperty("os.name").startsWith("Mac")) {
        add("kspMacosArm64", "onl.ycode:annproc:$stormifyVersion")
        add("kspMacosX64", "onl.ycode:annproc:$stormifyVersion")
    }
}

kotlin.sourceSets.named("linuxX64Main") {
    kotlin.srcDir("build/generated/ksp/linuxX64/linuxX64Main/kotlin")
}
kotlin.sourceSets.named("linuxArm64Main") {
    kotlin.srcDir("build/generated/ksp/linuxArm64/linuxArm64Main/kotlin")
}
kotlin.sourceSets.named("mingwX64Main") {
    kotlin.srcDir("build/generated/ksp/mingwX64/mingwX64Main/kotlin")
}
if (System.getProperty("os.name").startsWith("Mac")) {
    kotlin.sourceSets.named("macosArm64Main") {
        kotlin.srcDir("build/generated/ksp/macosArm64/macosArm64Main/kotlin")
    }
    kotlin.sourceSets.named("macosX64Main") {
        kotlin.srcDir("build/generated/ksp/macosX64/macosX64Main/kotlin")
    }
}

tasks.named("clean") {
    doLast {
        delete("build/kspCaches")
    }
}
