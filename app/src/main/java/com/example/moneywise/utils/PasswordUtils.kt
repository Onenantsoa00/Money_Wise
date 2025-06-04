package com.example.moneywise.utils

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Utilitaire pour le hachage sécurisé des mots de passe
 */
object PasswordUtils {

    private const val SALT_LENGTH = 16
    private const val HASH_ALGORITHM = "SHA-256"
    private const val DELIMITER = ":"

    /**
     * Hache un mot de passe avec un sel aléatoire
     * @return Le mot de passe haché au format "sel:hash"
     */
    fun hashPassword(password: String): String {
        try {
            // Générer un sel aléatoire
            val salt = ByteArray(SALT_LENGTH)
            SecureRandom().nextBytes(salt)

            // Hacher le mot de passe avec le sel
            val hash = getHash(password, salt)

            // Encoder le sel et le hash en Base64 (Android)
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

            // Retourner "sel:hash"
            return "$saltBase64$DELIMITER$hashBase64"
        } catch (e: Exception) {
            // Fallback vers un hash simple en cas d'erreur
            return hashPasswordSimple(password)
        }
    }

    /**
     * Vérifie si un mot de passe correspond à un hash stocké
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        try {
            // Vérifier si c'est un hash avec sel (contient ":")
            if (storedHash.contains(DELIMITER)) {
                return verifyPasswordWithSalt(password, storedHash)
            } else {
                // Hash simple (pour compatibilité avec anciens mots de passe)
                return verifyPasswordSimple(password, storedHash)
            }
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Vérifie un mot de passe avec hash salé
     */
    private fun verifyPasswordWithSalt(password: String, storedHash: String): Boolean {
        try {
            // Séparer le sel et le hash
            val parts = storedHash.split(DELIMITER)
            if (parts.size != 2) return false

            // Décoder le sel et le hash
            val salt = Base64.decode(parts[0], Base64.NO_WRAP)
            val expectedHash = Base64.decode(parts[1], Base64.NO_WRAP)

            // Calculer le hash du mot de passe fourni
            val actualHash = getHash(password, salt)

            // Comparer les hash de manière sécurisée
            return MessageDigest.isEqual(expectedHash, actualHash)
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Vérifie un mot de passe avec hash simple (pour compatibilité)
     */
    private fun verifyPasswordSimple(password: String, storedHash: String): Boolean {
        return try {
            val hashedInput = hashPasswordSimple(password)
            MessageDigest.isEqual(hashedInput.toByteArray(), storedHash.toByteArray())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Hash simple pour compatibilité avec anciens mots de passe
     */
    private fun hashPasswordSimple(password: String): String {
        val digest = MessageDigest.getInstance(HASH_ALGORITHM)
        val hash = digest.digest(password.toByteArray())
        return hash.fold("") { str, it -> str + "%02x".format(it) }
    }

    /**
     * Calcule le hash d'un mot de passe avec un sel donné
     */
    private fun getHash(password: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance(HASH_ALGORITHM)
        md.update(salt)
        return md.digest(password.toByteArray())
    }

    /**
     * Vérifie la force d'un mot de passe
     */
    fun checkPasswordStrength(password: String): PasswordStrength {
        return when {
            password.length < 6 -> PasswordStrength.WEAK
            password.length < 8 -> PasswordStrength.MEDIUM
            password.length >= 8 &&
                    password.any { it.isDigit() } &&
                    password.any { it.isLetter() } &&
                    password.any { it.isUpperCase() } &&
                    password.any { it.isLowerCase() } -> PasswordStrength.STRONG
            else -> PasswordStrength.MEDIUM
        }
    }

    /**
     * Génère un mot de passe aléatoire sécurisé
     */
    fun generateSecurePassword(length: Int = 12): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*"
        val random = SecureRandom()
        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Migre un ancien hash simple vers un hash avec sel
     */
    fun migratePassword(password: String, oldHash: String): String? {
        return if (verifyPasswordSimple(password, oldHash)) {
            hashPassword(password)
        } else {
            null
        }
    }

    /**
     * Vérifie si un hash utilise le nouveau format avec sel
     */
    fun isModernHash(hash: String): Boolean {
        return hash.contains(DELIMITER) && hash.split(DELIMITER).size == 2
    }
}

/**
 * Énumération pour la force des mots de passe
 */
enum class PasswordStrength {
    WEAK, MEDIUM, STRONG
}