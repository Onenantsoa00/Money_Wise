package com.example.moneywise.ui.project

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Projet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val projetDao = database.ProjetDao()

    // Statistiques des projets
    val activeProjects: LiveData<Int> = projetDao.countActiveProjects()
    val ongoingProjects: LiveData<Int> = projetDao.countOngoingProjects()
    val completedProjects: LiveData<Int> = projetDao.countCompletedProjects()

    // Liste des projets
    val projects: LiveData<List<Projet>> = projetDao.getAllProjet()

    // Événements
    private val _navigateToAddProject = MutableLiveData<Boolean>()
    val navigateToAddProject: LiveData<Boolean> = _navigateToAddProject

    private val _navigateToProjectDetail = MutableLiveData<Int>()
    val navigateToProjectDetail: LiveData<Int> = _navigateToProjectDetail

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

    // Fonction pour convertir une chaîne de date en objet Date
    fun parseDate(dateString: String): Date? {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun onProjectClicked(projet: Projet) {
        _navigateToProjectDetail.value = projet.id
    }

    fun onAddProjectClicked() {
        _navigateToAddProject.value = true
    }

    fun onAddProjectComplete() {
        _navigateToAddProject.value = false
    }

    fun onProjectDetailComplete() {
        _navigateToProjectDetail.value
    }
}