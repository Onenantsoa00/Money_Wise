package com.example.moneywise.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.moneywise.utils.FloatingWidgetManager
import com.example.moneywise.utils.NotificationHelper
import com.example.moneywise.utils.ReminderManager
import com.example.moneywise.utils.SessionManager

/**
 * Récepteur de diffusion qui s'exécute au démarrage de l'appareil
 * pour redémarrer les services nécessaires.
 *
 * Note: Les BroadcastReceiver ne supportent pas complètement l'injection Hilt,
 * donc nous créons les instances manuellement.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d(TAG, "🔄 Appareil démarré ou application mise à jour")

            try {
                val sessionManager = SessionManager(context)
                if (sessionManager.isLoggedIn()) {
                    Log.d(TAG, "👤 Utilisateur connecté, redémarrage des services")

                    // Créer les instances manuellement (sans injection Hilt)
                    val notificationHelper = NotificationHelper(context)
                    val reminderManager = ReminderManager(context, notificationHelper)
                    val floatingWidgetManager = FloatingWidgetManager(context)

                    // Redémarrer les rappels si activés
                    reminderManager.restartRemindersIfEnabled()

                    // Redémarrer le widget flottant si activé
                    floatingWidgetManager.startFloatingWidgetIfEnabled()

                    Log.d(TAG, "✅ Services redémarrés avec succès")
                } else {
                    Log.d(TAG, "ℹ️ Utilisateur non connecté, aucune action")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors du redémarrage des services: ${e.message}", e)
            }
        }
    }
}