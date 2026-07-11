package com.iamsubho.drivesync.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.iamsubho.drivesync.data.local.entity.DriveAccountEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {
    @Query("SELECT * FROM drive_accounts ORDER BY addedAt ASC")
    fun observeAll(): Flow<List<DriveAccountEntity>>

    @Query("SELECT * FROM drive_accounts WHERE email = :email")
    suspend fun get(email: String): DriveAccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(account: DriveAccountEntity)

    @Query("DELETE FROM drive_accounts WHERE email = :email")
    suspend fun delete(email: String)

    @Query("UPDATE drive_accounts SET storageUsedBytes = :used, storageTotalBytes = :total WHERE email = :email")
    suspend fun updateStorage(email: String, used: Long, total: Long)
}
