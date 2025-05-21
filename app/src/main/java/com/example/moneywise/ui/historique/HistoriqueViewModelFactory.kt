package com.example.moneywise.ui.historique

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.data.AppDatabase

class HistoriqueViewModelFactory(private val database: AppDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HistoriqueViewModel::class.java)) {
            return HistoriqueViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}