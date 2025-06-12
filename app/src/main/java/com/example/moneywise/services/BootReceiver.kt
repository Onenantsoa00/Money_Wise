package com.example.moneywise.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.moneywise.utils.FloatingWidgetManager

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📱 Événement système reçu: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Log.d(TAG, "🔄 Redémarrage détecté, relancement des services")

                try {
                    // DÉMARRER LE SERVICE SMS EN ARRIÈRE-PLAN
                    val smsServiceIntent = Intent(context, SMSBackgroundService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(smsServiceIntent)
                    } else {
                        context.startService(smsServiceIntent)
                    }
                    Log.d(TAG, "✅ Service SMS redémarré")

                    // Redémarrer le widget flottant si il était activé
                    val widgetManager = FloatingWidgetManager(context)
                    widgetManager.startFloatingWidgetIfEnabled()
                    Log.d(TAG, "✅ Widget flottant vérifié")

                } catch (e: Exception) {
                    Log.e(TAG, "❌ Erreur lors du redémarrage des services", e)
                }
            }
        }
    }
}
