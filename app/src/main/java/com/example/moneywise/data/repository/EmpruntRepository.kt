package com.example.moneywise.data.repository

import androidx.lifecycle.LiveData
import com.example.moneywise.data.dao.EmpruntDao
import com.example.moneywise.data.entity.Emprunt

class EmpruntRepository (private val empruntDao: EmpruntDao){
    val readAllData:LiveData<List<Emprunt>> = empruntDao.getAllEmprunt()

    suspend fun addEmprunt(emprunt: Emprunt){
        empruntDao.insertEmprunt(emprunt)
    }
}