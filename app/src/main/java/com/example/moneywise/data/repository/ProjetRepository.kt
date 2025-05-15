package com.example.moneywise.data.repository

import androidx.lifecycle.LiveData
import com.example.moneywise.data.dao.AcquittementDao
import com.example.moneywise.data.entity.Acquittement

class ProjetRepository(private val projetDao: AcquittementDao) {
    val readAllData: LiveData<List<Acquittement>> = projetDao.getAllAcquittement()

    suspend fun addProjet(projet: Acquittement) {
        projetDao.insertAcquittement(projet)
    }
}