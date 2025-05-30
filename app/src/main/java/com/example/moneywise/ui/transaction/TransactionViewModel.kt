package com.example.moneywise.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.dao.BanqueDao
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.data.entity.Utilisateur
import com.example.moneywise.services.BalanceUpdateService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class TransactionViewModel @Inject constructor(
    private val db: AppDatabase,
    private val banqueDao: BanqueDao,
    private val balanceUpdateService: BalanceUpdateService
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

                // 🔥 MISE À JOUR DU SOLDE avec le service
                val balanceUpdated = balanceUpdateService.updateUserBalance(
                    context = db.openHelper.writableDatabase.path.let {
                        // Récupérer le context depuis l'application
                        android.app.Application().applicationContext
                    },
                    userId = userId,
                    transactionType = type,
                    amount = amount
                )

                if (balanceUpdated) {
                    onSuccess()
                } else {
                    onError("Erreur lors de la mise à jour du solde")
                }

            } catch (e: Exception) {
                onError(e.message ?: "Erreur inconnue lors de l'ajout de la transaction")
            }
        }
    }

    /**
     * Recalcule le solde total de l'utilisateur
     */
    fun recalculateUserBalance(userId: Int, onComplete: (Double) -> Unit) {
        viewModelScope.launch {
            try {
                val newBalance = balanceUpdateService.recalculateBalance(
                    context = db.openHelper.writableDatabase.path.let {
                        android.app.Application().applicationContext
                    },
                    userId = userId
                )
                onComplete(newBalance)
            } catch (e: Exception) {
                onComplete(0.0)
            }
        }
    }
}
