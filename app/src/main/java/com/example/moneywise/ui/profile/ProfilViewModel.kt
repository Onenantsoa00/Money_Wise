package com.example.moneywise.ui.profile

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Utilisateur
import com.example.moneywise.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfilViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val utilisateurDao = database.utilisateurDao()
    private val transactionDao = database.transactionDao()
    private val projetDao = database.ProjetDao()
    private val empruntDao = database.empruntDao()
    private val acquittementDao = database.AcquittementDao()
    private val userRepository = UserRepository(utilisateurDao)

    private val _currentUser = MutableStateFlow<Utilisateur?>(null)
    val currentUser: StateFlow<Utilisateur?> = _currentUser

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Statistiques dynamiques
    val transactionsCount: LiveData<Int> = transactionDao.getTotalTransactionsCount()
    val unfinishedProjectsCount: LiveData<Int> = projetDao.getTotalUnfinishedProjectsCount()

    // Combinaison des emprunts et acquittements pour les rappels
    private val unpaidLoansCount: LiveData<Int> = empruntDao.getUnpaidLoansCount()
    private val totalAcquittementCount: LiveData<Int> = acquittementDao.getTotalAcquittementCount()

    val remindersCount: LiveData<Int> = MediatorLiveData<Int>().apply {
        var loansCount = 0
        var acquittementCount = 0

        addSource(unpaidLoansCount) { count ->
            loansCount = count ?: 0
            value = loansCount + acquittementCount
        }

        addSource(totalAcquittementCount) { count ->
            acquittementCount = count ?: 0
            value = loansCount + acquittementCount
        }
    }

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
                _isLoading.value = true
                _currentUser.value = utilisateurDao.getAllUtilisateurs()
                    .firstOrNull()
                    ?.firstOrNull()
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("ProfilViewModel", "Erreur lors du chargement de l'utilisateur", e)
                _errorMessage.value = "Erreur lors du chargement du profil"
            } finally {
                _isLoading.value = false
            }
        }
    }

    val fullName = currentUser.map { user ->
        user?.let { "${it.nom} ${it.prenom}" } ?: ""
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val avatarUri = currentUser.map { user ->
        user?.avatar
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    suspend fun updateUser(user: Utilisateur): Result<Unit> {
        return try {
            _isLoading.value = true
            val result = userRepository.updateUser(user)
            if (result.isSuccess) {
                _currentUser.value = user
                _errorMessage.value = null
            } else {
                _errorMessage.value = result.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour"
            }
            result
        } catch (e: Exception) {
            _errorMessage.value = e.message ?: "Erreur lors de la mise à jour"
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    fun updateAvatar(avatarUri: Uri?) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentUserValue = _currentUser.value
                if (currentUserValue != null) {
                    val avatarPath = avatarUri?.toString()
                    val result = userRepository.updateUserAvatar(currentUserValue.id, avatarPath)

                    if (result.isSuccess) {
                        _currentUser.value = currentUserValue.copy(avatar = avatarPath)
                        _errorMessage.value = null
                    } else {
                        _errorMessage.value = result.exceptionOrNull()?.message ?: "Erreur lors de la mise à jour de l'avatar"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Erreur lors de la mise à jour de l'avatar"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun logout() {
        viewModelScope.launch {
            _currentUser.value = null
        }
    }
}