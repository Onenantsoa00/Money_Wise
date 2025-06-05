package com.example.moneywise.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.moneywise.R
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.entity.Acquittement
import com.example.moneywise.data.entity.Emprunt
import com.example.moneywise.data.entity.Projet
import com.example.moneywise.utils.NotificationChannelManager
import com.example.moneywise.utils.NotificationHelper
import com.example.moneywise.utils.ReminderManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class ReminderNotificationService : Service() {

    @Inject
    lateinit var db: AppDatabase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderManager: ReminderManager

    @Inject
    lateinit var notificationChannelManager: NotificationChannelManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val alarmManager by lazy { getSystemService(Context.ALARM_SERVICE) as AlarmManager }

    companion object {
        private const val TAG = "ReminderNotificationService"
        private const val NOTIFICATION_ID = 2001
        private const val REQUEST_CODE = 1234

        // IDs pour différents types de notifications
        private const val EMPRUNT_NOTIFICATION_ID = 3001
        private const val ACQUITTEMENT_NOTIFICATION_ID = 3002
        private const val PROJET_NOTIFICATION_ID = 3003
        private const val TEST_NOTIFICATION_ID = 3000
        private const val GENERAL_NOTIFICATION_ID = 3004

        // Constantes d'intervalles pour éviter les erreurs de référence
        private const val FIFTEEN_SECONDS = 15 * 1000L
        private const val THREE_HOURS = 3 * 60 * 60 * 1000L
        private const val SIX_HOURS = 6 * 60 * 60 * 1000L
        private const val TWELVE_HOURS = 12 * 60 * 60 * 1000L
        private const val ONE_DAY = 24 * 60 * 60 * 1000L
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "🚀 Service de rappel créé")

        // Créer les canaux de notification
        notificationChannelManager.createReminderChannel()
        notificationChannelManager.createFloatingWidgetChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "🔄 Service de rappel démarré")

        // Vérifier si les rappels sont activés
        if (!reminderManager.areRemindersEnabled()) {
            Log.d(TAG, "❌ Les rappels sont désactivés, arrêt du service")
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            // Démarrer en tant que service de premier plan
            startForeground(NOTIFICATION_ID, createForegroundNotification())

            // Traiter les rappels de manière asynchrone
            processReminders()

            // Programmer la prochaine exécution
            scheduleNextReminder()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur dans onStartCommand: ${e.message}", e)
            stopSelf()
            return START_NOT_STICKY
        }

        return START_STICKY
    }

    private fun createForegroundNotification() = NotificationCompat.Builder(this, NotificationChannelManager.FLOATING_WIDGET_CHANNEL_ID)
        .setContentTitle("MoneyWise Rappels")
        .setContentText("Surveillance des rappels en cours...")
        .setSmallIcon(R.drawable.ic_notifications)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setSilent(true)
        .build()

    private fun processReminders() {
        serviceScope.launch {
            try {
                Log.d(TAG, "🔍 Vérification des rappels...")

                var notificationsSent = 0

                // 🔥 CORRECTION: Utiliser la nouvelle méthode synchrone pour les emprunts
                val empruntsNonRembourses = withContext(Dispatchers.IO) {
                    try {
                        db.empruntDao().getEmpruntsNonRemboursesSync()
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erreur lors de la récupération des emprunts: ${e.message}")
                        emptyList<Emprunt>()
                    }
                }

                if (empruntsNonRembourses.isNotEmpty()) {
                    val totalMontant = empruntsNonRembourses.sumOf { it.montant }

                    val message = if (empruntsNonRembourses.size == 1) {
                        "Vous avez un emprunt non remboursé de ${empruntsNonRembourses[0].montant} MGA à ${empruntsNonRembourses[0].nom_emprunte}."
                    } else {
                        "Vous avez ${empruntsNonRembourses.size} emprunts non remboursés totalisant ${String.format("%.0f", totalMontant)} MGA."
                    }

                    notificationHelper.sendNotification(
                        id = EMPRUNT_NOTIFICATION_ID,
                        title = "💰 Rappel d'emprunt",
                        message = message,
                        playSound = true
                    )
                    notificationsSent++
                    Log.d(TAG, "📤 Notification d'emprunt envoyée")
                }

                // 🔥 CORRECTION: Utiliser la méthode existante pour les acquittements
                val acquittements = withContext(Dispatchers.IO) {
                    try {
                        db.acquittementDao().getAcquittementNonPayes()
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erreur lors de la récupération des acquittements: ${e.message}")
                        emptyList<Acquittement>()
                    }
                }

                if (acquittements.isNotEmpty()) {
                    val totalMontant = acquittements.sumOf { it.montant }

                    val message = if (acquittements.size == 1) {
                        "Vous avez un acquittement de ${acquittements[0].montant} MGA pour ${acquittements[0].personne_acquittement}."
                    } else {
                        "Vous avez ${acquittements.size} acquittements totalisant ${String.format("%.0f", totalMontant)} MGA."
                    }

                    notificationHelper.sendNotification(
                        id = ACQUITTEMENT_NOTIFICATION_ID,
                        title = "💸 Rappel d'acquittement",
                        message = message,
                        playSound = true
                    )
                    notificationsSent++
                    Log.d(TAG, "📤 Notification d'acquittement envoyée")
                }

                // 🔥 CORRECTION: Utiliser la méthode existante pour les projets
                val projetsEnCours = withContext(Dispatchers.IO) {
                    try {
                        db.projetDao().getProjetsEnCours()
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Erreur lors de la récupération des projets: ${e.message}")
                        emptyList<Projet>()
                    }
                }

                if (projetsEnCours.isNotEmpty()) {
                    val message = if (projetsEnCours.size == 1) {
                        "Vous avez un projet en cours: ${projetsEnCours[0].nom} (${projetsEnCours[0].progression}% complété)."
                    } else {
                        "Vous avez ${projetsEnCours.size} projets en cours qui nécessitent votre attention."
                    }

                    notificationHelper.sendNotification(
                        id = PROJET_NOTIFICATION_ID,
                        title = "📊 Rappel de projet",
                        message = message,
                        playSound = true
                    )
                    notificationsSent++
                    Log.d(TAG, "📤 Notification de projet envoyée")
                }

                // Gestion des cas spéciaux
                val currentInterval = reminderManager.getCurrentInterval()
                val isTestInterval = reminderManager.isTestInterval(currentInterval)

                if (notificationsSent == 0) {
                    if (isTestInterval) {
                        // Pour les tests, toujours envoyer une notification
                        notificationHelper.sendNotification(
                            id = TEST_NOTIFICATION_ID,
                            title = "🧪 Test de notification",
                            message = "Ceci est une notification de test avec son de pièce. Intervalle: ${reminderManager.formatInterval(currentInterval)}",
                            playSound = true
                        )
                        Log.d(TAG, "📤 Notification de test envoyée")
                    } else {
                        // Pour les intervalles normaux, envoyer un rappel général occasionnellement
                        if (shouldSendGeneralReminder()) {
                            notificationHelper.sendNotification(
                                id = GENERAL_NOTIFICATION_ID,
                                title = "✅ MoneyWise",
                                message = "Tout va bien ! Aucun emprunt, acquittement ou projet nécessitant votre attention immédiate.",
                                playSound = false
                            )
                            Log.d(TAG, "📤 Notification générale envoyée")
                        } else {
                            Log.d(TAG, "ℹ️ Aucun rappel nécessaire pour le moment")
                        }
                    }
                } else {
                    Log.d(TAG, "✅ $notificationsSent notification(s) de rappel envoyée(s)")
                }

            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors du traitement des rappels: ${e.message}", e)
            }
        }
    }

    /**
     * Détermine s'il faut envoyer un rappel général
     * (par exemple, une fois par jour pour les intervalles longs)
     */
    private fun shouldSendGeneralReminder(): Boolean {
        val currentInterval = reminderManager.getCurrentInterval()

        // Envoyer un rappel général seulement pour les intervalles de 12h ou plus
        return when (currentInterval) {
            TWELVE_HOURS,
            ONE_DAY -> {
                // Utiliser un système simple basé sur l'heure pour éviter les rappels trop fréquents
                val currentHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                currentHour == 9 || currentHour == 18 // 9h ou 18h
            }
            else -> false
        }
    }

    private fun scheduleNextReminder() {
        try {
            // Vérifier si les rappels sont toujours activés
            if (!reminderManager.areRemindersEnabled()) {
                Log.d(TAG, "❌ Les rappels ont été désactivés, pas de programmation")
                stopSelf()
                return
            }

            // Obtenir l'intervalle configuré
            val interval = reminderManager.getCurrentInterval()

            // Créer l'intent pour le service
            val intent = Intent(this, ReminderNotificationService::class.java)
            val pendingIntent = PendingIntent.getService(
                this,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Annuler toute alarme existante pour éviter les doublons
            alarmManager.cancel(pendingIntent)

            // Programmer la prochaine exécution
            val nextExecutionTime = SystemClock.elapsedRealtime() + interval

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    nextExecutionTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    nextExecutionTime,
                    pendingIntent
                )
            }

            val intervalText = reminderManager.formatInterval(interval)
            Log.d(TAG, "⏰ Prochaine exécution programmée dans $intervalText")

            // Avertissement pour les intervalles de test
            if (reminderManager.isTestInterval(interval)) {
                Log.w(TAG, "🧪 ATTENTION: Mode test activé - Intervalle très court ($intervalText)")
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la programmation du prochain rappel: ${e.message}", e)
            // En cas d'erreur, arrêter le service pour éviter les boucles infinies
            stopSelf()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "🛑 Service de rappel arrêté")

        try {
            // Annuler toutes les coroutines en cours
            serviceScope.cancel()

            // Libérer les ressources du NotificationHelper
            notificationHelper.release()

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la destruction du service: ${e.message}")
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d(TAG, "📱 Application fermée, mais service continue")

        // Le service continue à fonctionner même si l'application est fermée
        // C'est le comportement souhaité pour les rappels
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "⚠️ Mémoire faible détectée")

        // En cas de mémoire faible, on peut libérer certaines ressources
        // mais on garde le service actif
    }
}
