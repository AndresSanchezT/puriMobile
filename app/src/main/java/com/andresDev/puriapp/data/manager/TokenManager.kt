package com.andresDev.puriapp.data.manager

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sharedPreferences =
        context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun saveToken(token: String) {
        sharedPreferences.edit().putString("auth_token", token).apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString("auth_token", null)
    }

    fun saveUserData(id: Long, username: String, role: String, nombre: String) {
        sharedPreferences.edit().apply {
            putLong("user_id", id)
            putString("username", username)
            putString("role", role)
            putString("nombre", nombre)
            apply()
        }
    }

    fun getUserId(): Long {
        return sharedPreferences.getLong("user_id", 0L)
    }

    fun getRole(): String? {
        return sharedPreferences.getString("role", null)
    }

    fun clearSession() {
        sharedPreferences.edit().clear().apply()
    }

    fun isLoggedIn(): Boolean {
        return getToken() != null
    }
}
