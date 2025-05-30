package com.example.moneywise.ai

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton
import java.util.regex.Pattern

/**
 * Classe pour représenter le résultat d'une classification avec confiance
 */
data class TransactionClassification(
    val type: String,
    val confidence: Double
)

@Singleton
class TransactionClassifier @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val TAG = "TransactionClassifier"

        /**
         * Factory method pour créer une instance sans injection Hilt
         */
        fun create(context: Context): TransactionClassifier {
            return TransactionClassifier(context)
        }
    }

    // Base de données de patterns pour la classification - AMÉLIORÉE
    private val transactionPatterns = mapOf(
        "DEPOT" to listOf(
            // Français - PATTERNS SPÉCIFIQUES POUR RÉCEPTION
            "recu de", "reçu de", "crédit", "dépôt", "versement", "rechargé", "ajouté", "crédité",
            "vous avez reçu", "votre compte a été crédité", "versement effectué",
            "recharge", "depot", "credit",
            // Anglais
            "received from", "credit", "deposit", "credited", "added", "topped up",
            "you have received", "your account has been credited"
        ),
        "RETRAIT" to listOf(
            // Français - PATTERNS SPÉCIFIQUES POUR ENVOI
            "envoye a", "envoyé à", "débit", "retrait", "retiré", "payé", "débité", "prélevé",
            "vous avez envoyé", "votre compte a été débité", "paiement effectué",
            "paiement", "retrait",
            // Anglais
            "sent to", "debit", "withdrawal", "withdrawn", "paid", "debited",
            "you have sent", "your account has been debited", "payment made"
        ),
        "TRANSFERT" to listOf(
            // Français
            "transfert", "envoi", "transféré", "virement", "transfer",
            "transfert effectué", "envoi d'argent", "virement bancaire",
            // Anglais
            "transfer", "money transfer", "fund transfer", "transferred"
        ),
        "ACHAT" to listOf(
            // Français
            "achat", "acheté", "commande", "facture", "paiement marchand",
            "achat effectué", "paiement boutique", "transaction commerciale",
            // Anglais
            "purchase", "bought", "order", "invoice", "merchant payment",
            "purchase made", "shop payment", "commercial transaction"
        )
    )

    // Patterns spécifiques par opérateur - AMÉLIORÉS
    private val operatorSpecificPatterns = mapOf(
        "mvola" to mapOf(
            "DEPOT" to listOf("recu de", "compte mvola crédité", "dépôt mvola", "recharge mvola"),
            "RETRAIT" to listOf("envoye a", "retrait mvola", "paiement mvola"),
            "TRANSFERT" to listOf("transfert mvola", "envoi mvola")
        ),
        "airtel" to mapOf(
            "DEPOT" to listOf("airtel money received", "airtel credit", "airtel deposit"),
            "RETRAIT" to listOf("airtel money sent", "airtel withdrawal", "airtel payment", "sent to"),
            "TRANSFERT" to listOf("airtel transfer", "airtel money transfer")
        ),
        "orange" to mapOf(
            "DEPOT" to listOf("orange money reçu", "crédit orange", "dépôt orange"),
            "RETRAIT" to listOf("orange money envoyé", "retrait orange", "paiement orange", "envoyé à"),
            "TRANSFERT" to listOf("transfert orange", "envoi orange money")
        )
    )

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

        return false
    }

    /**
     * Classifie une transaction basée sur le texte du SMS
     */
    fun classifyTransaction(text: String, sender: String = ""): String {
        Log.d(TAG, "Classifying transaction: $text")

        val textLower = text.lowercase()
        val operator = getOperatorFromSender(sender)

        // 🔥 VÉRIFICATION PRÉALABLE: Exclure les messages promotionnels
        if (isPromotionalMessage(text)) {
            Log.d(TAG, "Promotional message detected - returning PROMOTION")
            return "PROMOTION"
        }

        // 🔥 LOGIQUE SPÉCIALE POUR LES MESSAGES MVOLA
        if (operator == "mvola") {
            when {
                textLower.contains("recu de") -> {
                    Log.d(TAG, "Detected MVola 'recu de' pattern - classifying as DEPOT")
                    return "DEPOT"
                }
                textLower.contains("envoye a") -> {
                    Log.d(TAG, "Detected MVola 'envoye a' pattern - classifying as RETRAIT")
                    return "RETRAIT"
                }
            }
        }

        // Vérifier d'abord les patterns spécifiques à l'opérateur
        if (operator != "unknown") {
            val operatorPatterns = operatorSpecificPatterns[operator]
            operatorPatterns?.forEach { (type, patterns) ->
                if (patterns.any { textLower.contains(it) }) {
                    Log.d(TAG, "Classified as $type (operator-specific)")
                    return type
                }
            }
        }

        // Ensuite, vérifier les patterns généraux avec scoring amélioré
        val scores = mutableMapOf<String, Double>()

        transactionPatterns.forEach { (type, patterns) ->
            var score = 0.0
            patterns.forEach { pattern ->
                if (textLower.contains(pattern)) {
                    // Score plus élevé pour les patterns plus spécifiques
                    score += when (pattern.length) {
                        in 1..5 -> 1.0
                        in 6..10 -> 2.0
                        in 11..20 -> 3.0
                        else -> 4.0
                    }
                }
            }
            if (score > 0) {
                scores[type] = score
            }
        }

        // Retourner le type avec le score le plus élevé
        val bestMatch = scores.maxByOrNull { it.value }
        val result = bestMatch?.key ?: "AUTRE"

        Log.d(TAG, "Classification scores: $scores")
        Log.d(TAG, "Final classification: $result")

        return result
    }

    /**
     * Classifie une transaction avec un score de confiance
     */
    fun classifyTransactionWithConfidence(text: String, sender: String = ""): TransactionClassification {
        val textLower = text.lowercase()
        val operator = getOperatorFromSender(sender)

        // 🔥 VÉRIFICATION PRÉALABLE: Messages promotionnels
        if (isPromotionalMessage(text)) {
            Log.d(TAG, "Promotional message detected - zero confidence")
            return TransactionClassification("PROMOTION", 0.0)
        }

        val scores = mutableMapOf<String, Double>()
        var maxScore = 0.0

        // 🔥 LOGIQUE SPÉCIALE POUR MVOLA avec haute confiance
        if (operator == "mvola") {
            when {
                textLower.contains("recu de") -> {
                    Log.d(TAG, "MVola 'recu de' detected - high confidence DEPOT")
                    return TransactionClassification("DEPOT", 0.95)
                }
                textLower.contains("envoye a") -> {
                    Log.d(TAG, "MVola 'envoye a' detected - high confidence RETRAIT")
                    return TransactionClassification("RETRAIT", 0.95)
                }
            }
        }

        // Vérifier les patterns spécifiques à l'opérateur (bonus de confiance)
        if (operator != "unknown") {
            val operatorPatterns = operatorSpecificPatterns[operator]
            operatorPatterns?.forEach { (type, patterns) ->
                patterns.forEach { pattern ->
                    if (textLower.contains(pattern)) {
                        scores[type] = (scores[type] ?: 0.0) + 5.0 // Bonus pour patterns spécifiques
                    }
                }
            }
        }

        // Vérifier les patterns généraux
        transactionPatterns.forEach { (type, patterns) ->
            patterns.forEach { pattern ->
                if (textLower.contains(pattern)) {
                    val patternScore = when (pattern.length) {
                        in 1..5 -> 1.0
                        in 6..10 -> 2.0
                        in 11..20 -> 3.0
                        else -> 4.0
                    }
                    scores[type] = (scores[type] ?: 0.0) + patternScore
                }
            }
        }

        // Calculer le score total et la confiance
        val totalScore = scores.values.sum()
        maxScore = scores.values.maxOrNull() ?: 0.0

        val bestType = scores.maxByOrNull { it.value }?.key ?: "AUTRE"
        val confidence = if (totalScore > 0) maxScore / totalScore else 0.0

        Log.d(TAG, "Classification with confidence: $bestType ($confidence)")

        return TransactionClassification(bestType, confidence)
    }

    private fun getOperatorFromSender(sender: String): String {
        val senderLower = sender.lowercase()
        return when {
            senderLower.contains("mvola") || senderLower.contains("telma") -> "mvola"
            senderLower.contains("airtel") -> "airtel"
            senderLower.contains("orange") -> "orange"
            else -> "unknown"
        }
    }

    /**
     * Obtient des statistiques sur les classifications
     */
    fun getClassificationStats(): Map<String, Int> {
        return mapOf(
            "DEPOT" to (transactionPatterns["DEPOT"]?.size ?: 0),
            "RETRAIT" to (transactionPatterns["RETRAIT"]?.size ?: 0),
            "TRANSFERT" to (transactionPatterns["TRANSFERT"]?.size ?: 0),
            "ACHAT" to (transactionPatterns["ACHAT"]?.size ?: 0)
        )
    }
}
