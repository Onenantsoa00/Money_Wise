package com.example.moneywise.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Acquittement
import com.example.moneywise.data.entity.Emprunt
import com.example.moneywise.data.entity.Utilisateur
import kotlinx.coroutines.launch

class HomeViewModel(private val db: AppDatabase) : ViewModel() {

    // Données pour le dashboard
    val empruntsNonRembourses: LiveData<List<Emprunt>> = db.empruntDao().getEmpruntsNonRembourses()
    val acquittementsRecents: LiveData<List<Acquittement>> = db.AcquittementDao().getRecentAcquittements()
    val soldeUtilisateur: LiveData<Double?> = db.utilisateurDao().getSoldeUtilisateur()

    // Récupère les données principales pour le dashboard
    fun refreshData() {
        viewModelScope.launch {
            // Les LiveData se mettront à jour automatiquement
        }
    }

    // Pour mettre à jour le nom de l'utilisateur affiché
    fun getNomUtilisateur(): LiveData<String?> {
        return db.utilisateurDao().getNomUtilisateur()
    }
}