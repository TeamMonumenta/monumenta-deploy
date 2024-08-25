package com.playmonumenta.gradleconfig

import com.playmonumenta.gradleconfig.ssh.easySetup
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.bungee.BungeePluginDescription
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.tasks.Jar
import java.net.URI

class MonumentaExtensionImpl(private val target: Project) : MonumentaExtension {
    init {
        target.afterEvaluate { afterEvaluate() }
    }

    private var snapshotUrl: String = "https://maven.playmonumenta.com/snapshots"
    private var releasesUrl: String = "https://maven.playmonumenta.com/releases"
    private var mavenUsername: String? = System.getenv("USERNAME")
    private var mavenPassword: String? = System.getenv("TOKEN")
    private var isBukkitConfigured: Boolean = false
    private var isBungeeConfigured: Boolean = false
    private var pluginName: String? = null
    private var hasVersionAdapter: Boolean = false

    override fun versionAdapter(name: String) {
        hasVersionAdapter = true
    }

    override fun snapshotRepo(url: String) {
        snapshotUrl = url
    }

    override fun releasesRepo(url: String) {
        releasesUrl = url
    }

    override fun name(name: String) {
        if (pluginName != null) {
            throw IllegalStateException("name(...) can only be called once")
        }

        this.pluginName = name
    }

    override fun publishingCredentials(name: String, token: String) {
        mavenUsername = name
        mavenPassword = token
    }

    override fun paper(
        main: String,
        order: BukkitPluginDescription.PluginLoadOrder,
        apiVersion: String,
        authors: List<String>,
        depends: List<String>,
        softDepends: List<String>
    ) {
        if (isBukkitConfigured) {
            throw IllegalStateException("Bukkit can't be configured multiple times")
        }

        if (this.pluginName == null) {
            throw IllegalStateException("name(...) must be called first")
        }

        isBukkitConfigured = true

        target.applyPlugin("net.minecrell.plugin-yml.bukkit")
        with(target.extensions.getByType(BukkitPluginDescription::class.java)) {
            this.load = order
            this.main = main
            this.apiVersion = apiVersion
            this.name = pluginName
            this.authors = authors
            this.depend = depends
            this.softDepend = softDepends
        }

        target.addCompileOnly("io.papermc.paper:paper-api:$apiVersion-R0.1-SNAPSHOT")
    }

    override fun waterfall(
        main: String, apiVersion: String,
        authors: List<String>, depends: List<String>, softDepends: List<String>
    ) {
        if (isBungeeConfigured) {
            throw IllegalStateException("Bungee can't be configured multiple times")
        }

        if (this.pluginName == null) {
            throw IllegalStateException("name(...) must be called first")
        }

        target.applyPlugin("net.minecrell.plugin-yml.bungee")
        with(target.extensions.getByType(BungeePluginDescription::class.java)) {
            this.main = main
            this.name = pluginName
            this.author = authors.joinToString(", ")
            this.depends = setOf(*depends.toTypedArray())
            this.softDepends = setOf(*softDepends.toTypedArray())
            this.version = apiVersion
        }

        target.addCompileOnly("io.github.waterfallmc:waterfall-api:$apiVersion-R0.1-SNAPSHOT")
    }

    private fun afterEvaluate() {
        if (this.pluginName == null) {
            throw IllegalStateException("name(...) must be called first")
        }

        with(target.extensions.getByType(PublishingExtension::class.java)) {
            publications { container ->
                container.create("maven", MavenPublication::class.java) {
                    if (hasVersionAdapter) {
                        it.artifact(target.tasks.getByName("shadowJar"))
                        it.artifact(target.tasks.getByName("javadocJar"))
                        it.artifact(target.tasks.getByName("sourcesJar"))
                    } else {
                        it.from(target.components.getByName("java"))
                    }
                }
            }
            repositories { repo ->
                repo.maven { maven ->
                    maven.name = "MainMaven"
                    maven.url = URI(if (target.version.toString().endsWith("SNAPSHOT")) snapshotUrl else releasesUrl)
                    maven.credentials { cred ->
                        cred.username = mavenUsername
                        cred.password = mavenPassword
                    }
                }
            }
        }

        easySetup(target, target.tasks.getByName("shadowJar") as Jar, pluginName!!)
    }
}
