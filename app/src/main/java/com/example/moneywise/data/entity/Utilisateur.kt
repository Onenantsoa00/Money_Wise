package com.example.moneywise.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Utilisateur")
data class Utilisateur (
    @PrimaryKey(autoGenerate = true) val id: Int ,
    @ColumnInfo(name = "nom") val nom : String,
    @ColumnInfo(name = "prenoms") val prenoms : String,
    @ColumnInfo(name = "solde") val solde : Double,
    @ColumnInfo(name = "email") val email : String,
    @ColumnInfo(name = "password") val password : String
)