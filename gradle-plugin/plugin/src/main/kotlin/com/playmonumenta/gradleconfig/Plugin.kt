package com.playmonumenta.gradleconfig

import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import net.ltgt.gradle.errorprone.CheckSeverity
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.PmdExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private fun applyPlugins(project: Project) {
    project.applyPlugin(
        "pmd",
        "java-library",
        "maven-publish",
        "checkstyle",
        "net.ltgt.errorprone",
        "net.ltgt.nullaway",
        "com.github.johnrengelman.shadow",
        "com.palantir.git-version"
    )
}

private fun addRepo(project: Project) {
    with(project.repositories) {
        mavenLocal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.maven.apache.org/maven2/")
        maven("https://jitpack.io")
        maven("https://oss.sonatype.org/content/repositories/snapshots")
        maven("https://repo.codemc.org/repository/maven-public/")
        maven("https://maven.playmonumenta.com/")
    }
}

private fun addDependencies(project: Project) {
    with(project.dependencies) {
        add("errorprone", "com.google.errorprone:error_prone_core:2.29.1")
        add("errorprone", "com.uber.nullaway:nullaway:0.9.5")
    }
}

private fun setupChecks(project: Project) {
    project.tasks.withTypeWrap(JavaCompile::class.java) {
        options.encoding = "UTF-8"

        options.compilerArgs.add("-Xmaxwarns")
        options.compilerArgs.add("10000")
        options.compilerArgs.add("-Xlint:deprecation")

        options.errorproneWrap {
            option("NullAway:AnnotatedPackages", "com.playmonumenta")
            allErrorsAsWarnings.set(true)

            /*** Disabled checks ***/
            check(
                "CatchAndPrintStackTrace",
                CheckSeverity.OFF
            ) // This is the primary way a lot of exceptions are handled
            check(
                "FutureReturnValueIgnored", CheckSeverity.OFF
            ) // This one is dumb and doesn't let you check return values with .whenComplete()
            check(
                "ImmutableEnumChecker",
                CheckSeverity.OFF
            ) // Would like to turn this on but we'd have to annotate a bunch of base classes
            check(
                "LockNotBeforeTry",
                CheckSeverity.OFF
            ) // Very few locks in our code, those that we have are simple and refactoring like this would be ugly
            check("StaticAssignmentInConstructor", CheckSeverity.OFF) // We have tons of these on purpose
            check(
                "StringSplitter",
                CheckSeverity.OFF
            ) // We have a lot of string splits too which are fine for this use
            check(
                "MutablePublicArray",
                CheckSeverity.OFF
            ) // These are bad practice but annoying to refactor and low risk of actual bugs
            check("InlineMeSuggester", CheckSeverity.OFF) // This seems way overkill
        }
    }

    with(project.extensions.getByType(PmdExtension::class.java)) {
        isConsoleOutput = true
        toolVersion = "7.2.0"
        ruleSetConfig = project.embeddedResource("/pmd-ruleset.xml")
        isIgnoreFailures = true
    }

    with(project.extensions.getByType(CheckstyleExtension::class.java)) {
        config = project.embeddedResource("/checkstyle.xml")
    }
}

fun setupJava(project: Project) {
    with(project.extensions.getByType(JavaPluginExtension::class.java)) {
        withJavadocJar()
        withSourcesJar()
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

fun setupVersion(project: Project) {
    val extra = project.extensions.getByType(ExtraPropertiesExtension::class.java)

    val gitVersion = extra.get("gitVersion") as Closure<String>
    val versionDetails = extra.get("versionDetails") as Closure<VersionDetails>

    project.group = "com.playmonumenta"
    project.version = gitVersion.call() + (if (versionDetails.call().isCleanTag) "" else "-SNAPSHOT")
}

class MonumentaGradlePlugin : Plugin<Project> {
    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger("MonumentaGradlePlugin")
    }

    override fun apply(target: Project) {
        applyPlugins(target)
        addRepo(target)
        addDependencies(target)
        setupChecks(target)
        setupJava(target)
        setupVersion(target)

        target.extensions.add("monumenta", MonumentaExtensionImpl(target))
    }
}
