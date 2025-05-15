package com.example.moneywise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.repository.UserRepository
import kotlinx.coroutines.launch

class AuthViewModel(private val userRepository: UserRepository) : ViewModel() {

    fun registerUser(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        onResult: (Result<Boolean>) -> Unit
    ) {
        viewModelScope.launch {
            val result = userRepository.registerUser(nom, prenom, email, password)
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
            val result = userRepository.loginUser(email, password)
            result.fold(
                onSuccess = { onResult(Result.success(true)) },
                onFailure = { onResult(Result.failure(it)) }
            )
        }
    }
}