package com.playmonumenta.deployment.auth

class EnvKeyProvider : FileKeyProvider("IdentityFileEnv") {
    override fun getIdentityFile(): String? {
        return System.getenv("IDENTITY_FILE")
    }
}