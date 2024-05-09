import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val shadowImplementation: Configuration by configurations.creating

plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.jvm)
    id("com.github.johnrengelman.shadow") version "8.+"
}

group = "com.playmonumenta.deployment"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.mwiede:jsch:0.2.17")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
    shadowImplementation("com.github.mwiede:jsch:0.2.17")
}

java {

}

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    val plugin by plugins.creating {
        id = group.toString()
        implementationClass = "com.playmonumenta.deployment.SshPlugin"
    }
}

val shadowJarTask = tasks.named<ShadowJar>("shadowJar") {
    relocate("com.jcraft.jsch", "com.playmonumenta.deployment.internal.jsch")
    archiveClassifier.set("")
    configurations = listOf(shadowImplementation)

    manifest {
        attributes(mapOf(
            Pair("Implementation-Version", version)
        ))
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
            name = "FloweyMaven"
            url = uri("https://maven.floweytf.com/releases")
            credentials {
                username = System.getenv("USERNAME")
                password = System.getenv("TOKEN")
            }

        }
        maven {
            name = "FloweySnapshot"
            url = uri("https://maven.floweytf.com/snapshots")
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

