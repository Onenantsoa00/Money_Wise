package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.moneywise.data.entity.Historique

@Dao
interface HistoriqueDao {
    @Insert
    suspend fun insert(historique: Historique)

    @Query("SELECT * FROM Historique ORDER BY date_heure DESC")
    fun getAllHistorique(): LiveData<List<Historique>>

    @Query("SELECT COALESCE(SUM(montant), 0.0) FROM Historique WHERE type_transaction IN ('EMPRUNT', 'REMBOURSEMENT_ACQUITTEMENT')")
    suspend fun getTotalCredits(): Double

    @Query("SELECT COALESCE(SUM(montant), 0.0) FROM Historique WHERE type_transaction IN ('ACQUITTEMENT', 'REMBOURSEMENT_EMPRUNT')")
    suspend fun getTotalDebits(): Double

    @Query("DELETE FROM Historique")
    suspend fun deleteAll()
}