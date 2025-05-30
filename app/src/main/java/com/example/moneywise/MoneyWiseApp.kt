package com.example.moneywise

import android.app.Application
import android.util.Log
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

            Log.d("MoneyWiseApp", "Application initialized successfully")
        } catch (e: Exception) {
            Log.e("MoneyWiseApp", "Error initializing app: ${e.message}")
        }
    }
}
