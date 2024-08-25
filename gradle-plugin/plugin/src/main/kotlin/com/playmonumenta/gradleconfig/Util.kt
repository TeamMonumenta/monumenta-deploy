package com.playmonumenta.gradleconfig

import net.ltgt.gradle.errorprone.ErrorProneOptions
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.DomainObjectCollection
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.resources.TextResource
import org.gradle.api.tasks.compile.CompileOptions
import java.net.URI

fun RepositoryHandler.maven(maven: String) {
    maven { it.url = URI(maven) }
}

fun Project.applyPlugin(vararg maven: String) {
    maven.forEach {
        apply(mapOf("plugin" to it))
    }
}

fun <T, S : T> DomainObjectCollection<T>.withTypeWrap(
    type: Class<S>,
    configureAction: S.() -> Unit
): DomainObjectCollection<S> = withType(type, configureAction)

fun CompileOptions.errorproneWrap(action: ErrorProneOptions.() -> Unit) = errorprone(action)

fun Project.embeddedResource(path: String): TextResource {
    return resources.text.fromString(MonumentaGradlePlugin::class.java.getResource(path)?.readText()!!)
}

fun Project.addCompileOnly(deps: String) {
    with(project.dependencies) {
        add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, deps)
    }
}
