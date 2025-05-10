package com.example.moneywise.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Argent" // Changé pour correspondre à votre UI
    }

    private val _balance = MutableLiveData<String>().apply {
        value = "1.000.000.000 MGA"
    }

    val text: LiveData<String> = _text
    val balance: LiveData<String> = _balance

    fun updateBalance(newBalance: String) {
        _balance.value = newBalance
    }
}