package com.example.moneywise.ui.acquittement

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.dao.AcquittementDao
import com.example.moneywise.data.entity.Acquittement
import kotlinx.coroutines.launch
import java.util.Date

class AcquittementViewModel(private val acquittementDao: AcquittementDao) : ViewModel() {

    val allAcquittements: LiveData<List<Acquittement>> = acquittementDao.getAllAcquittement()

    fun insertAcquittement(
        nom: String,
        contact: String,
        montant: Double,
        dateCredit: Date,
        dateRemise: Date
    ) {
        viewModelScope.launch {
            val nouvelAcquittement = Acquittement(
                personne_acquittement = nom,
                contacte = contact,
                montant = montant,
                date_crédit = dateCredit,
                date_remise_crédit = dateRemise
            )
            acquittementDao.insertAcquittement(nouvelAcquittement)
        }
    }
}