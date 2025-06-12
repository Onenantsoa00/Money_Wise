package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.moneywise.data.entity.Historique
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoriqueDao {
    @Insert
    suspend fun insert(historique: Historique): Long

    @Query("SELECT * FROM Historique ORDER BY date_heure DESC")
    fun getAllHistorique(): LiveData<List<Historique>>

    @Query("SELECT * FROM Historique ORDER BY date_heure DESC")
    fun getAllHistoriqueFlow(): Flow<List<Historique>>

    @Query("SELECT * FROM Historique WHERE id = :id")
    suspend fun getHistoriqueById(id: Int): Historique?

    @Query("SELECT * FROM Historique ORDER BY date_heure DESC LIMIT :limit")
    fun getRecentHistorique(limit: Int = 10): Flow<List<Historique>>

    @Query("SELECT * FROM Historique ORDER BY date_heure DESC LIMIT 10")
    fun getRecentHistoriqueLiveData(): LiveData<List<Historique>>

    // Pour vérifier les doublons par montant
    @Query("SELECT * FROM Historique WHERE montant = :amount ORDER BY date_heure DESC")
    suspend fun getHistoriqueByTransactionAmount(amount: Double): List<Historique>

    // Pour vérifier les doublons par montant et type
    @Query("SELECT * FROM Historique WHERE montant = :amount AND type_transaction = :type ORDER BY date_heure DESC LIMIT 5")
    suspend fun getHistoriqueByAmountAndType(amount: Double, type: String): List<Historique>

    // Pour récupérer l'historique depuis une date
    @Query("SELECT * FROM Historique WHERE date_heure >= :since ORDER BY date_heure DESC")
    suspend fun getHistoriqueSince(since: Long): List<Historique>

    // Pour récupérer l'historique par période
    @Query("SELECT * FROM Historique WHERE date_heure BETWEEN :startDate AND :endDate ORDER BY date_heure DESC")
    suspend fun getHistoriqueByDateRange(startDate: Long, endDate: Long): List<Historique>

    // Pour récupérer l'historique par type de transaction
    @Query("SELECT * FROM Historique WHERE type_transaction = :type ORDER BY date_heure DESC")
    suspend fun getHistoriqueByType(type: String): List<Historique>

    @Query("SELECT * FROM Historique WHERE type_transaction = :type ORDER BY date_heure DESC")
    fun getHistoriqueByTypeLiveData(type: String): LiveData<List<Historique>>

    @Query("SELECT COALESCE(SUM(montant), 0.0) FROM Historique WHERE type_transaction IN ('EMPRUNT', 'REMBOURSEMENT_ACQUITTEMENT')")
    suspend fun getTotalCredits(): Double

    @Query("SELECT COALESCE(SUM(montant), 0.0) FROM Historique WHERE type_transaction IN ('ACQUITTEMENT', 'REMBOURSEMENT_EMPRUNT')")
    suspend fun getTotalDebits(): Double

    // Pour les statistiques détaillées
    @Query("SELECT COALESCE(SUM(montant), 0.0) FROM Historique WHERE type_transaction = 'DEPOT'")
    suspend fun getTotalDepots(): Double

    @Query("SELECT COALESCE(SUM(montant), 0.0) FROM Historique WHERE type_transaction = 'RETRAIT'")
    suspend fun getTotalRetraits(): Double

    @Query("SELECT COALESCE(SUM(montant), 0.0) FROM Historique WHERE type_transaction = 'TRANSFERT'")
    suspend fun getTotalTransferts(): Double

    @Query("SELECT COUNT(*) FROM Historique")
    suspend fun getHistoriqueCount(): Int

    @Query("SELECT COUNT(*) FROM Historique WHERE type_transaction = :type")
    suspend fun getHistoriqueCountByType(type: String): Int

    // Pour rechercher dans les détails
    @Query("SELECT * FROM Historique WHERE details LIKE '%' || :searchTerm || '%' ORDER BY date_heure DESC")
    suspend fun searchInDetails(searchTerm: String): List<Historique>

    // Pour rechercher par motif
    @Query("SELECT * FROM Historique WHERE motif LIKE '%' || :searchTerm || '%' ORDER BY date_heure DESC")
    suspend fun searchByMotif(searchTerm: String): List<Historique>

    @Update
    suspend fun update(historique: Historique)

    @Delete
    suspend fun delete(historique: Historique)

    @Query("DELETE FROM Historique WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM Historique")
    suspend fun deleteAll()

    @Query("DELETE FROM Historique WHERE type_transaction = :type")
    suspend fun deleteByType(type: String)

    // Pour nettoyer l'historique ancien
    @Query("DELETE FROM Historique WHERE date_heure < :beforeDate")
    suspend fun deleteOldHistorique(beforeDate: Long)
}
