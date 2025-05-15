package com.example.moneywise.data.repository

import com.example.moneywise.data.dao.UtilisateurDao
import com.example.moneywise.data.entity.Utilisateur
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

class UserRepository(private val utilisateurDao: UtilisateurDao) {

    suspend fun registerUser(
        nom: String,
        prenom: String,
        email: String,
        password: String
    ): Result<Utilisateur> = withContext(Dispatchers.IO) {
        try {
            // Vérifie si l'email existe déjà
            if (utilisateurDao.getUserByEmail(email) != null) {
                return@withContext Result.failure(Exception("Email déjà utilisé"))
            }

            val newUser = Utilisateur(
                nom = nom,
                prenom = prenom,
                email = email,
                password = password,
                solde = 0.0
            )

            utilisateurDao.insert(newUser)
            Result.success(newUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(
        email: String,
        password: String
    ): Result<Utilisateur> = withContext(Dispatchers.IO) {
        try {
            val user = utilisateurDao.login(email, password)
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Email ou mot de passe incorrect"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}