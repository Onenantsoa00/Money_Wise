package com.example.moneywise.di

import android.content.Context
import com.example.moneywise.data.AppDatabase
import com.example.moneywise.utils.NotificationHelper
import com.example.moneywise.utils.ReminderManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Singleton
    @Provides
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    @Singleton
    @Provides
    fun provideNotificationHelper(@ApplicationContext context: Context): NotificationHelper {
        return NotificationHelper(context)
    }

    @Singleton
    @Provides
    fun provideReminderManager(
        @ApplicationContext context: Context,
        notificationHelper: NotificationHelper
    ): ReminderManager {
        return ReminderManager(context, notificationHelper)
    }
}