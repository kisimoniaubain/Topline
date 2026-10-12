package com.kisimoni.topline.data.local

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class UserPreferences(context: Context) {
    private companion object {
        const val PREFS_FILE_NAME = "topline_secure_prefs"
        const val TOKEN_KEY = "auth_token"
        const val CURRENT_USER_KEY = "current_user"
        const val DARK_MODE_KEY = "dark_mode"
        const val LANGUAGE_KEY = "language"
        const val PUSH_TOKEN_KEY = "push_token"
        const val PUSH_WELCOME_NOTIFICATION_SHOWN_KEY = "push_welcome_notification_shown"
    }

    private val prefs = try {
        val masterKeyAlias = "topline_master_key"
        MasterKey.Builder(context, masterKeyAlias)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    } catch (_: Exception) {
        context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
    }

    private val _authTokenFlow = MutableStateFlow(getToken())
    val authTokenFlow: StateFlow<String?> = _authTokenFlow.asStateFlow()
    private val _pushTokenFlow = MutableStateFlow(getPushToken())
    val pushTokenFlow: StateFlow<String?> = _pushTokenFlow.asStateFlow()

    private val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == TOKEN_KEY) {
            _authTokenFlow.value = getToken()
        } else if (key == PUSH_TOKEN_KEY) {
            _pushTokenFlow.value = getPushToken()
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun getToken(): String? = prefs.getString(TOKEN_KEY, null)

    fun saveAuthToken(token: String) {
        prefs.edit().putString(TOKEN_KEY, token).apply()
    }

    fun clearSession() {
        prefs.edit()
            .remove(TOKEN_KEY)
            .remove(CURRENT_USER_KEY)
            .apply()
    }

    fun getPushToken(): String? = prefs.getString(PUSH_TOKEN_KEY, null)

    fun savePushToken(token: String) {
        prefs.edit().putString(PUSH_TOKEN_KEY, token).apply()
        _pushTokenFlow.value = token
    }

    fun clearPushToken() {
        prefs.edit().remove(PUSH_TOKEN_KEY).apply()
        _pushTokenFlow.value = null
    }

    fun hasShownPushWelcomeNotification(): Boolean =
        prefs.getBoolean(PUSH_WELCOME_NOTIFICATION_SHOWN_KEY, false)

    fun markPushWelcomeNotificationShown() {
        prefs.edit().putBoolean(PUSH_WELCOME_NOTIFICATION_SHOWN_KEY, true).apply()
    }

    fun isDarkModeEnabled(): Boolean = prefs.getBoolean(DARK_MODE_KEY, false)

    fun saveDarkModeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(DARK_MODE_KEY, enabled).apply()
    }

    fun getLanguage(): String = prefs.getString(LANGUAGE_KEY, "English") ?: "English"

    fun saveLanguage(language: String) {
        prefs.edit().putString(LANGUAGE_KEY, language).apply()
    }
}
