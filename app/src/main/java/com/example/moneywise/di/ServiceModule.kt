package com.example.moneywise.di

import android.content.Context
import com.example.moneywise.services.BalanceUpdateService
import com.example.moneywise.ai.TransactionClassifier
import com.example.moneywise.ai.NLPExtractor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideBalanceUpdateService(): BalanceUpdateService {
        return BalanceUpdateService()
    }

    @Provides
    @Singleton
    fun provideTransactionClassifier(@ApplicationContext context: Context): TransactionClassifier {
        return TransactionClassifier.create(context)
    }

    @Provides
    @Singleton
    fun provideNLPExtractor(@ApplicationContext context: Context): NLPExtractor {
        return NLPExtractor.create(context)
    }
}
