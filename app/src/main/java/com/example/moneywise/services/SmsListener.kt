package com.example.moneywise.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log

class SmsListener : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsListener"
        // 🔥 NOUVEAU: Set pour éviter les doublons de SMS
        private val processedMessages = mutableSetOf<String>()
        private const val MAX_PROCESSED_MESSAGES = 100 // Limite pour éviter la surcharge mémoire
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            Log.d(TAG, "📨 SMS reçu")

            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (message in messages) {
                val sender = message.originatingAddress ?: continue
                val body = message.messageBody ?: continue
                val timestamp = message.timestampMillis

                Log.d(TAG, "📱 SMS de: $sender")
                Log.d(TAG, "💬 Message: $body")

                // 🔥 NOUVEAU: Créer une clé unique pour éviter les doublons
                val messageKey = "${sender}_${body.hashCode()}_${timestamp}"

                // Vérifier si ce message a déjà été traité
                if (processedMessages.contains(messageKey)) {
                    Log.d(TAG, "⚠️ Message déjà traité, ignoré: $messageKey")
                    continue
                }

                // Vérifier si c'est un SMS mobile money
                if (isMobileMoneyMessage(sender)) {
                    Log.d(TAG, "🏦 SMS Mobile Money détecté")

                    // Ajouter à la liste des messages traités
                    processedMessages.add(messageKey)

                    // Nettoyer la liste si elle devient trop grande
                    if (processedMessages.size > MAX_PROCESSED_MESSAGES) {
                        val iterator = processedMessages.iterator()
                        repeat(20) { // Supprimer les 20 plus anciens
                            if (iterator.hasNext()) {
                                iterator.next()
                                iterator.remove()
                            }
                        }
                    }

                    // 🔥 CORRECTION: SEULEMENT le service en arrière-plan (pas de traitement direct)
                    val serviceIntent = Intent(context, SMSBackgroundService::class.java).apply {
                        action = SMSBackgroundService.ACTION_PROCESS_SMS
                        putExtra(SMSBackgroundService.EXTRA_MESSAGE_BODY, body)
                        putExtra(SMSBackgroundService.EXTRA_SENDER, sender)
                        putExtra(SMSBackgroundService.EXTRA_MESSAGE_KEY, messageKey) // 🔥 NOUVEAU
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }

                    Log.d(TAG, "🚀 Service en arrière-plan lancé pour: $messageKey")
                } else {
                    Log.d(TAG, "❌ SMS non mobile money ignoré")
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
}
