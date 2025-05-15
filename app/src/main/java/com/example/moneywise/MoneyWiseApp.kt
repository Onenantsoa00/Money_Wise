package com.example.moneywise

import android.app.Application
import com.example.moneywise.data.AppDatabase

class MoneyWiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialisation de la base de données
        AppDatabase.getDatabase(this)
    }
}