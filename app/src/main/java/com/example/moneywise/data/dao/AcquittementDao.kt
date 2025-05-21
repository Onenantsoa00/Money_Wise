package com.example.moneywise.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.moneywise.data.entity.Acquittement

@Dao
@JvmSuppressWildcards
interface AcquittementDao {
    @Insert
    suspend fun insertAcquittement(acquittement: Acquittement)

    //READ
    @Query("SELECT * FROM Acquittement")
    fun getAllAcquittement(): LiveData<List<Acquittement>>

    @Query("SELECT * FROM Acquittement WHERE id = :id")
    suspend fun getAcquittementById(id: Int): Acquittement?

    @Update
    suspend fun updateAcquittement(acquittement: Acquittement)

    @Delete
    suspend fun deleteAcquittement(acquittement: Acquittement)

    @Query("DELETE FROM Acquittement")
    suspend fun deleteAllAcquittement()

    @Query("SELECT * FROM Acquittement ORDER BY date_remise_crédit DESC LIMIT 5")
    fun getRecentAcquittements(): LiveData<List<Acquittement>>
}