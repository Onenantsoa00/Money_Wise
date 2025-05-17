package com.example.moneywise.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.dao.BanqueDao
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.data.entity.Utilisateur
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val db: AppDatabase,
    private val banqueDao: BanqueDao
) : ViewModel() {

    suspend fun getCurrentUser(): Utilisateur? {
        return db.utilisateurDao().getFirstUtilisateur()
    }

    suspend fun getBanks(): List<String> {
        return banqueDao.getAllBanques()
            .first()
            .map { it.nom }
    }

    fun addTransaction(
        type: String,
        amount: Double,
        date: Date,
        userId: Int,
        bankId: Int?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                // Créer la transaction
                val transaction = Transaction(
                    type = type,
                    montants = amount.toString(),
                    date = date,
                    id_utilisateur = userId,
                    id_banque = bankId ?: 0
                )

                // Insérer la transaction
                db.transactionDao().insertTransaction(transaction)

                // Mettre à jour le solde de l'utilisateur
                val user = db.utilisateurDao().getUserById(userId)
                user?.let {
                    val newBalance = when (type) {
                        "Dépôt" -> it.solde + amount
                        "Retrait" -> it.solde - amount
                        else -> it.solde
                    }
                    db.utilisateurDao().update(it.copy(solde = newBalance))
                }

                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue lors de l'ajout de la transaction")
            }
        }
    }
}