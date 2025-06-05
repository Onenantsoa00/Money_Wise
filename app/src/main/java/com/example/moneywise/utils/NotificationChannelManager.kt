package com.example.moneywise.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationChannelManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "NotificationChannelManager"
        const val REMINDER_CHANNEL_ID = "reminder_channel"
        const val FLOATING_WIDGET_CHANNEL_ID = "floating_widget_channel"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createReminderChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Vérifier si le canal existe déjà
                val existingChannel = notificationManager.getNotificationChannel(REMINDER_CHANNEL_ID)
                if (existingChannel != null) {
                    Log.d(TAG, "✅ Canal de rappel déjà créé")
                    return
                }

                // Créer le canal avec une importance élevée
                val name = "Rappels"
                val description = "Notifications de rappel pour les emprunts, acquittements et projets"
                val importance = NotificationManager.IMPORTANCE_HIGH
                val channel = NotificationChannel(REMINDER_CHANNEL_ID, name, importance).apply {
                    this.description = description
                    enableLights(true)
                    enableVibration(true)

                    // Configurer le son par défaut
                    val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    val audioAttributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                    setSound(defaultSoundUri, audioAttributes)
                }

                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "✅ Canal de rappel créé avec succès")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la création du canal de rappel: ${e.message}")
            }
        }
    }

    fun createFloatingWidgetChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                // Vérifier si le canal existe déjà
                val existingChannel = notificationManager.getNotificationChannel(FLOATING_WIDGET_CHANNEL_ID)
                if (existingChannel != null) {
                    Log.d(TAG, "✅ Canal du widget flottant déjà créé")
                    return
                }

                // Créer le canal avec une importance basse
                val name = "Widget flottant"
                val description = "Notifications pour le service du widget flottant"
                val importance = NotificationManager.IMPORTANCE_LOW
                val channel = NotificationChannel(FLOATING_WIDGET_CHANNEL_ID, name, importance).apply {
                    this.description = description
                    enableLights(false)
                    enableVibration(false)
                    setSound(null, null)
                }

                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "✅ Canal du widget flottant créé avec succès")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de la création du canal du widget flottant: ${e.message}")
            }
        }
    }
}
