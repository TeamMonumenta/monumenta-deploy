package com.playmonumenta.deployment.auth

import com.jcraft.jsch.JSch
import java.io.File
import java.util.*

abstract class FileKeyProvider(name: String, shouldContinue: Boolean = false) : AuthProvider(name, shouldContinue) {
    abstract fun getIdentityFile(): String?

    override fun tryProvider(jsch: JSch): Optional<String> {
        val identityPath = getIdentityFile()
        val identityFilePassword = System.getenv("IDENTITY_FILE_PASSWORD") ?: ""

        if (identityPath == null)
            return Optional.of("no path specified")

        val identityFile = File(identityPath)

        if (!identityFile.exists()) {
            return Optional.of("cannot find identity file at '$identityPath'")
        }

        try {
            jsch.addIdentity(identityPath, identityFilePassword)
        } catch (e: Exception) {
            return Optional.of("failed to parse identity '$identityPath': ${e.message}")
        }

        return Optional.empty()
    }
}