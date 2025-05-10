package com.example.moneywise.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Emprunt")
data class Emprunt (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nom_emprunte") val nom_emprunte : String,
    @ColumnInfo(name = "contacte") val contacte : String,
    @ColumnInfo(name = "montant") val montant : Double,
    @ColumnInfo(name = "date_emprunt") val date_emprunt : Date,
    @ColumnInfo(name = "date_remboursement") val date_remboursement : Date,
)