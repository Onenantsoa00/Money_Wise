package com.example.moneywise.data.repository

import com.example.moneywise.data.dao.TransactionDao
import com.example.moneywise.data.entity.Transaction
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class TransactionRepository @Inject constructor(
    private val transactionDao: TransactionDao
) {
    suspend fun insertTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    fun getAllTransactions(): Flow<List<Transaction>> {
        return transactionDao.getAllTransaction()
    }

    suspend fun getTransactionById(id: Int): Transaction? {
        return transactionDao.getTransactionById(id)
    }

    suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }

    fun getRecentTransactions(): Flow<List<Transaction>> {
        return transactionDao.getRecentTransactions()
    }

    fun getTotalTransactionsCount(): Flow<Int> {
        return transactionDao.getTotalTransactionsCount()
    }
}
