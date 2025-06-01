package com.example.moneywise.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.moneywise.utils.FloatingWidgetManager
import com.example.moneywise.utils.SessionManager

class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "📱 Événement reçu: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                val sessionManager = SessionManager(context)
                val floatingWidgetManager = FloatingWidgetManager(context)

                if (sessionManager.isLoggedIn()) {
                    Log.d(TAG, "🚀 Redémarrage automatique du widget")
                    floatingWidgetManager.startFloatingWidgetIfEnabled()
                }
            }
        }
    }
}