package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Acquittement

@Dao
@JvmSuppressWildcards
interface AcquittementDao {
    @Insert
    suspend fun insertAcquittement(acquittement: Acquittement)

    //READ
    @Query("SELECT * FROM Acquittement")
    fun getAllAcquittement(): LiveData<List<Acquittement>>

    // Version synchrone pour les services
    @Query("SELECT * FROM Acquittement")
    suspend fun getAllAcquittementSync(): List<Acquittement>

    @Delete
    suspend fun deleteAcquittement(acquittement: Acquittement)

    @Query("SELECT * FROM Acquittement ORDER BY id DESC LIMIT 5")
    fun getRecentAcquittements(): LiveData<List<Acquittement>>

    // Nouvelle méthode pour compter tous les acquittements
    @Query("SELECT COUNT(*) FROM Acquittement")
    fun getTotalAcquittementCount(): LiveData<Int>

    @Update
    suspend fun updateAcquittement(acquittement: Acquittement)

    // 🔥 MÉTHODE EXISTANTE CONSERVÉE: Méthode pour les rappels - tous les acquittements
    // Comme il n'y a pas de champ est_paye dans l'entité, on retourne tous les acquittements
    @Query("SELECT * FROM Acquittement")
    suspend fun getAcquittementNonPayes(): List<Acquittement>
}
