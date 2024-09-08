package com.playmonumenta.deployment.auth

import com.jcraft.jsch.AgentIdentityRepository
import com.jcraft.jsch.JSch
import com.jcraft.jsch.SSHAgentConnector
import com.playmonumenta.deployment.SshPlugin
import java.util.*

class SSHAgentKeyProvider : AuthProvider("SSHAgent") {
    override fun tryProvider(jsch: JSch): Optional<String> {
        if (System.getenv("SSH_AUTH_SOCK") == null) {
            return Optional.of("missing env variable SSH_AUTH_SOCK")
        }

        try {
            jsch.identityRepository = AgentIdentityRepository(SSHAgentConnector())
            return Optional.empty()
        } catch (e: Exception) {
            SshPlugin.LOGGER.debug("Failed to use SSHAgentConnector: ", e)
            return Optional.of("failed to use SSHAgentConnector: ${e.message}")
        }
    }
}
