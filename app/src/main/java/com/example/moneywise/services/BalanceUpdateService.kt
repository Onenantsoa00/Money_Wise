package com.example.moneywise.services

import android.content.Context
import android.util.Log
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Utilisateur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BalanceUpdateService @Inject constructor() {

    companion object {
        private const val TAG = "BalanceUpdateService"

        /**
         * Factory method pour créer une instance sans injection Hilt
         */
        fun create(): BalanceUpdateService {
            return BalanceUpdateService()
        }
    }

    /**
     * Met à jour le solde de l'utilisateur en fonction du type de transaction
     */
    suspend fun updateUserBalance(
        context: Context,
        userId: Int,
        transactionType: String,
        amount: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val utilisateurDao = db.utilisateurDao()

            // Récupérer l'utilisateur actuel
            val user = utilisateurDao.getUserById(userId)
            if (user == null) {
                Log.e(TAG, "Utilisateur non trouvé avec ID: $userId")
                return@withContext false
            }

            // Calculer le nouveau solde
            val newBalance = calculateNewBalance(user.solde, transactionType, amount)

            // Mettre à jour le solde
            val updatedUser = user.copy(solde = newBalance)
            utilisateurDao.update(updatedUser)

            Log.d(TAG, "Solde mis à jour: ${user.solde} -> $newBalance (${transactionType}: $amount)")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors de la mise à jour du solde", e)
            return@withContext false
        }
    }

    /**
     * Calcule le nouveau solde en fonction du type de transaction
     */
    private fun calculateNewBalance(currentBalance: Double, transactionType: String, amount: Double): Double {
        return when (transactionType.uppercase()) {
            "DEPOT", "CREDIT", "RECU" -> {
                // Argent reçu = augmentation du solde
                currentBalance + amount
            }
            "RETRAIT", "DEBIT", "ENVOYE", "PAIEMENT" -> {
                // Argent envoyé/retiré = diminution du solde
                currentBalance - amount
            }
            "TRANSFERT" -> {
                // Pour les transferts, on considère que c'est une sortie d'argent
                currentBalance - amount
            }
            else -> {
                Log.w(TAG, "Type de transaction non reconnu: $transactionType")
                currentBalance // Pas de changement pour les types inconnus
            }
        }
    }

    /**
     * Recalcule le solde total basé sur toutes les transactions
     */
    suspend fun recalculateBalance(context: Context, userId: Int): Double = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val transactionDao = db.transactionDao()
            val utilisateurDao = db.utilisateurDao()

            // Récupérer toutes les transactions de l'utilisateur
            val transactions = transactionDao.getTransactionsByUserId(userId)

            // Calculer le solde total
            var totalBalance = 0.0
            transactions.forEach { transaction ->
                val amount = transaction.montants.toDoubleOrNull() ?: 0.0
                totalBalance = calculateNewBalance(totalBalance, transaction.type, amount)
            }

            // Mettre à jour le solde de l'utilisateur
            val user = utilisateurDao.getUserById(userId)
            user?.let {
                val updatedUser = it.copy(solde = totalBalance)
                utilisateurDao.update(updatedUser)
            }

            Log.d(TAG, "Solde recalculé pour l'utilisateur $userId: $totalBalance")
            return@withContext totalBalance

        } catch (e: Exception) {
            Log.e(TAG, "Erreur lors du recalcul du solde", e)
            return@withContext 0.0
        }
    }
}
