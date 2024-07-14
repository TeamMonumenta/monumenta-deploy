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
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

private fun getUsername(): String {
    val envVar = System.getenv("LOCKOUT_USERNAME")

    if(envVar != null)
        return envVar

    val git = Runtime.getRuntime().exec("git config user.name")
    git.waitFor()
    return git.inputStream.readAllBytes().toString(StandardCharsets.UTF_8).lowercase().trim();
}

fun attemptLockout(session: SessionHandler, domain: String, shard: String, time: Int) {
    val result =
        session.execute("~/4_SHARED/lockouts/lockout '$domain' claim '$shard' \"${getUsername()}\" $time \"Automatic lockout (deploy script)\"")
    if (result.first != 0) {
        throw RuntimeException("Failed to deploy! Shard is currently being used by another developer!")
    }
}

fun checkLockout(session: SessionHandler, domain: String, shard: String) {
    val result =
        session.execute("~/4_SHARED/lockouts/lockout '$domain' check '$shard'")
    if (result.first != 0) {
        throw RuntimeException("Failed to deploy! Shard is currently being used by another developer!")
    }
}

class RunHandler {
    fun session(remote: RemoteConfig, closure: SessionHandler.() -> Unit) {
        closure(SessionHandler(remote))
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

    class ShardLockInfo(
        private val domain: String,
        private val shard: String,
        private val defaultTime: Int,
        private val checkOnly: Boolean = false
    ) {
        fun doLock(session: SessionHandler) {
            if (checkOnly) {
                checkLockout(session, domain, shard)
            } else {
                attemptLockout(session, domain, shard, defaultTime)
            }
        }
    }

    fun easyConfigureDeployTask(shadowJarTask: Jar, name: String, category: String, config: RunHandler.() -> Unit) {
        proj.tasks.create(name) {
            it.group = category
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
        lockConfig: ShardLockInfo?,
        vararg paths: String
    ) {
        if (paths.isEmpty())
            throw IllegalArgumentException("paths must be non-empty")

        easyConfigureDeployTask(shadowJarTask, "$name-deploy-lock", "Deploy (locking)") {
            session(ssh) {
                lockConfig?.doLock(this)

                for (path in paths)
                    execute("cd $path && rm -f ${shadowJarTask.archiveBaseName.get()}*.jar")
                for (path in paths)
                    put(shadowJarTask.archiveFile.get().asFile, path)
            }
        }

        easyConfigureDeployTask(shadowJarTask, "$name-deploy", "Deploy") {
            session(ssh) {
                for (path in paths)
                    execute("cd $path && rm -f ${shadowJarTask.archiveBaseName.get()}*.jar")
                for (path in paths)
                    put(shadowJarTask.archiveFile.get().asFile, path)
            }
        }
    }

    fun easyCreateSymlinkDeploy(
        shadowJarTask: Jar,
        ssh: RemoteConfig,
        name: String,
        fileName: String,
        lockConfig: ShardLockInfo?,
        vararg paths: String
    ) {
        if (paths.isEmpty())
            throw IllegalArgumentException("paths must be non-empty")

        easyConfigureDeployTask(shadowJarTask, "$name-deploy-lock", "Deploy (locking)") {
            session(ssh) {
                lockConfig?.doLock(this)

                for (path in paths)
                    put(shadowJarTask.archiveFile.get().asFile, path)
                for (path in paths)
                    execute("cd $path && rm -f $fileName.jar && ln -s ${shadowJarTask.archiveFileName.get()} $fileName.jar")
            }
        }

        easyConfigureDeployTask(shadowJarTask, "$name-deploy", "Deploy") {
            session(ssh) {
                for (path in paths)
                    put(shadowJarTask.archiveFile.get().asFile, path)
                for (path in paths)
                    execute("cd $path && rm -f $fileName.jar && ln -s ${shadowJarTask.archiveFileName.get()} $fileName.jar")
            }
        }
    }

    fun easySetup(shadowJarTask: Jar, fileName: String) {
        val basicssh = easyCreateRemote("basicssh", 8822)
        val adminssh = easyCreateRemote("adminssh", 9922)

        for (i in 1..4) {
            easyCreateNormalDeploy(
                shadowJarTask,
                basicssh,
                "dev$i",
                ShardLockInfo("build", "dev$i", 30),
                "/home/epic/dev${i}_shard_plugins"
            )
        }

        easyCreateNormalDeploy(
            shadowJarTask,
            basicssh,
            "futurama",
            ShardLockInfo("build", "futurama", 30),
            "/home/epic/futurama_shard_plugins"
        )

        easyCreateNormalDeploy(
            shadowJarTask,
            basicssh,
            "mob",
            ShardLockInfo("build", "mob", 30),
            "/home/epic/mob_shard_plugins"
        )

        easyCreateSymlinkDeploy(
            shadowJarTask,
            basicssh,
            "volt",
            fileName,
            ShardLockInfo("volt", "*", 30),
            "/home/epic/volt/m12/server_config/plugins",
            "/home/epic/volt/m13/server_config/plugins"
        )

        easyCreateSymlinkDeploy(
            shadowJarTask,
            adminssh,
            "m119",
            fileName,
            ShardLockInfo("build", "m119", 30),
            "/home/epic/project_epic/m119/plugins"
        )

        easyCreateSymlinkDeploy(
            shadowJarTask,
            adminssh,
            "build",
            fileName,
            ShardLockInfo("build", "*", 0, true),
            "/home/epic/project_epic/server_config/plugins"
        )

        easyCreateSymlinkDeploy(
            shadowJarTask,
            adminssh,
            "play",
            fileName,
            ShardLockInfo("play", "*", 0, true),
            "/home/epic/play/m12/server_config/plugins",
            "/home/epic/play/m13/server_config/plugins",
            "/home/epic/play/m17/server_config/plugins"
        )
    }
}

class SshPlugin : Plugin<Project> {
    companion object {
        val LOGGER = LoggerFactory.getLogger("monumenta-ssh");
    }
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
