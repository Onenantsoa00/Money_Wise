package com.example.moneywise.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Acquittement
import com.example.moneywise.data.entity.Emprunt
import com.example.moneywise.data.entity.Projet
import kotlinx.coroutines.launch
import java.util.*

class HomeViewModel(private val db: AppDatabase) : ViewModel() {

    // Récupère les 5 emprunts non remboursés les plus récents
    val empruntsRecents: LiveData<List<Emprunt>> = db.empruntDao().getRecentEmpruntsNonRembourses()

    // Récupère les 5 acquittements les plus récents
    val acquittementsRecents: LiveData<List<Acquittement>> = db.AcquittementDao().getRecentAcquittements()

    // Solde de l'utilisateur
    val soldeUtilisateur: LiveData<Double?> = db.utilisateurDao().getSoldeUtilisateur()

    // Nom de l'utilisateur
    val nomUtilisateur: LiveData<String?> = db.utilisateurDao().getNomUtilisateur()

    //pour récupérer les projets
    val projetsRecents: LiveData<List<Projet>> = db.ProjetDao().getRecentProjects()

    fun refreshData() {
        viewModelScope.launch {
            // Les LiveData se mettront à jour automatiquement
        }
    }
}