package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Projet

@Dao
interface ProjetDao {
    @Insert
    suspend fun insertProjet(projet: Projet)

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

    // Nouvelles requêtes pour les statistiques
    @Query("SELECT COUNT(*) FROM Projet WHERE progression < 100 AND progression >= 50")
    fun countActiveProjects(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM Projet WHERE progression < 50 AND progression > 0")
    fun countOngoingProjects(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM Projet WHERE progression = 100")
    fun countCompletedProjects(): LiveData<Int>

    @Query("SELECT * FROM Projet ORDER BY date_limite ASC LIMIT 4")
    fun getRecentProjects(): LiveData<List<Projet>>
}