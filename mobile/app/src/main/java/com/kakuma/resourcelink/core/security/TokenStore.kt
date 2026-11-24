package com.kakuma.resourcelink.core.security

import kotlinx.coroutines.flow.StateFlow

data class AuthTokens(val access: String, val refresh: String)

/** Secure storage for JWTs. Implementations must never persist tokens in plain text. */
interface TokenStore {
    val isLoggedIn: StateFlow<Boolean>
    fun tokens(): AuthTokens?
    fun save(tokens: AuthTokens)
    fun clear()
}
