package com.example.moneywise.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.data.entity.Transaction

data class BanqueAvecTransactions(
    @Embedded val banque: Banque,
    @Relation(
        parentColumn = "id", // La colonne ID dans Banque
        entityColumn = "id_banque" // La colonne de référence dans Transaction
    )
    val transactions: List<Transaction>
)