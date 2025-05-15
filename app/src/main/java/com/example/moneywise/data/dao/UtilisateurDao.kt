package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.moneywise.data.entity.Utilisateur
import kotlinx.coroutines.flow.Flow

@Dao
interface UtilisateurDao {
    @Insert
    suspend fun insertUtilisateur(utilisateur: Utilisateur)

    @Query("SELECT * FROM Utilisateur WHERE email = :email AND password = :password")
    suspend fun login(email: String, password: String): Utilisateur?

    @Query("SELECT * FROM Utilisateur WHERE email = :email")
    suspend fun getUserByEmail(email: String): Utilisateur?

    @Query("SELECT * FROM utilisateur")
    fun getAllUtilisateurs(): Flow<List<Utilisateur>>

    @Query("SELECT * FROM utilisateur LIMIT 1")
    suspend fun getFirstUtilisateur(): Utilisateur?

    @Insert
    suspend fun insert(utilisateur: Utilisateur)

    @Query("SELECT * FROM Utilisateur WHERE id = :id")
    suspend fun getUserById(id: Int): Utilisateur?

    @Query("SELECT * FROM Utilisateur WHERE id = :id")
    suspend fun getUtilisateurById(id: Int): Utilisateur?

    @Update
    suspend fun update(user: Utilisateur)

    @Delete
    suspend fun delete(user: Utilisateur)
}