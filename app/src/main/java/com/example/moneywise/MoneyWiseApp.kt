package com.example.moneywise

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.moneywise.services.SMSBackgroundService
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class MoneyWiseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            // Créer le dossier pour les modèles si nécessaire
            val modelsDir = File(filesDir, "python_models")
            if (!modelsDir.exists()) {
                modelsDir.mkdirs()
            }

            // DÉMARRER LE SERVICE SMS EN ARRIÈRE-PLAN AU DÉMARRAGE DE L'APP
            startSMSBackgroundService()

            Log.d("MoneyWiseApp", "Application initialized successfully")
        } catch (e: Exception) {
            Log.e("MoneyWiseApp", "Error initializing app: ${e.message}")
        }
    }

    private fun startSMSBackgroundService() {
        try {
            val smsServiceIntent = Intent(this, SMSBackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(smsServiceIntent)
            } else {
                startService(smsServiceIntent)
            }
            Log.d("MoneyWiseApp", "🚀 Service SMS en arrière-plan démarré")
        } catch (e: Exception) {
            Log.e("MoneyWiseApp", "❌ Erreur démarrage service SMS: ${e.message}")
        }
    }
}
