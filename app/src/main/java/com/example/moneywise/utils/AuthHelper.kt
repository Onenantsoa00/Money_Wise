package com.example.moneywise.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.example.moneywise.ui.auth.LoginActivity

object AuthHelper {
    fun logout(context: Context) {
        // Nettoyage supplémentaire si nécessaire
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
        (context as? Activity)?.finish()
    }
}