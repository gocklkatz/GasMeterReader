package com.example.greetingcard.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private val tokenKey = stringPreferencesKey("auth_token")

    val tokenFlow: Flow<String?> = dataStore.data.map { it[tokenKey] }

    suspend fun saveToken(token: String) {
        dataStore.edit { it[tokenKey] = token }
    }

    suspend fun getToken(): String? = tokenFlow.firstOrNull()

    suspend fun clearToken() {
        dataStore.edit { it.remove(tokenKey) }
    }
}
