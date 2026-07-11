package com.iamsubho.drivesync.di

import android.content.Context
import androidx.room.Room
import com.iamsubho.drivesync.data.local.AppDatabase
import com.iamsubho.drivesync.data.local.dao.AccountDao
import com.iamsubho.drivesync.data.local.dao.SyncJobDao
import com.iamsubho.drivesync.data.local.dao.TransferLogDao
import com.iamsubho.drivesync.data.local.dao.TransferQueueDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "drivesync.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideSyncJobDao(db: AppDatabase): SyncJobDao = db.syncJobDao()

    @Provides
    fun provideTransferLogDao(db: AppDatabase): TransferLogDao = db.transferLogDao()

    @Provides
    fun provideTransferQueueDao(db: AppDatabase): TransferQueueDao = db.transferQueueDao()
}
