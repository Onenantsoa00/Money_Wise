package com.example.moneywise.data.repository

import androidx.lifecycle.LiveData
import com.example.moneywise.data.dao.UtilisateurDao
import com.example.moneywise.data.entity.Utilisateur
import com.example.moneywise.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class UtilisateurRepository(private val utilisateurDao: UtilisateurDao) {

    suspend fun registerUser(
        nom: String,
        prenom: String,
        email: String,
        password: String,
        avatar: String? = null
    ): Result<Utilisateur> = withContext(Dispatchers.IO) {
        try {
            // Vérifie si l'email existe déjà
            if (utilisateurDao.getUserByEmail(email) != null) {
                return@withContext Result.failure(Exception("Email déjà utilisé"))
            }

            // Hacher le mot de passe avant de l'enregistrer
            val hashedPassword = PasswordUtils.hashPassword(password)

            val newUser = Utilisateur(
                nom = nom,
                prenom = prenom,
                email = email,
                password = hashedPassword, // Mot de passe haché
                solde = 0.0,
                avatar = avatar
            )

            utilisateurDao.insert(newUser)

            // Récupérer l'utilisateur avec son ID généré
            val createdUser = utilisateurDao.getUserByEmail(email)
            if (createdUser != null) {
                Result.success(createdUser)
            } else {
                Result.failure(Exception("Erreur lors de la création de l'utilisateur"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(
        email: String,
        password: String
    ): Result<Utilisateur> = withContext(Dispatchers.IO) {
        try {
            // Récupérer l'utilisateur par email
            val user = utilisateurDao.getUserByEmail(email)

            if (user != null) {
                // Vérifier le mot de passe
                if (PasswordUtils.verifyPassword(password, user.password)) {

                    // 🔥 MIGRATION AUTOMATIQUE: Si l'ancien format, migrer vers le nouveau
                    if (!PasswordUtils.isModernHash(user.password)) {
                        val newHash = PasswordUtils.hashPassword(password)
                        val updatedUser = user.copy(password = newHash)
                        utilisateurDao.update(updatedUser)
                        Result.success(updatedUser)
                    } else {
                        Result.success(user)
                    }
                } else {
                    Result.failure(Exception("Email ou mot de passe incorrect"))
                }
            } else {
                Result.failure(Exception("Email ou mot de passe incorrect"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 🔥 NOUVELLES MÉTHODES AJOUTÉES POUR MOT DE PASSE OUBLIÉ
    suspend fun checkUserExists(email: String): Boolean = withContext(Dispatchers.IO) {
        try {
            utilisateurDao.getUserByEmail(email) != null
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resetPassword(email: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val user = utilisateurDao.getUserByEmail(email)

            if (user != null) {
                // Hacher le nouveau mot de passe avec le système sécurisé
                val hashedPassword = PasswordUtils.hashPassword(newPassword)
                val updatedUser = user.copy(password = hashedPassword)

                utilisateurDao.update(updatedUser)
                Result.success(Unit)
            } else {
                Result.failure(Exception("Utilisateur non trouvé"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserAvatar(
        userId: Int,
        avatarPath: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            utilisateurDao.updateAvatar(userId, avatarPath)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: Utilisateur): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            utilisateurDao.update(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByEmail(email: String): Utilisateur? {
        return utilisateurDao.getUserByEmail(email)
    }

    fun getSoldeUtilisateur(): LiveData<Double?> {
        return utilisateurDao.getSoldeUtilisateur()
    }

    fun getNomUtilisateur(): LiveData<String?> {
        return utilisateurDao.getNomUtilisateur()
    }

    fun getAvatarUtilisateur(): LiveData<String?> {
        return utilisateurDao.getAvatarUtilisateur()
    }

    fun getAllUtilisateurs(): Flow<List<Utilisateur>> {
        return utilisateurDao.getAllUtilisateurs()
    }

    suspend fun getFirstUtilisateur(): Utilisateur? {
        return utilisateurDao.getFirstUtilisateur()
    }

    suspend fun getUserById(id: Int): Utilisateur? {
        return utilisateurDao.getUserById(id)
    }

    suspend fun update(user: Utilisateur) {
        utilisateurDao.update(user)
    }

    suspend fun delete(user: Utilisateur) {
        utilisateurDao.delete(user)
    }

    suspend fun updateAvatar(userId: Int, avatarPath: String?) {
        utilisateurDao.updateAvatar(userId, avatarPath)
    }
}