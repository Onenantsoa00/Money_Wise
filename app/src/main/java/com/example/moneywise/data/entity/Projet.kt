package com.example.moneywise.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Projet",
    foreignKeys = [
        ForeignKey(
            entity = Utilisateur::class,
            parentColumns = ["id"],
            childColumns = ["id_utilisateur"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Projet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "nom") val nom: String,
    @ColumnInfo(name = "montant_necessaire") val montant_necessaire: Double,
    @ColumnInfo(name = "montant_actuel") val montant_actuel: Double = 0.0,
    @ColumnInfo(name = "progression") val progression: Int = 0,
    @ColumnInfo(name = "date_limite") val date_limite: Date,
    @ColumnInfo(name = "id_utilisateur", defaultValue = "1") val id_utilisateur: Int? = null // Rend nullable
)