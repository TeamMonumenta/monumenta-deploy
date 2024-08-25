package com.playmonumenta.deployment.auth

import com.jcraft.jsch.JSch
import java.util.*

abstract class AuthProvider(val name: String, val shouldContinue: Boolean = false) {
    abstract fun tryProvider(jsch: JSch): Optional<String>
}