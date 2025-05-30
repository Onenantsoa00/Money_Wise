package com.example.moneywise.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Banque")
data class Banque(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nom") val nom: String,
    @ColumnInfo(name = "code") val code: String = "",
    @ColumnInfo(name = "type") val type: String = "MOBILE_MONEY",
    @ColumnInfo(name = "numero_compte") val numeroCompte: String? = null,
    @ColumnInfo(name = "solde") val solde: Double = 0.0
)
