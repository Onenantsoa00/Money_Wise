package com.example.moneywise.di

import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.dao.*
import com.example.moneywise.data.repository.*
import com.example.moneywise.services.BalanceUpdateService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    // Providers pour les DAOs
    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun provideHistoriqueDao(database: AppDatabase): HistoriqueDao {
        return database.historiqueDao()
    }

    @Provides
    fun provideUtilisateurDao(database: AppDatabase): UtilisateurDao {
        return database.utilisateurDao()
    }

    @Provides
    fun provideBanqueDao(database: AppDatabase): BanqueDao {
        return database.banqueDao()
    }

    @Provides
    fun provideEmpruntDao(database: AppDatabase): EmpruntDao {
        return database.empruntDao()
    }

    @Provides
    fun provideAcquittementDao(database: AppDatabase): AcquittementDao {
        return database.acquittementDao()
    }

    @Provides
    fun provideProjetDao(database: AppDatabase): ProjetDao {
        return database.projetDao()
    }

    // Providers pour les Repositories
    @Provides
    @Singleton
    fun provideTransactionRepository(transactionDao: TransactionDao): TransactionRepository {
        return TransactionRepository(transactionDao)
    }

    @Provides
    @Singleton
    fun provideHistoriqueRepository(historiqueDao: HistoriqueDao): HistoriqueRepository {
        return HistoriqueRepository(historiqueDao)
    }

    @Provides
    @Singleton
    fun provideUtilisateurRepository(utilisateurDao: UtilisateurDao): UtilisateurRepository {
        return UtilisateurRepository(utilisateurDao)
    }

    // 🔥 NOUVEAU: Provider pour le service de mise à jour du solde
    @Provides
    @Singleton
    fun provideBalanceUpdateService(): BalanceUpdateService {
        return BalanceUpdateService()
    }
}
