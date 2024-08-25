package com.playmonumenta.deployment.auth

import com.jcraft.jsch.AgentIdentityRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.PageantConnector
import com.playmonumenta.deployment.SshPlugin
import java.util.*

class PageantKeyProvider : AuthProvider("Pageant") {
    override fun tryProvider(jsch: JSch): Optional<String> {
        if (!System.getProperty("os.name").lowercase(Locale.getDefault()).contains("win")) {
            return Optional.of("pageant can only be used on windows")
        }

        try {
            jsch.identityRepository = AgentIdentityRepository(PageantConnector())
            return Optional.empty()
        } catch (e: Exception) {
            SshPlugin.LOGGER.debug("Failed to load Pageant: ", e)
            return Optional.of("failed to use PageantConnector: ${e.message}")
        }
    }
}
