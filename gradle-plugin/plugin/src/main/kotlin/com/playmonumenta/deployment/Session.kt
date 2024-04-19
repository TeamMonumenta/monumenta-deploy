package com.playmonumenta.deployment

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import java.io.File
import java.util.*

class SessionHandler(private val remote: RemoteConfig) {
    private val jsch = JSch()
    private val session: Session

    private fun resolveHostKey(knownHosts: Any) {
        when (knownHosts) {
            is File -> {
                TODO("implement File")
            }

            is AllowAnyHosts -> {
                session.setConfig("StrictHostKeyChecking", "no")
                println("[ssh/warn]: Host key checking is off. It may be vulnerable to man-in-the-middle attacks.")
            }

            else -> throw IllegalArgumentException("knownHosts must be file, collection of files, or allowAnyHosts")
        }
    }

    init {
        println("[ssh/debug]: Attempting to connect to remote $remote")
        session = jsch.getSession(remote.user, remote.host, remote.port)

        resolveHostKey(remote.knownHosts)

        println("[ssh/debug]: Using the following authentication attempts:")
        var found = false

        for (auth in remote.auth) {
            var res: Optional<String>

            try {
                res = auth.tryProvider(jsch)
            } catch (e: Exception) {
                println("FAIL - ${auth.name}: ${e.message}")
                continue
            }

            if (res.isEmpty) {
                println("USING - ${auth.name}")
                found = true
                if (!auth.shouldContinue) {
                    break
                }
            } else {
                println("SKIP - ${auth.name}: ${res.get()}")
            }
        }

        if (!found)
            throw RuntimeException("Exhausted authentication methods, check logs!")

        session.connect(remote.timeout)
    }

    fun execute(commandLine: String) {
        println("[ssh/debug]: Executing command '$commandLine' on $remote")
        val chan = session.openChannel("exec") as ChannelExec
        chan.setCommand(commandLine)
        chan.connect(remote.timeout)
        // TODO: stop ignoring output
    }

    fun put(from: File, to: String) {
        println("[ssh/debug]: Copying '$from' -> '$to' on $remote")
        val chan = session.openChannel("sftp") as ChannelSftp
        chan.connect()
        chan.put(from.path, to)
    }
}