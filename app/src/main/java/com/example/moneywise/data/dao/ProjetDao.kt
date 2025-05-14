package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Projet

@Dao
@JvmSuppressWildcards
interface ProjetDao {
    @Insert
    suspend fun insertProjet(projet: Projet)

    //READ
    @Query("SELECT * FROM Projet")
    fun getAllProjet(): LiveData<List<Projet>>

    @Query("SELECT * FROM Projet WHERE id = :id")
    suspend fun getProjetById(id: Int): Projet?

    @Update
    suspend fun updateProjet(projet: Projet)

    @Delete
    suspend fun deleteProjet(projet: Projet)

    @Query("DELETE FROM Projet")
    suspend fun deleteAllProjet()
}