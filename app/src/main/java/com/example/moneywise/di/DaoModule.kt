package com.example.moneywise.di

import com.example.moneywise.data.AppDatabase
import com.example.moneywise.data.dao.BanqueDao
import com.example.moneywise.data.dao.TransactionDao
import com.example.moneywise.data.dao.UtilisateurDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DaoModule {

    @Provides
    @Singleton
    fun provideUserDao(database: AppDatabase): UtilisateurDao {
        return database.utilisateurDao()
    }

    @Provides
    @Singleton
    fun provideTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    @Singleton
    fun provideBanqueDao(database: AppDatabase): BanqueDao {
        return database.banqueDao()
    }
}