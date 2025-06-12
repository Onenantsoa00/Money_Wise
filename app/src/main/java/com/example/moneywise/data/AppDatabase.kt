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
    version = 7,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun utilisateurDao(): UtilisateurDao
    abstract fun empruntDao(): EmpruntDao
    // Nom de méthode en minuscules
    abstract fun acquittementDao(): AcquittementDao
    // Nom de méthode en minuscules
    abstract fun projetDao(): ProjetDao
    abstract fun banqueDao(): BanqueDao
    abstract fun transactionDao(): TransactionDao
    abstract fun historiqueDao(): HistoriqueDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Initial schema creation if needed
            }
        }

        // Migration from version 2 to 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout de nouvelles tables ou colonnes si nécessaire
            }
        }

        // Migration from version 3 to 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajout de nouvelles tables ou colonnes si nécessaire
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

        // Migration from version 6 to 7 - Ajout du champ avatar
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Ajouter la colonne avatar à la table Utilisateur
                database.execSQL("""
                    ALTER TABLE Utilisateur ADD COLUMN avatar TEXT
                """.trimIndent())
            }
        }

        // Migration alternative pour les cas où la base de données n'existe pas encore
        private val MIGRATION_1_7 = object : Migration(1, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Créer toutes les tables avec le schéma le plus récent
                createAllTables(database)
            }
        }

        private val MIGRATION_2_7 = object : Migration(2, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Appliquer toutes les migrations de 2 à 7
                MIGRATION_2_3.migrate(database)
                MIGRATION_3_4.migrate(database)
                MIGRATION_4_5.migrate(database)
                MIGRATION_5_6.migrate(database)
                MIGRATION_6_7.migrate(database)
            }
        }

        private val MIGRATION_3_7 = object : Migration(3, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Appliquer toutes les migrations de 3 à 7
                MIGRATION_3_4.migrate(database)
                MIGRATION_4_5.migrate(database)
                MIGRATION_5_6.migrate(database)
                MIGRATION_6_7.migrate(database)
            }
        }

        private val MIGRATION_4_7 = object : Migration(4, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Appliquer toutes les migrations de 4 à 7
                MIGRATION_4_5.migrate(database)
                MIGRATION_5_6.migrate(database)
                MIGRATION_6_7.migrate(database)
            }
        }

        private val MIGRATION_5_7 = object : Migration(5, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Appliquer toutes les migrations de 5 à 7
                MIGRATION_5_6.migrate(database)
                MIGRATION_6_7.migrate(database)
            }
        }

        private fun createAllTables(database: SupportSQLiteDatabase) {
            // Créer la table Utilisateur avec le champ avatar
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS Utilisateur (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nom TEXT NOT NULL,
                    prenom TEXT NOT NULL,
                    solde REAL NOT NULL DEFAULT 0.0,
                    email TEXT NOT NULL,
                    password TEXT NOT NULL,
                    date_creation INTEGER NOT NULL,
                    avatar TEXT
                )
            """.trimIndent())

            // Créer la table Transaction
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS `Transaction` (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    type TEXT NOT NULL,
                    montants TEXT NOT NULL,
                    date INTEGER NOT NULL,
                    motif TEXT,
                    description TEXT,
                    id_utilisateur INTEGER NOT NULL,
                    id_banque INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(id_utilisateur) REFERENCES Utilisateur(id) ON DELETE CASCADE
                )
            """.trimIndent())

            // Créer la table Projet
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS Projet (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nom TEXT NOT NULL,
                    montant_necessaire REAL NOT NULL,
                    montant_actuel REAL NOT NULL DEFAULT 0.0,
                    progression INTEGER NOT NULL DEFAULT 0,
                    date_limite INTEGER NOT NULL,
                    id_utilisateur INTEGER DEFAULT 1,
                    FOREIGN KEY(id_utilisateur) REFERENCES Utilisateur(id) ON DELETE CASCADE
                )
            """.trimIndent())

            // Créer la table Banque
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS Banque (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nom TEXT NOT NULL,
                    type TEXT NOT NULL,
                    numero_compte TEXT,
                    solde REAL NOT NULL DEFAULT 0.0
                )
            """.trimIndent())

            // Créer la table Emprunt
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS Emprunt (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nom_emprunte TEXT NOT NULL,
                    contacte TEXT NOT NULL,
                    montant REAL NOT NULL,
                    date_emprunt INTEGER NOT NULL,
                    date_remboursement INTEGER NOT NULL,
                    est_rembourse INTEGER NOT NULL DEFAULT 0
                )
            """.trimIndent())

            // Créer la table Acquittement
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS Acquittement (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    nom_acquittement TEXT NOT NULL,
                    montant REAL NOT NULL,
                    date_acquittement INTEGER NOT NULL,
                    description TEXT
                )
            """.trimIndent())

            // Créer la table Historique
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

        private val ALL_MIGRATIONS = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_1_7,
            MIGRATION_2_7,
            MIGRATION_3_7,
            MIGRATION_4_7,
            MIGRATION_5_7
        )

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "moneywise"
                )
                    .addMigrations(*ALL_MIGRATIONS)
                    .fallbackToDestructiveMigration() // En cas d'échec de migration
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Méthode utilitaire pour réinitialiser la base de données (utile en développement)
        fun resetDatabase(context: Context) {
            synchronized(this) {
                INSTANCE?.close()
                context.deleteDatabase("moneywise")
                INSTANCE = null
            }
        }
    }
}
