package com.example.moneywise.ai

import android.content.Context
import android.util.Log
import com.example.moneywise.data.dao.TransactionDao
import com.example.moneywise.data.dao.HistoriqueDao
import com.example.moneywise.data.dao.UtilisateurDao
import com.example.moneywise.data.entity.Historique
import com.example.moneywise.data.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

class SMSProcessor @Inject constructor(
    private val context: Context,
    private val transactionDao: TransactionDao,
    private val historiqueDao: HistoriqueDao,
    private val utilisateurDao: UtilisateurDao,
    private val transactionClassifier: TransactionClassifier,
    private val nlpExtractor: NLPExtractor
) {
    companion object {
        private const val TAG = "SMSProcessor"
    }

    fun processSMS(messageBody: String, sender: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Processing SMS from: $sender")
                Log.d(TAG, "Message: $messageBody")

                // Utiliser le classifier pour déterminer le type
                val (transactionType, confidence) = transactionClassifier.classifyTransactionWithConfidence(messageBody, sender)

                // Utiliser l'extracteur pour obtenir les détails
                val extractedDetails = nlpExtractor.extractTransactionDetails(messageBody, sender)

                // Créer le résultat d'analyse
                val analysisResult = SMSAnalysisResult(
                    isValid = extractedDetails["is_valid"] as? Boolean ?: false,
                    type = transactionType,
                    amount = extractedDetails["amount"] as? Double ?: 0.0,
                    phoneNumber = extractedDetails["phone_number"] as? String ?: "",
                    reference = extractedDetails["reference"] as? String ?: "",
                    bankId = getBankIdFromSender(sender),
                    originalMessage = messageBody,
                    sender = sender,
                    confidence = confidence
                )

                if (analysisResult.isValid && confidence > 0.5) {
                    // Obtenir l'utilisateur actuel
                    val currentUser = utilisateurDao.getFirstUtilisateur()
                    val userId = currentUser?.id ?: 1

                    // Enregistrer la transaction
                    saveTransaction(analysisResult, userId)

                    // Enregistrer dans l'historique
                    saveToHistory(analysisResult)

                    Log.d(TAG, "Transaction saved successfully: ${analysisResult.type} - ${analysisResult.amount} (confidence: $confidence)")
                } else {
                    Log.d(TAG, "SMS not recognized as valid mobile money transaction (confidence: $confidence)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }

    private fun getBankIdFromSender(sender: String): Int {
        return when {
            sender.contains("MVola", true) || sender.contains("Telma", true) -> 1 // Telma
            sender.contains("Airtel", true) -> 2 // Airtel
            sender.contains("Orange", true) -> 3 // Orange
            else -> 1 // Par défaut
        }
    }

    private suspend fun saveTransaction(result: SMSAnalysisResult, userId: Int) {
        val transaction = Transaction(
            type = result.type,
            montants = result.amount.toString(),
            date = Date(),
            id_utilisateur = userId,
            id_banque = result.bankId
        )

        transactionDao.insertTransaction(transaction)
    }

    private suspend fun saveToHistory(result: SMSAnalysisResult) {
        val historique = Historique(
            typeTransaction = result.type,
            montant = result.amount,
            dateHeure = Date(),
            motif = "Transaction automatique via SMS (IA Kotlin)",
            details = "SMS: ${result.originalMessage.take(100)}... | Ref: ${result.reference} | Confiance: ${String.format("%.2f", result.confidence)}"
        )

        historiqueDao.insert(historique)
    }
}

data class SMSAnalysisResult(
    val isValid: Boolean,
    val type: String,
    val amount: Double,
    val phoneNumber: String,
    val reference: String,
    val bankId: Int,
    val originalMessage: String,
    val sender: String,
    val confidence: Double = 0.0
)
