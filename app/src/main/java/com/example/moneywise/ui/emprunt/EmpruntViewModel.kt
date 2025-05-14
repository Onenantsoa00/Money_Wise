package com.example.moneywise.ui.emprunt

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.dao.EmpruntDao
import com.example.moneywise.data.entity.Emprunt
import kotlinx.coroutines.launch
import java.util.Date

class EmpruntViewModel(private val empruntDao: EmpruntDao) : ViewModel() {
    val allEmprunts: LiveData<List<Emprunt>> = empruntDao.getAllEmprunt()

    fun insertEmprunt(
        nom: String,
        contact: String,
        montant: Double,
        dateEmprunt: Date,
        dateRemboursement: Date
    ) {
        if (nom.isBlank()) {
            throw IllegalArgumentException("Le nom ne peut pas être vide")
        }
        viewModelScope.launch {
            val nouvelEmprunt = Emprunt(
                nom_emprunte = nom,
                contacte = contact,
                montant = montant,
                date_emprunt = dateEmprunt,
                date_remboursement = dateRemboursement
            )
            empruntDao.insertEmprunt(nouvelEmprunt)
        }
    }
}