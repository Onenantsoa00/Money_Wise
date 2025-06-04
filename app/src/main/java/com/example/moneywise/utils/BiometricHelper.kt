package com.example.moneywise.utils

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager

/**
 * Classe utilitaire pour gérer l'authentification biométrique
 */
object BiometricHelper {

    /**
     * Vérifie si l'appareil supporte l'authentification biométrique
     */
    fun isBiometricAvailable(context: Context): BiometricAvailability {
        val biometricManager = BiometricManager.from(context)

        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                BiometricAvailability.AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                BiometricAvailability.NO_HARDWARE
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                BiometricAvailability.HARDWARE_UNAVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                BiometricAvailability.NONE_ENROLLED
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                BiometricAvailability.SECURITY_UPDATE_REQUIRED
            }
            BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> {
                BiometricAvailability.UNSUPPORTED
            }
            BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> {
                BiometricAvailability.STATUS_UNKNOWN
            }
            else -> BiometricAvailability.NOT_AVAILABLE
        }
    }

    /**
     * Obtient les types d'authentification disponibles
     */
    fun getAvailableAuthenticationTypes(context: Context): List<AuthenticationType> {
        val types = mutableListOf<AuthenticationType>()
        val biometricManager = BiometricManager.from(context)

        // Vérifier l'empreinte digitale
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS) {
            types.add(AuthenticationType.FINGERPRINT)
        }

        // Vérifier la reconnaissance faciale (disponible sur certains appareils)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS) {
                types.add(AuthenticationType.FACE)
            }
        }

        // Vérifier le code PIN/motif/mot de passe de l'appareil
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS) {
            types.add(AuthenticationType.DEVICE_CREDENTIAL)
        }

        return types
    }

    /**
     * Obtient le message d'authentification approprié
     */
    fun getAuthenticationMessage(types: List<AuthenticationType>): String {
        return when {
            types.contains(AuthenticationType.FINGERPRINT) && types.contains(AuthenticationType.FACE) -> {
                "Utilisez votre empreinte digitale, reconnaissance faciale ou code de l'appareil"
            }
            types.contains(AuthenticationType.FINGERPRINT) -> {
                "Utilisez votre empreinte digitale ou code de l'appareil"
            }
            types.contains(AuthenticationType.FACE) -> {
                "Utilisez la reconnaissance faciale ou code de l'appareil"
            }
            types.contains(AuthenticationType.DEVICE_CREDENTIAL) -> {
                "Utilisez votre code PIN, motif ou mot de passe de l'appareil"
            }
            else -> "Authentification requise"
        }
    }

    /**
     * Énumération des types d'authentification disponibles
     */
    enum class AuthenticationType {
        FINGERPRINT,
        FACE,
        DEVICE_CREDENTIAL
    }

    /**
     * Énumération de la disponibilité biométrique
     */
    enum class BiometricAvailability {
        AVAILABLE,
        NO_HARDWARE,
        HARDWARE_UNAVAILABLE,
        NONE_ENROLLED,
        SECURITY_UPDATE_REQUIRED,
        UNSUPPORTED,
        STATUS_UNKNOWN,
        NOT_AVAILABLE
    }
}