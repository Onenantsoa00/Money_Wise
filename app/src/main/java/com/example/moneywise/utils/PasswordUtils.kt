package com.example.moneywise.utils

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

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
        // Générer un sel aléatoire
        val salt = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(salt)

        // Hacher le mot de passe avec le sel
        val hash = getHash(password, salt)

        // Encoder le sel et le hash en Base64
        val saltBase64 = Base64.getEncoder().encodeToString(salt)
        val hashBase64 = Base64.getEncoder().encodeToString(hash)

        // Retourner "sel:hash"
        return "$saltBase64$DELIMITER$hashBase64"
    }

    /**
     * Vérifie si un mot de passe correspond à un hash stocké
     */
    fun verifyPassword(password: String, storedHash: String): Boolean {
        try {
            // Séparer le sel et le hash
            val parts = storedHash.split(DELIMITER)
            if (parts.size != 2) return false

            // Décoder le sel et le hash
            val salt = Base64.getDecoder().decode(parts[0])
            val expectedHash = Base64.getDecoder().decode(parts[1])

            // Calculer le hash du mot de passe fourni
            val actualHash = getHash(password, salt)

            // Comparer les hash
            return MessageDigest.isEqual(expectedHash, actualHash)
        } catch (e: Exception) {
            return false
        }
    }

    /**
     * Calcule le hash d'un mot de passe avec un sel donné
     */
    private fun getHash(password: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance(HASH_ALGORITHM)
        md.update(salt)
        return md.digest(password.toByteArray())
    }
}
