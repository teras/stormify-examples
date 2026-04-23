plugins {
    kotlin("jvm") version "2.2.21"
    id("onl.ycode.stormify") version "2.5.0"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
}

stormify {
    // JVM-only: skip the generated registrar and rely on reflection-based
    // entity discovery. The Paths object is still emitted for type-safe
    // column references.
    generateRegistrar.set(false)
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")
}

application {
    mainClass.set("demo.MainKt")
}
