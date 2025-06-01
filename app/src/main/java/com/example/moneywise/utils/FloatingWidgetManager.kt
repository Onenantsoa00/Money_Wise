package com.example.moneywise.utils

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.moneywise.services.FloatingBalanceService

class FloatingWidgetManager(private val context: Context) {

    companion object {
        private const val TAG = "FloatingWidgetManager"
        const val OVERLAY_PERMISSION_REQUEST_CODE = 1001
        private const val PREFS_NAME = "floating_widget_prefs"
        private const val KEY_WIDGET_ENABLED = "widget_enabled"
        private const val KEY_PERMISSION_REQUESTED = "permission_requested"
        private const val KEY_PERMISSION_DENIED_COUNT = "permission_denied_count"
        private const val MAX_PERMISSION_REQUESTS = 5
    }

    private val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Vérifie si l'application a la permission d'affichage par-dessus d'autres apps
     */
    fun hasOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val hasPermission = Settings.canDrawOverlays(context)
            Log.d(TAG, "🔍 Vérification permission d'overlay: $hasPermission")
            hasPermission
        } else {
            Log.d(TAG, "📱 Android < M, permission d'overlay accordée automatiquement")
            true
        }
    }

    /**
     * Vérifie si le widget est activé par l'utilisateur
     */
    fun isWidgetEnabled(): Boolean {
        val enabled = sharedPrefs.getBoolean(KEY_WIDGET_ENABLED, false)
        Log.d(TAG, "🎛️ Widget activé: $enabled")
        return enabled
    }

    /**
     * Active/désactive le widget
     */
    fun setWidgetEnabled(enabled: Boolean) {
        sharedPrefs.edit()
            .putBoolean(KEY_WIDGET_ENABLED, enabled)
            .apply()
        Log.d(TAG, "⚙️ Widget ${if (enabled) "✅ activé" else "❌ désactivé"}")
    }

    /**
     * Vérifie si on doit demander la permission
     */
    private fun shouldRequestPermission(): Boolean {
        val permissionRequested = sharedPrefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
        val deniedCount = sharedPrefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0)

        Log.d(TAG, "📊 Statistiques permission - Demandée: $permissionRequested, Refusée: $deniedCount fois")

        if (permissionRequested && deniedCount >= MAX_PERMISSION_REQUESTS) {
            Log.w(TAG, "🚫 Permission refusée trop de fois ($deniedCount/$MAX_PERMISSION_REQUESTS), arrêt des demandes automatiques")
            return false
        }

        return !permissionRequested || deniedCount < MAX_PERMISSION_REQUESTS
    }

    /**
     * Demande la permission avec plus de flexibilité
     */
    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Log.d(TAG, "🔐 Demande de permission d'overlay")
                markPermissionRequested()
                openOverlaySettings()
            } else {
                Log.d(TAG, "✅ Permission d'overlay déjà accordée")
                resetPermissionPreferences()
            }
        }
    }

    /**
     * Force la demande de permission (ignore les compteurs)
     */
    fun forceRequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Log.d(TAG, "🔥 FORCE - Demande de permission d'overlay")
                openOverlaySettings()
            }
        }
    }

    /**
     * Marque que la permission a été demandée
     */
    private fun markPermissionRequested() {
        sharedPrefs.edit()
            .putBoolean(KEY_PERMISSION_REQUESTED, true)
            .apply()
        Log.d(TAG, "📝 Permission marquée comme demandée")
    }

    /**
     * Incrémente le compteur de refus
     */
    fun markPermissionDenied() {
        val currentCount = sharedPrefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0)
        sharedPrefs.edit()
            .putInt(KEY_PERMISSION_DENIED_COUNT, currentCount + 1)
            .apply()
        Log.w(TAG, "❌ Permission refusée ${currentCount + 1} fois")
    }

    /**
     * Réinitialise les préférences de permission
     */
    fun resetPermissionPreferences() {
        sharedPrefs.edit()
            .putBoolean(KEY_PERMISSION_REQUESTED, false)
            .putInt(KEY_PERMISSION_DENIED_COUNT, 0)
            .apply()
        Log.d(TAG, "🔄 Préférences de permission réinitialisées")
    }

    /**
     * Obtient les statistiques de permission
     */
    fun getPermissionStats(): String {
        val requested = sharedPrefs.getBoolean(KEY_PERMISSION_REQUESTED, false)
        val deniedCount = sharedPrefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0)
        val hasPermission = hasOverlayPermission()
        val isEnabled = isWidgetEnabled()

        return """
            📊 Statistiques du Widget Flottant:
            • Permission accordée: ${if (hasPermission) "✅ Oui" else "❌ Non"}
            • Widget activé: ${if (isEnabled) "✅ Oui" else "❌ Non"}
            • Permission demandée: ${if (requested) "✅ Oui" else "❌ Non"}
            • Refus: $deniedCount/$MAX_PERMISSION_REQUESTS fois
        """.trimIndent()
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intent)
                Log.d(TAG, "🔧 Ouverture des paramètres d'overlay")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors de l'ouverture des paramètres", e)
            }
        }
    }

    /**
     * Démarre le widget flottant
     */
    fun startFloatingWidget() {
        Log.d(TAG, "🚀 Tentative de démarrage du widget flottant")

        if (hasOverlayPermission()) {
            try {
                val intent = Intent(context, FloatingBalanceService::class.java).apply {
                    action = FloatingBalanceService.ACTION_START_FLOATING
                }

                // 🔥 CORRECTION: Utiliser startForegroundService pour Android O+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }

                Log.d(TAG, "✅ Service de widget flottant démarré")

                // Marquer le widget comme activé
                setWidgetEnabled(true)

                // Réinitialiser les préférences si permission accordée
                resetPermissionPreferences()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erreur lors du démarrage du service", e)
            }
        } else {
            Log.w(TAG, "🚫 Permission d'overlay manquante")
        }
    }

    /**
     * Démarre automatiquement le widget si les conditions sont remplies
     */
    fun startFloatingWidgetIfEnabled() {
        Log.d(TAG, "🔍 Vérification du démarrage automatique du widget")

        val hasPermission = hasOverlayPermission()
        val isEnabled = isWidgetEnabled()

        Log.d(TAG, "📊 État: Permission=$hasPermission, Activé=$isEnabled")

        if (isEnabled && hasPermission) {
            Log.d(TAG, "✅ Conditions remplies, démarrage automatique du widget")
            startFloatingWidget()
        } else {
            Log.d(TAG, "❌ Conditions non remplies - Widget activé: $isEnabled, Permission: $hasPermission")
        }
    }

    /**
     * Démarre le widget avec demande de permission si nécessaire
     */
    fun startFloatingWidgetWithPermissionRequest() {
        Log.d(TAG, "🔥 Démarrage du widget avec demande de permission si nécessaire")

        if (hasOverlayPermission()) {
            startFloatingWidget()
        } else {
            Log.d(TAG, "🔐 Permission d'overlay manquante, demande de permission")
            if (shouldRequestPermission()) {
                requestOverlayPermission()
            } else {
                Log.w(TAG, "🚫 Trop de demandes, utilisation de forceRequestOverlayPermission")
                forceRequestOverlayPermission()
            }
        }
    }

    /**
     * Arrête le widget et le désactive
     */
    fun stopFloatingWidget() {
        try {
            val intent = Intent(context, FloatingBalanceService::class.java).apply {
                action = FloatingBalanceService.ACTION_STOP_FLOATING
            }
            context.startService(intent)

            // Marquer le widget comme désactivé
            setWidgetEnabled(false)

            Log.d(TAG, "🛑 Arrêt du widget flottant demandé")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de l'arrêt du service", e)
        }
    }

    /**
     * Met à jour le solde du widget
     */
    fun updateFloatingWidget() {
        try {
            val intent = Intent(context, FloatingBalanceService::class.java).apply {
                action = FloatingBalanceService.ACTION_UPDATE_BALANCE
            }
            context.startService(intent)
            Log.d(TAG, "🔄 Mise à jour du widget demandée")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erreur lors de la mise à jour", e)
        }
    }
}