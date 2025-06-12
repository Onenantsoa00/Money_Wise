package com.example.moneywise.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.moneywise.MainActivity
import com.example.moneywise.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "NotificationHelper"
        private const val CHANNEL_ID_TRANSACTIONS = "transactions_channel"
        private const val CHANNEL_ID_REMINDERS = "reminders_channel"
        private const val CHANNEL_ID_SERVICES = "services_channel"
        private const val CHANNEL_ID = "reminder_channel" // Pour compatibilité avec votre code existant
    }

    private var mediaPlayer: MediaPlayer? = null

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Canal pour les transactions (NOUVEAU)
            val transactionsChannel = NotificationChannel(
                CHANNEL_ID_TRANSACTIONS,
                "Transactions Mobile Money",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications pour les transactions mobile money détectées"
            }

            // Canal pour les rappels (EXISTANT - préservé)
            val remindersChannel = NotificationChannel(
                CHANNEL_ID,
                "Rappels",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Rappels pour emprunts, acquittements et projets"
            }

            // Canal pour les services (NOUVEAU)
            val servicesChannel = NotificationChannel(
                CHANNEL_ID_SERVICES,
                "Services en arrière-plan",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications pour les services en arrière-plan"
                setShowBadge(false)
            }

            notificationManager.createNotificationChannel(transactionsChannel)
            notificationManager.createNotificationChannel(remindersChannel)
            notificationManager.createNotificationChannel(servicesChannel)
        }
    }

    // PRÉSERVÉE
    fun sendNotification(
        id: Int,
        title: String,
        message: String,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT,
        playSound: Boolean = true
    ) {
        // Vérifier les permissions pour Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w(TAG, "❌ Permission de notification non accordée")
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, id, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentIntent(pendingIntent)
            .setPriority(priority)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
            Log.d(TAG, "✅ Notification envoyée: $title")

            // Jouer le son de pièce d'argent
            if (playSound) {
                playCoinDropSound()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'envoi de la notification: ${e.message}")
        }
    }

    // NOUVELLES MÉTHODES POUR LES TRANSACTIONS
    fun showTransactionNotification(
        title: String,
        content: String,
        transactionType: String,
        amount: Double
    ) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "❌ Permission de notification non accordée")
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRANSACTIONS)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_account_balance_wallet)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        // Jouer le son de pièce
        playCoinDropSound()
    }

    fun showReminderNotification(
        title: String,
        content: String,
        notificationId: Int
    ) {
        if (!hasNotificationPermission()) {
            Log.w(TAG, "❌ Permission de notification non accordée")
            return
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_account_balance_wallet)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }

    // PRÉSERVÉE
    private fun playCoinDropSound() {
        try {
            // Libérer le MediaPlayer précédent s'il existe
            mediaPlayer?.release()

            // Créer un nouveau MediaPlayer
            mediaPlayer = MediaPlayer.create(context, R.raw.coin_drop)

            if (mediaPlayer != null) {
                // Configurer les attributs audio
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    mediaPlayer?.setAudioAttributes(audioAttributes)
                }

                // Configurer le listener pour libérer les ressources après lecture
                mediaPlayer?.setOnCompletionListener { mp ->
                    mp.release()
                    mediaPlayer = null
                    Log.d(TAG, "🔊 Son de pièce terminé et ressources libérées")
                }

                // Configurer le listener d'erreur
                mediaPlayer?.setOnErrorListener { mp, what, extra ->
                    Log.e(TAG, "❌ Erreur MediaPlayer: what=$what, extra=$extra")
                    mp.release()
                    mediaPlayer = null
                    true
                }

                // Démarrer la lecture
                mediaPlayer?.start()
                Log.d(TAG, "🪙 Son de pièce d'argent joué")
            } else {
                Log.w(TAG, "⚠️ Impossible de créer MediaPlayer pour le son")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la lecture du son: ${e.message}")
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    // PRÉSERVÉE
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    // PRÉSERVÉE
    fun release() {
        try {
            mediaPlayer?.release()
            mediaPlayer = null
            Log.d(TAG, "🔇 Ressources MediaPlayer libérées")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la libération des ressources: ${e.message}")
        }
    }
}
