plugins {
    kotlin("multiplatform") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    id("onl.ycode.stormify") version "2.5.0"
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.3.3"
val datetimeVersion = "0.7.1"
val serializationVersion = "1.9.0"

kotlin {
    val isMac = System.getProperty("os.name").startsWith("Mac")

    linuxX64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
    linuxArm64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
    mingwX64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
    if (isMac) {
        macosArm64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
        macosX64 { binaries { executable { entryPoint = "com.example.kotlinrest.main" } } }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
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
