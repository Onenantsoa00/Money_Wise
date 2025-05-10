package com.example.moneywise.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Projet",
    foreignKeys = [
        ForeignKey(
            entity = Utilisateur::class,  // Table parente
            parentColumns = ["id"],       // Colonne référencée
            childColumns = ["id_utilisateur"],  // Colonne locale
            onDelete = ForeignKey.CASCADE  // Suppression en cascade
        )
    ]
)
data class Projet (
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nom") val nom : String,
    @ColumnInfo(name = "montant_necessaire") val montant_necessaire : Double,
    @ColumnInfo(name = "montant_actuel") val montant_actuel : Double,
    @ColumnInfo(name = "progression") val progression : Int,
    @ColumnInfo(name = "date_limite") val date_limite : Date,
    @ColumnInfo(name = "id_utilisateur") val id_utilisateur : Int,
)