package com.example.moneywise.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.example.moneywise.data.relations.BanqueAvecTransactions

@Dao
interface BanqueDao {
    @Dao
    interface BanqueDao {
        @Transaction // Important pour les relations
        @Query("SELECT * FROM Banque WHERE id = :banqueId")
        fun getBanqueAvecTransactions(banqueId: Int): BanqueAvecTransactions
    }
}