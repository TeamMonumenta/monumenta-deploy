package com.playmonumenta.gradleconfig

import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtraPropertiesExtension
import org.slf4j.Logger
import org.slf4j.LoggerFactory

private fun setupVersion(project: Project) {
    project.applyPlugin("com.palantir.git-version")
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
        target.charset("UTF-8")
        target.applyPlugin("java-library")
        setupVersion(target)

        target.extensions.add("monumenta", MonumentaExtensionImpl(target))
    }
}
