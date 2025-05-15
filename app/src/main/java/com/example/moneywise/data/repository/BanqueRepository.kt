package com.example.moneywise.data.repository

import com.example.moneywise.data.dao.BanqueDao
import com.example.moneywise.data.entity.Banque
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class BanqueRepository @Inject constructor(
    private val banqueDao: BanqueDao
) {
    suspend fun insertBanque(banque: Banque) {
        banqueDao.insertBanque(banque)
    }

    fun getAllBanques(): Flow<List<Banque>> {
        return banqueDao.getAllBanques()
    }
}