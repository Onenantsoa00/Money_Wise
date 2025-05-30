package com.example.moneywise.ui.profile

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Utilisateur
import com.example.moneywise.data.repository.UtilisateurRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilViewModel @Inject constructor(
    private val utilisateurRepository: UtilisateurRepository,
    application: Application
) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)

    // StateFlow pour l'utilisateur actuel
    private val _currentUser = MutableStateFlow<Utilisateur?>(null)
    val currentUser: StateFlow<Utilisateur?> = _currentUser.asStateFlow()

    // StateFlow pour les messages d'erreur
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // StateFlow pour l'état de chargement
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // LiveData pour les statistiques
    private val _transactionsCount = MutableLiveData<Int>()
    val transactionsCount: LiveData<Int> = _transactionsCount

    private val _projectsCount = MutableLiveData<Int>()
    val projectsCount: LiveData<Int> = _projectsCount

    private val _remindersCount = MutableLiveData<Int>()
    val remindersCount: LiveData<Int> = _remindersCount

    // Propriété calculée pour le nom complet
    val fullName: String
        get() = _currentUser.value?.let { "${it.nom} ${it.prenom}" } ?: ""

    init {
        loadCurrentUser()
    }

    /**
     * Charge les données de l'utilisateur par ID
     */
    fun loadUserData(userId: Int) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = utilisateurRepository.getUserById(userId)
                _currentUser.value = user
                if (user != null) {
                    loadStatistics()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors du chargement des données: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Charge l'utilisateur actuel (le premier dans la base)
     */
    private fun loadCurrentUser() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val user = utilisateurRepository.getFirstUtilisateur()
                _currentUser.value = user
                if (user != null) {
                    loadStatistics()
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors du chargement de l'utilisateur: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Met à jour les informations de l'utilisateur
     */
    suspend fun updateUser(user: Utilisateur): Result<Unit> {
        return try {
            _isLoading.value = true
            val result = utilisateurRepository.updateUser(user)
            if (result.isSuccess) {
                _currentUser.value = user
            }
            result
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Met à jour l'avatar de l'utilisateur
     */
    fun updateAvatar(avatarUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    val avatarPath = avatarUri?.toString()
                    val result = utilisateurRepository.updateUserAvatar(currentUser.id, avatarPath)

                    if (result.isSuccess) {
                        // Mettre à jour l'utilisateur local
                        _currentUser.value = currentUser.copy(avatar = avatarPath)
                    } else {
                        _errorMessage.value = "Erreur lors de la mise à jour de l'avatar"
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors de la mise à jour de l'avatar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * 🔥 MÉTHODE CORRIGÉE - Charge les statistiques de l'utilisateur
     */
    private fun loadStatistics() {
        viewModelScope.launch {
            try {
                val currentUser = _currentUser.value
                if (currentUser != null) {
                    // Charger le nombre de transactions
                    val userTransactions = database.transactionDao().getTransactionsByUserId(currentUser.id)
                    _transactionsCount.value = userTransactions.size

                    // Charger le nombre de projets non terminés
                    val userProjects = database.ProjetDao().getProjetsByUserId(currentUser.id)
                    val unfinishedProjects = userProjects.filter { it.progression < 100 }
                    _projectsCount.value = unfinishedProjects.size

                    // Charger le nombre de rappels (emprunts non remboursés)
                    val allEmprunts = database.empruntDao().getAllEmprunts()
                    val unpaidLoans = allEmprunts.filter { !it.estRembourse }
                    _remindersCount.value = unpaidLoans.size
                }
            } catch (e: Exception) {
                _errorMessage.value = "Erreur lors du chargement des statistiques: ${e.message}"
            }
        }
    }

    /**
     * Efface le message d'erreur
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Méthode de déconnexion (la logique réelle est dans le Fragment)
     */
    fun logout() {
        // Cette méthode peut être utilisée pour nettoyer les données du ViewModel
        _currentUser.value = null
        _transactionsCount.value = 0
        _projectsCount.value = 0
        _remindersCount.value = 0
    }
}
