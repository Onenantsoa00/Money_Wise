package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.moneywise.data.entity.Transaction
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long

    @Query("SELECT * FROM `Transaction`")
    suspend fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM `Transaction` ORDER BY date DESC")
    fun getAllTransactionsLiveData(): LiveData<List<Transaction>>

    @Query("SELECT * FROM `Transaction` ORDER BY date DESC")
    fun getAllTransaction(): Flow<List<Transaction>>

    @Query("SELECT * FROM `Transaction` WHERE id_utilisateur = :userId")
    suspend fun getTransactionsByUserId(userId: Int): List<Transaction>

    @Query("SELECT * FROM `Transaction` WHERE id_utilisateur = :userId ORDER BY date DESC")
    fun getTransactionsByUserIdLiveData(userId: Int): LiveData<List<Transaction>>

    @Query("SELECT * FROM `Transaction` WHERE id_utilisateur = :userId ORDER BY date DESC")
    fun getTransactionsByUser(userId: Int): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM `Transaction` WHERE id_utilisateur = :userId")
    suspend fun getTransactionCountByUserId(userId: Int): Int

    @Query("SELECT COUNT(*) FROM `Transaction` WHERE id_utilisateur = :userId")
    fun getTransactionCountByUserIdLiveData(userId: Int): LiveData<Int>

    @Query("SELECT COUNT(*) FROM `Transaction` WHERE id_utilisateur = :userId")
    suspend fun getTransactionCount(userId: Int): Int

    @Query("SELECT * FROM `Transaction` WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Query("SELECT * FROM `Transaction` ORDER BY date DESC LIMIT 5")
    fun getRecentTransactions(): Flow<List<Transaction>>

    @Query("SELECT COUNT(*) FROM `Transaction`")
    fun getTotalTransactionsCount(): Flow<Int>

    @Query("SELECT * FROM `Transaction` WHERE type = :type AND id_utilisateur = :userId")
    suspend fun getTransactionsByTypeAndUserId(type: String, userId: Int): List<Transaction>

    @Query("SELECT * FROM `Transaction` WHERE date BETWEEN :startDate AND :endDate AND id_utilisateur = :userId")
    suspend fun getTransactionsByDateRangeAndUserId(startDate: Long, endDate: Long, userId: Int): List<Transaction>

    // Récupérer les transactions depuis une date spécifique
    @Query("SELECT * FROM `Transaction` WHERE date >= :since ORDER BY date DESC")
    suspend fun getTransactionsSince(since: Date): List<Transaction>

    // Pour les statistiques de dépôts et retraits
    @Query("SELECT SUM(CAST(montants AS REAL)) FROM `Transaction` WHERE id_utilisateur = :userId AND type = 'DEPOT'")
    suspend fun getTotalDeposits(userId: Int): Double?

    @Query("SELECT SUM(CAST(montants AS REAL)) FROM `Transaction` WHERE id_utilisateur = :userId AND type = 'RETRAIT'")
    suspend fun getTotalWithdrawals(userId: Int): Double?

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM `Transaction` WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    @Query("DELETE FROM `Transaction`")
    suspend fun deleteAllTransactions()
}
