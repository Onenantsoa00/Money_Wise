package com.example.moneywise.ui.acquittement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.data.dao.AcquittementDao

class AcquittementViewModelFactory(private val acquittementDao: AcquittementDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AcquittementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AcquittementViewModel(acquittementDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}