package com.example.moneywise.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.SystemClock
import android.util.Log
import com.example.moneywise.services.ReminderNotificationService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notificationHelper: NotificationHelper
) {
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "ReminderManager"
        private const val PREFS_NAME = "reminder_preferences"
        private const val KEY_REMINDERS_ENABLED = "reminders_enabled"
        private const val KEY_REMINDER_INTERVAL = "reminder_interval"
        private const val REQUEST_CODE = 1234

        // Constantes pour les intervalles de rappel
        object Intervals {
            const val THREE_HOURS = 3 * 60 * 60 * 1000L  // 3 heures en millisecondes
            const val SIX_HOURS = 6 * 60 * 60 * 1000L    // 6 heures en millisecondes
            const val TWELVE_HOURS = 12 * 60 * 60 * 1000L // 12 heures en millisecondes
            const val ONE_DAY = 24 * 60 * 60 * 1000L     // 24 heures en millisecondes

            // Pour les tests uniquement
            const val THIRTY_SECONDS = 30 * 1000L        // 30 secondes en millisecondes
        }
    }

    /**
     * Vérifie si les rappels sont activés
     */
    fun areRemindersEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_REMINDERS_ENABLED, false)
    }

    /**
     * Alias pour areRemindersEnabled() pour compatibilité
     */
    fun isRemindersEnabled(): Boolean {
        return areRemindersEnabled()
    }

    /**
     * Obtient l'intervalle actuel des rappels
     */
    fun getCurrentInterval(): Long {
        return sharedPreferences.getLong(KEY_REMINDER_INTERVAL, Intervals.SIX_HOURS)
    }

    /**
     * Définit l'intervalle des rappels
     */
    fun setReminderInterval(interval: Long) {
        sharedPreferences.edit().putLong(KEY_REMINDER_INTERVAL, interval).apply()

        // Si les rappels sont activés, les redémarrer avec le nouvel intervalle
        if (areRemindersEnabled()) {
            restartReminders()
        }
    }

    /**
     * Formate l'intervalle pour l'affichage
     */
    fun formatInterval(interval: Long): String {
        return when (interval) {
            Intervals.THREE_HOURS -> "3 heures"
            Intervals.SIX_HOURS -> "6 heures"
            Intervals.TWELVE_HOURS -> "12 heures"
            Intervals.ONE_DAY -> "24 heures"
            Intervals.THIRTY_SECONDS -> "30 secondes (test)"
            else -> "${interval / (60 * 60 * 1000)} heures"
        }
    }

    /**
     * Démarre les rappels
     */
    fun startReminders() {
        Log.d(TAG, "🚀 Démarrage des rappels")

        // Marquer les rappels comme activés
        sharedPreferences.edit().putBoolean(KEY_REMINDERS_ENABLED, true).apply()

        // Créer l'intent pour le service
        val intent = Intent(context, ReminderNotificationService::class.java)
        val pendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Annuler toute alarme existante
        alarmManager.cancel(pendingIntent)

        // Obtenir l'intervalle configuré
        val interval = getCurrentInterval()

        // Configurer l'alarme répétitive
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 10000, // Première exécution après 10 secondes
                pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 10000, // Première exécution après 10 secondes
                pendingIntent
            )
        }

        // Démarrer le service immédiatement pour une première notification
        try {
            context.startService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors du démarrage du service: ${e.message}")
        }

        Log.d(TAG, "✅ Rappels programmés avec intervalle: ${formatInterval(interval)}")
    }

    /**
     * Arrête les rappels
     */
    fun stopReminders() {
        Log.d(TAG, "🛑 Arrêt des rappels")

        // Marquer les rappels comme désactivés
        sharedPreferences.edit().putBoolean(KEY_REMINDERS_ENABLED, false).apply()

        // Annuler l'alarme
        val intent = Intent(context, ReminderNotificationService::class.java)
        val pendingIntent = PendingIntent.getService(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        // Arrêter le service
        context.stopService(Intent(context, ReminderNotificationService::class.java))

        Log.d(TAG, "✅ Rappels arrêtés")
    }

    /**
     * Redémarre les rappels (utile après un redémarrage de l'appareil)
     */
    fun restartReminders() {
        stopReminders()
        startReminders()
    }

    /**
     * Redémarre les rappels s'ils étaient activés
     */
    fun restartRemindersIfEnabled() {
        if (areRemindersEnabled()) {
            Log.d(TAG, "🔄 Redémarrage des rappels car ils étaient activés")
            restartReminders()
        } else {
            Log.d(TAG, "ℹ️ Les rappels n'étaient pas activés, aucune action")
        }
    }

    /**
     * Méthode de compatibilité pour l'ancien code
     */
    fun startRemindersIfEnabled() {
        restartRemindersIfEnabled()
    }
}