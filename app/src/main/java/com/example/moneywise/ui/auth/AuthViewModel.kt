package com.example.moneywise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.entity.Utilisateur
import com.example.moneywise.data.repository.UtilisateurRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val utilisateurRepository: UtilisateurRepository
) : ViewModel() {

    fun registerUser(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        onResult: (Result<Utilisateur>) -> Unit
    ) {
        viewModelScope.launch {
            val result = utilisateurRepository.registerUser(nom, prenom, email, password)
            onResult(result)
        }
    }

    fun loginUser(
        email: String,
        password: String,
        onResult: (Result<Utilisateur>) -> Unit
    ) {
        viewModelScope.launch {
            val result = utilisateurRepository.loginUser(email, password)
            onResult(result)
        }
    }

    // 🔥 NOUVELLES MÉTHODES AJOUTÉES POUR MOT DE PASSE OUBLIÉ
    fun checkUserExists(email: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = utilisateurRepository.checkUserExists(email)
            callback(result)
        }
    }

    fun resetPassword(email: String, newPassword: String, callback: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = utilisateurRepository.resetPassword(email, newPassword)
            callback(result)
        }
    }

    fun logoutUser() {
        // Cette méthode sera utilisée par le ProfilFragment
        // La déconnexion est gérée par le SessionManager
    }
}