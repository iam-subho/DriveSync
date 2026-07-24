package com.iamsubho.drivesync.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.iamsubho.drivesync.data.local.dao.AccountDao
import com.iamsubho.drivesync.data.local.dao.SyncJobDao
import com.iamsubho.drivesync.data.local.dao.TransferLogDao
import com.iamsubho.drivesync.data.local.dao.TransferQueueDao
import com.iamsubho.drivesync.data.local.entity.DriveAccountEntity
import com.iamsubho.drivesync.data.local.entity.SyncJobEntity
import com.iamsubho.drivesync.data.local.entity.TransferLogEntity
import com.iamsubho.drivesync.data.local.entity.TransferQueueEntity

@Database(
    entities = [
        DriveAccountEntity::class,
        SyncJobEntity::class,
        TransferLogEntity::class,
        TransferQueueEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun syncJobDao(): SyncJobDao
    abstract fun transferLogDao(): TransferLogDao
    abstract fun transferQueueDao(): TransferQueueDao
}
