package com.kakuma.resourcelink.feature.auth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session_user")

/** Caches the signed-in user's public profile (no tokens) for offline greeting and profile display. */
@Singleton
class SessionUserStore @Inject constructor(
    @ApplicationContext private val context: Context,
    moshi: Moshi,
) {
    private val adapter = moshi.adapter(UserDto::class.java)

    val user: Flow<UserDto?> = context.sessionStore.data.map { prefs ->
        prefs[KEY]?.let { runCatching { adapter.fromJson(it) }.getOrNull() }
    }

    suspend fun save(user: UserDto) {
        context.sessionStore.edit { it[KEY] = adapter.toJson(user) }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }

    private companion object { val KEY = stringPreferencesKey("user") }
}
