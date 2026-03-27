package com.bimoraai.brahm.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "brahm_prefs")

@Singleton
class TokenDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ACCESS_TOKEN     = stringPreferencesKey("access_token")
    private val REFRESH_TOKEN    = stringPreferencesKey("refresh_token")
    private val USER_ID          = stringPreferencesKey("user_id")
    private val USER_PLAN        = stringPreferencesKey("user_plan")
    private val CHAT_SESSION_ID  = stringPreferencesKey("chat_session_id")
    private val RECENT_TOOLS     = stringPreferencesKey("recent_tools")  // comma-separated routes, max 4
    private val THEME_MODE       = stringPreferencesKey("theme_mode")    // "light" | "dark" | "system"

    val accessToken:    Flow<String?> = context.dataStore.data.map { it[ACCESS_TOKEN] }
    val refreshToken:   Flow<String?> = context.dataStore.data.map { it[REFRESH_TOKEN] }
    val userId:         Flow<String?> = context.dataStore.data.map { it[USER_ID] }
    val userPlan:       Flow<String?> = context.dataStore.data.map { it[USER_PLAN] }
    val chatSessionId:  Flow<String?> = context.dataStore.data.map { it[CHAT_SESSION_ID] }
    val recentTools:    Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[RECENT_TOOLS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.take(4)
            ?: emptyList()
    }
    val themeMode: Flow<String> = context.dataStore.data.map { it[THEME_MODE] ?: "system" }

    suspend fun saveChatSessionId(id: String) {
        context.dataStore.edit { it[CHAT_SESSION_ID] = id }
    }

    suspend fun clearChatSessionId() {
        context.dataStore.edit { it.remove(CHAT_SESSION_ID) }
    }

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN]  = accessToken
            prefs[REFRESH_TOKEN] = refreshToken
        }
    }

    suspend fun saveUserId(id: String, plan: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ID]   = id
            prefs[USER_PLAN] = plan
        }
    }

    /** Prepend [route] to the recent-tools list (max 4 entries, newest first). */
    suspend fun pushRecentTool(route: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[RECENT_TOOLS]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList()
            val updated = (listOf(route) + current)
                .distinct()
                .take(4)
            prefs[RECENT_TOOLS] = updated.joinToString(",")
        }
    }

    suspend fun saveThemeMode(mode: String) {
        context.dataStore.edit { it[THEME_MODE] = mode }
    }

    suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    @Provides
    @Singleton
    fun provideTokenDataStore(@ApplicationContext context: Context): TokenDataStore =
        TokenDataStore(context)
}
