package com.kakuma.resourcelink.core.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores tokens encrypted with an AES-256-GCM key that lives in the Android Keystore (hardware-backed
 * where available). Only ciphertext is written to disk; the key never leaves the Keystore.
 */
@Singleton
class KeystoreTokenStore @Inject constructor(
    @ApplicationContext context: Context,
) : TokenStore {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val lock = Any()

    @Volatile
    private var cached: AuthTokens? = load()

    private val loggedIn = MutableStateFlow(cached != null)
    override val isLoggedIn: StateFlow<Boolean> = loggedIn.asStateFlow()

    override fun tokens(): AuthTokens? = cached

    override fun save(tokens: AuthTokens) = synchronized(lock) {
        prefs.edit {
            putString(KEY_ACCESS, encrypt(tokens.access))
            putString(KEY_REFRESH, encrypt(tokens.refresh))
        }
        cached = tokens
        loggedIn.value = true
    }

    override fun clear() = synchronized(lock) {
        prefs.edit { clear() }
        cached = null
        loggedIn.value = false
    }

    private fun load(): AuthTokens? = try {
        val access = prefs.getString(KEY_ACCESS, null)?.let(::decrypt)
        val refresh = prefs.getString(KEY_REFRESH, null)?.let(::decrypt)
        if (access != null && refresh != null) AuthTokens(access, refresh) else null
    } catch (e: Exception) {
        // Key invalidated (e.g. device restored from backup): drop the unreadable session.
        Log.w(TAG, "Stored session could not be decrypted; signing out", e)
        prefs.edit { clear() }
        null
    }

    private fun key(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val payload = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): String {
        val payload = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = payload.copyOfRange(0, IV_LENGTH)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        }
        return String(cipher.doFinal(payload, IV_LENGTH, payload.size - IV_LENGTH), Charsets.UTF_8)
    }

    private companion object {
        const val TAG = "TokenStore"
        const val PREFS = "secure_session"
        const val KEY_ACCESS = "a"
        const val KEY_REFRESH = "r"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "kakuma_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
    }
}
