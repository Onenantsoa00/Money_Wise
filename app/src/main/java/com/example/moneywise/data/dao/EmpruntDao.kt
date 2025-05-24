package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Emprunt

@Dao
@JvmSuppressWildcards
interface EmpruntDao {
    @Insert
    suspend fun insertEmprunt(emprunt: Emprunt)

    //READ
    @Query("SELECT * FROM Emprunt")
    fun getAllEmprunt(): LiveData<List<Emprunt>>

    @Query("UPDATE Emprunt SET est_rembourse = :estRembourse WHERE id = :id")
    suspend fun updateRemboursementStatus(id: Int, estRembourse: Boolean)

    @Query("SELECT * FROM Emprunt WHERE est_rembourse = 0")
    fun getEmpruntsNonRembourses(): LiveData<List<Emprunt>>

    @Query("SELECT * FROM Emprunt WHERE est_rembourse = 0 ORDER BY id DESC LIMIT 5")
    fun getRecentEmpruntsNonRembourses(): LiveData<List<Emprunt>>

    // Nouvelle méthode pour compter les emprunts non remboursés
    @Query("SELECT COUNT(*) FROM Emprunt WHERE est_rembourse = 0")
    fun getUnpaidLoansCount(): LiveData<Int>
}