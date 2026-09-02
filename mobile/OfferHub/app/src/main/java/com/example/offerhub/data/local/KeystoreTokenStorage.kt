package com.example.offerhub.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class KeystoreTokenStorage(
    context: Context,
    private val sessionTokenProvider: SessionTokenProvider
) : TokenStorage {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override suspend fun save(tokens: StoredTokens) = withContext(Dispatchers.IO) {
        preferences.edit {
            putString(ACCESS_TOKEN, encrypt(tokens.accessToken))
            putString(REFRESH_TOKEN, encrypt(tokens.refreshToken))
            putLong(EXPIRES_AT, tokens.expiresAtEpochSeconds)
            putString(USER_ID, tokens.userId)
            putString(USER_ROLE, tokens.userRole)
            putBoolean(PASSWORD_CHANGE_REQUIRED, tokens.passwordChangeRequired)
            if (tokens.phone != null) {
                putString(USER_PHONE, encrypt(tokens.phone))
            } else {
                remove(USER_PHONE)
            }
            remove(LEGACY_EXPIRES_IN)
        }
        sessionTokenProvider.update(tokens.accessToken)
    }

    override suspend fun read(): StoredTokens? = withContext(Dispatchers.IO) {
        val access = preferences.getString(ACCESS_TOKEN, null) ?: return@withContext null
        val refresh = preferences.getString(REFRESH_TOKEN, null) ?: return@withContext null
        val expiresAt = preferences.getLong(EXPIRES_AT, 0L)
        val userId = preferences.getString(USER_ID, null) ?: return@withContext null
        val userRole = preferences.getString(USER_ROLE, null) ?: return@withContext null
        if (expiresAt <= 0L) return@withContext null
        runCatching {
            val phone = preferences.getString(USER_PHONE, null)?.let(::decrypt)
            val passwordChangeRequired = preferences.getBoolean(PASSWORD_CHANGE_REQUIRED, false)
            StoredTokens(
                decrypt(access),
                decrypt(refresh),
                expiresAt,
                userId,
                userRole,
                phone,
                passwordChangeRequired
            )
        }.getOrNull()?.also { tokens ->
            sessionTokenProvider.update(tokens.accessToken)
        }
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.edit { clear() }
        sessionTokenProvider.clear()
    }

    private fun encrypt(value: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val payload = Base64.encodeToString(cipher.doFinal(value.toByteArray()), Base64.NO_WRAP)
        return "$iv:$payload"
    }

    private fun decrypt(value: String): String {
        val (iv, payload) = value.split(':', limit = 2)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
        )
        return cipher.doFinal(Base64.decode(payload, Base64.NO_WRAP)).decodeToString()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE).run {
            init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build()
            )
            generateKey()
        }
    }

    private companion object {
        const val PREFS_NAME = "secure_auth_tokens"
        const val ACCESS_TOKEN = "access_token"
        const val REFRESH_TOKEN = "refresh_token"
        const val EXPIRES_AT = "expires_at_epoch_seconds"
        const val LEGACY_EXPIRES_IN = "expires_in"
        const val USER_ID = "user_id"
        const val USER_ROLE = "user_role"
        const val USER_PHONE = "user_phone"
        const val PASSWORD_CHANGE_REQUIRED = "password_change_required"
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "offerhub_auth_tokens"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}
