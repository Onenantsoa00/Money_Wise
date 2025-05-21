package com.example.moneywise.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Historique")
data class Historique(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "type_transaction") val typeTransaction: String,
    @ColumnInfo(name = "montant") val montant: Double,
    @ColumnInfo(name = "date_heure") val dateHeure: Date,
    @ColumnInfo(name = "motif") val motif: String,
    @ColumnInfo(name = "details") val details: String? = null // JSON ou texte libre pour plus d'infos
)