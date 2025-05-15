package com.example.moneywise.ui.profile

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Utilisateur
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilViewModel(application: Application) : AndroidViewModel(application) {
    private val utilisateurDao = AppDatabase.getDatabase(application).utilisateurDao()

    private val _currentUser = MutableStateFlow<Utilisateur?>(null)
    val currentUser: StateFlow<Utilisateur?> = _currentUser

    // Propriétés pour le data binding
    val text: String
        get() = _currentUser.value?.let { "${it.nom} ${it.prenom}" } ?: ""

    val balance: String
        get() = _currentUser.value?.let { it.email } ?: ""

    init {
        loadUserData()
    }

    private fun loadUserData() {
        viewModelScope.launch {
            try {
                _currentUser.value = utilisateurDao.getAllUtilisateurs()
                    .firstOrNull()
                    ?.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace() // Très important pour voir l’erreur
                Log.e("ProfilViewModel", "Erreur lors du chargement de l'utilisateur", e)
            }
        }
    }

    val fullName = currentUser.map { user ->
        user?.let { "${it.nom} ${it.prenom}" } ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")


    suspend fun updateUser(user: Utilisateur) {
        utilisateurDao.update(user)
        _currentUser.value = user
    }

    fun logout() {
        viewModelScope.launch {
            _currentUser.value = null
        }
    }
}