package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Transaction

@Dao
interface TransactionDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction)

    //READ
    @Query("SELECT * FROM 'Transaction'")
    fun getAllTransaction(): LiveData<List<Transaction>>

    @Query("SELECT * FROM 'Transaction' WHERE id = :id")
    suspend fun getTransactionById(id: Int): Transaction?

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("DELETE FROM 'Transaction'")
    suspend fun deleteAllTransaction()
}