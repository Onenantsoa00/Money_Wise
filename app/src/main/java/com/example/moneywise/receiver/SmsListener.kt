package com.example.moneywise.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Historique
import com.example.moneywise.data.entity.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class MobileMoneyReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MobileMoneyReceiver"
    }

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received: ${intent.action}")

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in smsMessages) {
                val messageBody = sms.messageBody
                val sender = sms.displayOriginatingAddress

                Log.d(TAG, "SMS from: $sender")
                Log.d(TAG, "Message: $messageBody")

                if (isMobileMoneySender(sender)) {
                    Log.d(TAG, "Mobile money SMS detected")
                    processSMS(context, messageBody, sender)
                }
            }
        }
    }

    private fun processSMS(context: Context, messageBody: String, sender: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Accéder à la base de données
                val database = AppDatabase.getDatabase(context)
                val transactionDao = database.transactionDao()
                val historiqueDao = database.historiqueDao()
                val utilisateurDao = database.utilisateurDao()

                // Analyser le SMS
                val analysisResult = analyzeWithKotlinAI(messageBody, sender)

                if (analysisResult.isValid) {
                    // Obtenir l'utilisateur actuel
                    val currentUser = utilisateurDao.getFirstUtilisateur()
                    val userId = currentUser?.id ?: 1

                    // Enregistrer la transaction
                    val transaction = Transaction(
                        type = analysisResult.type,
                        montants = analysisResult.amount.toString(),
                        date = Date(),
                        id_utilisateur = userId,
                        id_banque = analysisResult.bankId
                    )
                    transactionDao.insertTransaction(transaction)

                    // Enregistrer dans l'historique
                    val historique = Historique(
                        typeTransaction = analysisResult.type,
                        montant = analysisResult.amount,
                        dateHeure = Date(),
                        motif = "Transaction automatique via SMS",
                        details = "SMS: ${analysisResult.originalMessage.take(100)}... | Ref: ${analysisResult.reference}"
                    )
                    historiqueDao.insert(historique)

                    Log.d(TAG, "Transaction saved successfully: ${analysisResult.type} - ${analysisResult.amount}")
                } else {
                    Log.d(TAG, "SMS not recognized as mobile money transaction")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error processing SMS", e)
            }
        }
    }

    private fun isMobileMoneySender(sender: String): Boolean {
        val mobileMoneySenders = listOf(
            "MVola", "AirtelMoney", "OrangeMoney",
            "Telma", "Airtel", "Orange"
        )
        return mobileMoneySenders.any { sender.contains(it, ignoreCase = true) }
    }

    private fun analyzeWithKotlinAI(messageBody: String, sender: String): SMSAnalysisResult {
        // Normaliser le message
        val messageLower = messageBody.lowercase()

        // Extraire le montant
        val amount = extractAmount(messageBody)

        // Extraire le type de transaction
        val transactionType = extractTransactionType(messageLower, sender)

        // Extraire le numéro de téléphone
        val phoneNumber = extractPhoneNumber(messageBody)

        // Extraire la référence
        val reference = extractReference(messageBody)

        // Déterminer le bankId
        val bankId = getBankIdFromSender(sender)

        // Vérifier si c'est une transaction valide
        val isValid = amount > 0 && transactionType != "AUTRE"

        return SMSAnalysisResult(
            isValid = isValid,
            type = transactionType,
            amount = amount,
            phoneNumber = phoneNumber,
            reference = reference,
            bankId = bankId,
            originalMessage = messageBody,
            sender = sender
        )
    }

    private fun extractAmount(message: String): Double {
        val amountRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA|ariary)""", RegexOption.IGNORE_CASE)
        val amountMatch = amountRegex.find(message)
        return amountMatch?.groupValues?.get(1)?.replace(",", ".")?.toDoubleOrNull() ?: 0.0
    }

    private fun extractTransactionType(message: String, sender: String): String {
        return when {
            message.contains("reçu") || message.contains("crédit") || message.contains("dépôt") -> "DEPOT"
            message.contains("envoyé") || message.contains("débit") || message.contains("retrait") -> "RETRAIT"
            message.contains("transfert") || message.contains("envoi") -> "TRANSFERT"
            else -> "AUTRE"
        }
    }

    private fun extractPhoneNumber(message: String): String {
        val phoneRegex = Regex("""(?:\+261|261|0)?[23][2-9]\d{7}""")
        val phoneMatch = phoneRegex.find(message)
        return phoneMatch?.value ?: ""
    }

    private fun extractReference(message: String): String {
        val referenceRegex = Regex("""(?:ref|référence|transaction)[\s:]*([A-Z0-9]+)""", RegexOption.IGNORE_CASE)
        val referenceMatch = referenceRegex.find(message)
        return referenceMatch?.groupValues?.get(1) ?: ""
    }

    private fun getBankIdFromSender(sender: String): Int {
        return when {
            sender.contains("MVola", true) || sender.contains("Telma", true) -> 1 // Telma
            sender.contains("Airtel", true) -> 2 // Airtel
            sender.contains("Orange", true) -> 3 // Orange
            else -> 1 // Par défaut
        }
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
    val sender: String
)
