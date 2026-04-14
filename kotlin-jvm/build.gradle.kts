plugins {
    kotlin("jvm") version "2.2.20"
    application
}

val stormifyVersion: String = providers.gradleProperty("stormifyVersion").getOrElse("2.1.1")

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

kotlin {
    jvmToolchain(8)
}

dependencies {
    implementation("onl.ycode:stormify-jvm:$stormifyVersion")
    implementation("org.xerial:sqlite-jdbc:3.47.2.0")
}

application {
    mainClass.set("demo.MainKt")
}
