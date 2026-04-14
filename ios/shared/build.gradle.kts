plugins {
    kotlin("multiplatform")
    id("com.google.devtools.ksp")
}

val stormifyVersion: String = providers.gradleProperty("stormifyVersion").getOrElse("2.1.1")

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://central.sonatype.com/repository/maven-snapshots/")
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }

    jvmToolchain(11)

    sourceSets {
        commonMain.dependencies {
            implementation("onl.ycode:stormify:$stormifyVersion")
        }
        // iosArm64 reuses iosSimulatorArm64 sources and KSP output
        val iosArm64Main by getting {
            kotlin.srcDir("build/generated/ksp/iosSimulatorArm64/iosSimulatorArm64Main/kotlin")
        }
    }
}

dependencies {
    add("kspIosSimulatorArm64", "onl.ycode:annproc:$stormifyVersion")
}

tasks.matching { it.name == "compileKotlinIosArm64" }.configureEach {
    dependsOn("kspKotlinIosSimulatorArm64")
}

tasks.named("clean") {
    doLast {
        delete("build/kspCaches")
    }
}
