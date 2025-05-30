package com.example.moneywise.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Gestionnaire de session pour gérer l'état de connexion de l'utilisateur
 */
class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "moneywise_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
    }

    /**
     * Enregistre la session de l'utilisateur après une connexion réussie
     */
    fun createLoginSession(userId: Int, email: String) {
        val editor = sharedPreferences.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putInt(KEY_USER_ID, userId)
        editor.putString(KEY_USER_EMAIL, email)
        editor.apply()
    }

    /**
     * Vérifie si l'utilisateur est connecté
     */
    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Récupère l'ID de l'utilisateur connecté
     */
    fun getUserId(): Int {
        return sharedPreferences.getInt(KEY_USER_ID, -1)
    }

    /**
     * Récupère l'email de l'utilisateur connecté
     */
    fun getUserEmail(): String? {
        return sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    /**
     * Déconnecte l'utilisateur en effaçant les données de session
     */
    fun logout() {
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()
    }
}
