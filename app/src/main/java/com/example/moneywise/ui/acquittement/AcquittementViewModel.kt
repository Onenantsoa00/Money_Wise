package com.example.moneywise.ui.acquittement

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.dao.AcquittementDao
import com.example.moneywise.data.entity.Acquittement
import kotlinx.coroutines.launch
import java.util.Date

class AcquittementViewModel(
    private val acquittementDao: AcquittementDao,
    private val database: AppDatabase
) : ViewModel() {

    val allAcquittements: LiveData<List<Acquittement>> = acquittementDao.getAllAcquittement()

    sealed class AcquittementResult {
        object Success : AcquittementResult()
        data class Error(val message: String) : AcquittementResult()
    }

    suspend fun insertAcquittement(
        nom: String,
        contact: String,
        montant: Double,
        dateCredit: Date,
        dateRemise: Date
    ): AcquittementResult {
        val userDao = database.utilisateurDao()
        val currentUser = userDao.getFirstUtilisateur()

        return if (currentUser != null) {
            if (currentUser.solde >= montant) {
                val nouvelAcquittement = Acquittement(
                    personne_acquittement = nom,
                    contacte = contact,
                    montant = montant,
                    date_crédit = dateCredit,
                    date_remise_crédit = dateRemise
                )

                val newBalance = currentUser.solde - montant
                userDao.update(currentUser.copy(solde = newBalance))
                acquittementDao.insertAcquittement(nouvelAcquittement)
                AcquittementResult.Success
            } else {
                AcquittementResult.Error("Solde insuffisant. Votre solde actuel est ${currentUser.solde} MGA")
            }
        } else {
            AcquittementResult.Error("Utilisateur non trouvé")
        }
    }

    fun rembourserAcquittement(acquittement: Acquittement) {
        viewModelScope.launch {
            val userDao = database.utilisateurDao()
            val currentUser = userDao.getFirstUtilisateur()

            currentUser?.let { user ->
                val newBalance = user.solde + acquittement.montant
                userDao.update(user.copy(solde = newBalance))
                acquittementDao.deleteAcquittement(acquittement)
            }
        }
    }
}