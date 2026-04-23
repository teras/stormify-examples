plugins {
    java
    application
    kotlin("jvm") version "2.2.21"                // required by the Stormify plugin (KSP runs on the Kotlin compiler)
    id("onl.ycode.stormify") version "2.5.0"
}

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")
    implementation("org.slf4j:slf4j-simple:1.7.36")
    implementation("javax.persistence:javax.persistence-api:2.2")
}

application {
    mainClass.set("demo.Main")
}
