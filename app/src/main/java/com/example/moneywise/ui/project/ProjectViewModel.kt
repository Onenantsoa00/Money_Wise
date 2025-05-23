package com.example.moneywise.ui.project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Projet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ProjectFilter {
    ALL, ACTIVE, ONGOING, COMPLETED
}

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val projetDao = database.ProjetDao()

    // Statistiques des projets
    val activeProjects: LiveData<Int> = projetDao.countActiveProjects()
    val ongoingProjects: LiveData<Int> = projetDao.countOngoingProjects()
    val completedProjects: LiveData<Int> = projetDao.countCompletedProjects()

    // Filtre actuel
    private val _currentFilter = MutableLiveData<ProjectFilter>()
    val currentFilter: LiveData<ProjectFilter> = _currentFilter

    // Liste des projets filtrée
    val projects: LiveData<List<Projet>> = _currentFilter.switchMap { filter ->
        when (filter) {
            ProjectFilter.ALL -> projetDao.getAllProjectsOrdered()
            ProjectFilter.ACTIVE -> projetDao.getActiveProjects()
            ProjectFilter.ONGOING -> projetDao.getOngoingProjects()
            ProjectFilter.COMPLETED -> projetDao.getCompletedProjects()
            else -> projetDao.getAllProjectsOrdered() // Clause else nécessaire pour le when
        }
    }

    // Événements
    private val _navigateToAddProject = MutableLiveData<Boolean>()
    val navigateToAddProject: LiveData<Boolean> = _navigateToAddProject

    private val _navigateToProjectDetail = MutableLiveData<Int?>()
    val navigateToProjectDetail: LiveData<Int?> = _navigateToProjectDetail

    // Événement pour afficher le dialogue d'investissement
    private val _showInvestDialog = MutableLiveData<Projet?>()
    val showInvestDialog: LiveData<Projet?> = _showInvestDialog

    // Solde de l'utilisateur pour validation
    private val _userBalance = MutableLiveData<Double>()
    val userBalance: LiveData<Double> = _userBalance

    init {
        // Initialiser le filtre par défaut
        _currentFilter.value = ProjectFilter.ALL
        loadUserBalance()
    }

    private fun loadUserBalance() {
        viewModelScope.launch {
            // Récupérer le solde de l'utilisateur depuis la base de données
            val utilisateurDao = database.utilisateurDao()
            val user = utilisateurDao.getFirstUtilisateur()
            _userBalance.value = user?.solde ?: 0.0
        }
    }

    // Fonctions de filtrage
    fun setFilter(filter: ProjectFilter) {
        _currentFilter.value = filter
    }

    fun showAllProjects() {
        setFilter(ProjectFilter.ALL)
    }

    fun showActiveProjects() {
        setFilter(ProjectFilter.ACTIVE)
    }

    fun showOngoingProjects() {
        setFilter(ProjectFilter.ONGOING)
    }

    fun showCompletedProjects() {
        setFilter(ProjectFilter.COMPLETED)
    }

    // Fonction pour calculer automatiquement la progression
    fun calculateProgression(montantActuel: Double, montantNecessaire: Double): Int {
        if (montantNecessaire <= 0) return 0
        return ((montantActuel / montantNecessaire) * 100).toInt().coerceIn(0, 100)
    }

    // Fonction pour insérer un nouveau projet
    fun insertProjet(
        nom: String,
        montantNecessaire: Double,
        montantActuel: Double,
        dateLimite: Date,
        idUtilisateur: Int = 1
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            // Calcul automatique de la progression
            val progression = calculateProgression(montantActuel, montantNecessaire)

            val projet = Projet(
                nom = nom,
                montant_necessaire = montantNecessaire,
                montant_actuel = montantActuel,
                progression = progression,
                date_limite = dateLimite,
                id_utilisateur = idUtilisateur
            )
            projetDao.insertProjet(projet)
        }
    }

    // Fonction pour investir dans un projet
    fun investInProject(
        projetId: Int,
        montantInvestissement: Double,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Récupérer l'utilisateur courant
                val utilisateurDao = database.utilisateurDao()
                val user = utilisateurDao.getFirstUtilisateur()
                if (user == null) {
                    onError("Aucun utilisateur trouvé")
                    return@launch
                }

                // Vérifier si l'utilisateur a assez d'argent
                if (user.solde < montantInvestissement) {
                    onError("Solde insuffisant")
                    return@launch
                }

                // Récupérer le projet
                val projet = projetDao.getProjetById(projetId)
                if (projet == null) {
                    onError("Projet non trouvé")
                    return@launch
                }

                // Vérifier si le montant d'investissement ne dépasse pas ce qui est nécessaire
                val montantRestantNecessaire = projet.montant_necessaire - projet.montant_actuel
                if (montantInvestissement > montantRestantNecessaire) {
                    onError("Le montant dépasse ce qui est nécessaire pour compléter le projet")
                    return@launch
                }

                // Mettre à jour le montant actuel du projet
                val nouveauMontantActuel = projet.montant_actuel + montantInvestissement

                // Calculer la nouvelle progression (en pourcentage)
                val nouvelleProgression = ((nouveauMontantActuel / projet.montant_necessaire) * 100).toInt()

                // Mettre à jour le projet
                projetDao.updateProjetMontantAndProgression(
                    projetId = projetId,
                    montantActuel = nouveauMontantActuel,
                    progression = nouvelleProgression
                )

                // Créer une transaction pour l'investissement
                val transactionDao = database.transactionDao()
                val transaction = com.example.moneywise.data.entity.Transaction(
                    type = "Investissement",
                    montants = montantInvestissement.toString(),
                    date = Date(),
                    id_utilisateur = user.id,
                    id_banque = 1
                )

                // Insérer la transaction
                transactionDao.insertTransaction(transaction)

                // Mettre à jour le solde de l'utilisateur
                val nouveauSolde = user.solde - montantInvestissement
                utilisateurDao.update(user.copy(solde = nouveauSolde))

                // Mettre à jour le solde local
                _userBalance.value = nouveauSolde

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Une erreur est survenue lors de l'investissement")
            }
        }
    }

    // Fonction pour convertir une chaîne de date en objet Date
    fun parseDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun onProjectClicked(projet: Projet) {
        _showInvestDialog.value = projet
    }

    fun onInvestDialogComplete() {
        _showInvestDialog.value = null
    }

    fun onAddProjectClicked() {
        _navigateToAddProject.value = true
    }

    fun onAddProjectComplete() {
        _navigateToAddProject.value = false
    }

    fun onProjectDetailComplete() {
        _navigateToProjectDetail.value = null
    }
}