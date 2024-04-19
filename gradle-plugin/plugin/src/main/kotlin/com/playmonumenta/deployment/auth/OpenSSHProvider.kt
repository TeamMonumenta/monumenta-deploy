package com.playmonumenta.deployment.auth

import java.io.File

class OpenSSHProvider(private val keyName: String) : FileKeyProvider("\$HOME/.ssh/$keyName", true) {
    override fun getIdentityFile(): String? {
        return File(System.getProperty("user.home")).resolve(".ssh").resolve(keyName).path
    }
}