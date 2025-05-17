package com.example.moneywise.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.moneywise.data.dao.*
import com.example.moneywise.data.database.Converters
import com.example.moneywise.data.entity.*

@Database(
    entities = [
        Utilisateur::class,
        Transaction::class,
        Projet::class,
        Banque::class,
        Emprunt::class,
        Acquittement::class
    ],
    version = 5,
    exportSchema = true // Important pour suivre les migrations
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun utilisateurDao(): UtilisateurDao
    abstract fun empruntDao(): EmpruntDao
    abstract fun AcquittementDao(): AcquittementDao
    abstract fun ProjetDao(): ProjetDao
    abstract fun banqueDao(): BanqueDao
    abstract fun transactionDao(): TransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 4 to 5
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new temporary table with new schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS Emprunt_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        nom_emprunte TEXT NOT NULL,
                        contacte TEXT NOT NULL,
                        montant REAL NOT NULL,
                        date_emprunt INTEGER NOT NULL,
                        date_remboursement INTEGER NOT NULL,
                        est_rembourse INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())

                // Copy data from old table to new table
                database.execSQL("""
                    INSERT INTO Emprunt_new (id, nom_emprunte, contacte, montant, date_emprunt, date_remboursement, est_rembourse)
                    SELECT id, nom_emprunte, contacte, montant, date_emprunt, date_remboursement, 0 FROM Emprunt
                """.trimIndent())

                // Remove old table
                database.execSQL("DROP TABLE Emprunt")

                // Rename new table
                database.execSQL("ALTER TABLE Emprunt_new RENAME TO Emprunt")
            }
        }

        // Add all migrations
        private val ALL_MIGRATIONS = arrayOf(
            MIGRATION_4_5
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneywise" // Using .db extension is recommended
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigrationOnDowngrade()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}