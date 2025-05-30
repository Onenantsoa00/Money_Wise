package com.example.moneywise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.repository.UtilisateurRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val UtilisateurRepository: UtilisateurRepository) : ViewModel() {

    fun registerUser(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            val result = UtilisateurRepository.registerUser(nom, prenom, email, password)
            result.fold(
                onSuccess = { onResult(Result.success(true)) },
                onFailure = { onResult(Result.failure(it)) }
            )
        }
    }

    fun loginUser(
        email: String,
        password: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            val result = UtilisateurRepository.loginUser(email, password)
            result.fold(
                onSuccess = { onResult(Result.success(true)) },
                onFailure = { onResult(Result.failure(it)) }
            )
        }
    }
}