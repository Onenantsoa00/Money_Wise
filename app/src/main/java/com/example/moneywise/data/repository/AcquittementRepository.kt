package com.example.moneywise.data.repository

import androidx.lifecycle.LiveData
import com.example.moneywise.data.dao.AcquittementDao
import com.example.moneywise.data.entity.Acquittement

class AcquittementRepository (private  val acquittementDao: AcquittementDao) {
    val readAllData: LiveData<List<Acquittement>> = acquittementDao.getAllAcquittement()

    suspend fun addAcquittement(acquittement: Acquittement) {
        acquittementDao.insertAcquittement(acquittement)
    }
}