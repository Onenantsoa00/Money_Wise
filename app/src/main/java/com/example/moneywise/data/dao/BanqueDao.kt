package com.example.moneywise.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.data.relations.BanqueAvecTransactions
import kotlinx.coroutines.flow.Flow

@Dao
interface BanqueDao {
    @Insert
    suspend fun insertBanque(banque: Banque)

    @Transaction
    @Query("SELECT * FROM Banque WHERE id = :banqueId")
    fun getBanqueAvecTransactions(banqueId: Int): BanqueAvecTransactions

    @Query("SELECT * FROM Banque")
    fun getAllBanques(): Flow<List<Banque>>

    @Delete
    suspend fun deleteBanque(banque: Banque)

    @Update
    suspend fun updateBanque(banque: Banque)

    @Query("SELECT * FROM Banque WHERE nom = :nom LIMIT 1")
    suspend fun getBanqueByNom(nom: String): Banque?

}