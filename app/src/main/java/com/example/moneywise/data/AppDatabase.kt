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
        Acquittement::class,
        Historique::class
    ],
    version = 6,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun utilisateurDao(): UtilisateurDao
    abstract fun empruntDao(): EmpruntDao
    abstract fun AcquittementDao(): AcquittementDao
    abstract fun ProjetDao(): ProjetDao
    abstract fun banqueDao(): BanqueDao
    abstract fun transactionDao(): TransactionDao
    abstract fun historiqueDao(): HistoriqueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2 (exemple)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Initial schema creation if needed
            }
        }

        // Migration from version 4 to 5 - Correction importante
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Vérifier d'abord si la table Emprunt existe
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

                // Vérifier si l'ancienne table existe avant de copier les données
                val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='Emprunt'")
                if (cursor.moveToFirst()) {
                    database.execSQL("""
                        INSERT INTO Emprunt_new (id, nom_emprunte, contacte, montant, date_emprunt, date_remboursement, est_rembourse)
                        SELECT id, nom_emprunte, contacte, montant, date_emprunt, date_remboursement, 
                        CASE WHEN est_rembourse IS NULL THEN 0 ELSE est_rembourse END 
                        FROM Emprunt
                    """.trimIndent())
                    database.execSQL("DROP TABLE Emprunt")
                }
                cursor.close()

                database.execSQL("ALTER TABLE Emprunt_new RENAME TO Emprunt")
            }
        }

        // Migration from version 5 to 6
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS Historique (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type_transaction TEXT NOT NULL,
                        montant REAL NOT NULL,
                        date_heure INTEGER NOT NULL,
                        motif TEXT NOT NULL,
                        details TEXT
                    )
                """.trimIndent())
            }
        }

        private val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_4_5,
            MIGRATION_5_6
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneywise"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}