package com.example.moneywise.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.data.entity.Historique
import com.example.moneywise.ai.TransactionClassifier
import com.example.moneywise.ai.NLPExtractor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class SmsListener : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsListener"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.originatingAddress ?: continue
                val body = message.messageBody ?: continue

                Log.d(TAG, "SMS received from: $sender")
                Log.d(TAG, "Message: $body")

                // Vérifier si le SMS provient d'un service mobile money
                if (isMobileMoneyMessage(sender)) {
                    Log.d(TAG, "Mobile money SMS detected")
                    processMobileMoneyMessage(context, body, sender)
                }
            }
        }
    }

    private fun isMobileMoneyMessage(sender: String): Boolean {
        val mobileMoneyKeywords = listOf(
            "mvola", "telma", "airtel", "orange", "money",
            "034000001", // MVola
            "032000001", // Orange Money
            "033000001"  // Airtel Money
        )
        return mobileMoneyKeywords.any { sender.contains(it, ignoreCase = true) }
    }

    private fun processMobileMoneyMessage(context: Context, messageBody: String, sender: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Créer les instances des analyseurs IA Kotlin avec factory methods
                val transactionClassifier = TransactionClassifier.create(context)
                val nlpExtractor = NLPExtractor.create(context)
                val balanceService = BalanceUpdateService.create()

                // Analyser le message avec l'IA Kotlin native
                val classification = transactionClassifier.classifyTransactionWithConfidence(messageBody, sender)
                val extractedDetails = nlpExtractor.extractTransactionDetails(messageBody, sender)

                // Vérifier si c'est une transaction valide
                val isValid = extractedDetails["is_valid"] as? Boolean ?: false
                val confidence = classification.confidence
                val transactionType = classification.type
                val amount = extractedDetails["amount"] as? Double ?: 0.0

                Log.d(TAG, "Transaction analysis: type=$transactionType, amount=$amount, confidence=$confidence, valid=$isValid")

                if (isValid && confidence > 0.5 && amount > 0) {
                    val db = AppDatabase.getDatabase(context)

                    // Récupérer ou créer la banque correspondante
                    val operatorName = getOperatorName(sender)
                    var banque = db.banqueDao().getByCode(getOperatorCode(sender))

                    if (banque == null) {
                        // Créer une nouvelle banque si elle n'existe pas
                        banque = Banque(
                            nom = operatorName,
                            code = getOperatorCode(sender),
                            type = "MOBILE_MONEY"
                        )
                        val banqueId = db.banqueDao().insert(banque)
                        banque = banque.copy(id = banqueId.toInt())
                    }

                    // Obtenir l'utilisateur actuel
                    val currentUser = db.utilisateurDao().getFirstUtilisateur()
                    val userId = currentUser?.id ?: 1

                    // Créer la transaction
                    val transaction = Transaction(
                        type = transactionType,
                        montants = amount.toString(),
                        date = Date(),
                        id_utilisateur = userId,
                        id_banque = banque.id
                    )
                    db.transactionDao().insertTransaction(transaction)

                    // 🔥 MISE À JOUR DU SOLDE - C'EST LA PARTIE IMPORTANTE !
                    val balanceUpdated = balanceService.updateUserBalance(
                        context = context,
                        userId = userId,
                        transactionType = transactionType,
                        amount = amount
                    )

                    if (balanceUpdated) {
                        Log.d(TAG, "✅ Solde mis à jour avec succès pour la transaction: $transactionType $amount")
                    } else {
                        Log.e(TAG, "❌ Échec de la mise à jour du solde")
                    }

                    // Ajouter à l'historique
                    val historique = Historique(
                        typeTransaction = transactionType,
                        montant = amount,
                        dateHeure = Date(),
                        motif = "Transaction Mobile Money automatique (IA Kotlin)",
                        details = buildString {
                            append("Opérateur: $operatorName")
                            append(" | Confiance: ${String.format("%.2f", confidence)}")
                            append(" | Ref: ${extractedDetails["reference"] as? String ?: "N/A"}")
                            append(" | SMS: ${messageBody.take(50)}...")
                            append(" | Solde mis à jour: ${if (balanceUpdated) "✅" else "❌"}")
                        }
                    )
                    db.historiqueDao().insert(historique)

                    Log.d(TAG, "🎉 Transaction saved successfully: $transactionType - $amount (confidence: $confidence)")
                } else {
                    Log.d(TAG, "❌ SMS not recognized as valid mobile money transaction (confidence: $confidence, amount: $amount)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Erreur lors du traitement du message: ${e.message}", e)
            }
        }
    }

    private fun getOperatorName(sender: String): String {
        val senderLower = sender.lowercase()
        return when {
            senderLower.contains("mvola") || senderLower.contains("telma") -> "Telma MVola"
            senderLower.contains("airtel") -> "Airtel Money"
            senderLower.contains("orange") -> "Orange Money"
            senderLower.contains("034000001") -> "Telma MVola"
            senderLower.contains("033000001") -> "Airtel Money"
            senderLower.contains("032000001") -> "Orange Money"
            else -> "Mobile Money"
        }
    }

    private fun getOperatorCode(sender: String): String {
        val senderLower = sender.lowercase()
        return when {
            senderLower.contains("mvola") || senderLower.contains("telma") || senderLower.contains("034000001") -> "MVOLA"
            senderLower.contains("airtel") || senderLower.contains("033000001") -> "AIRTEL"
            senderLower.contains("orange") || senderLower.contains("032000001") -> "ORANGE"
            else -> "UNKNOWN"
        }
    }
}
