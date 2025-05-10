package com.example.moneywise.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ProfilViewModel : ViewModel() {
    private val _text = MutableLiveData<String>().apply {
        value = "John Doe" // Nom par défaut
    }

    private val _balance = MutableLiveData<String>().apply {
        value = "john.doe@example.com" // Email par défaut
    }

    val text: LiveData<String> = _text
    val balance: LiveData<String> = _balance

    fun updateProfile(name: String, email: String) {
        _text.value = name
        _balance.value = email
    }
}