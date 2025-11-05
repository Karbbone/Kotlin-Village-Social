package com.example.mobile.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

private val Context.dataStore by preferencesDataStore(name = "mobile_prefs")

class AuthRepository(private val context: Context) {
    private val ACCESS_TOKEN_KEY = stringPreferencesKey("access_token")
    private val USER_JSON_KEY = stringPreferencesKey("user_json")

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // expose DataStore-backed StateFlows
    val tokenState: StateFlow<String?> = context.dataStore.data
        .map { prefs -> prefs[ACCESS_TOKEN_KEY] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val userJsonState: StateFlow<String?> = context.dataStore.data
        .map { prefs -> prefs[USER_JSON_KEY] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun saveToken(accessToken: String, userJson: String?) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN_KEY] = accessToken
            if (userJson != null) prefs[USER_JSON_KEY] = userJson
        }
    }

    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN_KEY)
            prefs.remove(USER_JSON_KEY)
        }
    }
}
