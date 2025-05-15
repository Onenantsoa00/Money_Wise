package com.example.moneywise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.data.dao.UtilisateurDao
import com.example.moneywise.data.repository.UserRepository

class AuthViewModelFactory(private val utilisateurDao: UtilisateurDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(UserRepository(utilisateurDao)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}