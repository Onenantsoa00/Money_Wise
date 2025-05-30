package com.example.moneywise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.entity.Utilisateur
import com.example.moneywise.data.repository.UtilisateurRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val utilisateurRepository: UtilisateurRepository) : ViewModel() {

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

    fun logoutUser() {
        // Cette méthode sera utilisée par le ProfilFragment
        // La déconnexion est gérée par le SessionManager
    }
}
