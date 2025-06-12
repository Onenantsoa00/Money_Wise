package com.example.moneywise.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.moneywise.MainActivity
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.data.entity.Historique
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.ai.TransactionClassifier
import com.example.moneywise.ai.NLPExtractor
import com.example.moneywise.utils.FloatingWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Date

class SMSBackgroundService : Service() {

    companion object {
        private const val TAG = "SMSBackgroundService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "sms_processing_channel"
        const val ACTION_PROCESS_SMS = "PROCESS_SMS"
        const val EXTRA_MESSAGE_BODY = "message_body"
        const val EXTRA_SENDER = "sender"
        const val EXTRA_MESSAGE_KEY = "message_key"

        // Protection contre les doublons au niveau du service
        private val processedInService = mutableSetOf<String>()
        private const val MAX_SERVICE_PROCESSED = 50
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var balanceService: BalanceUpdateService

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Service SMS en arrière-plan créé")

        balanceService = BalanceUpdateService.create()
        createNotificationChannel()
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📨 Commande reçue: ${intent?.action}")

        when (intent?.action) {
            ACTION_PROCESS_SMS -> {
                val messageBody = intent.getStringExtra(EXTRA_MESSAGE_BODY) ?: ""
                val sender = intent.getStringExtra(EXTRA_SENDER) ?: ""
                val messageKey = intent.getStringExtra(EXTRA_MESSAGE_KEY) ?: ""

                if (messageBody.isNotEmpty() && messageKey.isNotEmpty()) {
                    // Vérifier les doublons au niveau du service
                    if (processedInService.contains(messageKey)) {
                        Log.w(TAG, "⚠️ Message déjà traité par le service, ignoré: $messageKey")
                        return START_STICKY
                    }

                    // Ajouter à la liste des traités
                    processedInService.add(messageKey)

                    // Nettoyer si nécessaire
                    if (processedInService.size > MAX_SERVICE_PROCESSED) {
                        val iterator = processedInService.iterator()
                        repeat(10) {
                            if (iterator.hasNext()) {
                                iterator.next()
                                iterator.remove()
                            }
                        }
                    }

                    processSMSInBackground(messageBody, sender, messageKey)
                } else {
                    Log.w(TAG, "⚠️ Données manquantes pour le traitement SMS")
                }
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Traitement SMS Mobile Money",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service de traitement des SMS mobile money en arrière-plan"
                setShowBadge(false)
                setSound(null, null)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundService() {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MoneyWise - Surveillance SMS")
            .setContentText("Surveillance des transactions mobile money active")
            .setSmallIcon(R.drawable.ic_account_balance_wallet)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "🔔 Service en premier plan démarré")
    }

    private fun processSMSInBackground(messageBody: String, sender: String, messageKey: String) {
        serviceScope.launch {
            try {
                Log.d(TAG, "🔍 Traitement SMS en arrière-plan")
                Log.d(TAG, "📱 Expéditeur: $sender")
                Log.d(TAG, "💬 Message: $messageBody")
                Log.d(TAG, "🔑 Clé unique: $messageKey")

                // Créer les instances des analyseurs IA
                val transactionClassifier = TransactionClassifier.create(this@SMSBackgroundService)
                val nlpExtractor = NLPExtractor.create(this@SMSBackgroundService)

                // Analyser le message
                val classification = transactionClassifier.classifyTransactionWithConfidence(messageBody, sender)
                val extractedDetails = nlpExtractor.extractTransactionDetails(messageBody, sender)

                val isValid = extractedDetails["is_valid"] as? Boolean ?: false
                val confidence = classification.confidence
                val transactionType = classification.type
                val amount = extractedDetails["amount"] as? Double ?: 0.0
                val reference = extractedDetails["reference"] as? String ?: ""

                Log.d(TAG, "📊 Analyse: type=$transactionType, montant=$amount, confiance=$confidence, valide=$isValid")

                if (isValid && confidence > 0.5 && amount > 0) {
                    // Vérification plus précise des doublons
                    val isDuplicate = isTransactionAlreadyProcessed(messageBody, sender, amount, reference, transactionType)

                    if (isDuplicate) {
                        Log.w(TAG, "⚠️ Transaction en doublon détectée, ignorée: $messageKey")
                        return@launch
                    }

                    val success = saveTransactionAndUpdateBalance(
                        messageBody, sender, transactionType, amount, extractedDetails, confidence, messageKey
                    )

                    if (success) {
                        showTransactionNotification(transactionType, amount)
                        Log.d(TAG, "✅ Transaction traitée avec succès: $messageKey")
                    } else {
                        Log.e(TAG, "❌ Échec du traitement de la transaction: $messageKey")
                    }
                } else {
                    Log.d(TAG, "❌ SMS non reconnu comme transaction valide: $messageKey")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Erreur lors du traitement SMS: $messageKey", e)
            }
        }
    }

    // Vérification plus précise des doublons
    private suspend fun isTransactionAlreadyProcessed(
        messageBody: String,
        sender: String,
        amount: Double,
        reference: String,
        transactionType: String
    ): Boolean {
        return try {
            val db = AppDatabase.getDatabase(this@SMSBackgroundService)
            val currentTime = System.currentTimeMillis()
            val twoMinutesAgo = Date(currentTime - 2 * 60 * 1000)

            // Chercher des transactions récentes similaires
            val recentTransactions = db.transactionDao().getTransactionsSince(twoMinutesAgo)

            Log.d(TAG, "🔍 Vérification doublons: ${recentTransactions.size} transactions récentes trouvées")

            // Vérification plus stricte avec plusieurs critères
            val isDuplicate = recentTransactions.any { transaction ->
                val transactionAmount = transaction.montants?.toDoubleOrNull() ?: 0.0 // Safe call
                val sameAmount = Math.abs(transactionAmount - amount) < 0.01
                val sameType = transaction.type == transactionType

                // Vérifier aussi la référence si disponible
                val sameReference = if (reference.isNotEmpty()) {
                    try {
                        // Chercher la référence dans les détails de l'historique
                        val historique = db.historiqueDao().getHistoriqueByTransactionAmount(amount)
                        historique.any { it.details?.contains(reference) == true } // Safe call
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Erreur vérification référence: ${e.message}")
                        false
                    }
                } else {
                    false
                }

                val result = sameAmount && sameType && sameReference

                if (result) {
                    Log.d(TAG, "🔍 Doublon trouvé: montant=$transactionAmount, type=${transaction.type}, ref=$reference")
                }

                result
            }

            Log.d(TAG, "🔍 Résultat vérification doublon: $isDuplicate")
            isDuplicate

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur vérification doublons", e)
            // En cas d'erreur, on laisse passer la transaction
            false
        }
    }

    private suspend fun saveTransactionAndUpdateBalance(
        messageBody: String,
        sender: String,
        transactionType: String,
        amount: Double,
        extractedDetails: Map<String, Any>,
        confidence: Double,
        messageKey: String
    ): Boolean {
        return try {
            val db = AppDatabase.getDatabase(this@SMSBackgroundService)

            // Récupérer ou créer la banque
            val operatorName = getOperatorName(sender)
            var banque = db.banqueDao().getByCode(getOperatorCode(sender))

            if (banque == null) {
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

            // Créer et sauvegarder la transaction
            val transaction = Transaction(
                type = transactionType,
                montants = amount.toString(),
                date = Date(),
                id_utilisateur = userId,
                id_banque = banque.id
            )
            db.transactionDao().insertTransaction(transaction)
            Log.d(TAG, "💾 Transaction insérée: $transactionType - $amount")

            // MISE À JOUR DU SOLDE
            val balanceUpdated = balanceService.updateUserBalance(
                context = this@SMSBackgroundService,
                userId = userId,
                transactionType = transactionType,
                amount = amount
            )

            Log.d(TAG, "💰 Mise à jour solde: ${if (balanceUpdated) "✅ Succès" else "❌ Échec"}") // Expression if complète

            // MISE À JOUR DU WIDGET FLOTTANT
            if (balanceUpdated) {
                try {
                    val widgetManager = FloatingWidgetManager(this@SMSBackgroundService)
                    widgetManager.updateFloatingWidget()
                    Log.d(TAG, "🔄 Widget mis à jour")
                } catch (e: Exception) {
                    Log.w(TAG, "⚠️ Erreur mise à jour widget: ${e.message}")
                }
            }

            // Ajouter à l'historique
            val historique = Historique(
                typeTransaction = transactionType,
                montant = amount,
                dateHeure = Date(),
                motif = "Transaction Mobile Money automatique (Service arrière-plan)",
                details = buildString {
                    append("Opérateur: $operatorName")
                    append(" | Confiance: ${String.format("%.2f", confidence)}")
                    append(" | Ref: ${extractedDetails["reference"] as? String ?: "N/A"}")
                    append(" | Clé: $messageKey")
                    append(" | Solde: ${if (balanceUpdated) "✅" else "❌"}") // Expression if complète
                }
            )
            db.historiqueDao().insert(historique)
            Log.d(TAG, "📝 Historique ajouté")

            balanceUpdated
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la sauvegarde: $messageKey", e)
            false
        }
    }

    private fun showTransactionNotification(transactionType: String, amount: Double) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val title = when (transactionType) {
            "DEPOT" -> "💰 Dépôt détecté"
            "RETRAIT" -> "💸 Retrait détecté"
            "TRANSFERT" -> "🔄 Transfert détecté"
            else -> "📱 Transaction détectée"
        }

        val content = "Montant: ${String.format("%.0f", amount)} MGA"

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_account_balance_wallet)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
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

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "💀 Service SMS arrière-plan détruit")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
