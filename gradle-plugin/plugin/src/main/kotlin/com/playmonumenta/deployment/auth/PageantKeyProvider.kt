package com.playmonumenta.deployment.auth

import com.jcraft.jsch.AgentIdentityRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.PageantConnector
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
            return Optional.of("failed to use PageantConnector: ${e.message}")
        }
    }
}
