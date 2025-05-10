package com.example.moneywise.ui.project

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProjectViewModel : ViewModel() {

    // Données des projets
    private val _activeProjects = MutableLiveData<Int>().apply { value = 12 }
    private val _ongoingProjects = MutableLiveData<Int>().apply { value = 3 }
    private val _completedProjects = MutableLiveData<Int>().apply { value = 7 }

    private val _projects = MutableLiveData<List<Project>>().apply {
        value = listOf(
            Project(
                id = 1,
                name = "Construction de maison",
                totalBudget = "5,000,000 MGA",
                usedBudget = "3,250,000 MGA",
                progress = 65,
                deadline = "15/06/2023"
            ),
            Project(
                id = 2,
                name = "Achat de voiture",
                totalBudget = "2,500,000 MGA",
                usedBudget = "2,500,000 MGA",
                progress = 100,
                deadline = "10/05/2023"
            )
        )
    }

    // LiveData exposés
    val activeProjects: LiveData<Int> = _activeProjects
    val ongoingProjects: LiveData<Int> = _ongoingProjects
    val completedProjects: LiveData<Int> = _completedProjects
    val projects: LiveData<List<Project>> = _projects

    // Événements
    private val _navigateToAddProject = MutableLiveData<Boolean>()
    val navigateToAddProject: LiveData<Boolean> = _navigateToAddProject

    private val _navigateToProjectDetail = MutableLiveData<Int>()
    val navigateToProjectDetail: LiveData<Int> = _navigateToProjectDetail

    fun onAddProjectClicked() {
        _navigateToAddProject.value = true
    }

    fun onAddProjectComplete() {
        _navigateToAddProject.value = false
    }

    fun onProjectClicked(projectId: Int) {
        _navigateToProjectDetail.value = projectId
    }

    fun onProjectDetailComplete() {
        _navigateToProjectDetail.value = null
    }

    // Classe de données pour les projets
    data class Project(
        val id: Int,
        val name: String,
        val totalBudget: String,
        val usedBudget: String,
        val progress: Int,
        val deadline: String
    )
}