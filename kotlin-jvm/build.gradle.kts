plugins {
    kotlin("jvm") version "2.2.21"
    id("onl.ycode.stormify") version "3.0.0"
    application
}

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(8)
    compilerOptions {
        // The entities apply @DbField to constructor parameters; opt in to the future
        // param+property target so the annotation applies to the property and stays
        // warning-free (KT-73255).
        freeCompilerArgs.add("-Xannotation-default-target=param-property")
    }
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
