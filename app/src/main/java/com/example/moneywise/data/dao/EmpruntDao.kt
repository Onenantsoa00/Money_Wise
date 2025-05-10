package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Emprunt

@Dao
interface EmpruntDao {
    @Insert
    suspend fun insertEmprunt(emprunt: Emprunt)

    //READ
    @Query("SELECT * FROM Emprunt")
    fun getAllEmprunt(): LiveData<List<Emprunt>>

    @Query("SELECT * FROM Emprunt WHERE id = :id")
    suspend fun getEmpruntById(id: Int): Emprunt?

    @Update
    suspend fun updateEmprunt(emprunt: Emprunt)

    @Delete
    suspend fun deleteEmprunt(emprunt: Emprunt)

    @Query("DELETE FROM Emprunt")
    suspend fun deleteAllEmprunt()
}