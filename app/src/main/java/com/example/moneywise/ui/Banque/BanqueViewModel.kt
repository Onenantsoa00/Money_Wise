package com.example.moneywise.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.data.repository.BanqueRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BanqueViewModel @Inject constructor(
    private val banqueRepository: BanqueRepository
) : ViewModel() {
    val allBanques = banqueRepository.getAllBanques()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertBanque(nom: String) {
        viewModelScope.launch {
            banqueRepository.insertBanque(Banque(nom = nom))
        }
    }

    fun deleteBanque(banque: Banque) {
        viewModelScope.launch {
            banqueRepository.deleteBanque(banque)
        }
    }

    fun updateBanque(banque: Banque) {
        viewModelScope.launch {
            banqueRepository.updateBanque(banque)
        }
    }
}