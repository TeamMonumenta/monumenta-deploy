plugins {
    `java-gradle-plugin`
    `maven-publish`
    alias(libs.plugins.jvm)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.mwiede:jsch:0.2.17")
    implementation("net.java.dev.jna:jna:5.14.0")
    implementation("net.java.dev.jna:jna-platform:5.14.0")
}

gradlePlugin {
    val greeting by plugins.creating {
        id = "com.playmonumenta.deployment"
        implementationClass = "com.playmonumenta.deployment.SshPlugin"
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/TeamMonumenta/monumenta-deploy")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}