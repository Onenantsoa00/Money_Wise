package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Utilisateur

@Dao
interface UtilisateurDao {
    @Insert
    suspend fun insertUtilisateur(utilisateur: Utilisateur)

    //READ
    @Query("SELECT * FROM Utilisateur")
    fun getAllUtilisateurs(): LiveData<List<Utilisateur>>

    @Query("SELECT * FROM Utilisateur WHERE id = :id")
    suspend fun getUtilisateurById(id: Int): Utilisateur?

    @Update
    suspend fun update(user: Utilisateur)

    @Delete
    suspend fun delete(user: Utilisateur)

    @Query("DELETE FROM Utilisateur")
    suspend fun deleteAll()
}