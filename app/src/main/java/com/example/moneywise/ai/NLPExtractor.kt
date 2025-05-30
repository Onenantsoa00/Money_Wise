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
            // 🔥 VÉRIFICATION PRÉALABLE: Exclure les messages promotionnels
            if (isPromotionalMessage(message)) {
                Log.d(TAG, "Message promotionnel détecté - ignoré")
                result["is_valid"] = false
                result["transaction_type"] = "PROMOTION"
                result["amount"] = 0.0
                result["confidence"] = 0.0
                result["provider"] = getProviderFromSender(sender)
                result["phone_number"] = ""
                result["reference"] = ""
                return result
            }

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
     * 🔥 NOUVELLE FONCTION: Détecte les messages promotionnels
     */
    private fun isPromotionalMessage(message: String): Boolean {
        val messageLower = message.lowercase()

        // Mots-clés de messages promotionnels
        val promotionalKeywords = listOf(
            "astuce", "conseil", "tip", "promo", "promotion", "offre", "réduction",
            "disponible sur", "playstore", "appstore", "téléchargez", "download",
            "app", "application", "moins cher", "économisez", "gratuit",
            "nouveau service", "découvrez", "profitez", "bénéficiez"
        )

        // Patterns de pourcentages promotionnels
        val promoPatterns = listOf(
            "-\\d+\\s*%", // -20%, -50%, etc.
            "\\d+\\s*%\\s*(?:de\\s*)?(?:réduction|remise|rabais)",
            "jusqu'à\\s*\\d+\\s*%"
        )

        // Vérifier les mots-clés
        if (promotionalKeywords.any { messageLower.contains(it) }) {
            return true
        }

        // Vérifier les patterns de promotion
        for (pattern in promoPatterns) {
            if (Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(message).find()) {
                return true
            }
        }

        // Messages multi-parties (1/2, 2/2) sans transaction réelle
        if (messageLower.matches(".*\\d+/\\d+.*".toRegex()) &&
            !messageLower.contains("solde") &&
            !messageLower.contains("ref")) {
            return true
        }

        return false
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

        // Exclure les messages promotionnels
        if (isPromotionalMessage(message)) {
            Log.d(TAG, "Invalid: promotional message")
            return false
        }

        // Critères de base
        if (amount <= 0) {
            Log.d(TAG, "Invalid: amount <= 0")
            return false
        }

        if (provider == "unknown") {
            Log.d(TAG, "Invalid: unknown provider")
            return false
        }

        // 🔥 VALIDATION STRICTE: Doit avoir un solde ET une référence pour être valide
        val hasSolde = messageLower.contains("solde")
        val hasReference = extractReference(message).isNotEmpty()

        if (!hasSolde && !hasReference) {
            Log.d(TAG, "Invalid: no balance or reference found")
            return false
        }

        // 🔥 LOGIQUE SPÉCIALE POUR MVOLA "recu de" (DEPOT)
        if (provider == "mvola" && messageLower.contains("recu de")) {
            Log.d(TAG, "Valid: MVola 'recu de' pattern detected (DEPOT)")
            return true
        }

        // 🔥 LOGIQUE SPÉCIALE POUR MVOLA "envoye a" (RETRAIT)
        if (provider == "mvola" && messageLower.contains("envoye a")) {
            Log.d(TAG, "Valid: MVola 'envoye a' pattern detected (RETRAIT)")
            return true
        }

        // Accepter si le type n'est pas "AUTRE" et qu'on a un montant avec solde
        if (transactionType != "AUTRE" && amount > 0 && hasSolde) {
            Log.d(TAG, "Valid: good type ($transactionType) with balance")
            return true
        }

        // Accepter si on a des mots-clés mobile money avec solde
        val mobileMoneyKeywords = listOf("mvola", "airtel money", "orange money")
        if (mobileMoneyKeywords.any { messageLower.contains(it) } && amount > 0 && hasSolde) {
            Log.d(TAG, "Valid: mobile money keywords found with amount and balance")
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
        // 🔥 LOGIQUE SPÉCIALE POUR MVOLA - PLUS PRÉCISE
        if (provider == "mvola") {
            when {
                message.contains("recu de") -> return "DEPOT"
                message.contains("envoye a") -> return "RETRAIT"
            }
        }

        // Patterns spécifiques par provider - AMÉLIORÉS
        val patterns = mapOf(
            "mvola" to mapOf(
                "DEPOT" to listOf("recu", "crédit", "dépôt", "versement", "rechargé", "recharge"),
                "RETRAIT" to listOf("envoyé", "envoye", "débit", "retrait", "retiré", "payé"),
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
        // 🔥 EXCLURE LES POURCENTAGES PROMOTIONNELS
        if (isPromotionalMessage(message)) {
            return 0.0
        }

        // Patterns pour les montants avec priorité - AMÉLIORÉS
        val amountPatterns = listOf(
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:Ar|MGA|ariary)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:ar|mga)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:montant|amount|somme)[\\s:]*(\\d+(?:[.,]\\d+)?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*(?:francs?|fr)", Pattern.CASE_INSENSITIVE)
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
            Pattern.compile("\$$(\\d{10})\$$"), // Format (0389914075)
            Pattern.compile("(?:de|from|to|vers|à)[\\s:]*(\\+?261[23][2-9]\\d{7})"),
            Pattern.compile("(?:de|from|to|vers|à)[\\s:]*(0[23][2-9]\\d{7})"),
            Pattern.compile("(?:\\+261|261|0)?[23][2-9]\\d{7}")
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
            Pattern.compile("(?:ref|référence)[\\s:]*([A-Z0-9]{4,})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ref[\\s:]*([0-9]{6,})", Pattern.CASE_INSENSITIVE), // Spécifique pour MVola
            Pattern.compile("([A-Z]{2,}\\d{4,})"),
            Pattern.compile("(\\d{10})") // Codes numériques de 10 chiffres comme dans votre exemple
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

        // Pénalité pour messages promotionnels
        if (isPromotionalMessage(message)) {
            return 0.0
        }

        // Base confidence si on a un type et un montant
        if (transactionType != "AUTRE" && amount > 0) {
            confidence += 0.4
        }

        // 🔥 BONUS SPÉCIAL POUR MVOLA avec patterns spécifiques
        if (provider == "mvola") {
            when {
                messageLower.contains("recu de") -> confidence += 0.5
                messageLower.contains("envoye a") -> confidence += 0.5
            }
        }

        // Bonus pour provider reconnu
        if (provider != "unknown") {
            confidence += 0.2
        }

        // Bonus pour la présence d'un solde (indicateur de vraie transaction)
        if (messageLower.contains("solde")) {
            confidence += 0.3
        }

        // Bonus pour la présence d'une référence
        if (extractReference(message).isNotEmpty()) {
            confidence += 0.1
        }

        return minOf(confidence, 1.0)
    }
}
