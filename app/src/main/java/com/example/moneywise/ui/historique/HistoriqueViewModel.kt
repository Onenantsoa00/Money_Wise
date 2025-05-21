package com.example.moneywise.ui.historique

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Historique
import kotlinx.coroutines.launch
import java.util.Date

class HistoriqueViewModel(private val database: AppDatabase) : ViewModel() {
    val allHistorique: LiveData<List<Historique>> = database.historiqueDao().getAllHistorique()

    fun addHistorique(
        typeTransaction: String,
        montant: Double,
        dateHeure: Date,
        motif: String,
        details: String? = null
    ) {
        viewModelScope.launch {
            val historique = Historique(
                typeTransaction = typeTransaction,
                montant = montant,
                dateHeure = dateHeure,
                motif = motif,
                details = details
            )
            database.historiqueDao().insert(historique)
        }
    }

    suspend fun getResumeHistorique(): Triple<Double, Double, Double> {
        return try {
            val credits = database.historiqueDao().getTotalCredits()
            val debits = database.historiqueDao().getTotalDebits()
            Triple(credits, debits, credits - debits)
        } catch (e: Exception) {
            Triple(0.0, 0.0, 0.0)
        }
    }
}