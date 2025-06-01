import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val shadowImplementation: Configuration by configurations.creating

plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.jvm)
    id("com.github.johnrengelman.shadow") version "8.+"
}

group = "com.playmonumenta.gradle-config"
version = "3.1"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.playmonumenta.com/releases/")
}

dependencies {
    implementation("com.github.mwiede:jsch:0.2.17")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation("com.kohlschutter.junixsocket:junixsocket-core:2.10.0")
    implementation("net.ltgt.gradle:gradle-errorprone-plugin:3.1.0")
    implementation("net.ltgt.gradle:gradle-nullaway-plugin:2.0.0")
    implementation("net.minecrell:plugin-yml:0.6.0")
    implementation("com.palantir.gradle.gitversion:gradle-git-version:3.1.0")
    implementation("com.github.johnrengelman:shadow:8.1.1")
    implementation("com.playmonumenta.paperweight-aw:paperweight-userdev:1.1.2")
    shadowImplementation("com.github.mwiede:jsch:0.2.17")
}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    val plugin by plugins.creating {
        id = group.toString()
        implementationClass = "com.playmonumenta.gradleconfig.MonumentaGradlePlugin"
    }
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    relocate("com.jcraft.jsch", "com.playmonumenta.gradleconfig.internal.jsch")
    archiveClassifier.set("")
    configurations = listOf(shadowImplementation)

    manifest {
        attributes(
            mapOf(
                Pair("Implementation-Version", version)
            )
        )
    }
}

tasks.named("jar").configure {
    enabled = false
}

tasks.whenTaskAdded {
    if (name == "publishPluginJar" || name == "generateMetadataFileForPluginMavenPublication") {
        dependsOn(tasks.named("shadowJar"))
    }
}

publishing {
    repositories {
        maven {
            name = "MonumentaMaven"
            url = when (version.toString().endsWith("SNAPSHOT")) {
                true -> uri("https://maven.playmonumenta.com/snapshots")
                false -> uri("https://maven.playmonumenta.com/releases")
            }

            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            withType<MavenPublication> {
                setArtifacts(listOf(shadowJarTask.get()))
            }
        }
    }
}

