package com.example.moneywise.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.moneywise.data.dao.EmpruntDao
import com.example.moneywise.data.dao.UtilisateurDao
import com.example.moneywise.data.database.Converters
import com.example.moneywise.data.entity.Banque
import com.example.moneywise.data.entity.Emprunt
import com.example.moneywise.data.entity.Projet
import com.example.moneywise.data.entity.Transaction
import com.example.moneywise.data.entity.Utilisateur
import kotlin.jvm.Volatile

@Database(entities = [Utilisateur::class, Transaction::class, Projet::class, Banque::class, Emprunt::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)

abstract class AppDatabase : RoomDatabase() {
    abstract fun utilisateurDao(): UtilisateurDao
    abstract fun empruntDao(): EmpruntDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneywise"  // Nom de la DB
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}