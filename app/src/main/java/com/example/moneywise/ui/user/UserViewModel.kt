package com.example.moneywise.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Utilisateur
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val db: AppDatabase
) : ViewModel() {

    fun getCurrentUser(): Flow<Utilisateur?> {
        return db.utilisateurDao().getAllUtilisateurs().map { it.firstOrNull() }
    }

    suspend fun updateUserBalance(userId: Int, newBalance: Double) {
        db.utilisateurDao().getUserById(userId)?.let { user ->
            db.utilisateurDao().update(user.copy(solde = newBalance))
        }
    }
}