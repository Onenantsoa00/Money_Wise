package com.example.moneywise.ai

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import java.util.regex.Pattern

@Singleton
class NLPExtractor @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "NLPExtractor"

        /**
         * Factory method pour créer une instance sans injection Hilt
         */
        fun create(context: Context): NLPExtractor {
            return NLPExtractor(context)
        }
    }

    /**
     * Extrait les détails d'une transaction à partir d'un message SMS
     */
    fun extractTransactionDetails(message: String, sender: String = ""): Map<String, Any> {
        Log.d(TAG, "Extracting transaction details from: $message")

        val result = mutableMapOf<String, Any>()

        try {
            // Déterminer le provider
            val provider = getProviderFromSender(sender)
            result["provider"] = provider

            // Extraire le type de transaction - LOGIQUE AMÉLIORÉE
            val transactionType = extractTransactionType(message.lowercase(), provider)
            result["transaction_type"] = transactionType

            // Extraire le montant
            val amount = extractAmount(message)
            result["amount"] = amount

            // Extraire le numéro de téléphone
            val phoneNumber = extractPhoneNumber(message)
            result["phone_number"] = phoneNumber

            // Extraire la référence
            val reference = extractReference(message)
            result["reference"] = reference

            // Calculer la confiance - LOGIQUE AMÉLIORÉE
            val confidence = calculateConfidence(message, transactionType, amount, provider)
            result["confidence"] = confidence

            // 🔥 LOGIQUE DE VALIDATION AMÉLIORÉE
            val isValid = isValidTransaction(message, transactionType, amount, confidence, provider)
            result["is_valid"] = isValid

            Log.d(TAG, "Extraction result: $result")

        } catch (e: Exception) {
            Log.e(TAG, "Error extracting transaction details", e)
            result["is_valid"] = false
            result["error"] = e.message ?: "Unknown error"
        }

        return result
    }

    /**
     * 🔥 NOUVELLE LOGIQUE DE VALIDATION PLUS INTELLIGENTE
     */
    private fun isValidTransaction(
        message: String,
        transactionType: String,
        amount: Double,
        confidence: Double,
        provider: String
    ): Boolean {
        val messageLower = message.lowercase()

        // Critères de base
        if (amount <= 0) {
            Log.d(TAG, "Invalid: amount <= 0")
            return false
        }

        if (provider == "unknown") {
            Log.d(TAG, "Invalid: unknown provider")
            return false
        }

        // 🔥 LOGIQUE SPÉCIALE POUR MVOLA "envoye a"
        if (provider == "mvola" && messageLower.contains("envoye a")) {
            Log.d(TAG, "Valid: MVola 'envoye a' pattern detected")
            return true
        }

        // Accepter si le type n'est pas "AUTRE" et qu'on a un montant
        if (transactionType != "AUTRE" && amount > 0) {
            Log.d(TAG, "Valid: good type ($transactionType) and amount ($amount)")
            return true
        }

        // Accepter si on a des mots-clés mobile money même avec type "AUTRE"
        val mobileMoneyKeywords = listOf("mvola", "airtel money", "orange money", "solde", "ref")
        if (mobileMoneyKeywords.any { messageLower.contains(it) } && amount > 0) {
            Log.d(TAG, "Valid: mobile money keywords found with amount")
            return true
        }

        Log.d(TAG, "Invalid: no criteria met (type=$transactionType, amount=$amount, confidence=$confidence)")
        return false
    }

    private fun getProviderFromSender(sender: String): String {
        val senderLower = sender.lowercase()
        return when {
            senderLower.contains("mvola") || senderLower.contains("telma") -> "mvola"
            senderLower.contains("airtel") -> "airtel"
            senderLower.contains("orange") -> "orange"
            else -> "unknown"
        }
    }

    private fun extractTransactionType(message: String, provider: String): String {
        // 🔥 LOGIQUE SPÉCIALE POUR MVOLA
        if (provider == "mvola" && message.contains("envoye a")) {
            return "RETRAIT"
        }

        // Patterns spécifiques par provider - AMÉLIORÉS
        val patterns = mapOf(
            "mvola" to mapOf(
                "DEPOT" to listOf("reçu", "crédit", "dépôt", "versement", "rechargé", "recharge"),
                "RETRAIT" to listOf("envoyé", "envoye", "débit", "retrait", "retiré", "payé", "envoye a"),
                "TRANSFERT" to listOf("transfert", "envoi", "transféré")
            ),
            "airtel" to mapOf(
                "DEPOT" to listOf("received", "crédit", "reçu", "deposit", "rechargé"),
                "RETRAIT" to listOf("sent", "débit", "envoyé", "withdrawal", "payé", "sent to"),
                "TRANSFERT" to listOf("transfer", "transfert", "envoi")
            ),
            "orange" to mapOf(
                "DEPOT" to listOf("reçu", "crédit", "dépôt", "versement", "rechargé"),
                "RETRAIT" to listOf("envoyé", "débit", "retrait", "retiré", "payé", "envoyé à"),
                "TRANSFERT" to listOf("transfert", "envoi", "transféré")
            )
        )

        val providerPatterns = patterns[provider] ?: patterns["mvola"]!!

        for ((type, keywords) in providerPatterns) {
            if (keywords.any { message.contains(it) }) {
                return type
            }
        }

        return "AUTRE"
    }

    private fun extractAmount(message: String): Double {
        // Patterns pour les montants avec priorité - AMÉLIORÉS
        val amountPatterns = listOf(
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:Ar|MGA|ariary)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:ar|mga)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:montant|amount|somme)[\\s:]*(\\d+(?:[.,]\\d+)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:francs?|fr)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{2,}(?:[.,]\\d+)?)") // Montants de 2 chiffres ou plus
        )

        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                val amountStr = matcher.group(1).replace(",", ".")
                try {
                    val amount = amountStr.toDouble()
                    if (amount > 0) return amount
                } catch (e: NumberFormatException) {
                    continue
                }
            }
        }

        return 0.0
    }

    private fun extractPhoneNumber(message: String): String {
        val phonePatterns = listOf(
            Pattern.compile("(?:\\+261|261|0)?[23][2-9]\\d{7}"),
            Pattern.compile("(?:de|from|to|vers|à)[\\s:]*(\\+?261[23][2-9]\\d{7})"),
            Pattern.compile("(?:de|from|to|vers|à)[\\s:]*(0[23][2-9]\\d{7})")
        )

        for (pattern in phonePatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return if (matcher.groupCount() > 0) matcher.group(1) else matcher.group(0)
            }
        }

        return ""
    }

    private fun extractReference(message: String): String {
        val refPatterns = listOf(
            Pattern.compile("(?:ref|référence|transaction|id|code)[\\s:]*([A-Z0-9]{4,})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("([A-Z]{2,}\\d{4,})"),
            Pattern.compile("(\\d{6,})") // Codes numériques de 6 chiffres ou plus
        )

        for (pattern in refPatterns) {
            val matcher = pattern.matcher(message)
            if (matcher.find()) {
                return matcher.group(1)
            }
        }

        return ""
    }

    private fun calculateConfidence(message: String, transactionType: String, amount: Double, provider: String): Double {
        var confidence = 0.0
        val messageLower = message.lowercase()

        // Base confidence si on a un type et un montant
        if (transactionType != "AUTRE" && amount > 0) {
            confidence += 0.4
        }

        // 🔥 BONUS SPÉCIAL POUR MVOLA "envoye a"
        if (provider == "mvola" && messageLower.contains("envoye a")) {
            confidence += 0.5
        }

        // Bonus pour provider reconnu
        if (provider != "unknown") {
            confidence += 0.2
        }

        // Bonus pour les mots-clés spécifiques mobile money
        val mobileMoneyKeywords = listOf("mvola", "airtel money", "orange money", "mobile money", "solde", "ref")
        if (mobileMoneyKeywords.any { messageLower.contains(it) }) {
            confidence += 0.2
        }

        // Bonus pour la présence d'une référence
        if (extractReference(message).isNotEmpty()) {
            confidence += 0.1
        }

        // Bonus pour format de message structuré
        if (message.contains(":") || message.contains("=") || message.contains("Ref")) {
            confidence += 0.1
        }

        return minOf(confidence, 1.0)
    }
}
