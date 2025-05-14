package com.example.moneywise.ui.emprunt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.moneywise.data.dao.EmpruntDao

class EmpruntViewModelFactory(private val empruntDao: EmpruntDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EmpruntViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EmpruntViewModel(empruntDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}