package com.kakuma.resourcelink.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class UserPreferences(
    val languageTag: String? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val onboardingDone: Boolean = false,
    val languageChosen: Boolean = false,
)

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/** Lightweight settings only. Tokens are never stored here (see KeystoreTokenStore). */
interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setLanguage(tag: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setOnboardingDone()

    /** After sign-out the app opens on the welcome screen again (language and theme are kept). */
    suspend fun resetOnboarding()
}

@Singleton
class DataStorePreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PreferencesRepository {

    override val preferences: Flow<UserPreferences> = context.dataStore.data.map { p ->
        UserPreferences(
            languageTag = p[LANGUAGE],
            themeMode = p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            onboardingDone = p[ONBOARDING] ?: false,
            languageChosen = p[LANGUAGE] != null,
        )
    }

    override suspend fun setLanguage(tag: String) {
        context.dataStore.edit { it[LANGUAGE] = tag }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[THEME] = mode.name }
    }

    override suspend fun resetOnboarding() {
        context.dataStore.edit { it[ONBOARDING] = false }
    }

    override suspend fun setOnboardingDone() {
        context.dataStore.edit { it[ONBOARDING] = true }
    }

    private companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME = stringPreferencesKey("theme_mode")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
    }
}
