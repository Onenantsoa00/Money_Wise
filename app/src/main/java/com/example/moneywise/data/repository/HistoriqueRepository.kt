package com.example.moneywise.data.repository

import androidx.lifecycle.LiveData
import com.example.moneywise.data.dao.HistoriqueDao
import com.example.moneywise.data.entity.Historique
import javax.inject.Inject

class HistoriqueRepository @Inject constructor(
    private val historiqueDao: HistoriqueDao
) {
    suspend fun insert(historique: Historique) {
        historiqueDao.insert(historique)
    }

    fun getAllHistorique(): LiveData<List<Historique>> {
        return historiqueDao.getAllHistorique()
    }

    suspend fun getTotalCredits(): Double {
        return historiqueDao.getTotalCredits()
    }

    suspend fun getTotalDebits(): Double {
        return historiqueDao.getTotalDebits()
    }

    suspend fun deleteAll() {
        historiqueDao.deleteAll()
    }
}
