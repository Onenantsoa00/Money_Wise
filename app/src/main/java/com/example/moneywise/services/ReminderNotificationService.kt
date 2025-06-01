package com.example.moneywise.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.moneywise.MainActivity
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.utils.NotificationHelper
import com.example.moneywise.utils.SessionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class ReminderNotificationService : Service() {

    companion object {
        private const val TAG = "ReminderNotificationService"
        const val ACTION_START_REMINDERS = "START_REMINDERS"
        const val ACTION_STOP_REMINDERS = "STOP_REMINDERS"
        const val ACTION_UPDATE_INTERVAL = "UPDATE_INTERVAL"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "reminder_channel"

        // Intervalles prédéfinis (en millisecondes)
        object ReminderIntervals {
            const val THIRTY_SECONDS = 30000L      // 30 secondes (pour test)
            const val ONE_MINUTE = 60000L          // 1 minute
            const val FIVE_MINUTES = 300000L       // 5 minutes
            const val FIFTEEN_MINUTES = 900000L    // 15 minutes
            const val THIRTY_MINUTES = 1800000L    // 30 minutes
            const val ONE_HOUR = 3600000L          // 1 heure
            const val THREE_HOURS = 10800000L      // 3 heures
            const val SIX_HOURS = 21600000L        // 6 heures
            const val TWELVE_HOURS = 43200000L     // 12 heures
            const val ONE_DAY = 86400000L          // 24 heures
        }

        // Intervalle par défaut
        private var REMINDER_INTERVAL = ReminderIntervals.SIX_HOURS

        fun setReminderInterval(intervalMs: Long) {
            REMINDER_INTERVAL = intervalMs
            Log.d(TAG, "🕐 Intervalle de rappel mis à jour: ${formatIntervalStatic(intervalMs)}")
        }

        fun getReminderInterval(): Long = REMINDER_INTERVAL

        private fun formatIntervalStatic(intervalMs: Long): String {
            return when (intervalMs) {
                ReminderIntervals.THIRTY_SECONDS -> "30 secondes"
                ReminderIntervals.ONE_MINUTE -> "1 minute"
                ReminderIntervals.FIVE_MINUTES -> "5 minutes"
                ReminderIntervals.FIFTEEN_MINUTES -> "15 minutes"
                ReminderIntervals.THIRTY_MINUTES -> "30 minutes"
                ReminderIntervals.ONE_HOUR -> "1 heure"
                ReminderIntervals.THREE_HOURS -> "3 heures"
                ReminderIntervals.SIX_HOURS -> "6 heures"
                ReminderIntervals.TWELVE_HOURS -> "12 heures"
                ReminderIntervals.ONE_DAY -> "24 heures"
                else -> "${intervalMs / 1000} secondes"
            }
        }
    }

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private lateinit var sessionManager: SessionManager
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var reminderHandler: Handler? = null
    private var reminderRunnable: Runnable? = null
    private var isRunning = false

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🔔 Service de rappel créé")

        sessionManager = SessionManager(this)
        reminderHandler = Handler(Looper.getMainLooper())

        loadReminderInterval()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "📨 Commande reçue: ${intent?.action}")

        when (intent?.action) {
            ACTION_START_REMINDERS -> {
                startForegroundService()
                startReminders()
            }
            ACTION_STOP_REMINDERS -> {
                stopReminders()
                stopSelf()
            }
            ACTION_UPDATE_INTERVAL -> {
                val newInterval = intent.getLongExtra("interval", REMINDER_INTERVAL)
                updateReminderInterval(newInterval)
            }
        }

        return START_STICKY
    }

    private fun loadReminderInterval() {
        val prefs = getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
        REMINDER_INTERVAL = prefs.getLong("reminder_interval", ReminderIntervals.SIX_HOURS)
        Log.d(TAG, "📱 Intervalle chargé: ${formatInterval(REMINDER_INTERVAL)}")
    }

    private fun saveReminderInterval(intervalMs: Long) {
        val prefs = getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
        prefs.edit().putLong("reminder_interval", intervalMs).apply()
        Log.d(TAG, "💾 Intervalle sauvegardé: ${formatInterval(intervalMs)}")
    }

    private fun updateReminderInterval(newInterval: Long) {
        val oldInterval = REMINDER_INTERVAL
        REMINDER_INTERVAL = newInterval
        saveReminderInterval(newInterval)

        Log.d(TAG, "🔄 Mise à jour intervalle: ${formatInterval(oldInterval)} → ${formatInterval(newInterval)}")

        if (isRunning) {
            stopReminders()
            startReminders()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Rappels MoneyWise",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications de rappel pour emprunts, acquittements et projets"
                setShowBadge(true)
                enableVibration(true)
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
            .setContentTitle("Rappels MoneyWise")
            .setContentText("Service actif - Intervalle: ${formatInterval(REMINDER_INTERVAL)}")
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "🔔 Service de rappel en premier plan démarré")
    }

    private fun startReminders() {
        if (isRunning) {
            Log.d(TAG, "⚠️ Rappels déjà en cours")
            return
        }

        isRunning = true
        Log.d(TAG, "🚀 Démarrage des rappels toutes les ${formatInterval(REMINDER_INTERVAL)}")

        scheduleNextReminder()
    }

    private fun scheduleNextReminder() {
        if (!isRunning || !sessionManager.isLoggedIn()) {
            Log.d(TAG, "⚠️ Arrêt des rappels - isRunning: $isRunning, isLoggedIn: ${sessionManager.isLoggedIn()}")
            return
        }

        reminderRunnable = Runnable {
            Log.d(TAG, "⏰ Exécution du rappel programmé")
            checkAndSendReminders()

            if (isRunning) {
                scheduleNextReminder()
            }
        }

        reminderHandler?.postDelayed(reminderRunnable!!, REMINDER_INTERVAL)
        Log.d(TAG, "📅 Prochain rappel programmé dans ${formatInterval(REMINDER_INTERVAL)}")
    }

    private fun stopReminders() {
        isRunning = false
        reminderRunnable?.let {
            reminderHandler?.removeCallbacks(it)
            Log.d(TAG, "🗑️ Callbacks supprimés")
        }
        Log.d(TAG, "🛑 Rappels arrêtés")
    }

    private fun checkAndSendReminders() {
        Log.d(TAG, "🔍 Vérification des rappels...")

        serviceScope.launch {
            try {
                val emprunts = runBlocking { database.empruntDao().getAllEmprunts() }
                val acquittements = runBlocking {
                    database.AcquittementDao().getAllAcquittementSync()
                }
                val projets = runBlocking { database.ProjetDao().getAllProjetSync() }

                Log.d(TAG, "📊 Données récupérées - Emprunts: ${emprunts.size}, Acquittements: ${acquittements.size}, Projets: ${projets.size}")

                analyzeAndNotify(emprunts, acquittements, projets)

            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la vérification des rappels", e)
            }
        }
    }

    private suspend fun analyzeAndNotify(
        emprunts: List<com.example.moneywise.data.entity.Emprunt>,
        acquittements: List<com.example.moneywise.data.entity.Acquittement>,
        projets: List<com.example.moneywise.data.entity.Projet>
    ) {
        val currentDate = Date()
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 7)
        val weekFromNow = calendar.time

        Log.d(TAG, "📅 Date actuelle: ${formatDate(currentDate)}")
        Log.d(TAG, "📅 Date limite (7 jours): ${formatDate(weekFromNow)}")

        // Analyser les emprunts
        val empruntsUrgents = emprunts.filter { !it.estRembourse && it.date_remboursement.before(weekFromNow) && it.date_remboursement.after(currentDate) }
        val empruntsEchus = emprunts.filter { !it.estRembourse && it.date_remboursement.before(currentDate) }

        // Analyser les acquittements
        val acquittementsUrgents = acquittements.filter { it.date_remise_crédit.before(weekFromNow) && it.date_remise_crédit.after(currentDate) }
        val acquittementsEchus = acquittements.filter { it.date_remise_crédit.before(currentDate) }

        // Analyser les projets
        val projetsUrgents = projets.filter {
            it.progression < 100 && it.date_limite.before(weekFromNow) && it.date_limite.after(currentDate)
        }
        val projetsEchus = projets.filter {
            it.progression < 100 && it.date_limite.before(currentDate)
        }

        Log.d(TAG, "🔍 Analyse terminée:")
        Log.d(TAG, "💰 Emprunts échus: ${empruntsEchus.size}, urgents: ${empruntsUrgents.size}")
        Log.d(TAG, "📋 Acquittements échus: ${acquittementsEchus.size}, urgents: ${acquittementsUrgents.size}")
        Log.d(TAG, "🎯 Projets échus: ${projetsEchus.size}, urgents: ${projetsUrgents.size}")

        // Envoyer les notifications avec détails
        if (empruntsEchus.isNotEmpty() || empruntsUrgents.isNotEmpty()) {
            sendDetailedEmpruntNotification(empruntsEchus, empruntsUrgents)
        }

        if (acquittementsEchus.isNotEmpty() || acquittementsUrgents.isNotEmpty()) {
            sendDetailedAcquittementNotification(acquittementsEchus, acquittementsUrgents)
        }

        if (projetsEchus.isNotEmpty() || projetsUrgents.isNotEmpty()) {
            sendDetailedProjetNotification(projetsEchus, projetsUrgents)
        }

        // Notification de résumé général
        sendGeneralSummaryNotification(emprunts, acquittements, projets, empruntsEchus.size + empruntsUrgents.size, acquittementsEchus.size + acquittementsUrgents.size, projetsEchus.size + projetsUrgents.size)
    }

    private fun sendDetailedEmpruntNotification(
        empruntsEchus: List<com.example.moneywise.data.entity.Emprunt>,
        empruntsUrgents: List<com.example.moneywise.data.entity.Emprunt>
    ) {
        val title = when {
            empruntsEchus.isNotEmpty() && empruntsUrgents.isNotEmpty() ->
                "💰 ${empruntsEchus.size} emprunts en retard, ${empruntsUrgents.size} urgents"
            empruntsEchus.isNotEmpty() -> "💰 ${empruntsEchus.size} emprunt(s) en retard !"
            else -> "⏰ ${empruntsUrgents.size} emprunt(s) urgent(s)"
        }

        val message = buildString {
            if (empruntsEchus.isNotEmpty()) {
                append("🚨 EN RETARD:\n")
                empruntsEchus.forEach { emprunt ->
                    append("• ${emprunt.nom_emprunte}\n")
                    append("  💵 ${formatAmount(emprunt.montant)}\n")
                    append("  📅 Échéance: ${formatDate(emprunt.date_remboursement)}\n")
                    append("  ⏱️ Retard: ${calculateDaysLate(emprunt.date_remboursement)} jours\n\n")
                }
            }

            if (empruntsUrgents.isNotEmpty()) {
                append("⚠️ URGENT (cette semaine):\n")
                empruntsUrgents.forEach { emprunt ->
                    append("• ${emprunt.nom_emprunte}\n")
                    append("  💵 ${formatAmount(emprunt.montant)}\n")
                    append("  📅 Échéance: ${formatDate(emprunt.date_remboursement)}\n")
                    append("  ⏳ Dans: ${calculateDaysRemaining(emprunt.date_remboursement)} jours\n\n")
                }
            }
        }

        notificationHelper.sendNotification(
            id = 3001,
            title = title,
            message = message.trim(),
            priority = if (empruntsEchus.isNotEmpty()) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        )

        Log.d(TAG, "📱 Notification emprunt détaillée envoyée")
    }

    private fun sendDetailedAcquittementNotification(
        acquittementsEchus: List<com.example.moneywise.data.entity.Acquittement>,
        acquittementsUrgents: List<com.example.moneywise.data.entity.Acquittement>
    ) {
        val title = when {
            acquittementsEchus.isNotEmpty() && acquittementsUrgents.isNotEmpty() ->
                "📋 ${acquittementsEchus.size} acquittements en retard, ${acquittementsUrgents.size} urgents"
            acquittementsEchus.isNotEmpty() -> "📋 ${acquittementsEchus.size} acquittement(s) en retard !"
            else -> "⏰ ${acquittementsUrgents.size} acquittement(s) urgent(s)"
        }

        val message = buildString {
            if (acquittementsEchus.isNotEmpty()) {
                append("🚨 EN RETARD:\n")
                acquittementsEchus.forEach { acquittement ->
                    append("• ${acquittement.personne_acquittement}\n")
                    append("  💵 ${formatAmount(acquittement.montant)}\n")
                    append("  📅 Date prévue: ${formatDate(acquittement.date_remise_crédit)}\n")
                    append("  ⏱️ Retard: ${calculateDaysLate(acquittement.date_remise_crédit)} jours\n")
                }
            }

            if (acquittementsUrgents.isNotEmpty()) {
                append("⚠️ URGENT (cette semaine):\n")
                acquittementsUrgents.forEach { acquittement ->
                    append("• ${acquittement.personne_acquittement}\n")
                    append("  💵 ${formatAmount(acquittement.montant)}\n")
                    append("  📅 Date prévue: ${formatDate(acquittement.date_remise_crédit)}\n")
                    append("  ⏳ Dans: ${calculateDaysRemaining(acquittement.date_remise_crédit)} jours\n")
                }
            }
        }

        notificationHelper.sendNotification(
            id = 3002,
            title = title,
            message = message.trim(),
            priority = if (acquittementsEchus.isNotEmpty()) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        )

        Log.d(TAG, "📱 Notification acquittement détaillée envoyée")
    }

    private fun sendDetailedProjetNotification(
        projetsEchus: List<com.example.moneywise.data.entity.Projet>,
        projetsUrgents: List<com.example.moneywise.data.entity.Projet>
    ) {
        val title = when {
            projetsEchus.isNotEmpty() && projetsUrgents.isNotEmpty() ->
                "🎯 ${projetsEchus.size} projets en retard, ${projetsUrgents.size} urgents"
            projetsEchus.isNotEmpty() -> "🎯 ${projetsEchus.size} projet(s) en retard !"
            else -> "⏰ ${projetsUrgents.size} projet(s) urgent(s)"
        }

        val message = buildString {
            if (projetsEchus.isNotEmpty()) {
                append("🚨 EN RETARD:\n")
                projetsEchus.forEach { projet ->
                    append("• ${projet.nom}\n")
                    append("  📊 Progression: ${projet.progression}%\n")
                    append("  💰 Budget: ${formatAmount(projet.montant_actuel)}/${formatAmount(projet.montant_necessaire)}\n")
                    append("  📅 Échéance: ${formatDate(projet.date_limite)}\n")
                    append("  ⏱️ Retard: ${calculateDaysLate(projet.date_limite)} jours\n")
                }
            }

            if (projetsUrgents.isNotEmpty()) {
                append("⚠️ URGENT (cette semaine):\n")
                projetsUrgents.forEach { projet ->
                    append("• ${projet.nom}\n")
                    append("  📊 Progression: ${projet.progression}%\n")
                    append("  💰 Budget: ${formatAmount(projet.montant_actuel)}/${formatAmount(projet.montant_necessaire)}\n")
                    append("  📅 Échéance: ${formatDate(projet.date_limite)}\n")
                    append("  ⏳ Dans: ${calculateDaysRemaining(projet.date_limite)} jours\n")
                }
            }
        }

        notificationHelper.sendNotification(
            id = 3003,
            title = title,
            message = message.trim(),
            priority = if (projetsEchus.isNotEmpty()) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT
        )

        Log.d(TAG, "📱 Notification projet détaillée envoyée")
    }

    private fun sendGeneralSummaryNotification(
        emprunts: List<com.example.moneywise.data.entity.Emprunt>,
        acquittements: List<com.example.moneywise.data.entity.Acquittement>,
        projets: List<com.example.moneywise.data.entity.Projet>,
        empruntsAlerts: Int,
        acquittementsAlerts: Int,
        projetsAlerts: Int
    ) {
        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val totalAlerts = empruntsAlerts + acquittementsAlerts + projetsAlerts

        val title = if (totalAlerts > 0) {
            "🔔 MoneyWise - $totalAlerts alerte(s) - $currentTime"
        } else {
            "✅ MoneyWise - Tout va bien - $currentTime"
        }

        val message = buildString {
            append("📊 RÉSUMÉ GÉNÉRAL:\n")
            append("🕐 Intervalle: ${formatInterval(REMINDER_INTERVAL)}\n\n")

            // Statistiques des emprunts
            val empruntsActifs = emprunts.filter { !it.estRembourse }
            val totalEmprunts = empruntsActifs.sumOf { it.montant }
            append("💰 EMPRUNTS:\n")
            append("  • Total: ${emprunts.size} (${empruntsActifs.size} actifs)\n")
            append("  • Montant total: ${formatAmount(totalEmprunts)}\n")
            if (empruntsAlerts > 0) append("  • ⚠️ Alertes: $empruntsAlerts\n")
            append("\n")

            // Statistiques des acquittements
            val totalAcquittements = acquittements.sumOf { it.montant }
            append("📋 ACQUITTEMENTS:\n")
            append("  • Total: ${acquittements.size}\n")
            append("  • Montant total: ${formatAmount(totalAcquittements)}\n")
            if (acquittementsAlerts > 0) append("  • ⚠️ Alertes: $acquittementsAlerts\n")
            append("\n")

            // Statistiques des projets
            val projetsActifs = projets.filter { it.progression < 100 }
            val totalBudgetProjets = projets.sumOf { it.montant_necessaire }
            val progressionMoyenne = if (projets.isNotEmpty()) projets.map { it.progression }.average() else 0.0
            append("🎯 PROJETS:\n")
            append("  • Total: ${projets.size} (${projetsActifs.size} en cours)\n")
            append("  • Budget total: ${formatAmount(totalBudgetProjets)}\n")
            append("  • Progression moyenne: ${String.format("%.1f", progressionMoyenne)}%\n")
            if (projetsAlerts > 0) append("  • ⚠️ Alertes: $projetsAlerts\n")

            if (totalAlerts == 0) {
                append("\n🎉 Aucune action urgente requise !")
            }
        }

        notificationHelper.sendNotification(
            id = 3000,
            title = title,
            message = message.trim(),
            priority = if (totalAlerts > 0) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_LOW
        )

        Log.d(TAG, "📱 Notification résumé général envoyée: $totalAlerts alertes")
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(date)
    }

    private fun formatAmount(amount: Double): String {
        return "${String.format("%,.0f", amount)} MGA"
    }

    private fun calculateDaysLate(dueDate: Date): Int {
        val currentDate = Date()
        val diffInMillis = currentDate.time - dueDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun calculateDaysRemaining(dueDate: Date): Int {
        val currentDate = Date()
        val diffInMillis = dueDate.time - currentDate.time
        return (diffInMillis / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun formatInterval(intervalMs: Long): String {
        return when (intervalMs) {
            ReminderIntervals.THIRTY_SECONDS -> "30 secondes"
            ReminderIntervals.ONE_MINUTE -> "1 minute"
            ReminderIntervals.FIVE_MINUTES -> "5 minutes"
            ReminderIntervals.FIFTEEN_MINUTES -> "15 minutes"
            ReminderIntervals.THIRTY_MINUTES -> "30 minutes"
            ReminderIntervals.ONE_HOUR -> "1 heure"
            ReminderIntervals.THREE_HOURS -> "3 heures"
            ReminderIntervals.SIX_HOURS -> "6 heures"
            ReminderIntervals.TWELVE_HOURS -> "12 heures"
            ReminderIntervals.ONE_DAY -> "24 heures"
            else -> "${intervalMs / 1000} secondes"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopReminders()
        Log.d(TAG, "💀 Service de rappel détruit")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}