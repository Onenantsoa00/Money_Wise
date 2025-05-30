package com.example.moneywise.data.dao

import androidx.room.*
import com.example.moneywise.data.entity.Banque
import kotlinx.coroutines.flow.Flow

@Dao
interface BanqueDao {
    @Insert
    suspend fun insert(banque: Banque): Long

    @Query("SELECT * FROM Banque WHERE id = :id")
    suspend fun getById(id: Int): Banque?

    @Query("SELECT * FROM Banque WHERE code = :code")
    suspend fun getByCode(code: String): Banque?

    @Query("SELECT * FROM Banque WHERE nom = :nom")
    suspend fun getByNom(nom: String): Banque?

    @Query("SELECT * FROM Banque")
    fun getAllBanques(): Flow<List<Banque>>

    @Delete
    suspend fun deleteBanque(banque: Banque)

    @Update
    suspend fun updateBanque(banque: Banque)

    @Query("INSERT OR IGNORE INTO Banque (nom, code, type) VALUES ('Telma MVola', 'MVOLA', 'MOBILE_MONEY')")
    suspend fun insertDefaultMVola()

    @Query("INSERT OR IGNORE INTO Banque (nom, code, type) VALUES ('Airtel Money', 'AIRTEL', 'MOBILE_MONEY')")
    suspend fun insertDefaultAirtel()

    @Query("INSERT OR IGNORE INTO Banque (nom, code, type) VALUES ('Orange Money', 'ORANGE', 'MOBILE_MONEY')")
    suspend fun insertDefaultOrange()

    // Méthodes supplémentaires pour compatibilité
    @Insert
    suspend fun insertBanque(banque: Banque): Long

    @Query("SELECT * FROM Banque WHERE nom = :nom")
    suspend fun getBanqueByNom(nom: String): Banque?
}
