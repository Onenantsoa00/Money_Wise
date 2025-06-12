package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Emprunt

@Dao
interface EmpruntDao {
    @Insert
    suspend fun insertEmprunt(emprunt: Emprunt)

    @Query("SELECT * FROM Emprunt")
    fun getAllEmprunt(): LiveData<List<Emprunt>>

    @Query("SELECT * FROM Emprunt")
    suspend fun getAllEmprunts(): List<Emprunt>

    @Query("SELECT * FROM Emprunt WHERE id = :id")
    suspend fun getEmpruntById(id: Int): Emprunt?

    @Query("UPDATE Emprunt SET est_rembourse = :estRembourse WHERE id = :id")
    suspend fun updateRemboursementStatus(id: Int, estRembourse: Boolean)

    @Query("SELECT * FROM Emprunt WHERE est_rembourse = 0")
    fun getEmpruntsNonRembourses(): LiveData<List<Emprunt>>

    @Query("SELECT * FROM Emprunt WHERE est_rembourse = 0 ORDER BY id DESC LIMIT 5")
    fun getRecentEmpruntsNonRembourses(): LiveData<List<Emprunt>>

    @Query("SELECT COUNT(*) FROM Emprunt WHERE est_rembourse = 0")
    fun getUnpaidLoansCount(): LiveData<Int>

    @Update
    suspend fun updateEmprunt(emprunt: Emprunt)

    @Delete
    suspend fun deleteEmprunt(emprunt: Emprunt)

    @Query("DELETE FROM Emprunt")
    suspend fun deleteAllEmprunts()

    @Query("SELECT * FROM Emprunt")
    suspend fun getAllEmpruntSync(): List<Emprunt>

    @Query("DELETE FROM Emprunt")
    suspend fun deleteAllEmprunt()

    // Méthodes pour les statistiques
    @Query("SELECT COUNT(*) FROM Emprunt WHERE est_rembourse = 0")
    fun getTotalUnpaidEmpruntCount(): LiveData<Int>

    @Query("SELECT SUM(montant) FROM Emprunt WHERE est_rembourse = 0")
    fun getTotalUnpaidEmpruntAmount(): LiveData<Double?>

    @Query("SELECT * FROM Emprunt ORDER BY date_emprunt DESC LIMIT 5")
    fun getRecentEmprunts(): LiveData<List<Emprunt>>

    @Query("UPDATE Emprunt SET est_rembourse = 1 WHERE id = :empruntId")
    suspend fun markAsRembourse(empruntId: Int)

    // Version synchrone pour les rappels
    @Query("SELECT * FROM Emprunt WHERE est_rembourse = 0")
    suspend fun getEmpruntsNonRemboursesSync(): List<Emprunt>
}
