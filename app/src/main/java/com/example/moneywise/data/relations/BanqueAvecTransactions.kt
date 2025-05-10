package com.example.moneywise.data.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.data.entity.Transaction

class BanqueAvecTransactions(
    @Embedded val banque: Banque,
    @Relation(
        parentColumn = "id",
        entityColumn = "banqueId"
    )
    val transactions: List<Transaction>
)
