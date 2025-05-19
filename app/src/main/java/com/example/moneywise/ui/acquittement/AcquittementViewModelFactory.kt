package com.example.moneywise.ui.acquittement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.dao.AcquittementDao

class AcquittementViewModelFactory(
    private val acquittementDao: AcquittementDao,
    private val database: AppDatabase
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AcquittementViewModel::class.java)) {
            return AcquittementViewModel(acquittementDao, database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}