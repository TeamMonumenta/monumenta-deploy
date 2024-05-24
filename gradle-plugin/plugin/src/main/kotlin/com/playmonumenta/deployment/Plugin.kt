package com.playmonumenta.deployment

import com.playmonumenta.deployment.auth.EnvKeyProvider
import com.playmonumenta.deployment.auth.OpenSSHProvider
import com.playmonumenta.deployment.auth.PageantKeyProvider
import com.playmonumenta.deployment.auth.SSHAgentKeyProvider
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.jvm.tasks.Jar

class RunHandler {
    fun session(remote: RemoteConfig, closure: Action<SessionHandler>) {
        closure.execute(SessionHandler(remote))
    }
}

class Service(private val proj: Project, private val remotes: NamedDomainObjectContainer<RemoteConfig>) {
    fun run(closure: Action<RunHandler>) {
        closure.execute(RunHandler())
    }

    fun easyCreateRemote(name: String, p: Int): RemoteConfig {
        return remotes.create(name) {
            it.host = "admin-eu.playmonumenta.com"
            it.port = p
            it.user = "epic"
            it.knownHosts = AllowAnyHosts.instance
            it.auth = arrayOf(
                EnvKeyProvider(),
                SSHAgentKeyProvider(),
                OpenSSHProvider("id_ed25519"),
                OpenSSHProvider("id_rsa"),
                PageantKeyProvider()
            )
        }
    }

    fun easyConfigureDeployTask(shadowJarTask: Jar, name: String, config: Action<RunHandler>) {
        proj.tasks.create(name) {
            it.group = "Deploy"
            it.dependsOn(shadowJarTask)
            it.doLast {
                run(config)
            }
        }
    }

    fun easyCreateNormalDeploy(
        shadowJarTask: Jar,
        ssh: RemoteConfig,
        name: String,
        fileName: String,
        vararg paths: String
    ) {
        if (paths.isEmpty())
            throw IllegalArgumentException("paths must be non-empty")

        easyConfigureDeployTask(shadowJarTask, "$name-deploy") {
            it.session(ssh) { session ->
                for (path in paths)
                    session.execute("cd $path && rm -f $fileName*.jar")
                for (path in paths)
                    session.put(shadowJarTask.archiveFile.get().asFile, path)
            }
        }
    }

    fun easyCreateSymlinkDeploy(
        shadowJarTask: Jar,
        ssh: RemoteConfig,
        name: String,
        fileName: String,
        vararg paths: String
    ) {
        if (paths.isEmpty())
            throw IllegalArgumentException("paths must be non-empty")

        easyConfigureDeployTask(shadowJarTask, "$name-deploy") {
            it.session(ssh) { session ->
                for (path in paths)
                    session.put(shadowJarTask.archiveFile.get().asFile, path)
                for (path in paths)
                    session.execute("cd $path && rm -f $fileName.jar && ln -s ${shadowJarTask.archiveFileName.get()} $fileName.jar")
            }
        }
    }

    fun easySetup(shadowJarTask: Jar, fileName: String) {
        val basicssh = easyCreateRemote("basicssh", 8822)
        val adminssh = easyCreateRemote("adminssh", 9922)

        for (i in 1..4) {
            easyCreateNormalDeploy(shadowJarTask, basicssh, "dev$i", fileName, "/home/epic/dev${i}_shard_plugins")
        }

        easyCreateNormalDeploy(shadowJarTask, basicssh, "futurama", fileName, "/home/epic/futurama_shard_plugins")
        easyCreateNormalDeploy(shadowJarTask, basicssh, "mob", fileName, "/home/epic/mob_shard_plugins")
        easyCreateSymlinkDeploy(
            shadowJarTask,
            basicssh,
            "stage",
            fileName,
            "/home/epic/stage/m13/server_config/plugins/"
        )
        easyCreateSymlinkDeploy(
            shadowJarTask,
            basicssh,
            "volt",
            fileName,
            "/home/epic/volt/m12/server_config/plugins",
            "/home/epic/volt/m13/server_config/plugins"
        )

        easyCreateSymlinkDeploy(shadowJarTask, adminssh, "m119", fileName, "/home/epic/project_epic/m119/plugins")
        easyCreateSymlinkDeploy(
            shadowJarTask,
            adminssh,
            "build",
            fileName,
            "/home/epic/project_epic/server_config/plugins"
        )
        easyCreateSymlinkDeploy(
            shadowJarTask, adminssh, "play", fileName,
            "/home/epic/play/m12/server_config/plugins",
            "/home/epic/play/m13/server_config/plugins",
            "/home/epic/play/m17/server_config/plugins"
        )
    }
}

class SshPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val remotes = createRemoteContainer(target)
        target.extensions.add("ssh", Service(target, remotes))
        target.extensions.add("remotes", remotes)
    }

    private fun createRemoteContainer(project: Project): NamedDomainObjectContainer<RemoteConfig> {
        val remotes = project.container(RemoteConfig::class.java)

        return remotes
    }
}
