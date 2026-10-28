package com.iamsubho.drivesync.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.iamsubho.drivesync.data.local.AppDatabase
import com.iamsubho.drivesync.data.local.dao.AccountDao
import com.iamsubho.drivesync.data.local.dao.SavedDriveFolderDao
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

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE sync_jobs ADD COLUMN excludedSubfolders TEXT NOT NULL DEFAULT ''",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS SavedDriveFolderEntity (" +
                    "accountEmail TEXT NOT NULL, folderId TEXT NOT NULL, folderName TEXT NOT NULL, " +
                    "PRIMARY KEY(accountEmail, folderId))",
            )
            db.execSQL(
                "INSERT OR IGNORE INTO SavedDriveFolderEntity (accountEmail, folderId, folderName) " +
                    "SELECT accountEmail, driveFolderId, driveFolderName FROM sync_jobs",
            )
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "drivesync.db")
            .addMigrations(MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideAccountDao(db: AppDatabase): AccountDao = db.accountDao()

    @Provides
    fun provideSyncJobDao(db: AppDatabase): SyncJobDao = db.syncJobDao()

    @Provides
    fun provideSavedDriveFolderDao(db: AppDatabase): SavedDriveFolderDao = db.savedDriveFolderDao()

    @Provides
    fun provideTransferLogDao(db: AppDatabase): TransferLogDao = db.transferLogDao()

    @Provides
    fun provideTransferQueueDao(db: AppDatabase): TransferQueueDao = db.transferQueueDao()
}
