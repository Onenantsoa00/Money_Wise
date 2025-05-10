package com.example.moneywise.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Acquittement")
data class Acquittement (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "personne_acquittement") val personne_acquittement : String,
    @ColumnInfo(name = "contacte") val contacte : String,
    @ColumnInfo(name = "montant") val montant : Double,
    @ColumnInfo(name = "date_crédit") val date_crédit : Date,
    @ColumnInfo(name = "date_remise_crédit") val date_remise_crédit : Date,
)