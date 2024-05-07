import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.jvm)
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.playmonumenta.deployment"
version = "1.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.mwiede:jsch:0.2.17")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
}

gradlePlugin {
    val plugin by plugins.creating {
        id = group.toString()
        implementationClass = "com.playmonumenta.deployment.SshPlugin"
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
    }
}
