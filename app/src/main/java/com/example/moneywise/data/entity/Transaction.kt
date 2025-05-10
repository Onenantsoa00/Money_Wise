package com.example.moneywise.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Transaction",
    foreignKeys = [
        ForeignKey(
            entity = Utilisateur::class,  // Table parente
            parentColumns = ["id"],       // Colonne référencée
            childColumns = ["id_utilisateur"],  // Colonne locale
            onDelete = ForeignKey.CASCADE  // Suppression en cascade
        ),
        ForeignKey(
            entity = Banque::class,
            parentColumns = ["id"],
            childColumns = ["id_banque"],
            onDelete = ForeignKey.CASCADE
        )]
)
data class Transaction (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "type") val type : String,
    @ColumnInfo(name = "montants") val montants : String,
    @ColumnInfo(name = "date") val date : Date,
    @ColumnInfo(name = "id_utilisateur") val id_utilisateur : Int,
    @ColumnInfo(name = "id_banque") val id_banque : Int,
)