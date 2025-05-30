package com.example.moneywise.ai

import android.content.Context
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

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
            // Français
            "reçu", "crédit", "dépôt", "versement", "rechargé", "ajouté", "crédité",
            "vous avez reçu", "votre compte a été crédité", "versement effectué",
            "recharge", "depot", "credit",
            // Anglais
            "received", "credit", "deposit", "credited", "added", "topped up",
            "you have received", "your account has been credited"
        ),
        "RETRAIT" to listOf(
            // Français - PATTERNS AMÉLIORÉS POUR VOTRE CAS
            "envoyé", "envoye", "débit", "retrait", "retiré", "payé", "débité", "prélevé",
            "vous avez envoyé", "votre compte a été débité", "paiement effectué",
            "envoye a", "envoyé à", "paiement", "retrait",
            // Anglais
            "sent", "debit", "withdrawal", "withdrawn", "paid", "debited",
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
        ),
        "FRAIS" to listOf(
            // Français - PATTERNS PLUS SPÉCIFIQUES
            "frais de transaction", "commission bancaire", "frais de service",
            "commission", "taxe", "charge", "coût de service",
            // Anglais
            "transaction fee", "bank commission", "service charge",
            "fee", "commission", "tax", "charge", "cost"
        )
    )

    // Patterns spécifiques par opérateur - AMÉLIORÉS
    private val operatorSpecificPatterns = mapOf(
        "mvola" to mapOf(
            "DEPOT" to listOf("mvola reçu", "compte mvola crédité", "dépôt mvola", "recharge mvola"),
            "RETRAIT" to listOf("mvola envoyé", "retrait mvola", "paiement mvola", "envoye a", "envoyé à"),
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
     * Classifie une transaction basée sur le texte du SMS
     */
    fun classifyTransaction(text: String, sender: String = ""): String {
        Log.d(TAG, "Classifying transaction: $text")

        val textLower = text.lowercase()
        val operator = getOperatorFromSender(sender)

        // 🔥 LOGIQUE SPÉCIALE POUR LES MESSAGES MVOLA COMME LE VÔTRE
        if (operator == "mvola" && textLower.contains("envoye a")) {
            Log.d(TAG, "Detected MVola 'envoye a' pattern - classifying as RETRAIT")
            return "RETRAIT"
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

        // 🔥 LOGIQUE SPÉCIALE: Si on détecte "frais" ET "envoyé", privilégier RETRAIT
        if (scores.containsKey("FRAIS") && scores.containsKey("RETRAIT")) {
            if (textLower.contains("envoye") || textLower.contains("envoyé")) {
                Log.d(TAG, "Detected both FRAIS and RETRAIT patterns, but 'envoyé' found - prioritizing RETRAIT")
                return "RETRAIT"
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

        val scores = mutableMapOf<String, Double>()
        var maxScore = 0.0

        // 🔥 LOGIQUE SPÉCIALE POUR MVOLA "envoye a"
        if (operator == "mvola" && textLower.contains("envoye a")) {
            Log.d(TAG, "MVola 'envoye a' detected - high confidence RETRAIT")
            return TransactionClassification("RETRAIT", 0.95)
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

        // 🔥 LOGIQUE SPÉCIALE: Privilégier RETRAIT si "envoyé" + "frais"
        if (scores.containsKey("FRAIS") && scores.containsKey("RETRAIT")) {
            if (textLower.contains("envoye") || textLower.contains("envoyé")) {
                scores["RETRAIT"] = (scores["RETRAIT"] ?: 0.0) + 10.0 // Bonus énorme
                Log.d(TAG, "Boosting RETRAIT score due to 'envoyé' pattern")
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
            "ACHAT" to (transactionPatterns["ACHAT"]?.size ?: 0),
            "FRAIS" to (transactionPatterns["FRAIS"]?.size ?: 0)
        )
    }
}
