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
     * @param context Le contexte de l'application
     * @param userId L'ID de l'utilisateur
     * @param transactionType Le type de transaction (DEPOT, RETRAIT, TRANSFERT, etc.)
     * @param amount Le montant de la transaction
     * @return true si la mise à jour a réussi, false sinon
     */
    suspend fun updateUserBalance(
        context: Context,
        userId: Int,
        transactionType: String,
        amount: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Début mise à jour solde: userId=$userId, type=$transactionType, amount=$amount")

            val db = AppDatabase.getDatabase(context)
            val utilisateurDao = db.utilisateurDao()

            // 🔥 UTILISATION DES MÉTHODES EXISTANTES DE VOTRE DAO
            val user = utilisateurDao.getUserById(userId) ?: utilisateurDao.getFirstUtilisateur()

            if (user == null) {
                Log.e(TAG, "❌ Utilisateur non trouvé avec ID: $userId")
                return@withContext false
            }

            Log.d(TAG, "👤 Utilisateur trouvé: ${user.nom} ${user.prenom}, solde actuel: ${user.solde}")

            // Calculer le nouveau solde
            val newBalance = calculateNewBalance(user.solde, transactionType, amount)

            // Vérifier que le nouveau solde est valide
            if (newBalance < 0 && transactionType.uppercase() in listOf("RETRAIT", "DEBIT", "ENVOYE", "PAIEMENT", "TRANSFERT")) {
                Log.w(TAG, "⚠️ Attention: Le nouveau solde sera négatif ($newBalance)")
                // On continue quand même car certains comptes peuvent être en découvert
            }

            // 🔥 UTILISATION DE VOTRE MÉTHODE updateBalance EXISTANTE
            utilisateurDao.updateBalance(userId, newBalance)

            Log.d(TAG, "✅ Solde mis à jour avec succès: ${user.solde} -> $newBalance (${transactionType}: $amount)")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la mise à jour du solde: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * 🔥 AMÉLIORATION: Calcule le nouveau solde en fonction du type de transaction avec plus de précision
     */
    private fun calculateNewBalance(currentBalance: Double, transactionType: String, amount: Double): Double {
        val type = transactionType.uppercase().trim()

        Log.d(TAG, "💰 Calcul nouveau solde: $currentBalance + ($type: $amount)")

        return when (type) {
            // 🔥 DÉPÔTS - Argent qui ENTRE dans le compte
            "DEPOT", "DÉPÔT", "CREDIT", "CRÉDIT", "RECU", "REÇU",
            "VERSEMENT", "RECHARGE", "RECHARGÉ", "AJOUTÉ", "CRÉDITÉ" -> {
                Log.d(TAG, "💵 Type DEPOT détecté - Ajout de $amount")
                currentBalance + amount
            }

            // 🔥 RETRAITS - Argent qui SORT du compte
            "RETRAIT", "DEBIT", "DÉBIT", "ENVOYE", "ENVOYÉ",
            "PAIEMENT", "PAYÉ", "DÉBITÉ", "PRÉLEVÉ", "RETIRÉ" -> {
                Log.d(TAG, "💸 Type RETRAIT détecté - Soustraction de $amount")
                currentBalance - amount
            }

            // 🔥 TRANSFERTS - Peut être entrant ou sortant selon le contexte
            "TRANSFERT", "TRANSFER", "ENVOI", "VIREMENT" -> {
                Log.d(TAG, "🔄 Type TRANSFERT détecté - Soustraction de $amount (par défaut)")
                // Par défaut, on considère un transfert comme une sortie
                // Cette logique peut être affinée selon le contexte du message
                currentBalance - amount
            }

            // 🔥 ACHATS - Argent qui sort
            "ACHAT", "ACHETÉ", "COMMANDE", "FACTURE", "PURCHASE" -> {
                Log.d(TAG, "🛒 Type ACHAT détecté - Soustraction de $amount")
                currentBalance - amount
            }

            // 🔥 TYPES SPÉCIAUX
            "FRAIS", "COMMISSION", "FEE" -> {
                Log.d(TAG, "💳 Type FRAIS détecté - Soustraction de $amount")
                currentBalance - amount
            }

            "REMBOURSEMENT", "REFUND" -> {
                Log.d(TAG, "💰 Type REMBOURSEMENT détecté - Ajout de $amount")
                currentBalance + amount
            }

            else -> {
                Log.w(TAG, "⚠️ Type de transaction non reconnu: '$type' - Aucun changement de solde")
                currentBalance // Pas de changement pour les types inconnus
            }
        }
    }

    /**
     * 🔥 AMÉLIORATION: Recalcule le solde total basé sur toutes les transactions
     * UTILISE VOS MÉTHODES DAO EXISTANTES
     */
    suspend fun recalculateBalance(context: Context, userId: Int): Double = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Début recalcul complet du solde pour userId: $userId")

            val db = AppDatabase.getDatabase(context)
            val transactionDao = db.transactionDao()
            val utilisateurDao = db.utilisateurDao()

            // 🔥 UTILISATION DE VOTRE MÉTHODE getTransactionsByUserId EXISTANTE
            val transactions = transactionDao.getTransactionsByUserId(userId)

            Log.d(TAG, "📊 ${transactions.size} transactions trouvées pour l'utilisateur $userId")

            // Calculer le solde total en partant de 0
            var totalBalance = 0.0
            var transactionCount = 0

            transactions.forEach { transaction ->
                val amount = try {
                    transaction.montants.replace(",", ".").replace("\\s".toRegex(), "").toDouble()
                } catch (e: NumberFormatException) {
                    Log.w(TAG, "⚠️ Montant invalide dans transaction ${transaction.id}: '${transaction.montants}'")
                    0.0
                }

                if (amount > 0) {
                    val oldBalance = totalBalance
                    totalBalance = calculateNewBalance(totalBalance, transaction.type, amount)
                    transactionCount++

                    Log.d(TAG, "📝 Transaction ${transaction.id}: ${transaction.type} $amount -> Solde: $oldBalance -> $totalBalance")
                }
            }

            Log.d(TAG, "💰 Solde final calculé: $totalBalance (basé sur $transactionCount transactions)")

            // 🔥 UTILISATION DE VOTRE MÉTHODE updateBalance EXISTANTE
            utilisateurDao.updateBalance(userId, totalBalance)
            Log.d(TAG, "✅ Solde utilisateur mis à jour à: $totalBalance")

            return@withContext totalBalance

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du recalcul du solde: ${e.message}", e)
            return@withContext 0.0
        }
    }

    /**
     * 🔥 NOUVELLE MÉTHODE: Vérifie la cohérence du solde
     * UTILISE VOS MÉTHODES DAO EXISTANTES
     */
    suspend fun validateBalance(context: Context, userId: Int): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val utilisateurDao = db.utilisateurDao()

            // Récupérer le solde actuel avec vos méthodes existantes
            val user = utilisateurDao.getUserById(userId) ?: utilisateurDao.getFirstUtilisateur()
            val currentBalance = user?.solde ?: 0.0

            // Recalculer le solde théorique
            val calculatedBalance = recalculateBalance(context, userId)

            // Comparer avec une tolérance de 0.01 pour les erreurs d'arrondi
            val isValid = kotlin.math.abs(currentBalance - calculatedBalance) < 0.01

            if (!isValid) {
                Log.w(TAG, "⚠️ Incohérence détectée - Solde actuel: $currentBalance, Calculé: $calculatedBalance")
            } else {
                Log.d(TAG, "✅ Solde cohérent: $currentBalance")
            }

            return@withContext isValid

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la validation du solde: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * 🔥 NOUVELLE MÉTHODE: Obtient le solde actuel de l'utilisateur
     * UTILISE VOS MÉTHODES DAO EXISTANTES
     */
    suspend fun getCurrentBalance(context: Context, userId: Int): Double = withContext(Dispatchers.IO) {
        try {
            val db = AppDatabase.getDatabase(context)
            val utilisateurDao = db.utilisateurDao()

            val user = utilisateurDao.getUserById(userId) ?: utilisateurDao.getFirstUtilisateur()
            val balance = user?.solde ?: 0.0

            Log.d(TAG, "💰 Solde actuel pour userId $userId: $balance")
            return@withContext balance

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la récupération du solde: ${e.message}", e)
            return@withContext 0.0
        }
    }

    /**
     * 🔥 NOUVELLE MÉTHODE: Réinitialise le solde à une valeur spécifique
     * UTILISE VOS MÉTHODES DAO EXISTANTES
     */
    suspend fun resetBalance(context: Context, userId: Int, newBalance: Double): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Réinitialisation du solde pour userId $userId à $newBalance")

            val db = AppDatabase.getDatabase(context)
            val utilisateurDao = db.utilisateurDao()

            // 🔥 UTILISATION DE VOTRE MÉTHODE updateBalance EXISTANTE
            utilisateurDao.updateBalance(userId, newBalance)
            Log.d(TAG, "✅ Solde réinitialisé à: $newBalance")
            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la réinitialisation du solde: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * 🔥 NOUVELLE MÉTHODE: Utilise vos méthodes addToBalance et subtractFromBalance existantes
     */
    suspend fun updateBalanceDirectly(
        context: Context,
        userId: Int,
        transactionType: String,
        amount: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "🔄 Mise à jour directe du solde: userId=$userId, type=$transactionType, amount=$amount")

            val db = AppDatabase.getDatabase(context)
            val utilisateurDao = db.utilisateurDao()

            val type = transactionType.uppercase().trim()

            when (type) {
                "DEPOT", "DÉPÔT", "CREDIT", "CRÉDIT", "RECU", "REÇU",
                "VERSEMENT", "RECHARGE", "RECHARGÉ", "AJOUTÉ", "CRÉDITÉ",
                "REMBOURSEMENT", "REFUND" -> {
                    // 🔥 UTILISATION DE VOTRE MÉTHODE addToBalance EXISTANTE
                    utilisateurDao.addToBalance(userId, amount)
                    Log.d(TAG, "✅ Ajout de $amount au solde")
                }

                "RETRAIT", "DEBIT", "DÉBIT", "ENVOYE", "ENVOYÉ",
                "PAIEMENT", "PAYÉ", "DÉBITÉ", "PRÉLEVÉ", "RETIRÉ",
                "TRANSFERT", "TRANSFER", "ENVOI", "VIREMENT",
                "ACHAT", "ACHETÉ", "COMMANDE", "FACTURE", "PURCHASE",
                "FRAIS", "COMMISSION", "FEE" -> {
                    // 🔥 UTILISATION DE VOTRE MÉTHODE subtractFromBalance EXISTANTE
                    utilisateurDao.subtractFromBalance(userId, amount)
                    Log.d(TAG, "✅ Soustraction de $amount du solde")
                }

                else -> {
                    Log.w(TAG, "⚠️ Type de transaction non reconnu: '$type' - Aucune modification")
                    return@withContext false
                }
            }

            return@withContext true

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la mise à jour directe du solde: ${e.message}", e)
            return@withContext false
        }
    }
}
