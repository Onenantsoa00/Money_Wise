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

    // Requêtes pour les statistiques
    @Query("SELECT COUNT(*) FROM Projet WHERE progression < 100 AND progression >= 50")
    fun countActiveProjects(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM Projet WHERE progression < 50 AND progression >= 0")
    fun countOngoingProjects(): LiveData<Int>

    @Query("SELECT COUNT(*) FROM Projet WHERE progression = 100")
    fun countCompletedProjects(): LiveData<Int>

    // Nouvelle méthode pour compter tous les projets non finis
    @Query("SELECT COUNT(*) FROM Projet WHERE progression < 100")
    fun getTotalUnfinishedProjectsCount(): LiveData<Int>

    @Query("SELECT * FROM Projet ORDER BY date_limite ASC LIMIT 4")
    fun getRecentProjects(): LiveData<List<Projet>>

    // Nouvelle méthode pour mettre à jour le montant actuel et la progression
    @Query("UPDATE Projet SET montant_actuel = :montantActuel, progression = :progression WHERE id = :projetId")
    suspend fun updateProjetMontantAndProgression(projetId: Int, montantActuel: Double, progression: Int)

    // NOUVELLES REQUÊTES POUR LE FILTRAGE
    @Query("SELECT * FROM Projet WHERE progression >= 50 AND progression < 100 ORDER BY date_limite DESC")
    fun getActiveProjects(): LiveData<List<Projet>>

    @Query("SELECT * FROM Projet WHERE progression < 50 AND progression >= 0 ORDER BY date_limite DESC")
    fun getOngoingProjects(): LiveData<List<Projet>>

    @Query("SELECT * FROM Projet WHERE progression = 100 ORDER BY date_limite DESC")
    fun getCompletedProjects(): LiveData<List<Projet>>

    @Query("SELECT * FROM Projet ORDER BY date_limite DESC")
    fun getAllProjectsOrdered(): LiveData<List<Projet>>
}