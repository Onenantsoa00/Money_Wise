package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
@JvmSuppressWildcards
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    @Query("SELECT * FROM 'Transaction' ORDER BY date DESC")
    fun getAllTransaction(): Flow<List<Transaction>>

    @Query("SELECT * FROM 'Transaction' WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM 'Transaction'")
    suspend fun deleteAllTransaction()

    @Query("SELECT * FROM 'Transaction' ORDER BY date DESC LIMIT 5")
    fun getRecentTransactions(): LiveData<List<Transaction>>

    // Nouvelle méthode pour compter toutes les transactions
    @Query("SELECT COUNT(*) FROM 'Transaction'")
    fun getTotalTransactionsCount(): LiveData<Int>
}